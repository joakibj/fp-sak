package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType.IKKE_FASTSATT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKARET_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_MOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OMSORGSVILKÅRET;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

class RyddVilkårTyper {

    private BehandlingRepository behandlingRepository;
    private final Behandling behandling;
    private final BehandlingskontrollKontekst kontekst;

    static Map<VilkårType, Consumer<RyddVilkårTyper>> OPPRYDDER_FOR_AVKLARTE_DATA = new HashMap<>();
    private FamilieHendelseRepository familieGrunnlagRepository;
    private MedlemskapRepository medlemskapRepository;

    static {
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FØDSELSVILKÅRET_MOR,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FØDSELSVILKÅRET_FAR_MEDMOR,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(ADOPSJONSVILKÅRET_ENGANGSSTØNAD,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(ADOPSJONSVILKARET_FORELDREPENGER,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(OMSORGSVILKÅRET,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FORELDREANSVARSVILKÅRET_2_LEDD,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FORELDREANSVARSVILKÅRET_4_LEDD,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(MEDLEMSKAPSVILKÅRET,
                r -> r.medlemskapRepository.slettAvklarteMedlemskapsdata(r.behandling.getId(), r.kontekst.getSkriveLås()));
    }

    RyddVilkårTyper(BehandlingRepositoryProvider repositoryProvider, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.behandling = behandling;
        this.kontekst = kontekst;
    }

    void ryddVedOverhoppFramover(List<VilkårType> vilkårTyper) {
        slettAvklarteFakta(vilkårTyper);
        nullstillVilkår(vilkårTyper);
    }

    void ryddVedTilbakeføring(List<VilkårType> vilkårTyper) {
        nullstillInngangsvilkår();
        nullstillVilkår(vilkårTyper);
        nullstillVedtaksresultat();
    }

    private void nullstillVedtaksresultat() {
        var behandlingsresultat = getBehandlingsresultat(behandling);
        if ((behandlingsresultat == null) ||
                Objects.equals(behandlingsresultat.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }

        Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling))
                .medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private void slettAvklarteFakta(List<VilkårType> vilkårTyper) {
        vilkårTyper.forEach(vilkårType -> {
            var ryddVilkårConsumer = OPPRYDDER_FOR_AVKLARTE_DATA.get(vilkårType);
            if (ryddVilkårConsumer != null) {
                ryddVilkårConsumer.accept(this);
            }
        });
    }

    private void nullstillInngangsvilkår() {
        Optional.ofNullable(getBehandlingsresultat(behandling))
            .map(Behandlingsresultat::getVilkårResultat)
            .filter(inng -> !inng.erOverstyrt() && !IKKE_FASTSATT.equals(inng.getVilkårResultatType()))
            .ifPresent(iv -> VilkårResultat.builderFraEksisterende(iv)
                .medVilkårResultatType(IKKE_FASTSATT)
                .buildFor(behandling));
    }

    private void nullstillVilkår(List<VilkårType> vilkårTyper) {
        Optional.ofNullable(getBehandlingsresultat(behandling))
            .map(Behandlingsresultat::getVilkårResultat)
            .ifPresent(vilkårResultat -> {
                var vilkårSomSkalNullstilles = vilkårResultat.getVilkårene().stream()
                    .filter(v -> vilkårTyper.contains(v.getVilkårType()))
                    .filter(v -> !v.erOverstyrt())
                    .collect(toList());
                if (!vilkårSomSkalNullstilles.isEmpty()) {
                    var builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
                    vilkårSomSkalNullstilles.forEach(vilkår -> builder.nullstillVilkår(vilkår.getVilkårType()));
                    builder.buildFor(behandling);
                }
            });
    }

}
