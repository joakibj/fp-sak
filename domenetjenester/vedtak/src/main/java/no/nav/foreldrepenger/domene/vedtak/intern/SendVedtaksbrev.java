package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
public class SendVedtaksbrev {

    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrev.class);

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private AnkeRepository ankeRepository;

    private BehandlingVedtakRepository behandlingVedtakRepository;

    SendVedtaksbrev() {
        // for CDI proxy
    }

    @Inject
    public SendVedtaksbrev(BehandlingRepository behandlingRepository,
                           BehandlingVedtakRepository behandlingVedtakRepository,
                           AnkeRepository ankeRepository,
                           DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                           DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                           KlageRepository klageRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.klageRepository = klageRepository;
        this.ankeRepository = ankeRepository;
    }

    void sendVedtaksbrev(Long behandlingId) {
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId);
        if (!behandlingVedtakOpt.isPresent()) {
            log.info("Det foreligger ikke vedtak i behandling: {}, kan ikke sende vedtaksbrev", behandlingId); //$NON-NLS-1$
            return;
        }
        BehandlingVedtak behandlingVedtak = behandlingVedtakOpt.get();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        boolean fritekstVedtaksbrev = Vedtaksbrev.FRITEKST.equals(behandlingVedtak.getBehandlingsresultat().getVedtaksbrev());
        if (Fagsystem.INFOTRYGD.equals(behandling.getMigrertKilde()) && !fritekstVedtaksbrev) {
            log.info("Sender ikke vedtaksbrev for sak som er migrert fra Infotrygd. Gjelder behandlingId {}", behandling.getId());
            return;
        }

        if (BehandlingType.KLAGE.equals(behandling.getType()) && !skalSendeVedtaksbrevIKlagebehandling(behandling)) {
            log.info("Sender ikke vedtaksbrev fra klagebehandlingen i behandlingen etter, eller når KlageVurderingResultat = null. For behandlingId {}", behandlingId); //$NON-NLS-1$
            return;
        }

        if (BehandlingType.ANKE.equals(behandling.getType()) && !skalSendeVedtaksbrevEtterAnke(behandling)) {
            log.info("Sender ikke vedtaksbrev for vedtak fra omgjøring fra klageinstansen på behandling {}, gjelder omgjør fra klageinstans", behandlingId); //$NON-NLS-1$
            return;
        }

        if (erBehandlingEtterKlage(behandling) && !skalSendeVedtaksbrevEtterKlage(behandling)) {
            log.info("Sender ikke vedtaksbrev for vedtak fra omgjøring fra klageinstansen på behandling {}, gjelder medhold fra klageinstans", behandlingId); //$NON-NLS-1$
            return;
        }

        if (behandlingVedtak.isBeslutningsvedtak()) {
            if (harSendtVarselOmRevurdering(behandlingId)) {
                log.info("Sender informasjonsbrev om uendret utfall i behandling: {}", behandlingId); //$NON-NLS-1$
            } else {
                log.info("Uendret utfall av revurdering og har ikke sendt varsel om revurdering. Sender ikke brev for behandling: {}", behandlingId); //$NON-NLS-1$
                return;
            }
        } else if (gjelderEngangsstønad(behandling)) {
            log.info("Sender vedtaksbrev({}) for engangsstønad i behandling: {}", behandlingVedtak.getVedtakResultatType().getKode(), behandlingId); //$NON-NLS-1$
        } else {
            log.info("Sender vedtaksbrev({}) for foreldrepenger i behandling: {}", behandlingVedtak.getVedtakResultatType().getKode(), behandlingId); //$NON-NLS-1
        }
        dokumentBestillerApplikasjonTjeneste.produserVedtaksbrev(behandlingVedtak);
    }

    private boolean gjelderEngangsstønad(Behandling behandling) {
        return behandling.getFagsak().getYtelseType().gjelderEngangsstønad();
    }

    private boolean skalSendeVedtaksbrevIKlagebehandling(Behandling behandling) {
        if (behandling.erRevurdering() && erBehandlingEtterKlage(behandling)) {
            return false;
        }
        Optional<KlageVurderingResultat> vurderingOpt = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        return vurderingOpt.isPresent();
    }

    private boolean skalSendeVedtaksbrevEtterAnke(Behandling behandling) {
        Behandling anke = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.ANKE).orElse(null);
        if (anke == null) {
            return true;
        }
        AnkeVurderingResultatEntitet vurdering = ankeRepository.hentAnkeVurderingResultat(anke.getId()).orElse(null);
        // henlagt eller ikke avsluttet
        if (vurdering == null) {
            return false;
        }

        AnkeVurdering ankeVurdering = vurdering.getAnkeVurdering();
        //utvider med flere ankebrev etterhvert som de kommer på plass
        return AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE.equals(ankeVurdering) || AnkeVurdering.ANKE_OMGJOER.equals(ankeVurdering);
    }

    private boolean skalSendeVedtaksbrevEtterKlage(Behandling behandling) {

        Behandling klage = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.KLAGE).orElse(null);

        if (klage == null) {
            return true;
        }
        KlageVurderingResultat vurdering = klageRepository.hentGjeldendeKlageVurderingResultat(klage).orElse(null);
        // henlagt eller ikke avsluttet
        return vurdering == null;
    }

    private boolean erBehandlingEtterKlage(Behandling behandling) {
        return BehandlingÅrsakType.årsakerEtterKlageBehandling().stream().anyMatch(behandling::harBehandlingÅrsak);
    }

    private Boolean harSendtVarselOmRevurdering(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.REVURDERING_DOK);
    }
}
