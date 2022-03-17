package no.nav.foreldrepenger.behandling.kabal;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "kabal.mottafrakabal", maxFailedRuns = 1)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class MottaFraKabalTask extends BehandlingProsessTask {

    public static final String HENDELSETYPE_KEY = "hendelse";
    public static final String UTFALL_KEY = "utfall";
    public static final String JOURNALPOST_KEY = "journalpostId";

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private KabalTjeneste kabalTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    MottaFraKabalTask() {
        // for CDI proxy
    }

    @Inject
    public MottaFraKabalTask(BehandlingRepositoryProvider repositoryProvider,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                             BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                             DokumentArkivTjeneste dokumentArkivTjeneste,
                             KabalTjeneste kabalTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kabalTjeneste = kabalTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {

        var hendelsetype = Optional.ofNullable(prosessTaskData.getPropertyValue(HENDELSETYPE_KEY))
            .map(KabalHendelse.BehandlingEventType::valueOf).orElse(null);
        if (!KabalHendelse.BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET.equals(hendelsetype)) {
            throw new IllegalStateException("Utviklerfeil: Mottatt ikke-støtte kabalisme");
        }
        var utfall = Optional.ofNullable(prosessTaskData.getPropertyValue(UTFALL_KEY))
            .map(KabalUtfall::valueOf).orElse(null);
        if (utfall == null) {
            throw new IllegalStateException("Utviklerfeil: Kabal-klage avsluttet men mangler utfall");
        }
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var journalpost = Optional.ofNullable(prosessTaskData.getPropertyValue(JOURNALPOST_KEY))
            .map(JournalpostId::new)
            .flatMap(j -> dokumentArkivTjeneste.hentUtgåendeJournalpostForSak(j));
        if (KabalUtfall.TRUKKET.equals(utfall)) {
            if (behandling.isBehandlingPåVent()) {
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtførtForHenleggelse(behandling, kontekst);
            }
            behandlingskontrollTjeneste.henleggBehandling(kontekst, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
            kabalTjeneste.lagHistorikkinnslagForHenleggelse(behandlingId, BehandlingResultatType.HENLAGT_KLAGE_TRUKKET);
        } else {
            if (behandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            }
            if (KabalUtfall.RETUR.equals(utfall)) {
                kabalTjeneste.fjerneKabalFlagg(behandling);
                behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.KLAGE_NFP);
                endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(behandling);
            } else {
                kabalTjeneste.lagreKlageUtfallFraKabal(behandling, utfall);
            }
            behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
        }
        journalpost.ifPresent(j -> kabalTjeneste.lagHistorikkinnslagForBrevSendt(behandling, j));
    }

    private void endreAnsvarligEnhetTilNFPVedTilbakeføringOgLagreHistorikkinnslag(Behandling behandling) {
        if ((behandling.getBehandlendeEnhet() != null)
            && !BehandlendeEnhetTjeneste.getKlageInstans().enhetId().equals(behandling.getBehandlendeEnhet())) {
            return;
        }
        var tilEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, tilEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
    }
}