package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulator.modell.iay.KravperioderPrArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.PerioderForKravDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.RefusjonsperiodeDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.typer.AktørId;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval.TIDENES_ENDE;

public class KravperioderMapper {

    public static List<KravperioderPrArbeidsforholdDto> map(BehandlingReferanse referanse,
                                                            Collection<Inntektsmelding> inntektsmeldinger,
                                                            InntektArbeidYtelseGrunnlag grunnlagDto) {
        Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold = finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(inntektsmeldinger);
        Map<Kravnøkkel, List<Inntektsmelding>> gruppertPrArbeidsforhold = finnInntektsmeldingMedRefusjonPrArbeidsforhold(inntektsmeldinger);

        List<KravperioderPrArbeidsforholdDto> kravPrArbeidsforhold = gruppertPrArbeidsforhold
            .entrySet()
            .stream()
            .filter(e -> sisteIMPrArbeidsforhold.containsKey(e.getKey()))
            .map(e -> mapTilKravPrArbeidsforhold(referanse, grunnlagDto, sisteIMPrArbeidsforhold, e))
            .collect(Collectors.toList());
        return kravPrArbeidsforhold;
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> finnInntektsmeldingMedRefusjonPrArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger) {
        List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav = filtrerKunRefusjon(inntektsmeldinger);
        return grupper(inntektsmeldingerMedRefusjonskrav);
    }

    private static List<Inntektsmelding> filtrerKunRefusjon(Collection<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .filter(im -> (im.getRefusjonBeløpPerMnd() != null && !im.getRefusjonBeløpPerMnd().erNullEllerNulltall()) ||
                im.getEndringerRefusjon().stream().anyMatch(e -> !e.getRefusjonsbeløp().erNullEllerNulltall()))
            .collect(Collectors.toList());
    }

    private static Map<Kravnøkkel, Inntektsmelding> finnSisteInntektsmeldingMedRefusjonPrArbeidsforhold(Collection<Inntektsmelding> inntektsmeldinger) {
        return grupperEneste(filtrerKunRefusjon(inntektsmeldinger));
    }

    private static KravperioderPrArbeidsforholdDto mapTilKravPrArbeidsforhold(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag grunnlagDto,
                                                                           Map<Kravnøkkel, Inntektsmelding> sisteIMPrArbeidsforhold, Map.Entry<Kravnøkkel, List<Inntektsmelding>> e) {
        List<PerioderForKravDto> alleTidligereKravPerioder = lagPerioderForAlle(referanse, grunnlagDto, e.getValue());
        PerioderForKravDto sistePerioder = lagPerioderForKrav(
            sisteIMPrArbeidsforhold.get(e.getKey()),
            referanse.getSkjæringstidspunkt().getSkjæringstidspunktOpptjening(),
            grunnlagDto.getAktørArbeidFraRegister(referanse.getAktørId()));
        return new KravperioderPrArbeidsforholdDto(
            mapTilArbeidsgiver(e.getKey().arbeidsgiver),
            mapReferanse(e.getKey().referanse),
            alleTidligereKravPerioder,
            sistePerioder.getPerioder().stream().map(RefusjonsperiodeDto::periode).collect(Collectors.toList()));
    }

