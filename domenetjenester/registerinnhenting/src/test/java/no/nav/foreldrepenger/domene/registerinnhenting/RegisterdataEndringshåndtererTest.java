package no.nav.foreldrepenger.domene.registerinnhenting;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningInnhenter;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;

@ExtendWith(MockitoExtension.class)
public class RegisterdataEndringshåndtererTest extends EntityManagerAwareTest {

    private static final AktørId SØKER_AKTØR_ID = AktørId.dummy();
    private static final PersonstatusType PERSONSTATUS = PersonstatusType.BOSA;
    private static final Landkoder LANDKODE = Landkoder.NOR;
    private static final NavBrukerKjønn KJØNN = NavBrukerKjønn.KVINNE;
    private static final String FNR_FORELDER = "01234567890";
    private static final String FNR_BARN = "12345678910";
    private static final LocalDate FORELDER_FØDSELSDATO = LocalDate.now().minusYears(30);

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private MedlemTjeneste medlemTjeneste;
    @Mock
    private AbakusTjeneste abakusTjeneste;

    @Mock
    private Endringskontroller endringskontroller;
    @Mock
    private EndringsresultatSjekker endringsresultatSjekker;
    @Mock
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;
    @Mock
    private MedlemskapRepository medlemskapRepository;
    @Mock
    private FamiliehendelseEventPubliserer familiehendelseEventPubliserer;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private final ScenarioMorSøkerEngangsstønad scenarioFødsel = ScenarioMorSøkerEngangsstønad.forFødsel();
    private final ScenarioMorSøkerEngangsstønad scenarioAdopsjon = ScenarioMorSøkerEngangsstønad.forAdopsjon();
    private EndringsresultatSnapshot snapshotFør;

    private BehandlingRepositoryProvider repositoryProvider;
    private final BehandlingModellRepository behandlingModellRepository = new BehandlingModellRepository();

    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;

