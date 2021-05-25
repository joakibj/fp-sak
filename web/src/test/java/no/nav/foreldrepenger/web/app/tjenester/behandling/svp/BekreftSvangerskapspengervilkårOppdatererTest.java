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
        var oppdaterer = oppdaterer();
        var dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_IKKE_GRAVID_KVINNE.getKode());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_ha_totrinn_ved_innvilgelse() {
        var oppdaterer = oppdaterer();
        var dto = new BekreftSvangerskapspengervilkårDto("begrunnelse", null);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        var resultat = oppdaterer.oppdater(dto, param);

        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void skal_avslå_vilkår() {
        var oppdaterer = oppdaterer();
        var dto = new BekreftSvangerskapspengervilkårDto("begrunnelse",
            Avslagsårsak.SØKER_ER_IKKE_I_ARBEID.getKode());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        var resultat = oppdaterer.oppdater(dto, param);

        var builder = VilkårResultat.builder();
        resultat.getVilkårResultatSomSkalLeggesTil()
            .forEach(v -> builder.leggTilVilkårResultat(v.getVilkårType(), v.getVilkårUtfallType(),
                v.getVilkårUtfallMerknad(), new Properties(), v.getAvslagsårsak(), true, false, null, null));

        assertThat(builder.buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(
            VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    public void skal_innvilge_vilkår() {
        var oppdaterer = oppdaterer();
        var dto = new BekreftSvangerskapspengervilkårDto("begrunnelse", null);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET,
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR);
        var behandling = scenario.lagre(repositoryProvider);
        var param = new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto);
        var resultat = oppdaterer.oppdater(dto, param);

        var builder = VilkårResultat.builder();
        resultat.getVilkårResultatSomSkalLeggesTil()
            .forEach(v -> builder.leggTilVilkårResultat(v.getVilkårType(), v.getVilkårUtfallType(),
                v.getVilkårUtfallMerknad(), new Properties(), v.getAvslagsårsak(), true, false, null, null));


        assertThat(builder.buildFor(behandling).getVilkårene().get(0).getGjeldendeVilkårUtfall()).isEqualTo(
            VilkårUtfallType.OPPFYLT);
    }

    private BekreftSvangerskapspengervilkårOppdaterer oppdaterer() {
        return new BekreftSvangerskapspengervilkårOppdaterer(
            new HistorikkTjenesteAdapter(repositoryProvider.getHistorikkRepository(),
                mock(DokumentArkivTjeneste.class), repositoryProvider.getBehandlingRepository()));
    }

}
