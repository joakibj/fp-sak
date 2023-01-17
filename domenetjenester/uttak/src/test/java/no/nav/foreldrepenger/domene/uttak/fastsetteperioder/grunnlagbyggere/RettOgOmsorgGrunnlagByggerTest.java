package no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenForelderRettEØSEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.RettOgOmsorg;

public class RettOgOmsorgGrunnlagByggerTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarRettAnnenForeldreHarIkkeRett() {
        var behandling = morMedRett(true, false).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);
        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isFalse();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarRettAnnenForeldreHarRett() {
        var behandling = morMedRett(true, true).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarRettAnnenForeldreHarRett() {
        var behandling = farMedRett(true, true).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarRettAnnenForeldreHarIkkeRett() {
        var behandling = farMedRett(true, false).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarIkkeRettAnnenForeldreHarIkkeRett() {
        var behandling = farMedRett(false, false).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isFalse();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerFarHarIkkeRettAnnenForeldreHarRett() {
        var behandling = farMedRett(false, true).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isFalse();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeTilHvemSomHarRett_SøkerMorHarIkkeRettAnnenForeldreHarRett() {
        var behandling = morMedRett(false, true).lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeHarAleneomsorgHvisAleneomsorg() {
        var scenario = medAleneomsorg();
        var perioderAleneOmsorg = new PerioderAleneOmsorgEntitet(true);
        scenario.medPeriodeMedAleneomsorg(perioderAleneOmsorg);
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(false, true, null, null));
        var behandling = scenario.lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getAleneomsorg()).isTrue();
        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isFalse();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalIkkeLeggeTilHarAleneomsorgHvisIkkeAleneomsorg() {
        var scenario = medAleneomsorg();
        var perioderAleneOmsorg = new PerioderAleneOmsorgEntitet(false);
        scenario.medPeriodeMedAleneomsorg(perioderAleneOmsorg);
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(true, false, null, null));
        var behandling = scenario.lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getAleneomsorg()).isFalse();
        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isFalse();
    }

    @Test
    public void skalLeggeTilOppgittOgRegisterUføre() {
        var behandling = bareFarMedRett(true, false)
            .medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, null, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, true);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isTrue();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }

    @Test
    public void skalLeggeTilOppgittOgIkkeRegisterUføre() {
        var behandling = bareFarMedRett(true, false)
            .medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, null, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, false);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }

    @Test
    public void skalLeggeTilOppgittIkkeRegisterMenOverstyrtUføre() {
        var behandling = bareFarMedRett(true, false)
            .medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, true, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, false);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isTrue();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }

    @Test
    public void skalLeggeTilOppgittIkkeRegisterMenOverstyrtIkkeUføre() {
        var behandling = bareFarMedRett(true, false)
            .medOverstyrtRettighet(new OppgittRettighetEntitet(false, false, false, null))
            .lagre(repositoryProvider);

        var grunnlag = byggGrunnlagMedRegisterUføre(behandling, false);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
        assertThat(grunnlag.getMorUføretrygd()).isFalse();
        assertThat(grunnlag.getMorOppgittUføretrygd()).isTrue();
    }


    @Test
    public void skalLeggeTilOppgittOgBekreftetEØS() {
        var scenario = bareFarMedRett(false, true);
        var periodeAnnenForelderRettEØS = new PerioderAnnenForelderRettEØSEntitet(true);
        scenario.medPeriodeAnnenForelderRettEØS(periodeAnnenForelderRettEØS);
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(null, null, null, true));
        var behandling = scenario.lagre(repositoryProvider);
        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isTrue();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    @Test
    public void skalLeggeTilOppgittOgAvkreftetEØS() {
        var scenario = bareFarMedRett(false, true);
        var periodeAnnenForelderRettEØS = new PerioderAnnenForelderRettEØSEntitet(false);
        scenario.medPeriodeAnnenForelderRettEØS(periodeAnnenForelderRettEØS);
        scenario.medOverstyrtRettighet(new OppgittRettighetEntitet(null, null, null, false));
        var behandling = scenario.lagre(repositoryProvider);

        var grunnlag = byggGrunnlag(behandling);

        assertThat(grunnlag.getMorHarRett()).isFalse();
        assertThat(grunnlag.getFarHarRett()).isTrue();
    }

    private AbstractTestScenario<?> medAleneomsorg() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));

        scenario.medOppgittRettighet(new OppgittRettighetEntitet(false, true, false, false));
        return scenario;
    }

    private RettOgOmsorgGrunnlagBygger grunnlagBygger() {
        var repositoryProvider = this.repositoryProvider;
        return new RettOgOmsorgGrunnlagBygger(repositoryProvider,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getFpUttakRepository()));
    }

    private AbstractTestScenario<?> morMedRett(boolean søkerHarRett, boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioMorSøkerForeldrepenger.forFødsel(), søkerHarRett, annenForelderHarRett);
    }

    private AbstractTestScenario<?> farMedRett(boolean søkerHarRett, boolean annenForelderHarRett) {
        return scenarioMedRett(ScenarioFarSøkerForeldrepenger.forFødsel(), søkerHarRett, annenForelderHarRett);
    }

    private AbstractTestScenario<?> bareFarMedRett(boolean morOppgittUføre, boolean morOppgittEØS) {
        return scenarioMedRett(ScenarioFarSøkerForeldrepenger.forFødsel(), true, false, morOppgittUføre, morOppgittEØS);
    }

    private AbstractTestScenario<?> scenarioMedRett(AbstractTestScenario<?> scenario,
                                       boolean søkerRett,
                                       boolean annenForelderHarRett) {
        return scenarioMedRett(scenario, søkerRett, annenForelderHarRett, false, false);
    }

    private AbstractTestScenario<?> scenarioMedRett(AbstractTestScenario<?> scenario,
                                       boolean søkerRett,
                                       boolean annenForelderHarRett,
                                       boolean morOppgittUføre,
                                       boolean morOppgittEØS) {
        scenario.medFordeling(new OppgittFordelingEntitet(Collections.emptyList(), true));
        scenario.medOppgittRettighet(new OppgittRettighetEntitet(annenForelderHarRett, false, morOppgittUføre, morOppgittEØS));

        if (!søkerRett) {
            var behandlingsresultat = behandlingsresultatMedAvslåttVilkår();
            scenario.medBehandlingsresultat(behandlingsresultat);
        } return scenario;
    }

    private Behandlingsresultat behandlingsresultatMedAvslåttVilkår() {
        var vilkårBuilder = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        vilkårBuilder.leggTilVilkårAvslått(VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, VilkårUtfallMerknad.VM_1004);
        var behandlingsresultat = Behandlingsresultat.builderForInngangsvilkår().build();
        behandlingsresultat.medOppdatertVilkårResultat(vilkårBuilder.build());
        return behandlingsresultat;
    }

    private RettOgOmsorg byggGrunnlag(Behandling behandling) {
        var bygger = grunnlagBygger();
        var fpGrunnlag = new ForeldrepengerGrunnlag();
        return bygger.byggGrunnlag(new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag)).build();
    }

    private RettOgOmsorg byggGrunnlagMedRegisterUføre(Behandling behandling, boolean uføreVerdi) {
        var bygger = grunnlagBygger();
        var fpGrunnlag = new ForeldrepengerGrunnlag()
            .medUføretrygdGrunnlag(UføretrygdGrunnlagEntitet.Builder.oppdatere(Optional.empty())
                .medRegisterUføretrygd(uføreVerdi, LocalDate.now(), LocalDate.now())
                .medBehandlingId(behandling.getId())
                .medAktørIdUføretrygdet(uføreVerdi ? AktørId.dummy() : null)
                .build());
        return bygger.byggGrunnlag(new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag)).build();
    }

}
