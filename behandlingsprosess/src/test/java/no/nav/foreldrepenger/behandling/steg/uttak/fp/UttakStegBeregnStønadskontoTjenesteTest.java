package no.nav.foreldrepenger.behandling.steg.uttak.fp;


import static no.nav.foreldrepenger.behandling.steg.uttak.fp.UttakStegImplTest.avsluttMedVedtak;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.steg.uttak.fp.UttakStegBeregnStønadskontoTjeneste.BeregningingAvStønadskontoResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class UttakStegBeregnStønadskontoTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;
    private UttakStegBeregnStønadskontoTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var uttakRepositoryProvider = new UttakRepositoryProvider(getEntityManager());
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(uttakRepositoryProvider.getFagsakRelasjonRepository(),
            null, uttakRepositoryProvider.getFagsakRepository());
        var uttakTjeneste = new ForeldrepengerUttakTjeneste(uttakRepositoryProvider.getFpUttakRepository());
        var beregnStønadskontoerTjeneste = new BeregnStønadskontoerTjeneste(uttakRepositoryProvider, fagsakRelasjonTjeneste, uttakTjeneste);
        var dekningsgradTjeneste = new DekningsgradTjeneste(fagsakRelasjonTjeneste, uttakRepositoryProvider.getBehandlingsresultatRepository());
        tjeneste = new UttakStegBeregnStønadskontoTjeneste(uttakRepositoryProvider, beregnStønadskontoerTjeneste, dekningsgradTjeneste, uttakTjeneste);
    }

    @Test
    public void skal_beregne_hvis_vedtak_uten_uttak() {
        var førsteScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling førsteBehandling = førsteScenario.lagre(repositoryProvider);
        opprettStønadskontoer(førsteBehandling);
        avsluttMedVedtak(førsteBehandling, repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);

        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        UttakInput input = new UttakInput(BehandlingReferanse.fra(revurdering), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    @Test
    public void skal_beregne_hvis_vedtak_har_uttak_der_alle_periodene_er_avslått() {
        var førsteScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling førsteBehandling = førsteScenario.lagre(repositoryProvider);
        opprettStønadskontoer(førsteBehandling);
        lagreUttak(førsteBehandling, avslåttUttak());
        avsluttMedVedtak(førsteBehandling, repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);

        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        UttakInput input = new UttakInput(BehandlingReferanse.fra(revurdering), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    @Test
    public void skal_ikke_beregne_hvis_vedtak_har_uttak_der_en_periode_er_innvilget_og_en_avslått() {
        var førsteScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling førsteBehandling = førsteScenario.lagre(repositoryProvider);
        opprettStønadskontoer(førsteBehandling);
        var uttak = avslåttUttak();
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(uttak.getPerioder().get(0).getFom().minusWeeks(1),
            uttak.getPerioder().get(0).getFom().minusDays(1))
            .medResultatType(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode);
        lagreUttak(førsteBehandling, uttak);
        avsluttMedVedtak(førsteBehandling, repositoryProvider);

        var revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førsteBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);

        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1))
            .medAnnenpart(new Annenpart(false, førsteBehandling.getId()));
        UttakInput input = new UttakInput(BehandlingReferanse.fra(revurdering), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.INGEN_BEREGNING);
    }

    @Test
    public void skal_ikke_beregne_hvis_annenpart_vedtak_har_uttak_innvilget() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morBehandling = morScenario.lagre(repositoryProvider);
        opprettStønadskontoer(morBehandling);
        var uttak = innvilgetUttak();
        lagreUttak(morBehandling, uttak);
        avsluttMedVedtak(morBehandling, repositoryProvider);

        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        farScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);

        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1))
            .medAnnenpart(new Annenpart(false, morBehandling.getId()));
        UttakInput input = new UttakInput(BehandlingReferanse.fra(farBehandling), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.INGEN_BEREGNING);
    }

    private ForeldrepengerGrunnlag familieHendelser(FamilieHendelse søknadFamilieHendelse) {
        var familieHendelser = new FamilieHendelser().medSøknadHendelse(søknadFamilieHendelse);
        return new ForeldrepengerGrunnlag().medFamilieHendelser(familieHendelser);
    }

    @Test
    public void skal_beregne_hvis_annenpart_vedtak_har_uten_uttak() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morBehandling = morScenario.lagre(repositoryProvider);
        opprettStønadskontoer(morBehandling);
        avsluttMedVedtak(morBehandling, repositoryProvider);

        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        farScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);

        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        UttakInput input = new UttakInput(BehandlingReferanse.fra(farBehandling), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    @Test
    public void skal_beregne_hvis_annenpart_vedtak_har_uttak_avslått() {
        var morScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling morBehandling = morScenario.lagre(repositoryProvider);
        opprettStønadskontoer(morBehandling);
        lagreUttak(morBehandling, avslåttUttak());
        avsluttMedVedtak(morBehandling, repositoryProvider);

        var farScenario = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOppgittRettighet(new OppgittRettighetEntitet(true, true, false));
        farScenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        Behandling farBehandling = farScenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morBehandling.getFagsak(), farBehandling.getFagsak(), morBehandling);

        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = familieHendelser(FamilieHendelse.forFødsel(null, LocalDate.now(), List.of(), 1));
        UttakInput input = new UttakInput(BehandlingReferanse.fra(farBehandling), null, ytelsespesifiktGrunnlag);
        var resultat = tjeneste.beregnStønadskontoer(input);

        assertThat(resultat).isEqualTo(BeregningingAvStønadskontoResultat.BEREGNET);
    }

    private void lagreUttak(Behandling førsteBehandling, UttakResultatPerioderEntitet opprinneligPerioder) {
        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(førsteBehandling.getId(), opprinneligPerioder);
    }

    private UttakResultatPerioderEntitet innvilgetUttak() {
        var uttak = new UttakResultatPerioderEntitet();
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(),
            LocalDate.now().plusWeeks(1))
            .medResultatType(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(new Trekkdager(5))
            .medUtbetalingsgrad(Utbetalingsgrad.TEN)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode);
        return uttak;
    }

    private UttakResultatPerioderEntitet avslåttUttak() {
        var uttak = new UttakResultatPerioderEntitet();
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(LocalDate.now(), LocalDate.now().plusWeeks(1))
            .medResultatType(PeriodeResultatType.AVSLÅTT, IkkeOppfyltÅrsak.BARNET_ER_DØD)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode, new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkonto(StønadskontoType.FELLESPERIODE)
            .medTrekkdager(Trekkdager.ZERO)
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        uttak.leggTilPeriode(periode);
        return uttak;
    }

    private void opprettStønadskontoer(Behandling førsteBehandling) {
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(førsteBehandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(førsteBehandling.getFagsak(), førsteBehandling.getId(), Stønadskontoberegning.builder()
            .medStønadskonto(new Stønadskonto.Builder().medMaxDager(10).medStønadskontoType(StønadskontoType.FELLESPERIODE).build())
            .medRegelEvaluering(" ")
            .medRegelInput(" ")
            .build());
    }
}
