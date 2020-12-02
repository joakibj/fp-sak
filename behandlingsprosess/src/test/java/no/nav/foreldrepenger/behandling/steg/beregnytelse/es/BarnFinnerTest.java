package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.vedtak.exception.FunksjonellException;

public class BarnFinnerTest {

    private final int maksStønadsalder = 15;

    @Test
    public void skal_finne_antall_barn_basert_på_fødsel() {
        // Arrange
        int antallBarnPåFødsel = 2;
        ScenarioMorSøkerEngangsstønad scenario = byggBehandlingsgrunnlagForFødsel(antallBarnPåFødsel);
        Behandling behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        int funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(antallBarnPåFødsel).isEqualTo(funnetAntallBarn);
    }

    @Test
    public void skal_finne_antall_barn_basert_på_termin() {
        // Arrange
        int antallBarnPåTerminBekreftelse = 2;
        ScenarioMorSøkerEngangsstønad scenario = byggBehandlingsgrunnlagForTermin(antallBarnPåTerminBekreftelse);
        Behandling behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        int funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(antallBarnPåTerminBekreftelse).isEqualTo(funnetAntallBarn);
    }

    @Test
    public void skal_finne_antall_barn_basert_på_adopsjon() {
        // Arrange
        LocalDate overtakelseDato = LocalDate.now();
        // Skal kun gis stønad for barn < 15 år, dvs 2 barn her
        LocalDate fødselsdato14År363Dager = overtakelseDato.minusYears(15).plusDays(2);
        LocalDate fødselsdato14År364Dager = overtakelseDato.minusYears(15).plusDays(1);
        LocalDate fødselsdato15År = overtakelseDato.minusYears(15);

        ScenarioMorSøkerEngangsstønad scenario = byggBehandlingsgrunnlagForAdopsjon(
                asList(fødselsdato14År363Dager, fødselsdato14År364Dager, fødselsdato15År), overtakelseDato);
        Behandling behandling = scenario.lagMocked();

        // Act
        BarnFinner barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        int funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(funnetAntallBarn).isEqualTo(2);
    }

    @Test
    public void skal_finne_antall_barn_basert_på_omsorgsovertakelse() {
        // Arrange
        ScenarioFarSøkerEngangsstønad scenario = byggBehandlingsgrunnlagForOmsorgsovertakelse(2);
        Behandling behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        int funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(2).isEqualTo(funnetAntallBarn);
    }

    @Test
    public void skal_kaste_feil_dersom_antall_barn_ikke_kan_finnes_i_grunnlag() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        assertThrows(FunksjonellException.class, () -> barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder));
    }

    private ScenarioMorSøkerEngangsstønad byggBehandlingsgrunnlagForFødsel(int antallBarn) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final FamilieHendelseBuilder hendelseBuilder = scenario.medBekreftetHendelse().medAntallBarn(antallBarn);
        IntStream.range(0, antallBarn).forEach(it -> hendelseBuilder.medFødselsDato(LocalDate.now()));
        scenario.medBekreftetHendelse(hendelseBuilder);
        return scenario;
    }

    private ScenarioMorSøkerEngangsstønad byggBehandlingsgrunnlagForTermin(int antallBarn) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medBekreftetHendelse()
                .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(LocalDate.now())
                        .medNavnPå("LEGEN MIN")
                        .medUtstedtDato(LocalDate.now()))
                .medAntallBarn(antallBarn);
        scenario.medBekreftetHendelse(familieHendelseBuilder);

        return scenario;
    }

    private ScenarioFarSøkerEngangsstønad byggBehandlingsgrunnlagForOmsorgsovertakelse(int antallBarn) {
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();

        for (int nr = 1; nr <= antallBarn; nr++) {
            scenario.medSøknadHendelse().leggTilBarn(LocalDate.now());
        }

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .kvinne(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT, Region.NORDEN)
                .statsborgerskap(Landkoder.NOR)
                .build();
        scenario.medRegisterOpplysninger(søker);

        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        final FamilieHendelseBuilder.AdopsjonBuilder adopsjonBuilder = scenario.medSøknadHendelse().getAdopsjonBuilder();
        scenario.medSøknadHendelse().erOmsorgovertagelse().medAdopsjon(adopsjonBuilder.medOmsorgsovertakelseDato(LocalDate.now()));
        return scenario;
    }

    private ScenarioMorSøkerEngangsstønad byggBehandlingsgrunnlagForAdopsjon(List<LocalDate> adopsjonsdatoer, LocalDate overtakelseDato) {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        final FamilieHendelseBuilder hendelseBuilder = scenario.medBekreftetHendelse().medAntallBarn(adopsjonsdatoer.size());
        hendelseBuilder.medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(overtakelseDato));

        for (int nr = 1; nr <= adopsjonsdatoer.size(); nr++) {
            hendelseBuilder.leggTilBarn(adopsjonsdatoer.get(nr - 1));
        }
        scenario.medBekreftetHendelse(hendelseBuilder);
        return scenario;
    }
}
