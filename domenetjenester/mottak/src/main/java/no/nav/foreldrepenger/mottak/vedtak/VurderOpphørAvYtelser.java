package no.nav.foreldrepenger.mottak.vedtak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.SjekkOverlappForeldrepengerInfotrygdTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.konfig.Tid;

/**
Funksjonen sjekker om det finnes løpende saker for den personen det innvilges foreldrepenger på.
Om det finnes løpende saker sjekkes det om det er ny sak overlapper med løpende sak. Det sjekkes både for mor, far og en eventuell medforelder.
Dersom det er overlapp opprettes en "vurder konsekvens for ytelse"-oppgave i Gosys, og en revurdering med egen årsak slik at saksbehandler kan
vurdere om opphør skal gjennomføres eller ikke. Saksbehandling må skje manuelt, og fritekstbrev må benyttes for opphør av løpende sak.
 */
@ApplicationScoped
public class VurderOpphørAvYtelser  {
    private FagsakRepository fagsakRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private RevurderingTjeneste revurderingTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private static final Logger log = LoggerFactory.getLogger(VurderOpphørAvYtelser.class);
    private SjekkOverlappForeldrepengerInfotrygdTjeneste sjekkOverlappInfortrygd;

    public VurderOpphørAvYtelser() {
        //NoSonar
    }

    @Inject
    public VurderOpphørAvYtelser(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                 @FagsakYtelseTypeRef("FP") RevurderingTjeneste revurderingTjeneste,
                                 ProsessTaskRepository prosessTaskRepository,
                                 BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                 BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                 SjekkOverlappForeldrepengerInfotrygdTjeneste sjekkOverlappInfortrygd
                                 ) {
        this.fagsakRepository = behandlingRepositoryProvider.getFagsakRepository();
        this.personopplysningRepository = behandlingRepositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
        this.sjekkOverlappInfortrygd = sjekkOverlappInfortrygd;
    }

    void vurderOpphørAvYtelser(Long fagsakId, Long behandlingId) {

        Fagsak gjeldendeFagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        if (!FagsakYtelseType.FORELDREPENGER.equals(gjeldendeFagsak.getYtelseType())) {
            return;
        }
        // Finner første fradato i vedtatt uttaksperiode for iverksatt behandling
        LocalDate startDatoIVB = finnMinDato(behandlingId);

        List<AktørId> aktørIdList = new ArrayList<>();

        aktørIdList.add(gjeldendeFagsak.getAktørId());

        List<AktørId> aktørIdListSjekkInfotrygd = new ArrayList<>();
        if(RelasjonsRolleType.erFar(gjeldendeFagsak.getRelasjonsRolleType())) {
            aktørIdListSjekkInfotrygd.add(gjeldendeFagsak.getAktørId());
        }

        if (RelasjonsRolleType.erMor(gjeldendeFagsak.getRelasjonsRolleType())) {
            Optional<AktørId> annenPartAktørId = hentAnnenPartAktørId(behandlingId);
            if( annenPartAktørId.isPresent()) {
                aktørIdList.add(annenPartAktørId.get());
                aktørIdListSjekkInfotrygd.add(annenPartAktørId.get());
            }
        }
        //Sjekker om det finnes overlapp på far og medforelder i Infotrygd
        aktørIdListSjekkInfotrygd.forEach(aktørId -> {
            Boolean overlappInfotrygd = sjekkOverlappInfortrygd.harForeldrepengerInfotrygdSomOverlapper(aktørId, startDatoIVB) ;
            if(overlappInfotrygd) {
                log.info("Overlapp i Infotrygd på aktør {} for vedtatt sak {}", aktørId, gjeldendeFagsak.getSaksnummer());
            }
        });
        //Sjekker om det finnes overlapp i fpsak
        aktørIdList.forEach(aktørId -> {
            List<Fagsak> sakerSomSkalOpphøre = løpendeSakerSomOverlapperUttakPåNySak(aktørId, gjeldendeFagsak.getSaksnummer(), startDatoIVB);
            if (!sakerSomSkalOpphøre.isEmpty()) {
                for (Fagsak sakOpphør : sakerSomSkalOpphøre) {
                    //For hver sak skal det opprettes en "vurder konsekvens for ytelse" oppgave, og en revurdering med egen årsak. Finnes det en åpen revurdering skal den oppdateres med årsak.
                    håndtereOpphør(sakOpphør);
                }
            }
        });
    }

