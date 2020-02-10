package no.nav.foreldrepenger.domene.medlem.kontrollerfakta;

import static java.util.Collections.emptyList;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;
import static no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;

@ApplicationScoped
public class AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden implements AksjonspunktUtleder {

    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden() {
    }

    @Inject
    AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden(InntektArbeidYtelseTjeneste iayTjeneste,
                                                                  YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.iayTjeneste = iayTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        Long behandlingId = param.getBehandlingId();
        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlagOptional = iayTjeneste.finnGrunnlag(behandlingId);
        Optional<InntektsmeldingAggregat> inntektsmeldingerOptional = inntektArbeidYtelseGrunnlagOptional.flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger);
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregatOptional = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);

        if (ytelseFordelingAggregatOptional.isEmpty() || inntektArbeidYtelseGrunnlagOptional.isEmpty() || inntektsmeldingerOptional.isEmpty()) {
            return INGEN_AKSJONSPUNKTER;
        }

        var grunnlag = inntektArbeidYtelseGrunnlagOptional.get();
        var inntektsmeldinger = inntektsmeldingerOptional.get();

        LocalDate skjæringstidspunkt = param.getSkjæringstidspunkt().getFørsteUttaksdato();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(param.getAktørId()))
            .før(skjæringstidspunkt);

        if (filter.getYrkesaktiviteter().isEmpty()) {
            return INGEN_AKSJONSPUNKTER;
        }

        LocalDate startdatoOppgittAvBruker = skjæringstidspunkt;

        if (samsvarerStartdatoerFraInntektsmeldingOgBruker(startdatoOppgittAvBruker, inntektsmeldinger) == NEI) {
            if (erMinstEttArbeidsforholdLøpende(filter) == JA) {
                if (samsvarerAlleLøpendeArbeidsforholdMedStartdatoFraBruker(filter, inntektsmeldinger, startdatoOppgittAvBruker) == NEI) {
                    return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_STARTDATO_FOR_FORELDREPENGEPERIODEN);
                }
            }
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall samsvarerAlleLøpendeArbeidsforholdMedStartdatoFraBruker(YrkesaktivitetFilter filter, InntektsmeldingAggregat inntektsmeldingAggregat,
                                                                           LocalDate startdatoOppgittAvBruker) {
        return filter.getYrkesaktiviteter()
            .stream()
            .filter(Yrkesaktivitet::erArbeidsforhold)
            .anyMatch(yrkesaktivitet -> samsvarerIkkeMellomLøpendeArbeidsforholdOgStartdatoFrabruker(filter.getAktivitetsAvtalerForArbeid(yrkesaktivitet),
                inntektsmeldingAggregat, startdatoOppgittAvBruker, yrkesaktivitet)) ? NEI : JA;
    }

    private boolean samsvarerIkkeMellomLøpendeArbeidsforholdOgStartdatoFrabruker(Collection<AktivitetsAvtale> aktivitetsAvtaler,
                                                                                 InntektsmeldingAggregat inntektsmeldingAggregat,
                                                                                 LocalDate startdatoOppgittAvBruker,
                                                                                 Yrkesaktivitet yrkesaktivitet) {
        var løpendeAvtaler = aktivitetsAvtaler
            .stream()
            .filter(AktivitetsAvtale::getErLøpende).collect(Collectors.toList());
        return løpendeAvtaler.stream()
            .anyMatch(aktivitetsAvtale -> {
                var inntektsmeldingerFor = inntektsmeldingAggregat.getInntektsmeldingerFor(yrkesaktivitet.getArbeidsgiver());
                return inntektsmeldingerFor.stream()
                    .anyMatch(inntektsmelding -> !samsvarerOppgittOgInntektsmeldingDato(startdatoOppgittAvBruker, inntektsmelding));
            });
    }

    private boolean samsvarerOppgittOgInntektsmeldingDato(LocalDate startdatoOppgittAvBruker, Inntektsmelding inntektsmelding) {
        return endreDatoHvisLørdagEllerSøndag(inntektsmelding.getStartDatoPermisjon().orElseThrow())
            .equals(endreDatoHvisLørdagEllerSøndag(startdatoOppgittAvBruker));
    }

    Utfall erMinstEttArbeidsforholdLøpende(YrkesaktivitetFilter filter) {
        boolean minstEttLøpende = filter.getYrkesaktiviteter().stream()
            .map(ya -> filter.getAnsettelsesPerioder(ya))
            .flatMap(Collection::stream)
            .anyMatch(AktivitetsAvtale::getErLøpende);
        return minstEttLøpende ? JA : NEI;
    }

    Utfall samsvarerStartdatoerFraInntektsmeldingOgBruker(LocalDate startdatoOppgittAvBruker, InntektsmeldingAggregat inntektsmeldingAggregat) {
        return inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes().stream()
            .anyMatch(im -> !samsvarerOppgittOgInntektsmeldingDato(startdatoOppgittAvBruker, im)) ? NEI : JA;

    }

    LocalDate endreDatoHvisLørdagEllerSøndag(LocalDate dato) {
        if (dato.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return dato.plusDays(2L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return dato.plusDays(1L);
        }
        return dato;
    }

}