    @BeforeEach
    public void before() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(
            repositoryProvider, new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));
        var behandlingskontrollServiceProvider = new BehandlingskontrollServiceProvider(
            getEntityManager(), behandlingModellRepository, BehandlingskontrollEventPubliserer.NULL_EVENT_PUB);
        opplysningsPeriodeTjeneste = new OpplysningsPeriodeTjeneste(skjæringstidspunktTjeneste,
            Period.of(1, 0, 0), Period.of(0, 6, 0), Period.of(0, 4, 0),
            Period.of(1, 0, 0), Period.of(1, 0, 0), Period.of(0, 6, 0));
        when(endringskontroller.erRegisterinnhentingPassert(any())).thenReturn(Boolean.TRUE);
        snapshotFør = EndringsresultatSnapshot.opprett();

        familieHendelseTjeneste = new FamilieHendelseTjeneste(familiehendelseEventPubliserer, repositoryProvider.getFamilieHendelseRepository());
    }

    @Test
    public void skal_returnere_når_behandling_er_før_registerinnhenting() {
        // Arrange
        var behandling = scenarioFødsel
            .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP)
            .lagre(repositoryProvider);
        when(endringskontroller.erRegisterinnhentingPassert(any())).thenReturn(Boolean.FALSE);
        // Act
        var skalinnhente = lagRegisterdataEndringshåndterer().skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(skalinnhente).isFalse();
    }

    @Test
    public void skal_returnere_når_forrige_innhenting_var_i_inneværende_døgn() {
        // Arrange
        scenarioFødsel.medOpplysningerOppdatertTidspunkt(LocalDateTime.now());
        var behandling = scenarioFødsel
            .medBehandlingStegStart(BehandlingStegType.INNHENT_SØKNADOPP)
            .lagre(repositoryProvider);
        // Act
        var skalinnhente = lagRegisterdataEndringshåndterer().skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(skalinnhente).isFalse();
    }

    @Test
    public void skal_skru_behandlingen_tilbake_når_det_er_diff_i_personinformasjon() {
        // Arrange
        var søker = opprettSøkerinfo();

        scenarioFødsel.medSøker(søker)
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1))
            .medBehandlingStegStart(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        scenarioFødsel.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2));
        var behandling = scenarioFødsel.lagre(repositoryProvider);

        var idDiff = EndringsresultatDiff.medDiff(PersonInformasjonEntitet.class, 1L, 2L);
        var sporingDiff = EndringsresultatDiff.medDiffPåSporedeFelt(idDiff, true, null);
        when(endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(Mockito.anyLong(), any(EndringsresultatSnapshot.class)))
            .thenReturn(sporingDiff);

        // Act
        lagRegisterdataEndringshåndterer()
            .utledDiffOgReposisjonerBehandlingVedEndringer(behandling, snapshotFør, false);

        // Assert
        verify(endringskontroller, times(1)).spolTilStartpunkt(any(Behandling.class), any(), any());
    }

    @Test
    public void skal_ikke_oppdatere_registeropplysninger_hvis_det_er_berørt_behandling() {
        // Arrange
        var søker = opprettSøkerinfo();

        scenarioFødsel.medSøker(søker)
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1))
            .medBehandlingStegStart(BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP);
        scenarioFødsel.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2));
        var behandling = scenarioFødsel.lagre(repositoryProvider);
        behandling.getBehandlingÅrsaker().addAll(BehandlingÅrsak.builder(BehandlingÅrsakType.BERØRT_BEHANDLING).buildFor(behandling));

        // Act
        var skalinnhente = lagRegisterdataEndringshåndterer().skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(skalinnhente).isFalse();
    }

    @Test
    public void skal_starte_behandlingen_på_nytt_25_dager_etter_termin_og_ingen_fødselsdato() {
        // Arrange
        var søker = opprettSøkerinfo();

        scenarioFødsel
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1))
            .medSøker(søker)
            .medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA);
        scenarioFødsel.medSøknadHendelse().medTerminbekreftelse(scenarioFødsel.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medTermindato(LocalDate.now().minusDays(30))
            .medUtstedtDato(LocalDate.now())
            .medNavnPå("Legen min"));
        var behandling = scenarioFødsel.lagre(repositoryProvider);

        // Act
        lagRegisterdataEndringshåndterer()
            .utledDiffOgReposisjonerBehandlingVedEndringer(behandling, null, false);

        // Assert
        final var behandling1 = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        assertThat(behandling1.getAktivtBehandlingSteg()).isEqualTo(BehandlingStegType.KONTROLLER_FAKTA);
    }

    @Test
    public void skal_starte_i_SRB_når_tre_uker_etter_termin_og_ingen_fødselsdato() {
        // Arrange
        var fDato = LocalDate.now().minusWeeks(3);

        var søker = opprettSøkerinfo();
        when(endringskontroller.erRegisterinnhentingPassert(any())).thenReturn(true);

        var scenarioFP = ScenarioMorSøkerForeldrepenger.forFødsel().medFødselAdopsjonsdato(List.of(fDato)).medBehandlingType(BehandlingType.REVURDERING)
            .medSøknadDato(fDato).medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1))
            .medSøker(søker).medBehandlingStegStart(BehandlingStegType.VURDER_OPPTJENINGSVILKÅR);
        var behandling = scenarioFP.lagre(repositoryProvider);
        behandling.setStartpunkt(StartpunktType.OPPTJENING);

        // Act
        lagRegisterdataEndringshåndterer()
            .utledDiffOgReposisjonerBehandlingVedEndringer(behandling, null, false);

        // Assert
        verify(endringskontroller, times(1)).spolTilStartpunkt(any(Behandling.class), any(EndringsresultatDiff.class), eq(StartpunktType.SØKERS_RELASJON_TIL_BARNET));
    }

    @Test
    public void skal_ikke_starte_behandlingen_på_nytt_for_adopsjonssak_der_ingenting_er_endret() {
        // Arrange
        var opplysningerOppdatertTidspunkt = LocalDateTime.now().minusDays(1);
        var søker = opprettSøkerinfo();

        scenarioAdopsjon
            .medOpplysningerOppdatertTidspunkt(opplysningerOppdatertTidspunkt)
            .medSøker(søker)
            .medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA);
        scenarioAdopsjon.medSøknadHendelse().leggTilBarn(LocalDate.now())
            .medAdopsjon(scenarioAdopsjon.medSøknadHendelse().getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        var behandling = scenarioAdopsjon.lagre(repositoryProvider);
        when(endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(Mockito.anyLong(), any(EndringsresultatSnapshot.class)))
            .thenReturn(EndringsresultatDiff.opprettForSporingsendringer());

        // Act
        lagRegisterdataEndringshåndterer()
            .utledDiffOgReposisjonerBehandlingVedEndringer(behandling, snapshotFør, false);

        // Assert
        final var behandling1 = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        assertThat(behandling1.getAktivtBehandlingSteg()).isEqualTo(BehandlingStegType.KONTROLLER_FAKTA);
    }


    private RegisterdataEndringshåndterer lagRegisterdataEndringshåndterer() {

        RegisterdataInnhenter registerdataInnhenter = new TestRegisterdataInnhenter(
            personinfoAdapter,
            medlemTjeneste,
            repositoryProvider,
            familieHendelseTjeneste,
            abakusTjeneste,
            medlemskapRepository,
            opplysningsPeriodeTjeneste);

        var durationInstance = "PT10H";
        return new RegisterdataEndringshåndterer(
            repositoryProvider,
            registerdataInnhenter,
            durationInstance,
            endringskontroller,
            endringsresultatSjekker,
            familieHendelseTjeneste,
            behandlingÅrsakTjeneste);
    }

    private Personinfo opprettSøkerinfo() {
        var familierelasjon = new FamilierelasjonVL(new PersonIdent(FNR_BARN), RelasjonsRolleType.BARN
        );

        return new Personinfo.Builder()
            .medAktørId(SØKER_AKTØR_ID)
            .medPersonIdent(new PersonIdent(FNR_FORELDER))
            .medNavn("Navn Navnesen")
            .medFødselsdato(FORELDER_FØDSELSDATO)
            .medNavBrukerKjønn(KJØNN)
            .medLandkoder(List.of(LANDKODE))
            .medPersonstatusType(PERSONSTATUS)
            .medSivilstandType(SivilstandType.UGIFT)
            .medFamilierelasjon(singleton(familierelasjon))
            .build();
    }

    private class TestRegisterdataInnhenter extends RegisterdataInnhenter {

        TestRegisterdataInnhenter(PersoninfoAdapter personinfoAdapter,
                                  MedlemTjeneste medlemTjeneste,
                                  BehandlingRepositoryProvider repositoryProvider,
                                  FamilieHendelseTjeneste familieHendelseTjeneste,
                                  AbakusTjeneste abakusTjeneste,
                                  MedlemskapRepository medlemskapRepository,
                                  OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste) {
            super(new PersonopplysningInnhenter(personinfoAdapter),
                medlemTjeneste,
                repositoryProvider,
                familieHendelseTjeneste,
                medlemskapRepository,
                opplysningsPeriodeTjeneste,
                abakusTjeneste);
        }
    }
}
