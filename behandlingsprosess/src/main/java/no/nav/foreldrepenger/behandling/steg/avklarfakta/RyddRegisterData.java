package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

public class RyddRegisterData {
    private final BehandlingRepository behandlingRepository;
    private final BehandlingskontrollKontekst kontekst;
    private MedlemskapRepository medlemskapRepository;

    public RyddRegisterData(BehandlingRepositoryProvider repositoryProvider, BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.kontekst = kontekst;
    }

    /**
     * @deprecated Erstatt med {@link #ryddRegisterdata()}
     */
    @Deprecated
    public void ryddRegisterdataLegacyEngangsstønad() {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        nullstillRegisterdata(behandling);
        behandlingRepository.slettTidligereBeregningerES(behandling, kontekst.getSkriveLås());
    }

    public void ryddRegisterdata() {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        nullstillRegisterdata(behandling);
    }

    private void nullstillRegisterdata(Behandling behandling) {

        // Sletter avklarte data, men ikke Fødsel/Adopsjon/Omsorg, da dette må ivaretas
        // hvis registerdata re-innhentes
        medlemskapRepository.slettAvklarteMedlemskapsdata(behandling.getId(), kontekst.getSkriveLås());
        behandling.nullstillToTrinnsBehandling();
    }
}
