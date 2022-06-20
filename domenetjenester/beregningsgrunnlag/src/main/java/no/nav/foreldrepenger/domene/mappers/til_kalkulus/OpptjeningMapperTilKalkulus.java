package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class OpptjeningMapperTilKalkulus {

    public static OpptjeningAktiviteterDto mapOpptjeningAktiviteter(OpptjeningAktiviteter opptjeningAktiviteter,
                                                                    InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse ref) {
        var inntektsmeldinger = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        var yrkesfilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(),
            iayGrunnlag.getAktørArbeidFraRegister(ref.aktørId()));
        return new OpptjeningAktiviteterDto(
            opptjeningAktiviteter.getOpptjeningPerioder().stream()
                .filter(opp -> finnesInntektsmeldingForEllerKanBeregnesUten(opp, inntektsmeldinger, yrkesfilter))
                .map(opptjeningPeriode -> OpptjeningAktiviteterDto.nyPeriode(
                    OpptjeningAktivitetType.fraKode(opptjeningPeriode.opptjeningAktivitetType().getKode()),
                    mapPeriode(opptjeningPeriode),
                    opptjeningPeriode.arbeidsgiverOrgNummer(),
                    opptjeningPeriode.arbeidsgiverAktørId(),
                    opptjeningPeriode.arbeidsforholdId() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(opptjeningPeriode.arbeidsforholdId()))).collect(Collectors.toList()));
    }

    private static boolean finnesInntektsmeldingForEllerKanBeregnesUten(OpptjeningAktiviteter.OpptjeningPeriode opp,
                                                                        List<Inntektsmelding> inntektsmeldinger,
                                                                        YrkesaktivitetFilter yrkesfilter) {
        if (opp.arbeidsgiverAktørId() == null && opp.arbeidsgiverOrgNummer() == null) {
            // Ikke et arbeidsforhold, trenger ikke ta stilling til IM
            return true;
        }
        var inntektsmeldingerForArbeidsforholdHosAG = inntektsmeldinger.stream()
            .filter(im -> im.getArbeidsgiver().equals(getArbeidsgiver(opp)))
            // Trenger ikke se på inntektsmeldinger med arbeidsforholdId som ikke er knyttet til et reelt arbeidsforhold
            .filter(im -> harArbeidsforholdIdSomEksisterer(im, yrkesfilter))
            .collect(Collectors.toList());
        if (inntektsmeldingerForArbeidsforholdHosAG.isEmpty()) {
            return true;
        }
        if (opp.arbeidsforholdId() == null) {
            return true;
        }
        return inntektsmeldingerForArbeidsforholdHosAG.stream()
            .anyMatch(im -> im.getArbeidsforholdRef().gjelderFor(opp.arbeidsforholdId()));
    }

    private static boolean harArbeidsforholdIdSomEksisterer(Inntektsmelding inntektsmelding, YrkesaktivitetFilter yrkesfilter) {
        return yrkesfilter.getYrkesaktiviteter().stream()
            .anyMatch(ya -> ya.gjelderFor(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef()));
    }

    private static Arbeidsgiver getArbeidsgiver(OpptjeningAktiviteter.OpptjeningPeriode opp) {
        if (opp.arbeidsgiverAktørId() != null) {
            return Arbeidsgiver.person(new AktørId(opp.arbeidsgiverAktørId()));
        }
        return Arbeidsgiver.virksomhet(opp.arbeidsgiverOrgNummer());
    }

    private static Intervall mapPeriode(OpptjeningAktiviteter.OpptjeningPeriode opptjeningPeriode) {
        if (opptjeningPeriode.periode().getTom() == null) {
            return Intervall.fraOgMed(opptjeningPeriode.periode().getFom());
        }
        return Intervall.fraOgMedTilOgMed(opptjeningPeriode.periode().getFom(), opptjeningPeriode.periode().getTom());
    }
}