    private void håndtereOpphør(Fagsak sakOpphør) {
        Optional<Behandling> sisteBehandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(sakOpphør.getId());
        if (sisteBehandling.isPresent() && !sisteBehandling.get().harBehandlingÅrsak(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN)) {

            opprettTaskForÅVurdereKonsekvens(sakOpphør.getId(), sisteBehandling.get().getBehandlendeEnhet());

            if (sisteBehandling.get().erAvsluttet()) {
                Behandling revurderingOpphør = opprettRevurdering(sakOpphør, BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN);
                if (revurderingOpphør != null) {
                    log.info("Vurder opphør av ytelse har opprettet revurdering med behandlingId {} på sak med saksnummer {} pga behandlingId {}", revurderingOpphør.getId(), sakOpphør.getSaksnummer(), sisteBehandling.get().getId());
                } else {
                    log.info("Vurder opphør av ytelse kunne ikke opprette revurdering på sak med saksnummer {} pga behandlingId {}", sakOpphør.getSaksnummer(), sisteBehandling.get().getId());
                }
            } else {
                oppdatereBehMedÅrsak(sisteBehandling.get());
            }
        }
    }

    private List<Fagsak> løpendeSakerSomOverlapperUttakPåNySak(AktørId aktørId, Saksnummer saksnummer, LocalDate startDato) {
        return fagsakRepository.hentForBruker(aktørId).stream()
            .filter(Fagsak::erÅpen)
            .filter(f -> FagsakYtelseType.FORELDREPENGER.equals(f.getYtelseType()))
            .filter(f -> !saksnummer.equals(f.getSaksnummer()))
            .filter(f -> erMaxDatoPåLøpendeSakEtterStartDatoNysak(f.getId(), startDato))
            .collect(Collectors.toList());
    }

    private boolean erMaxDatoPåLøpendeSakEtterStartDatoNysak(long fagsakId, LocalDate startDato) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        behandling.ifPresent(behandling1 -> {
            var maxDato= finnMaxDato(behandling1);
            if(!maxDato.isBefore(startDato)) {
                log.info("Oppdaget overlapp. Mulig Avslått periode for fagsak {} med maxDato {} ", fagsakId, maxDato );
            }
        });
        return behandling.
            map(behandling1 -> !finnMaxDatoUtenAvslåtte(behandling1).isBefore(startDato)).orElse(Boolean.FALSE);
    }

    private LocalDate finnMinDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        Optional<LocalDate> minFom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
        return minFom.orElse(Tid.TIDENES_ENDE);
    }

    private LocalDate finnMaxDato(Behandling behandling) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        Optional<LocalDate> maxTom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder());
        return maxTom.orElse(Tid.TIDENES_BEGYNNELSE);
    }

    private LocalDate finnMaxDatoUtenAvslåtte(Behandling behandling) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandling.getId());
        Optional<LocalDate> maxTom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder());
        return maxTom.orElse(Tid.TIDENES_BEGYNNELSE);
    }

    private Optional<AktørId> hentAnnenPartAktørId(long behId) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart)
            .map(OppgittAnnenPartEntitet::getAktørId);
    }

    private Behandling opprettRevurdering(Fagsak sakRevurdering, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling revurdering;
        Optional<OrganisasjonsEnhet> enhet = behandlendeEnhetTjeneste.sjekkEnhetVedNyAvledetBehandling(sakRevurdering);

        revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(sakRevurdering, behandlingÅrsakType, enhet);

        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);

        return revurdering;
    }

    private void opprettTaskForÅVurdereKonsekvens(Long fagsakId, String behandlendeEnhetsId) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderKonsekvensTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, "Nytt barn: Vurder om ytelse skal opphøre");
        prosessTaskData.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        prosessTaskData.setFagsakId(fagsakId);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void oppdatereBehMedÅrsak(Behandling behandling) {
        BehandlingÅrsak.builder(BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN).buildFor(behandling);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }
}
