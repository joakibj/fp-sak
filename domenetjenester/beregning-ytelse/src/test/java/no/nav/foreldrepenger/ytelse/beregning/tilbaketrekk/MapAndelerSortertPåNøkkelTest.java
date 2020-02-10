package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetOgArbeidsgiverNøkkel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class MapAndelerSortertPåNøkkelTest {
    private static final LocalDate STARTDATO_YTELSE = LocalDate.of(2019,9,1);
    private static final Arbeidsgiver AG1 = Arbeidsgiver.virksomhet("999999999");
    private static final Arbeidsgiver AG2 = Arbeidsgiver.virksomhet("888888888");
    private static final Arbeidsgiver AG3 = Arbeidsgiver.person(new AktørId("0000000000000"));
    private static final InternArbeidsforholdRef REF1 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF2 = InternArbeidsforholdRef.nyRef();
    private static final InternArbeidsforholdRef REF3 = InternArbeidsforholdRef.nyRef();

    private BeregningsresultatPeriode periode = lagResultatMedPeriode();

    @Test
    public void skal_mappe_en_arbeidsgiver_med_et_arbeidsforhold() {
        // Arrange
        BeregningsresultatAndel andel = lagAndelForPeriode(periode, AG1, REF1);

        // Act
        List<BRNøkkelMedAndeler> resultat = MapAndelerSortertPåNøkkel.map(periode.getBeregningsresultatAndelList());

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getNøkkel()).isEqualTo(andel.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat.get(0).getAndelerTilknyttetNøkkel()).hasSize(1);
        assertThat(resultat.get(0).getAndelerTilknyttetNøkkel().get(0)).isEqualTo(andel);
    }

    @Test
    public void skal_ikke_legge_til_flere_andeler_hos_samme_ag_med_samme_referanse() {
        // Arrange
        BeregningsresultatAndel andel = lagAndelForPeriode(periode, AG1, REF1);
        @SuppressWarnings("unused")
        BeregningsresultatAndel andel2 = lagAndelForPeriode(periode, AG1, REF1);

        // Act
        List<BRNøkkelMedAndeler> resultat = MapAndelerSortertPåNøkkel.map(periode.getBeregningsresultatAndelList());

        // Assert
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getNøkkel()).isEqualTo(andel.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat.get(0).getAndelerTilknyttetNøkkel()).hasSize(1);
        assertThat(resultat.get(0).getAndelerTilknyttetNøkkel().get(0)).isEqualTo(andel);
    }

    @Test
    public void skal_mappe_to_arbeidsgivere_med_et_arbeidsforhold() {
        // Arrange
        BeregningsresultatAndel andel1 = lagAndelForPeriode(periode, AG1, REF1);
        BeregningsresultatAndel andel2 = lagAndelForPeriode(periode, AG2, REF1);

        // Act
        List<BRNøkkelMedAndeler> resultat = MapAndelerSortertPåNøkkel.map(periode.getBeregningsresultatAndelList());

        // Assert
        assertInnhold(resultat, Arrays.asList(andel1.getAktivitetOgArbeidsgiverNøkkel(), andel2.getAktivitetOgArbeidsgiverNøkkel()));

        BRNøkkelMedAndeler resultat1 = finnAndelForNøkkel(resultat, andel1.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).hasSize(1);
        assertThat(resultat1.getAndelerTilknyttetNøkkel().get(0)).isEqualTo(andel1);

        BRNøkkelMedAndeler resultat2 = finnAndelForNøkkel(resultat, andel2.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat2.getAndelerTilknyttetNøkkel()).hasSize(1);
        assertThat(resultat2.getAndelerTilknyttetNøkkel().get(0)).isEqualTo(andel2);

    }

    @Test
    public void skal_mappe_en_arbeidsgiver_med_to_arbeidsforhold() {
        // Arrange
        BeregningsresultatAndel andel1 = lagAndelForPeriode(periode, AG1, REF1);
        BeregningsresultatAndel andel2 = lagAndelForPeriode(periode, AG1, REF2);

        // Act
        List<BRNøkkelMedAndeler> resultat = MapAndelerSortertPåNøkkel.map(periode.getBeregningsresultatAndelList());

        // Assert
        assertInnhold(resultat, Collections.singletonList(andel1.getAktivitetOgArbeidsgiverNøkkel()));

        BRNøkkelMedAndeler resultat1 = finnAndelForNøkkel(resultat, andel1.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).hasSize(2);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel1);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel2);
    }

    @Test
    public void skal_mappe_en_arbeidsgiver_med_fire_arbeidsforhold_deriblant_en_med_nullref() {
        // Arrange
        BeregningsresultatAndel andel1 = lagAndelForPeriode(periode, AG1, REF1);
        BeregningsresultatAndel andel2 = lagAndelForPeriode(periode, AG1, REF2);
        BeregningsresultatAndel andel3 = lagAndelForPeriode(periode, AG1, REF3);
        BeregningsresultatAndel andel4 = lagAndelForPeriode(periode, AG1, null);

        // Act
        List<BRNøkkelMedAndeler> resultat = MapAndelerSortertPåNøkkel.map(periode.getBeregningsresultatAndelList());

        // Assert
        assertInnhold(resultat, Collections.singletonList(andel1.getAktivitetOgArbeidsgiverNøkkel()));

        BRNøkkelMedAndeler resultat1 = finnAndelForNøkkel(resultat, andel1.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).hasSize(4);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel1);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel2);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel3);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel4);
    }

    @Test
    public void skal_mappe_tre_arbeidsgiver_med_forskjellige_antall_arbeidsforhold_deriblant_en_med_nullref() {
        // Arrange
        BeregningsresultatAndel andel1 = lagAndelForPeriode(periode, AG1, REF1); // Ny nøkkel
        BeregningsresultatAndel andel2 = lagAndelForPeriode(periode, AG1, REF2);
        BeregningsresultatAndel andel3 = lagAndelForPeriode(periode, AG1, REF3);
        BeregningsresultatAndel andel4 = lagAndelForPeriode(periode, AG2, null); // Ny nøkkel
        BeregningsresultatAndel andel5 = lagAndelForPeriode(periode, AG2, REF1);
        BeregningsresultatAndel andel6 = lagAndelForPeriode(periode, AG3, REF1); // Ny nøkkel
        BeregningsresultatAndel andel7 = lagAndelForPeriode(periode, AG3, REF2);


        // Act
        List<BRNøkkelMedAndeler> resultat = MapAndelerSortertPåNøkkel.map(periode.getBeregningsresultatAndelList());

        // Assert
        assertInnhold(resultat, List.of(andel1.getAktivitetOgArbeidsgiverNøkkel(), andel4.getAktivitetOgArbeidsgiverNøkkel(), andel6.getAktivitetOgArbeidsgiverNøkkel()));

        BRNøkkelMedAndeler resultat1 = finnAndelForNøkkel(resultat, andel1.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).hasSize(3);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel1);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel2);
        assertThat(resultat1.getAndelerTilknyttetNøkkel()).contains(andel3);

        BRNøkkelMedAndeler resultat2 = finnAndelForNøkkel(resultat, andel4.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat2.getAndelerTilknyttetNøkkel()).hasSize(2);
        assertThat(resultat2.getAndelerTilknyttetNøkkel()).contains(andel4);
        assertThat(resultat2.getAndelerTilknyttetNøkkel()).contains(andel5);

        BRNøkkelMedAndeler resultat3 = finnAndelForNøkkel(resultat, andel6.getAktivitetOgArbeidsgiverNøkkel());
        assertThat(resultat3.getAndelerTilknyttetNøkkel()).hasSize(2);
        assertThat(resultat3.getAndelerTilknyttetNøkkel()).contains(andel6);
        assertThat(resultat3.getAndelerTilknyttetNøkkel()).contains(andel7);

    }


    private BRNøkkelMedAndeler finnAndelForNøkkel(List<BRNøkkelMedAndeler> resultat, AktivitetOgArbeidsgiverNøkkel aktivitetsnøkkel) {
        return resultat.stream()
            .filter(a -> a.getNøkkel().equals(aktivitetsnøkkel))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke forventet andel"));
    }

    private void assertInnhold(List<BRNøkkelMedAndeler> resultat, List<AktivitetOgArbeidsgiverNøkkel> forventedeAndeler) {
        assertThat(resultat).hasSize(forventedeAndeler.size());
        resultat.forEach(andel -> assertThat(forventedeAndeler).contains(andel.getNøkkel()));
    }


    private BeregningsresultatAndel lagAndelForPeriode(BeregningsresultatPeriode periode, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(false)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medDagsats(900)
            .medDagsatsFraBg(900)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(ref)
            .build(periode);
    }

    private BeregningsresultatPeriode lagResultatMedPeriode() {
        BeregningsresultatEntitet resultat = BeregningsresultatEntitet.builder().medRegelInput("test").medRegelSporing("test").build();
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(STARTDATO_YTELSE, STARTDATO_YTELSE.plusDays(45))
            .build(resultat);
    }

}
