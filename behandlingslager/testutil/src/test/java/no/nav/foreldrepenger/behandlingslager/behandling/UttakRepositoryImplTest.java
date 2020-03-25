package no.nav.foreldrepenger.behandlingslager.behandling;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatDokRegelEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.jpa.TomtResultatException;
import no.nav.vedtak.felles.testutilities.db.Repository;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class UttakRepositoryImplTest {

    private static final String ORGNR = KUNSTIG_ORG;
    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    private final Repository repository = repoRule.getRepository();
    private final UttakRepository uttakRepository = new UttakRepository(repoRule.getEntityManager());
    private Arbeidsgiver arbeidsgiver;
    private Behandlingsresultat behandlingsresultat;

    @Before
    public void setUp() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final FamilieHendelseBuilder familieHendelseBuilder = scenario.medSøknadHendelse();
        familieHendelseBuilder.medAntallBarn(1)
            .medFødselsDato(LocalDate.now())
            .medAdopsjon(familieHendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(LocalDate.now()));
        behandlingsresultat = scenario.lagre(repositoryProvider).getBehandlingsresultat();
        repository.lagre(behandlingsresultat);

        arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
    }

    @Test
    public void hentOpprinneligUttakResultat() {
        //Arrange
        LocalDate fom = LocalDate.now();
        LocalDate tom = fom.plusMonths(3);
        StønadskontoType stønadskontoType = StønadskontoType.FORELDREPENGER;
        PeriodeResultatType resultatType = PeriodeResultatType.INNVILGET;
        UttakResultatPerioderEntitet perioder = opprettUttakResultatPeriode(resultatType, fom, tom, stønadskontoType);

        //Act
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), perioder);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(hentetUttakResultatOpt).isPresent();
        UttakResultatEntitet hentetUttakResultat = hentetUttakResultatOpt.get();//NOSONAR

        List<UttakResultatPeriodeEntitet> resultat = hentetUttakResultat.getOpprinneligPerioder().getPerioder();
        assertThat(resultat).hasSize(1);

        assertThat(resultat.get(0).getFom()).isEqualTo(fom);
        assertThat(resultat.get(0).getTom()).isEqualTo(tom);
        assertThat(resultat.get(0).getResultatType()).isEqualTo(resultatType);
        assertThat(resultat.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(stønadskontoType);
        assertThat(resultat.get(0).getDokRegel()).isNotNull();
        assertThat(resultat.get(0).getPeriodeSøknad()).isNotNull();
        assertThat(resultat.get(0).getAktiviteter().get(0).getUttakAktivitet()).isNotNull();
    }

    @Test
    public void skal_kunne_endre_opprinnelig_flere_ganger_uten_å_feile_pga_unikhetssjekk_for_aktiv() {
        UttakResultatPerioderEntitet uttakResultat1 = opprettUttakResultatPeriode(PeriodeResultatType.IKKE_FASTSATT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet overstyrt1 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet uttakResultat2 = opprettUttakResultatPeriode(PeriodeResultatType.AVSLÅTT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet uttakResultat3 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);

        //Act
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), uttakResultat1);
        uttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt1);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT);
        assertThat(uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId()).get().getOverstyrtPerioder()).isNotNull(); //NOSONAR
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), uttakResultat2);
        assertOpprinneligHarResultatType(PeriodeResultatType.AVSLÅTT);
        assertThat(uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId()).get().getOverstyrtPerioder()).isNull(); //NOSONAR
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), uttakResultat3);
        assertOpprinneligHarResultatType(PeriodeResultatType.INNVILGET);
    }

    @Test
    public void hentOverstyrtUttakResultat() {
        //Arrange
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), opprinnelig);

        LocalDate overstyrtFom = LocalDate.now().plusDays(1);
        LocalDate overstyrtTom = LocalDate.now().plusMonths(4);
        PeriodeResultatType overstyrtResultatType = PeriodeResultatType.AVSLÅTT;
        StønadskontoType overstyrtKonto = StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
        UttakResultatPerioderEntitet overstyrt = opprettUttakResultatPeriode(
            overstyrtResultatType,
            overstyrtFom,
            overstyrtTom,
            overstyrtKonto);

        //Act
        uttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(hentetUttakResultatOpt).isPresent();
        UttakResultatEntitet hentetUttakResultat = hentetUttakResultatOpt.get();//NOSONAR

        assertThat(hentetUttakResultat.getOpprinneligPerioder().getPerioder()).hasSize(1);
        List<UttakResultatPeriodeEntitet> resultat = hentetUttakResultat.getOverstyrtPerioder().getPerioder();
        assertThat(resultat).hasSize(1);

        assertThat(resultat.get(0).getFom()).isEqualTo(overstyrtFom);
        assertThat(resultat.get(0).getTom()).isEqualTo(overstyrtTom);
        assertThat(resultat.get(0).getResultatType()).isEqualTo(overstyrtResultatType);
        assertThat(resultat.get(0).getAktiviteter().get(0).getTrekkonto()).isEqualTo(overstyrtKonto);
    }

    @Test
    public void endringAvOverstyrtSkalResultereINyttUttakResultatMedSammeOpprinnelig() {
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.IKKE_FASTSATT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet overstyrt1 = opprettUttakResultatPeriode(PeriodeResultatType.AVSLÅTT, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        UttakResultatPerioderEntitet overstyrt2 = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET, LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER);
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), opprinnelig);

        //Act
        uttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt1);
        assertOverstyrtHarResultatType(PeriodeResultatType.AVSLÅTT);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT);
        uttakRepository.lagreOverstyrtUttakResultatPerioder(behandlingsresultat.getBehandlingId(), overstyrt2);
        assertOverstyrtHarResultatType(PeriodeResultatType.INNVILGET);
        assertOpprinneligHarResultatType(PeriodeResultatType.IKKE_FASTSATT);
    }

    @Test
    public void utbetalingsgradOgArbeidstidsprosentSkalHa2Desimaler() {
        //Arrange
        UttakResultatPerioderEntitet opprinnelig = opprettUttakResultatPeriode(PeriodeResultatType.INNVILGET,
            LocalDate.now(), LocalDate.now().plusMonths(3), StønadskontoType.FORELDREPENGER,
            new BigDecimal("10.55"), new BigDecimal("20.57"));
        uttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingsresultat.getBehandlingId(), opprinnelig);

        //Assert
        Optional<UttakResultatEntitet> hentetUttakResultatOpt = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());

        UttakResultatPeriodeAktivitetEntitet aktivitet = hentetUttakResultatOpt.get().getGjeldendePerioder().getPerioder().get(0).getAktiviteter().get(0);
        assertThat(aktivitet.getUtbetalingsgrad()).isEqualTo(new BigDecimal("20.57"));
        assertThat(aktivitet.getArbeidsprosent()).isEqualTo(new BigDecimal("10.55"));
    }

    @Test
    public void skal_lagre_og_sette_uttaksperiodegrense_inaktiv() {
        // Arrange
        Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultat)
            .medMottattDato(LocalDate.now())
            .medFørsteLovligeUttaksdag((LocalDate.now().minusDays(5)))
            .build();

        // Act
        uttakRepository.lagreUttaksperiodegrense(behandlingsresultat.getBehandlingId(), uttaksperiodegrense);

        // Assert
        Uttaksperiodegrense uttaksperiodegrenseFør = uttakRepository.hentUttaksperiodegrense(behandlingsresultat.getBehandlingId());
        assertThat(uttaksperiodegrense).isEqualTo(uttaksperiodegrenseFør);

        // Act
        uttakRepository.ryddUttaksperiodegrense(behandlingsresultat.getBehandlingId());

        // Assert
        try {
            uttakRepository.hentUttaksperiodegrense(behandlingsresultat.getBehandlingId());
        } catch (TomtResultatException e) {
            return;
        }
        fail("Hadde forventet TomtResultatException etter rydding");
    }

    private void assertOverstyrtHarResultatType(PeriodeResultatType type) {
        Optional<UttakResultatEntitet> uttakResultatEntitet = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOverstyrtPerioder()); //NOSONAR
    }

    private void assertOpprinneligHarResultatType(PeriodeResultatType type) {
        Optional<UttakResultatEntitet> uttakResultatEntitet = uttakRepository.hentUttakResultatHvisEksisterer(behandlingsresultat.getBehandlingId());
        assertThat(uttakResultatEntitet).isPresent();
        assertHarResultatType(type, uttakResultatEntitet.get().getOpprinneligPerioder()); //NOSONAR
    }

    private void assertHarResultatType(PeriodeResultatType type, UttakResultatPerioderEntitet perioderEntitet) {
        List<UttakResultatPeriodeEntitet> perioder = perioderEntitet.getPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getResultatType()).isEqualTo(type);
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType) {
        return opprettUttakResultatPeriode(resultat, fom, tom, stønadskontoType, new BigDecimal("100.00"));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType,
                                                                     BigDecimal graderingArbeidsprosent) {
        return opprettUttakResultatPeriode(resultat, fom, tom, stønadskontoType, graderingArbeidsprosent, BigDecimal.valueOf(100));
    }

    private UttakResultatPerioderEntitet opprettUttakResultatPeriode(PeriodeResultatType resultat,
                                                                     LocalDate fom,
                                                                     LocalDate tom,
                                                                     StønadskontoType stønadskontoType,
                                                                     BigDecimal graderingArbeidsprosent,
                                                                     BigDecimal utbetalingsgrad) {

        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
        UttakResultatPeriodeSøknadEntitet periodeSøknad = new UttakResultatPeriodeSøknadEntitet.Builder()
            .medMottattDato(LocalDate.now())
            .medUttakPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medGraderingArbeidsprosent(graderingArbeidsprosent)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(BigDecimal.TEN)
            .build();
        UttakResultatDokRegelEntitet dokRegel = UttakResultatDokRegelEntitet.utenManuellBehandling()
            .medRegelInput(" ")
            .medRegelEvaluering(" ")
            .build();
        UttakResultatPeriodeEntitet uttakResultatPeriode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medDokRegel(dokRegel)
            .medResultatType(resultat, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(periodeSøknad)
            .build();

        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = UttakResultatPeriodeAktivitetEntitet.builder(uttakResultatPeriode,
            uttakAktivitet)
            .medTrekkonto(stønadskontoType)
            .medTrekkdager(new Trekkdager(BigDecimal.TEN))
            .medArbeidsprosent(graderingArbeidsprosent)
            .medUtbetalingsgrad(utbetalingsgrad)
            .build();

        uttakResultatPeriode.leggTilAktivitet(periodeAktivitet);

        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttakResultatPeriode);

        return perioder;
    }

}