    private static no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver mapTilArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.erAktørId()
            ? no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdentifikator()))
            : no.nav.folketrygdloven.kalkulator.modell.typer.Arbeidsgiver.virksomhet(arbeidsgiver.getOrgnr());
    }

    private static List<PerioderForKravDto> lagPerioderForAlle(BehandlingReferanse referanse, InntektArbeidYtelseGrunnlag grunnlagDto, List<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(im -> lagPerioderForKrav(im, referanse.getSkjæringstidspunkt().getSkjæringstidspunktOpptjening(), grunnlagDto.getAktørArbeidFraRegister(referanse.getAktørId())))
            .collect(Collectors.toList());
    }

    private static Map<Kravnøkkel, List<Inntektsmelding>> grupper(List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav) {
        Map<Kravnøkkel, List<Inntektsmelding>> resultMap = inntektsmeldingerMedRefusjonskrav.stream()
            .map(im -> new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .distinct()
            .collect(Collectors.toMap(n -> n, n -> new ArrayList<>()));
        inntektsmeldingerMedRefusjonskrav.forEach(im -> {
            var nøkler = finnKeysSomSkalHaInntektsmelding(resultMap, im);
            nøkler.forEach(n -> resultMap.get(n).add(im));
        });
        return resultMap;
    }

    private static Set<Kravnøkkel> finnKeysSomSkalHaInntektsmelding(Map<Kravnøkkel, List<Inntektsmelding>> resultMap, Inntektsmelding im) {
        return resultMap.keySet().stream().filter(n -> n.arbeidsgiver.equals(im.getArbeidsgiver()) && n.referanse.gjelderFor(im.getArbeidsforholdRef())).collect(Collectors.toSet());
    }

    private static Map<Kravnøkkel, Inntektsmelding> grupperEneste(List<Inntektsmelding> inntektsmeldingerMedRefusjonskrav) {
        return inntektsmeldingerMedRefusjonskrav.stream()
            .collect(Collectors.toMap(im ->
                    new Kravnøkkel(im.getArbeidsgiver(), im.getArbeidsforholdRef()),
                im -> im));
    }

    private static InternArbeidsforholdRefDto mapReferanse(InternArbeidsforholdRef arbeidsforholdRef) {
        return InternArbeidsforholdRefDto.ref(arbeidsforholdRef.getReferanse());
    }

    private static PerioderForKravDto lagPerioderForKrav(Inntektsmelding im,
                                                         LocalDate skjæringstidspunktBeregning,
                                                         Optional<AktørArbeid> arbeidDto) {
        LocalDate startRefusjon = finnStartdatoRefusjon(im, skjæringstidspunktBeregning, arbeidDto);
        return new PerioderForKravDto(im.getInnsendingstidspunkt().toLocalDate(), mapRefusjonsperioder(im, startRefusjon));
    }

    private static LocalDate finnStartdatoRefusjon(Inntektsmelding im, LocalDate skjæringstidspunktBeregning,
                                                   Optional<AktørArbeid> arbeidDto) {
        LocalDate startRefusjon;
        var aktørArbeid = arbeidDto
            .orElseThrow(() ->  new IllegalStateException("Har motatt inntektsmeldinger, men har ikke arbeidsaggregat"));
        LocalDate startDatoArbeid = aktørArbeid.hentAlleYrkesaktiviteter().stream()
            .filter(y -> y.getArbeidsgiver().getIdentifikator().equals(im.getArbeidsgiver().getIdentifikator()) &&
                matcherReferanse(y.getArbeidsforholdRef(), im.getArbeidsforholdRef()))
            .flatMap(y -> y.getAlleAktivitetsAvtaler().stream())
            .filter(AktivitetsAvtale::erAnsettelsesPeriode)
            .map(AktivitetsAvtale::getPeriode)
            .filter(periode -> !periode.getTomDato().isBefore(skjæringstidspunktBeregning))
            .map(DatoIntervallEntitet::getFomDato)
            .min(Comparator.naturalOrder())
            .orElse(skjæringstidspunktBeregning);
        if (startDatoArbeid.isAfter(skjæringstidspunktBeregning)) {
            if (im.getStartDatoPermisjon().isEmpty()) {
                startRefusjon = startDatoArbeid;
            } else {
                startRefusjon = startDatoArbeid.isAfter(im.getStartDatoPermisjon().get()) ?
                    startDatoArbeid : im.getStartDatoPermisjon().get();
            }
        } else {
            startRefusjon = skjæringstidspunktBeregning;
        }
        return startRefusjon;
    }

    private static List<RefusjonsperiodeDto> mapRefusjonsperioder(Inntektsmelding im, LocalDate startdatoRefusjon) {
        ArrayList<LocalDateSegment<BigDecimal>> alleSegmenter = new ArrayList<>();
        if (im.getRefusjonOpphører() != null && im.getRefusjonOpphører().isBefore(startdatoRefusjon)) {
            return Collections.emptyList();
        }
        if (!(im.getRefusjonBeløpPerMnd() == null || im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) == 0)) {
            alleSegmenter.add(new LocalDateSegment<>(startdatoRefusjon, TIDENES_ENDE, im.getRefusjonBeløpPerMnd().getVerdi()));
        }

        alleSegmenter.addAll(im.getEndringerRefusjon().stream().map(e ->
            new LocalDateSegment<>(e.getFom(), TIDENES_ENDE, e.getRefusjonsbeløp().getVerdi())
        ).collect(Collectors.toList()));

        if (im.getRefusjonOpphører() != null && !im.getRefusjonOpphører().equals(TIDENES_ENDE)) {
            alleSegmenter.add(new LocalDateSegment<>(im.getRefusjonOpphører().plusDays(1), TIDENES_ENDE, BigDecimal.ZERO));
        }

        var refusjonTidslinje = new LocalDateTimeline<>(alleSegmenter, (interval, lhs, rhs) -> {
            if (lhs.getFom().isBefore(rhs.getFom())) {
                return new LocalDateSegment<>(interval, rhs.getValue());
            }
            return new LocalDateSegment<>(interval, lhs.getValue());
        });
        return refusjonTidslinje.stream()
            .map(r -> new RefusjonsperiodeDto(Intervall.fraOgMedTilOgMed(r.getFom(), r.getTom()), r.getValue()))
            .collect(Collectors.toList());

    }

    private static boolean matcherReferanse(InternArbeidsforholdRef ref1, InternArbeidsforholdRef ref2) {
        return (ref1 == null && ref2 == null)
            || (ref1 != null && ref2 != null && Objects.equals(ref1.getReferanse(), ref2.getReferanse()));
    }

    public static record Kravnøkkel(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef referanse) { }
}
