package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.UttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttaksstegTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettUttakManueltAksjonspunktUtleder;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderRevurderingUtil;
import no.nav.foreldrepenger.domene.uttak.fastsetteperioder.FastsettePerioderTjeneste;

@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class UttakStegImpl implements UttakSteg {

    private static final Logger LOG = LoggerFactory.getLogger(UttakStegImpl.class);

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FastsettePerioderTjeneste fastsettePerioderTjeneste;
    private FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FpUttakRepository fpUttakRepository;
    private UttakInputTjeneste uttakInputTjeneste;
    private UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public UttakStegImpl(BehandlingRepositoryProvider repositoryProvider,
                         FastsettePerioderTjeneste fastsettePerioderTjeneste,
                         FastsettUttakManueltAksjonspunktUtleder fastsettUttakManueltAksjonspunktUtleder,
                         UttakInputTjeneste uttakInputTjeneste,
                         UttakStegBeregnStønadskontoTjeneste beregnStønadskontoTjeneste,
                         BehandlingRepository behandlingRepository) {
        this.fastsettUttakManueltAksjonspunktUtleder = fastsettUttakManueltAksjonspunktUtleder;
        this.fastsettePerioderTjeneste = fastsettePerioderTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.fpUttakRepository = repositoryProvider.getFpUttakRepository();
        this.beregnStønadskontoTjeneste = beregnStønadskontoTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        var input = uttakInputTjeneste.lagInput(behandlingId);

        beregnStønadskontoTjeneste.beregnStønadskontoer(input);

        fastsettePerioderTjeneste.fastsettePerioder(input);

        List<AksjonspunktResultat> aksjonspunkter = fastsettUttakManueltAksjonspunktUtleder.utledAksjonspunkterFor(input);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg,
                                   BehandlingStegType sisteSteg) {
        if (!Objects.equals(BehandlingStegType.VURDER_UTTAK, førsteSteg)) {
            ryddUttak(kontekst.getBehandlingId());
            ryddStønadskontoberegning(kontekst.getBehandlingId(), kontekst.getFagsakId());
        }
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst,
                                    BehandlingStegModell modell,
                                    BehandlingStegType førsteSteg,
                                    BehandlingStegType sisteSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var behandlingsårsaker = behandlingsårsaker(behandling);
        if (SkalKopiereUttaksstegTjeneste.skalKopiereStegResultat(behandlingsårsaker)) {
            var originalBehandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getOriginalBehandlingId().orElseThrow();
            var uttak = fpUttakRepository.hentUttakResultatHvisEksisterer(originalBehandling);
            if (uttak.isPresent()) {
                kopierUttak(kontekst.getBehandlingId(), uttak.get());
            }
        } else {
            ryddUttak(kontekst.getBehandlingId());
            ryddStønadskontoberegning(kontekst.getBehandlingId(), kontekst.getFagsakId());
        }
    }

    private List<BehandlingÅrsakType> behandlingsårsaker(Behandling behandling) {
        return behandling.getBehandlingÅrsaker()
            .stream()
            .map(behandlingÅrsak -> behandlingÅrsak.getBehandlingÅrsakType())
            .collect(Collectors.toList());
    }

    private void kopierUttak(Long behandlingId, UttakResultatEntitet uttak) {
        LOG.info("Kopierer uttaksresultat id {}, til behandling {}", uttak.getId(), behandlingId);
        var kopiertOpprinneligPerioder = FastsettePerioderRevurderingUtil.kopier(uttak.getOpprinneligPerioder());
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, kopiertOpprinneligPerioder);
        if (uttak.getOverstyrtPerioder() != null) {
            var kopiertOverstyrtPerioder = FastsettePerioderRevurderingUtil.kopier(uttak.getOverstyrtPerioder());
            fpUttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingId, kopiertOverstyrtPerioder);
        }
    }

    private void ryddUttak(Long behandlingId) {
        fpUttakRepository.deaktivterAktivtResultat(behandlingId);
    }

    private void ryddStønadskontoberegning(Long behandlingId, Long fagsakId) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        if (behandlingsresultat.isEndretStønadskonto()) {
            var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
            fagsakRelasjonRepository.nullstillOverstyrtStønadskontoberegning(fagsak);
            var nyttBehandlingsresultat = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medEndretStønadskonto(false).build();
            behandlingsresultatRepository.lagre(behandlingId, nyttBehandlingsresultat);
        }
    }
}
