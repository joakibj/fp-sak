package no.nav.foreldrepenger.domene.uttak.fakta.uttak;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_INGEN_PERIODER;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.FRILANS;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.uttak.PerioderUtenHelgUtil;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class FaktaUttakAksjonspunktUtleder {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @Inject
    public FaktaUttakAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    FaktaUttakAksjonspunktUtleder() {
        //CDI
    }

    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(input.getBehandlingReferanse().behandlingId());
        return utledAksjonspunkterFor(input, ytelseFordelingAggregat.getJustertFordeling().orElseThrow().getPerioder());
    }

    public List<AksjonspunktDefinisjon> utledAksjonspunkterFor(UttakInput input, List<OppgittPeriodeEntitet> perioder) {

        var list = new ArrayList<AksjonspunktDefinisjon>();
        if (perioder.isEmpty()) {
            list.add(FAKTA_UTTAK_INGEN_PERIODER);
        }
        if (input.finnesAndelerMedGraderingUtenBeregningsgrunnlag()) {
            list.add(FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG);
        }
        if (graderingPåUkjentAktivitet(input.getBeregningsgrunnlagStatuser(), perioder)) {
            list.add(FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET);
        }
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(input.getBehandlingReferanse().behandlingId());
        if (!avklartStartdatLikFørsteDagIPerioder(perioder,
            ytelseFordelingAggregat.getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getFørsteUttaksdato))) {
            list.add(FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO);
        }
        return list;
    }

    private static boolean avklartStartdatLikFørsteDagIPerioder(List<OppgittPeriodeEntitet> perioder, Optional<LocalDate> avklartFUD) {
        return avklartFUD.map(localDate -> førsteSøkteDag(perioder).map(
            oppgittPeriode -> PerioderUtenHelgUtil.datoerLikeNårHelgIgnoreres(oppgittPeriode.getFom(), localDate)).orElse(true)).orElse(true);
    }

    private static Optional<OppgittPeriodeEntitet> førsteSøkteDag(List<OppgittPeriodeEntitet> perioder) {
        return perioder
            .stream()
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom));
    }

    private static boolean graderingPåUkjentAktivitet(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser, List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(periode -> periode.isGradert())
            .anyMatch(periode -> gradererUkjentAktivitet(periode, beregningsgrunnlagStatuser));
    }

    private static boolean gradererUkjentAktivitet(OppgittPeriodeEntitet søknadsperiode, Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        if (søknadsperiode.getGraderingAktivitetType() == ARBEID && !beregningHarArbeidsgiver(søknadsperiode.getArbeidsgiver(), beregningsgrunnlagStatuser)) {
            return true;
        }
        if (søknadsperiode.getGraderingAktivitetType() == FRILANS && !beregningHarFrilanser(beregningsgrunnlagStatuser)) {
            return true;
        }
        return søknadsperiode.getGraderingAktivitetType() == SELVSTENDIG_NÆRINGSDRIVENDE && !beregningHarSelvstendigNæringsdrivende(beregningsgrunnlagStatuser);
    }

    private static boolean beregningHarSelvstendigNæringsdrivende(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        return beregningsgrunnlagStatuser.stream().anyMatch(bs -> bs.erSelvstendigNæringsdrivende());
    }

    private static boolean beregningHarFrilanser(Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        return beregningsgrunnlagStatuser.stream().anyMatch(bs -> bs.erFrilanser());
    }

    private static boolean beregningHarArbeidsgiver(Arbeidsgiver arbeidsgiver, Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser) {
        if (arbeidsgiver == null) {
            return beregningsgrunnlagStatuser.stream().anyMatch(statusPeriode -> statusPeriode.erArbeidstaker());
        }
        return beregningsgrunnlagStatuser.stream().anyMatch(statusPeriode ->
            statusPeriode.erArbeidstaker()
                && statusPeriode.getArbeidsgiver().isPresent()
                && statusPeriode.getArbeidsgiver().get().equals(arbeidsgiver)
        );
    }
}