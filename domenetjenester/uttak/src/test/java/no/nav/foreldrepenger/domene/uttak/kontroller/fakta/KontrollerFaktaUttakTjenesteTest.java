package no.nav.foreldrepenger.domene.uttak.kontroller.fakta;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioFarSøkerForeldrepenger;

@CdiDbAwareTest
public class KontrollerFaktaUttakTjenesteTest {

    private final AktørId aktørId = AktørId.dummy();

    @Inject
    private UttakRepositoryProvider repositoryProvider;

    @Inject
    private KontrollerFaktaUttakTjeneste tjeneste;

    @Test
    public void aksjonspunkt_dersom_far_søker_og_ikke_oppgitt_omsorg_til_barnet() {
        //Arrange
        Behandling behandling = opprettBehandlingForFarSomSøker();
        //Act
        var familieHendelse = FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(new Barn()), 1);
        YtelsespesifiktGrunnlag fpGrunnlag = new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser()
            .medBekreftetHendelse(familieHendelse));
        var input = new UttakInput(BehandlingReferanse.fra(behandling, LocalDate.now()), null, fpGrunnlag);
        var aksjonspunktResultater = tjeneste.utledAksjonspunkter(input);

        //Assert
        assertThat(aksjonspunktResultater).containsExactly(MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG);
    }

    @Test
    public void automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_ikke_er_i_tps_og_oppgitt_at_annen_forelder_har_rett() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenario.medSøknadAnnenPart().medUtenlandskFnr("SVERIGE NUMBA 1").medUtenlandskFnrLand(Landkoder.SWE).medAktørId(null);
        Behandling behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarOmAnnenForelderHarRett(BehandlingReferanse.fra(behandling));

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isPresent();
        assertThat(perioderAnnenforelderHarRett.get().getPerioder()).isEmpty();
    }

    @Test
    public void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_ikke_er_i_tps_og_ikke_oppgitt_at_annen_forelder_har_rett() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(false, true, false));
        scenario.medSøknadAnnenPart().medUtenlandskFnr("SVERIGE NUMBA 1").medUtenlandskFnrLand(Landkoder.SWE).medAktørId(null);
        Behandling behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarOmAnnenForelderHarRett(BehandlingReferanse.fra(behandling));

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isEmpty();
    }

    @Test
    public void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_er_i_tps() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
        Behandling behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarOmAnnenForelderHarRett(BehandlingReferanse.fra(behandling));

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isEmpty();
    }

    @Test
    public void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_ikke_finnes() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        Behandling behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarOmAnnenForelderHarRett(BehandlingReferanse.fra(behandling));

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isEmpty();
    }

    @Test
    public void ikke_automatisk_avklare_at_annen_forelder_ikke_har_rett_hvis_annen_forelder_er_ukjent() {
        ScenarioFarSøkerForeldrepenger scenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        //Ukjent forelder lagres slik i db
        scenario.medSøknadAnnenPart().medAktørId(null).medUtenlandskFnr(null);
        Behandling behandling = scenario.lagre(repositoryProvider);

        tjeneste.avklarOmAnnenForelderHarRett(BehandlingReferanse.fra(behandling));

        var perioderAnnenforelderHarRett = repositoryProvider.getYtelsesFordelingRepository().hentAggregat(behandling.getId()).getPerioderAnnenforelderHarRett();
        assertThat(perioderAnnenforelderHarRett).isEmpty();
    }

    private Behandling opprettBehandlingForFarSomSøker() {
        var scenario = ScenarioFarSøkerForeldrepenger.forFødselMedGittAktørId(aktørId);
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(true, false, false);
        scenario.medOppgittRettighet(rettighet);

        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny().medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriode(LocalDate.now().plusWeeks(6), LocalDate.now().plusWeeks(10))
            .build();

        scenario.medFordeling(new OppgittFordelingEntitet(Collections.singletonList(periode), true));
        return scenario.lagre(repositoryProvider);
    }
}
