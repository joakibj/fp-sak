package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import java.util.Properties;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class BekreftSvangerskapspengervilkårOppdatererTest {

    private BehandlingRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    @Test
    public void skal_sette_totrinn_ved_avslag() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_IKKE_GRAVID_KVINNE.getKode());

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_ha_totrinn_ved_innvilgelse() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse", null);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_avslå_vilkår() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_ER_IKKE_I_ARBEID.getKode());

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        VilkårResultat.Builder builder = VilkårResultat.builder();
        resultat.getVilkårResultatSomSkalLeggesTil()
            .forEach(v -> builder.leggTilVilkårResultat(v.getVilkårType(), v.getVilkårUtfallType(),
                v.getVilkårUtfallMerknad(), new Properties(), v.getAvslagsårsak(), true, false, null, null));

        assertThat(builder.buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(
            VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    public void skal_innvilge_vilkår() {
        BekreftSvangerskapspengervilkårOppdaterer oppdaterer = oppdaterer();
        BekreftSvangerskapspengervilkårDto dto = new BekreftSvangerskapspengervilkårDto("begrunnelse", null);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        AksjonspunktOppdaterParameter param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        OppdateringResultat resultat = oppdaterer.oppdater(dto, param);

        VilkårResultat.Builder builder = VilkårResultat.builder();
        resultat.getVilkårResultatSomSkalLeggesTil()
            .forEach(v -> builder.leggTilVilkårResultat(v.getVilkårType(), v.getVilkårUtfallType(),
                v.getVilkårUtfallMerknad(), new Properties(), v.getAvslagsårsak(), true, false, null, null));


        assertThat(builder.buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(
            VilkårUtfallType.OPPFYLT);
    }

    private BekreftSvangerskapspengervilkårOppdaterer oppdaterer() {
        return new BekreftSvangerskapspengervilkårOppdaterer(
            new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(),
                mock(DokumentArkivTjeneste.class)));
    }

}
