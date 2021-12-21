package no.nav.foreldrepenger.domene.risikoklassifisering;

import java.io.IOException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.risikoklassifisering.task.RisikoklassifiseringUtførTask;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.AktoerIdDto;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.AnnenPart;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.Opplysningsperiode;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.RequestWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.kafka.RisikovurderingRequest;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class Risikoklassifisering {

    private static final Logger LOG = LoggerFactory.getLogger(Risikoklassifisering.class);
    private ProsessTaskTjeneste taskTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private RisikovurderingTjeneste risikovurderingTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    Risikoklassifisering() {
        // CDI proxy
    }

    @Inject
    public Risikoklassifisering(ProsessTaskTjeneste taskTjeneste,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                RisikovurderingTjeneste risikovurderingTjeneste,
                                OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                PersonopplysningRepository personopplysningRepository,
                                FamilieHendelseRepository familieHendelseRepository) {
        this.taskTjeneste = taskTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.risikovurderingTjeneste = risikovurderingTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.personopplysningRepository = personopplysningRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public void opprettProsesstaskForRisikovurdering(BehandlingReferanse ref) {
        try {
            var task = opprettPotensiellTaskProsesstask(ref);
            task.ifPresent(t -> taskTjeneste.lagre(t));
        } catch (Exception ex) {
            LOG.warn("Publisering av Risikovurderingstask feilet", ex);
        }
    }

    Optional<ProsessTaskData> opprettPotensiellTaskProsesstask(BehandlingReferanse ref) throws IOException {
        var behandlingId = ref.getBehandlingId();
        if (risikovurderingTjeneste.behandlingHarBlittRisikoklassifisert(ref)) {
            LOG.info("behandling = {} Har Blitt Risikoklassifisert", behandlingId);
            return Optional.empty();
        }
        var risikovurderingRequest = opprettRequest(ref, behandlingId);
        return Optional.of(opprettTaskForRequest(ref, behandlingId, risikovurderingRequest));
    }

    private String hentBehandlingTema(BehandlingReferanse ref) {
        var grunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(
            ref.getBehandlingId());
        var behandlingTema = BehandlingTema.fraFagsak(ref.getFagsakYtelseType(),
            grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElseThrow());
        return behandlingTema.getOffisiellKode();
    }

    private ProsessTaskData opprettTaskForRequest(BehandlingReferanse ref,
                                                  Long behandlingId,
                                                  RisikovurderingRequest risikovurderingRequest) throws IOException {
        var callId = MDCOperations.getCallId();
        if (callId == null || callId.isBlank()) callId = MDCOperations.generateCallId();
        var taskData = ProsessTaskData.forProsessTask(RisikoklassifiseringUtførTask.class);
        taskData.setBehandling(ref.getFagsakId(), behandlingId, ref.getAktørId().getId());
        taskData.setCallId(callId);
        var requestWrapper = new RequestWrapper(callId, risikovurderingRequest);
        taskData.setProperty(RisikoklassifiseringUtførTask.KONSUMENT_ID,
            risikovurderingRequest.getKonsumentId().toString());
        taskData.setPayload(getJson(requestWrapper));
        return taskData;
    }

    private RisikovurderingRequest opprettRequest(BehandlingReferanse ref, Long behandlingId) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId)
            .getUtledetSkjæringstidspunkt();
        var interval = opplysningsPeriodeTjeneste.beregn(behandlingId, ref.getFagsakYtelseType());
        return RisikovurderingRequest.builder()
            .medSoekerAktoerId(new AktoerIdDto(ref.getAktørId().getId()))
            .medBehandlingstema(hentBehandlingTema(ref))
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medOpplysningsperiode(leggTilOpplysningsperiode(interval))
            .medAnnenPart(leggTilAnnenPart(ref))
            .medKonsumentId(ref.getBehandlingUuid())
            .build();
    }

    private AnnenPart leggTilAnnenPart(BehandlingReferanse ref) {
        var oppgittAnnenPart = personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(ref.getBehandlingId());
        if (oppgittAnnenPart.isPresent()) {
            var aktoerId =
                oppgittAnnenPart.get().getAktørId() == null ? null : oppgittAnnenPart.get().getAktørId().getId();
            if (aktoerId != null) {
                return new AnnenPart(new AktoerIdDto(aktoerId));
            }
            var utenlandskFnr = oppgittAnnenPart.get().getUtenlandskPersonident();
            if (utenlandskFnr != null) {
                return new AnnenPart(utenlandskFnr);
            }
        }
        return null;
    }

    private Opplysningsperiode leggTilOpplysningsperiode(SimpleLocalDateInterval interval) {
        return new Opplysningsperiode(interval.getFomDato(), interval.getTomDato());
    }

    private String getJson(RequestWrapper risikovurderingRequest) throws IOException {
        return StandardJsonConfig.toJson(risikovurderingRequest);
    }
}
