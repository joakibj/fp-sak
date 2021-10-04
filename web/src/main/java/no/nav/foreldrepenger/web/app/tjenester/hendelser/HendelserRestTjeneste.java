package no.nav.foreldrepenger.web.app.tjenester.hendelser;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.AktørIdDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.v2.HendelseWrapperDto;
import no.nav.foreldrepenger.mottak.hendelser.KlargjørHendelseTask;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;

@Path("/hendelser")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class HendelserRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(HendelserRestTjeneste.class);

    private HendelseSorteringRepository sorteringRepository;
    private HendelsemottakRepository hendelsemottakRepository;
    private ProsessTaskTjeneste taskTjeneste;

    public HendelserRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public HendelserRestTjeneste(HendelseSorteringRepository sorteringRepository,
            HendelsemottakRepository hendelsemottakRepository,
                                 ProsessTaskTjeneste taskTjeneste) {
        this.sorteringRepository = sorteringRepository;
        this.hendelsemottakRepository = hendelsemottakRepository;
        this.taskTjeneste = taskTjeneste;
    }

    @POST
    @Path("/ping")
    @Operation(description = "Ping", tags = "hendelser")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public EnkelRespons ping() {
        return new EnkelRespons("pong");
    }

    @POST
    @Path("/motta")
    @Operation(description = "Mottak av hendelser", tags = "hendelser")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public EnkelRespons mottaHendelse(@Parameter(description = "Hendelse fra Fpabonnent") @Valid AbacHendelseWrapperDto wrapperDto) {
        var hendelseDto = wrapperDto.getHendelse();
        var beskrivelse = String.format("Hendelse mottatt fra %s av typen %s med hendelseId: %s.",
                hendelseDto.getAvsenderSystem(), hendelseDto.getHendelsetype(), hendelseDto.getId());
        LOG.info(beskrivelse);
        if (!hendelsemottakRepository.hendelseErNy(hendelseDto.getId())) {
            return new EnkelRespons("Hendelse ble ignorert. Hendelse med samme ID er allerede registrert.");
        }
        return registrerHendelse(hendelseDto, beskrivelse);
    }

    @POST
    @Operation(description = "Grovsortering av aktørID-er. Returnerer aktørID-er i listen som har en sak.", tags = "hendelser")
    @Path("/grovsorter")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public List<String> grovSorter(@Parameter(description = "Liste med aktør IDer som skal sorteres") @Valid List<AbacAktørIdDto> aktoerIdListe) {
        var aktørIdList = aktoerIdListe.stream().map(AbacAktørIdDto::getAktørId).map(AktørId::new).collect(Collectors.toList()); // NOSONAR
        return sorteringRepository.hentEksisterendeAktørIderMedSak(aktørIdList).stream().map(AktørId::getId).collect(Collectors.toList());
    }

    private EnkelRespons registrerHendelse(HendelseDto hendelse, String beskrivelse) {
        var hendelsType = ForretningshendelseType.fraKode(hendelse.getHendelsetype());
        if (ForretningshendelseType.UDEFINERT.equals(hendelsType)) {
            LOG.warn("Kan ikke håndtere {}", beskrivelse);
            return new EnkelRespons("Ukjent hendelse");
        }

        hendelsemottakRepository.registrerMottattHendelse(hendelse.getId());
        var taskData = ProsessTaskData.forProsessTask(KlargjørHendelseTask.class);
        taskData.setPayload(StandardJsonConfig.toJson(hendelse));
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE, hendelse.getHendelsetype());
        taskData.setProperty(KlargjørHendelseTask.PROPERTY_UID, hendelse.getId());
        taskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(taskData);
        return new EnkelRespons("OK");
    }

    public static class AbacAktørIdDto extends AktørIdDto implements AbacDto {
        public AbacAktørIdDto() {
        }

        public AbacAktørIdDto(String aktørId) {
            super(aktørId);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, this.getAktørId());
        }
    }

    public static class AbacHendelseWrapperDto extends HendelseWrapperDto implements AbacDto {
        public AbacHendelseWrapperDto() {
        }

        public AbacHendelseWrapperDto(HendelseDto hendelse) {
            super(hendelse);
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, new HashSet<>(this.getAlleAktørId()));
        }
    }
}
