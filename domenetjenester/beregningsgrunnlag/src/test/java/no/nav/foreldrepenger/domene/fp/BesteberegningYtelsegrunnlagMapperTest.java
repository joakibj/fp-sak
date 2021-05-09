package no.nav.foreldrepenger.domene.fp;

import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelsegrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelseperiode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BesteberegningYtelsegrunnlagMapperTest {
    private static final LocalDate STP = LocalDate.of(2021,1,1);

    @Test
    public void skal_mappe_sykepenger() {
        YtelseBuilder sykeBuilder = lagSykepenger(DatoIntervallEntitet.fraOgMedTilOgMed(månederFørStp(15), månederFørStp(6)));
        lagYtelseGrunnlag(sykeBuilder, Arbeidskategori.DAGPENGER);
        YtelseFilter filter = new YtelseFilter(Collections.singletonList(sykeBuilder.build()));
        Optional<Ytelsegrunnlag> mappetGrunnlag = BesteberegningYtelsegrunnlagMapper.mapSykepengerTilYtelegrunnlag(STP.minusMonths(12), filter);
        assertThat(mappetGrunnlag).isPresent();
        assertThat(mappetGrunnlag.get().getPerioder()).hasSize(1);
        assertThat(mappetGrunnlag.get().getPerioder().get(0).getPeriode().getFomDato()).isEqualTo(månederFørStp(15));
        assertThat(mappetGrunnlag.get().getPerioder().get(0).getPeriode().getTomDato()).isEqualTo(månederFørStp(6));
        assertThat(mappetGrunnlag.get().getPerioder().get(0).getAndeler()).hasSize(1);
        assertThat(mappetGrunnlag.get().getPerioder().get(0).getAndeler().get(0).getArbeidskategori().getKode()).isEqualTo(Arbeidskategori.DAGPENGER.getKode());
    }

    @Test
    public void skal_mappe_foreldrepenger() {
        var brres = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        lagFpsakResultatAndel(lagFpsakResultatPeriode(brres, månederFørStp(12), månederFørStp(7)), AktivitetStatus.ARBEIDSTAKER, 350);
        lagFpsakResultatAndel(lagFpsakResultatPeriode(brres, månederFørStp(5), månederFørStp(2)), AktivitetStatus.ARBEIDSTAKER, 700);
        Optional<Ytelsegrunnlag> mappetGrunnlag = BesteberegningYtelsegrunnlagMapper.mapFpsakYtelseTilYtelsegrunnlag(brres, FagsakYtelseType.FORELDREPENGER);
        assertThat(mappetGrunnlag).isPresent();
        List<Ytelseperiode> sortertePerioder = mappetGrunnlag.get().getPerioder().stream().sorted(Comparator.comparing(Ytelseperiode::getPeriode)).collect(Collectors.toList());
        assertThat(sortertePerioder).hasSize(2);
        assertThat(sortertePerioder.get(0).getPeriode().getFomDato()).isEqualTo(månederFørStp(12));
        assertThat(sortertePerioder.get(0).getPeriode().getTomDato()).isEqualTo(månederFørStp(7));
        assertThat(sortertePerioder.get(0).getAndeler()).hasSize(1);
        assertThat(sortertePerioder.get(0).getAndeler().get(0).getAktivitetStatus().getKode()).isEqualTo(AktivitetStatus.ARBEIDSTAKER.getKode());
        assertThat(sortertePerioder.get(0).getAndeler().get(0).getDagsats()).isEqualTo(350L);


        assertThat(sortertePerioder.get(1).getPeriode().getFomDato()).isEqualTo(månederFørStp(5));
        assertThat(sortertePerioder.get(1).getPeriode().getTomDato()).isEqualTo(månederFørStp(2));
        assertThat(sortertePerioder.get(1).getAndeler()).hasSize(1);
        assertThat(sortertePerioder.get(1).getAndeler().get(0).getAktivitetStatus().getKode()).isEqualTo(AktivitetStatus.ARBEIDSTAKER.getKode());
        assertThat(sortertePerioder.get(1).getAndeler().get(0).getDagsats()).isEqualTo(700L);
    }


    private LocalDate månederFørStp(int måneder) {
        return STP.minusDays(måneder);
    }

    private void lagYtelseGrunnlag(YtelseBuilder builder, Arbeidskategori arbeidskategori) {
        builder.medYtelseGrunnlag(builder.getGrunnlagBuilder()
            .medArbeidskategori(arbeidskategori)
            .medDekningsgradProsent(BigDecimal.ZERO)
            .medVedtaksDagsats(Beløp.ZERO)
            .medInntektsgrunnlagProsent(BigDecimal.ZERO)
            .build());
    }

    private YtelseBuilder lagSykepenger(DatoIntervallEntitet periode) {
        return YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(periode)
            .medYtelseType(RelatertYtelseType.SYKEPENGER)
            .medStatus(RelatertYtelseTilstand.AVSLUTTET)
            .medKilde(Fagsystem.INFOTRYGD)
            .medSaksnummer(new Saksnummer("333"));
    }

    private BeregningsresultatPeriode lagFpsakResultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    private BeregningsresultatAndel lagFpsakResultatAndel(BeregningsresultatPeriode beregningsresultatPeriode,
                                                                   AktivitetStatus aktivitetStatus, int dagsats) {
        var arbeidsgiver = Arbeidsgiver.virksomhet("999999999");
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .medArbeidsforholdType(OpptjeningAktivitetType.ARBEID)
            .build(beregningsresultatPeriode);
    }


}