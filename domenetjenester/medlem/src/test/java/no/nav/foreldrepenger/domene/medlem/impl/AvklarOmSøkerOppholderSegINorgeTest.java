package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingsgrunnlagKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personas;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AvklarOmSøkerOppholderSegINorgeTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    
    @Inject
    private BehandlingsgrunnlagKodeverkRepository behandlingsgrunnlagKodeverkRepository;

    private AvklarOmSøkerOppholderSegINorge tjeneste;

    @Before
    public void setUp() {
        this.tjeneste = new AvklarOmSøkerOppholderSegINorge(provider, behandlingsgrunnlagKodeverkRepository, personopplysningTjeneste, iayTjeneste);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_fodt() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(1);
        scenario.medBekreftetHendelse()
            .medFødselsDato(fødselsdato)
            .medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.FIN);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, fødselsdato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(provider);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_fodt_søkt_termin() {
        // Arrange
        LocalDate fødselsdato = LocalDate.now();
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(fødselsdato)
            .medUtstedtDato(fødselsdato.minusMonths(2))
            .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(fødselsdato)
            .medNavnPå("LEGEN MIN")
            .medUtstedtDato(fødselsdato.minusMonths(2)));

        scenario.medBekreftetHendelse()
            .medFødselsDato(fødselsdato.minusDays(12))
            .medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.BEL);
        scenario.medSøknadDato(fødselsdato.minusMonths(2).plusWeeks(1));
        scenario.medSøknad()
            .medMottattDato(fødselsdato.minusMonths(2).plusWeeks(1));
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, fødselsdato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_dato_for_omsorgsovertakelse() {
        // Arrange
        LocalDate omsorgsovertakelseDato = LocalDate.now();
        ScenarioFarSøkerEngangsstønad scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();

        FarSøkerType farSøkerType = FarSøkerType.OVERTATT_OMSORG;
        scenario.medSøknadHendelse().medAdopsjon(scenario.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelseDato));
        scenario.medSøknad().medFarSøkerType(farSøkerType);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.FIN);

        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, omsorgsovertakelseDato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_er_nordisk() {
        // Arrange
        LocalDate termindato = LocalDate.now();
        AktørId aktørId1 = AktørId.dummy();

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN")
            .medUtstedtDato(termindato));
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.FIN);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    private Optional<MedlemResultat> kallTjeneste(Behandling behandling, LocalDate dato) {
        var ref = BehandlingReferanse.fra(behandling, dato);
        return tjeneste.utled(ref, dato);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_annet_statsborgerskap() {
        // Arrange
        LocalDate termindato = LocalDate.now();
        AktørId aktørId1 = AktørId.dummy();

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN")
            .medUtstedtDato(termindato));
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.CAN);
        Behandling behandling = lagre(scenario);

        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_er_gift_med_nordisk() {
        // Arrange
        LocalDate termindato = LocalDate.now();

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        AktørId annenPartAktørId = AktørId.dummy();

        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon gift = builderForRegisteropplysninger
            .medPersonas()
            .mann(annenPartAktørId, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.FIN)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.EKTE)
            .build();
        scenario.medRegisterOpplysninger(gift);

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, Region.EOS)
            .statsborgerskap(Landkoder.ESP)
            .relasjonTil(annenPartAktørId, RelasjonsRolleType.EKTE)
            .build();

        scenario.medRegisterOpplysninger(søker);

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(termindato)
            .medNavnPå("LEGEN MIN"));
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("LEGEN MIN"));
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_er_gift_med_ANNET_statsborgerskap() {
        // Arrange
        LocalDate termindato = LocalDate.now();

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        AktørId annenPartAktørId = AktørId.dummy();

        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, Region.EOS)
            .statsborgerskap(Landkoder.ESP)
            .relasjonTil(annenPartAktørId, RelasjonsRolleType.EKTE)
            .build();

        PersonInformasjon gift = builderForRegisteropplysninger
            .medPersonas()
            .mann(annenPartAktørId, SivilstandType.GIFT, Region.UDEFINERT)
            .statsborgerskap(Landkoder.CAN)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.EKTE)
            .build();

        scenario.medRegisterOpplysninger(gift);
        scenario.medRegisterOpplysninger(søker);

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(termindato)
            .medNavnPå("LEGEN MIN"));
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("LEGEN MIN"));
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_hatt_inntekt_i_Norge_de_siste_tre_mnd() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        LocalDate fom = LocalDate.now().minusWeeks(3L);
        LocalDate tom = LocalDate.now().minusWeeks(1L);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        LocalDate termindato = LocalDate.now().plusDays(40);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now().minusDays(7))
            .medNavnPå("LEGEN MIN"));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now().minusDays(7))
            .medNavnPå("LEGEN MIN"));
        scenario.medSøknad().medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        Behandling behandling = lagre(scenario);
        
        leggTilInntekt(behandling, behandling.getAktørId(), fom, tom);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    private void leggTilInntekt(Behandling behandling, AktørId aktørId, LocalDate fom, LocalDate tom) {
        // Arrange - inntekt
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørInntekt = builder.getAktørInntektBuilder(aktørId);
        aktørInntekt.leggTilInntekt(InntektBuilder.oppdatere(empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medBeløp(BigDecimal.TEN)
                .medInntektspostType(InntektspostType.LØNN)
                .medPeriode(fom, tom)));
        builder.leggTilAktørInntekt(aktørInntekt);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    @Test
    public void skal_opprette_aksjonspunkt_om_medsoker_har_hatt_inntekt_i_Norge_de_siste_tre_mnd() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        AktørId aktørId2 = AktørId.dummy();
        LocalDate fom = LocalDate.now().minusWeeks(3L);
        LocalDate tom = LocalDate.now().minusWeeks(1L);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        LocalDate termindato = LocalDate.now().plusDays(40);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now().minusDays(7))
            .medNavnPå("LEGEN MIN"));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now().minusDays(7))
            .medNavnPå("LEGEN MIN"));
        scenario.medSøknad().medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);

        Behandling behandling = lagre(scenario);
        
        leggTilInntekt(behandling, aktørId2, fom, tom);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).contains(MedlemResultat.VENT_PÅ_FØDSEL);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_termindato_ikke_har_passert_14_dager() {
        // Arrange
        LocalDate termindato = LocalDate.now().minusDays(14L);
        AktørId aktørId1 = AktørId.dummy();

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN")
            .medUtstedtDato(termindato));
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        Behandling behandling = lagre(scenario);

        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).contains(MedlemResultat.VENT_PÅ_FØDSEL);
    }

    @Test
    public void skal_ikke_opprette_vent_om_termindato_har_passert_28_dager() {
        // Arrange
        LocalDate termindato = LocalDate.now().minusMonths(2);
        AktørId aktørId1 = AktørId.dummy();
        LocalDate fom = LocalDate.now().minusWeeks(60L);
        LocalDate tom = LocalDate.now().minusWeeks(58L);

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(termindato.minusMonths(2))
            .medNavnPå("LEGEN MIN"));
        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(termindato.minusMonths(2))
            .medNavnPå("LEGEN MIN"));
        scenario.medSøknad().medMottattDato(termindato.minusMonths(2).plusDays(3));
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);

        Behandling behandling = lagre(scenario);
        
        leggTilInntekt(behandling, behandling.getAktørId(), fom, tom);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    @Test
    public void skal_oprette_aksjonspunkt_ved_uavklart_oppholdsrett() {
        // Arrange
        LocalDate termindato = LocalDate.now().minusDays(15L);
        AktørId aktørId1 = AktørId.dummy();

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel().medBruker(aktørId1, NavBrukerKjønn.KVINNE);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("LEGEN MIN"));

        scenario.medBekreftetHendelse().medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN")
            .medUtstedtDato(termindato));
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        Behandling behandling = lagre(scenario);

        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    private AktørId leggTilSøker(AbstractTestScenario<?> scenario, AdresseType adresseType, Landkoder adresseLand) {
        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        Personas persona = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.UOPPGITT, Region.UDEFINERT)
            .personstatus(PersonstatusType.UDEFINERT)
            .statsborgerskap(adresseLand);

        PersonAdresse.Builder adresseBuilder = PersonAdresse.builder().adresselinje1("Portveien 2").land(adresseLand);
        persona.adresse(adresseType, adresseBuilder);
        PersonInformasjon søker = persona.build();
        scenario.medRegisterOpplysninger(søker);
        return søkerAktørId;
    }

}
