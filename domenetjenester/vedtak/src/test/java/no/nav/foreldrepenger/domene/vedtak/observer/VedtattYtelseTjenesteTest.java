package no.nav.foreldrepenger.domene.vedtak.observer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
public class VedtattYtelseTjenesteTest {

    @Mock
    private BehandlingVedtakRepository behandlingVedtakRepository;
    @Mock
    private BehandlingVedtak vedtak;
    @Mock
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;
    @Mock
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    @Mock
    private FamilieHendelseRepository familieHendelseRepository;

    LocalDate stp = LocalDate.now().minusMonths(3);
    LocalDate knekk1 = LocalDate.now().minusMonths(2);
    LocalDate knekk2 = LocalDate.now().minusMonths(1);


    @Test
    public void skal_teste_arena_ytelser_finnes_ikke() {
        // Arrange
        var behandling = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger().lagMocked();
        var bg = lagBG();
        var br = lagBR();
        when(beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandling.getId())).thenReturn(Optional.of(bg));
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())).thenReturn(Optional.of(br));
        when(behandlingVedtakRepository.hentForBehandling(behandling.getId())).thenReturn(vedtak);
        when(vedtak.getVedtakstidspunkt()).thenReturn(stp.atStartOfDay());
        var tjeneste = new VedtattYtelseTjeneste(behandlingVedtakRepository, beregningsgrunnlagRepository, beregningsresultatRepository, inntektArbeidYtelseTjeneste, familieHendelseRepository);

        var ytelse= (YtelseV1)tjeneste.genererYtelse(behandling, false);
        // Assert
        assertThat(ytelse.getAnvist()).hasSize(3);
        assertThat(ytelse.getAnvist().get(0).getUtbetalingsgrad().getVerdi().longValue()).isEqualTo(20);
        assertThat(ytelse.getAnvist().get(2).getUtbetalingsgrad().getVerdi().longValue()).isEqualTo(60);
    }

    @Test
    public void skal_lage_es_med_periode_lik_stp() {
        // Arrange
        var stp = LocalDate.now().plusDays(40);
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final var terminbekreftelse = scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
            .medTermindato(stp)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(LocalDate.now().minusDays(7));
        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(terminbekreftelse);
        var behandling = scenario.lagMocked();
        var rp = scenario.mockBehandlingRepositoryProvider();
        when(beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())).thenReturn(Optional.empty());
        when(behandlingVedtakRepository.hentForBehandling(behandling.getId())).thenReturn(vedtak);
        when(vedtak.getVedtakstidspunkt()).thenReturn(stp.atStartOfDay());
        var tjeneste = new VedtattYtelseTjeneste(behandlingVedtakRepository, beregningsgrunnlagRepository, beregningsresultatRepository, inntektArbeidYtelseTjeneste, rp.getFamilieHendelseRepository());

        var ytelse= (YtelseV1)tjeneste.genererYtelse(behandling, false);
        // Assert
        assertThat(ytelse.getAnvist()).isEmpty();
        assertThat(ytelse.getPeriode().getFom()).isEqualTo(stp);
        assertThat(ytelse.getPeriode().getTom()).isEqualTo(stp);
    }

    private BeregningsgrunnlagEntitet lagBG() {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(new BigDecimal(100000))
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(stp, knekk1.minusDays(1))
                .medRedusertPrÅr(new BigDecimal(120000))
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(new BigDecimal(700000))
                    .medRedusertPrÅr(new BigDecimal(120000))
                    .medRedusertBrukersAndelPrÅr(new BigDecimal(120000))
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                        .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                    .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)))
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(knekk1, knekk2.minusDays(1))
                .medRedusertPrÅr(new BigDecimal(240000))
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(new BigDecimal(700000))
                    .medRedusertPrÅr(new BigDecimal(240000))
                    .medRedusertBrukersAndelPrÅr(new BigDecimal(240000))
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                        .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                    .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)))
            .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(knekk2, Tid.TIDENES_ENDE)
                .medRedusertPrÅr(new BigDecimal(360000))
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                    .medBeregnetPrÅr(new BigDecimal(700000))
                    .medRedusertPrÅr(new BigDecimal(360000))
                    .medRedusertBrukersAndelPrÅr(new BigDecimal(360000))
                    .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                        .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                        .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999")))
                    .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)))
            .build();
        return beregningsgrunnlag;
    }


    private BeregningsresultatEntitet lagBR() {
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        var per1a = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp, stp.plusDays(14))
            .build(beregningsresultat);
        var per1b = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(stp.plusDays(15), knekk1.minusDays(1))
            .build(beregningsresultat);
        var per2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(knekk1, knekk2.minusDays(1))
            .build(beregningsresultat);
        var per3 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(knekk2, LocalDate.now().plusWeeks(2))
            .build(beregningsresultat);
        byggandel(per1a, 460);
        byggandel(per1b, 460);
        byggandel(per2, 920);
        byggandel(per3, 1380);
        return beregningsresultat;
    }

    private void byggandel(BeregningsresultatPeriode periode, int dagsats) {
        BeregningsresultatAndel.builder()
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medStillingsprosent(BigDecimal.TEN.multiply(BigDecimal.TEN))
            .medUtbetalingsgrad(BigDecimal.TEN.multiply(BigDecimal.TEN))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .build(periode);
    }

}
