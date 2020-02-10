package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public final class MapYrkesaktivitetTilOpptjeningsperiodeTjeneste {


    private MapYrkesaktivitetTilOpptjeningsperiodeTjeneste() {
    }

    public static List<OpptjeningsperiodeForSaksbehandling> mapYrkesaktivitet(BehandlingReferanse behandlingReferanse,
                                                                       Yrkesaktivitet registerAktivitet,
                                                                       InntektArbeidYtelseGrunnlag grunnlag,
                                                                       OpptjeningAktivitetVurdering vurderForSaksbehandling,
                                                                       Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening,
                                                                       Yrkesaktivitet overstyrtAktivitet) {
        final OpptjeningAktivitetType type = utledOpptjeningType(mapArbeidOpptjening, registerAktivitet.getArbeidType());
        return new ArrayList<>(mapAktivitetsavtaler(behandlingReferanse, registerAktivitet, grunnlag,
            vurderForSaksbehandling, type, overstyrtAktivitet));
    }

    private static OpptjeningAktivitetType utledOpptjeningType(Map<ArbeidType, Set<OpptjeningAktivitetType>> mapArbeidOpptjening, ArbeidType arbeidType) {
        return mapArbeidOpptjening.get(arbeidType)
            .stream()
            .findFirst()
            .orElse(OpptjeningAktivitetType.UDEFINERT);
    }

    private static List<OpptjeningsperiodeForSaksbehandling> mapAktivitetsavtaler(BehandlingReferanse behandlingReferanse,
                                                                                  Yrkesaktivitet registerAktivitet,
                                                                                  InntektArbeidYtelseGrunnlag grunnlag,
                                                                                  OpptjeningAktivitetVurdering vurderForSaksbehandling,
                                                                                  OpptjeningAktivitetType type,
                                                                                  Yrkesaktivitet overstyrtAktivitet) {
        List<OpptjeningsperiodeForSaksbehandling> perioderForAktivitetsavtaler = new ArrayList<>();
        LocalDate skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();
        for (AktivitetsAvtale avtale : gjeldendeAvtaler(grunnlag, skjæringstidspunkt, registerAktivitet, overstyrtAktivitet)) {
            var builder = OpptjeningsperiodeForSaksbehandling.Builder.ny()
                .medOpptjeningAktivitetType(type)
                .medPeriode(avtale.getPeriode())
                .medBegrunnelse(avtale.getBeskrivelse())
                .medStillingsandel(finnStillingsprosent(registerAktivitet));
            harSaksbehandlerVurdert(builder, type, behandlingReferanse, registerAktivitet, vurderForSaksbehandling, grunnlag);
            settArbeidsgiverInformasjon(gjeldendeAktivitet(registerAktivitet, overstyrtAktivitet), builder);
            builder.medVurderingsStatus(
                vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, registerAktivitet, overstyrtAktivitet, grunnlag, grunnlag.harBlittSaksbehandlet()));
            if (harEndretPåPeriode(avtale.getPeriode(), overstyrtAktivitet)) {
                builder.medErPeriodenEndret();
            }
            perioderForAktivitetsavtaler.add(builder.build());
        }
        return perioderForAktivitetsavtaler;
    }

    public static void settArbeidsgiverInformasjon(Yrkesaktivitet yrkesaktivitet, OpptjeningsperiodeForSaksbehandling.Builder builder) {
        Arbeidsgiver arbeidsgiver = yrkesaktivitet.getArbeidsgiver();
        if (arbeidsgiver != null) {
            builder.medArbeidsgiver(arbeidsgiver);
            builder.medOpptjeningsnøkkel(new Opptjeningsnøkkel(yrkesaktivitet.getArbeidsforholdRef(), arbeidsgiver));
        }
        if (yrkesaktivitet.getNavnArbeidsgiverUtland() != null) {
            builder.medArbeidsgiverUtlandNavn(yrkesaktivitet.getNavnArbeidsgiverUtland());
        }
    }

    private static boolean harEndretPåPeriode(DatoIntervallEntitet periode, Yrkesaktivitet overstyrtAktivitet) {
        if (overstyrtAktivitet == null) {
            return false;
        }

        return new YrkesaktivitetFilter(null, List.of(overstyrtAktivitet)).getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode).noneMatch(p -> p.equals(periode));
    }

    private static Stillingsprosent finnStillingsprosent(Yrkesaktivitet registerAktivitet) {
        final Stillingsprosent defaultStillingsprosent = new Stillingsprosent(0);
        if (registerAktivitet.erArbeidsforhold()) {
            var filter = new YrkesaktivitetFilter(null, List.of(registerAktivitet));
            return filter.getAktivitetsAvtalerForArbeid()
                .stream()
                .filter(aa -> aa.getProsentsats() != null)
                .map(AktivitetsAvtale::getProsentsats)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Stillingsprosent::getVerdi))
                .orElse(defaultStillingsprosent);
        }
        return defaultStillingsprosent;
    }

    private static Collection<AktivitetsAvtale> gjeldendeAvtaler(InntektArbeidYtelseGrunnlag grunnlag,
                                                                 LocalDate skjæringstidspunktForOpptjening,
                                                                 Yrkesaktivitet registerAktivitet,
                                                                 Yrkesaktivitet overstyrtAktivitet) {
        Yrkesaktivitet gjeldendeAktivitet = gjeldendeAktivitet(registerAktivitet, overstyrtAktivitet);
        if (registerAktivitet.erArbeidsforhold()) {
            return MapAnsettelsesPeriodeOgPermisjon.beregn(grunnlag, gjeldendeAktivitet);
        }
        return new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon().orElse(null), gjeldendeAktivitet).før(skjæringstidspunktForOpptjening).getAktivitetsAvtalerForArbeid();
    }

    private static Yrkesaktivitet gjeldendeAktivitet(Yrkesaktivitet registerAktivitet, Yrkesaktivitet overstyrtAktivitet) {
        return overstyrtAktivitet == null ? registerAktivitet : overstyrtAktivitet;
    }

    private static void harSaksbehandlerVurdert(OpptjeningsperiodeForSaksbehandling.Builder builder, OpptjeningAktivitetType type,
                                                BehandlingReferanse behandlingReferanse, Yrkesaktivitet registerAktivitet,
                                                OpptjeningAktivitetVurdering vurderForSaksbehandling, InntektArbeidYtelseGrunnlag grunnlag) {
        if (vurderForSaksbehandling.vurderStatus(type, behandlingReferanse, registerAktivitet, grunnlag, false).equals(VurderingsStatus.TIL_VURDERING)) {
            builder.medErManueltBehandlet();
        }
    }

}
