package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.AVKLAR_OM_ER_BOSATT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personas;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.FPDateUtil;

@RunWith(CdiRunner.class)
public class AvklarOmErBosattTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    private AvklarOmErBosatt avklarOmErBosatt;

    @Before
    public void setUp() {
        this.avklarOmErBosatt = new AvklarOmErBosatt(provider, medlemskapPerioderTjeneste, personopplysningTjeneste);
    }

    @Test
    public void skal_gi_medlem_resultat_AVKLAR_OM_ER_BOSATT() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato).medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.SWE);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_gi_medlem_resultat_AVKLAR_OM_ER_BOSATT() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato).medAntallBarn(1);

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_når_bruker_har_utenlandsk_postadresse_og_dekningsgraden_er_frivillig_medlem() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, Landkoder.USA);
        LocalDate fødselsDato = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(fødselsDato);
        MedlemskapPerioderEntitet gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_9_2_a) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
            .medPeriode(LocalDate.now(), LocalDate.now())
            .build();

        scenario.leggTilMedlemskapPeriode(gyldigPeriodeUnderFødsel);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_når_bruker_har_utenlandsk_postadresse_og_dekningsgraden_er_ikke_medlem() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, Landkoder.USA);
        LocalDate fødselsDato = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        MedlemskapPerioderEntitet gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_6) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
            .medPeriode(LocalDate.now(), LocalDate.now())
            .build();

        scenario.leggTilMedlemskapPeriode(gyldigPeriodeUnderFødsel);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_minst_to_av_spørsmål_til_bruker_om_tilknytning_er_nei() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        MedlemskapOppgittLandOppholdEntitet oppholdUtlandForrigePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(true)
            .medLand(Landkoder.BEL)
            .medPeriode(LocalDate.now(), LocalDate.now().plusYears(1))
            .build();
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning().medOpphold(singletonList(oppholdUtlandForrigePeriode)).medOppholdNå(false);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_ikke_skal_bo_i_norge_de_neste_12_månedene() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(FPDateUtil.iDag());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        MedlemskapOppgittLandOppholdEntitet fremtidigOppholdISverige = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.SWE)
            .medPeriode(FPDateUtil.iDag().plusDays(20), FPDateUtil.iDag().plusYears(2))
            .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning()
            .medOpphold(List.of(fremtidigOppholdISverige))
            .medOppholdNå(true);
        Behandling behandling = scenario.lagre(provider);


        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_skal_bo_i_mange_land_i_fremtiden_men_til_sammen_under_12_måneder() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(FPDateUtil.iDag());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        MedlemskapOppgittLandOppholdEntitet swe = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.SWE)
            .medPeriode(FPDateUtil.iDag().plusDays(0), FPDateUtil.iDag().plusMonths(2))
            .build();

        MedlemskapOppgittLandOppholdEntitet usa = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.USA)
            .medPeriode(FPDateUtil.iDag().plusMonths(2), FPDateUtil.iDag().plusMonths(4))
            .build();

        MedlemskapOppgittLandOppholdEntitet bel = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.BEL)
            .medPeriode(FPDateUtil.iDag().plusMonths(4), FPDateUtil.iDag().plusMonths(6))
            .build();

        MedlemskapOppgittLandOppholdEntitet png = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.PNG)
            .medPeriode(FPDateUtil.iDag().plusMonths(6), FPDateUtil.iDag().plusMonths(8))
            .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning()
            .medOpphold(List.of(swe, usa, bel, png))
            .medOppholdNå(true);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_skal_bo_i_mange_land_i_fremtiden_men_til_sammen_mer_12_måneder() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(FPDateUtil.iDag());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        MedlemskapOppgittLandOppholdEntitet swe = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.SWE)
            .medPeriode(FPDateUtil.iDag().plusDays(0), FPDateUtil.iDag().plusMonths(2))
            .build();

        MedlemskapOppgittLandOppholdEntitet usa = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.USA)
            .medPeriode(FPDateUtil.iDag().plusMonths(2), FPDateUtil.iDag().plusMonths(4))
            .build();

        MedlemskapOppgittLandOppholdEntitet bel = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.BEL)
            .medPeriode(FPDateUtil.iDag().plusMonths(4), FPDateUtil.iDag().plusMonths(6))
            .build();

        MedlemskapOppgittLandOppholdEntitet png = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.PNG)
            .medPeriode(FPDateUtil.iDag().plusMonths(6), FPDateUtil.iDag().plusMonths(15))
            .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning()
            .medOpphold(List.of(swe, usa, bel, png))
            .medOppholdNå(true);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_søker_har_søkt_fødsel_og_ikke_skal_bo_i_norge_de_neste_12_månedene() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultOppgittTilknytning();

        MedlemskapOppgittLandOppholdEntitet fremtidigOppholdISverige = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.SWE)
            .medPeriode(FPDateUtil.iDag().plusDays(20), FPDateUtil.iDag().plusYears(2))
            .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning()
            .medOpphold(List.of(fremtidigOppholdISverige))
            .medOppholdNå(true);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_søker_har_søkt_termin_og_oppholder_seg_i_utland_i_under_12_fremtidige_måneder() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad().medMottattDato(FPDateUtil.iDag());
        scenario.medDefaultOppgittTilknytning();
        scenario.medDefaultSøknadTerminbekreftelse();

        MedlemskapOppgittLandOppholdEntitet fremtidigOppholdISverige = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(false)
            .medLand(Landkoder.SWE)
            .medPeriode(FPDateUtil.iDag().plusDays(20), FPDateUtil.iDag().plusMonths(9))
            .build();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning()
            .medOpphold(List.of(fremtidigOppholdISverige))
            .medOppholdNå(true);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_bare_ett_av_spørsmål_til_bruker_om_tilknytning_er_nei() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now()).medAntallBarn(1);
        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, AdresseType adresseType, Landkoder adresseLand) {
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
    }
}
