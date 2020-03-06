package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaSteg;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.RyddRegisterData;
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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.inngangsvilkaar.impl.EngangsstønadVilkårUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.impl.UtledeteVilkår;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOFAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("ES")
@ApplicationScoped
class KontrollerFaktaStegImpl implements KontrollerFaktaSteg {

    private KontrollerFaktaTjeneste tjeneste;

    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private FamilieHendelseRepository familieGrunnlagRepository;

    KontrollerFaktaStegImpl() {
        // for CDI proxy
    }

    @Inject
    public KontrollerFaktaStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                   @FagsakYtelseTypeRef("ES") KontrollerFaktaTjeneste tjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.tjeneste = tjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspunktResultater = tjeneste.utledAksjonspunkter(ref);
        utledVilkår(kontekst);
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT); // Settes til første steg i Inngangsvilkår.
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

    private void utledVilkår(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        final Optional<FamilieHendelseType> hendelseType = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType);
        UtledeteVilkår utledeteVilkår = new EngangsstønadVilkårUtleder().utledVilkår(behandling, hendelseType);
        opprettVilkår(utledeteVilkår, behandling, kontekst.getSkriveLås());
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.KONTROLLER_FAKTA.equals(fraSteg)
            || (BehandlingStegType.KONTROLLER_FAKTA.equals(fraSteg) && BehandlingStegType.KONTROLLER_FAKTA.equals(tilSteg))) {
            RyddRegisterData rydder = new RyddRegisterData(repositoryProvider, kontekst);
            rydder.ryddRegisterdataLegacyEngangsstønad();
        }
    }

    private void opprettVilkår(UtledeteVilkår utledeteVilkår, Behandling behandling, BehandlingLås skriveLås) {
        // Opprett Vilkårsresultat med vilkårne som som skal vurderes, og sett dem som ikke vurdert
        Behandlingsresultat behandlingsresultat = behandling.getBehandlingsresultat();
        VilkårResultat.Builder vilkårBuilder = behandlingsresultat != null
            ? VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat())
            : VilkårResultat.builder();
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkårType -> vilkårBuilder.leggTilVilkår(vilkårType, VilkårUtfallType.IKKE_VURDERT));
        VilkårResultat vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, skriveLås);
    }

}
