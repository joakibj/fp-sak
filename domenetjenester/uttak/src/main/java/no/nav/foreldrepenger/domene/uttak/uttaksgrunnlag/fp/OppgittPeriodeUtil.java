package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;

class OppgittPeriodeUtil {

    private OppgittPeriodeUtil() {
        //Forhindrer instanser
    }

    static boolean finnesOverlapp(List<OppgittPeriodeEntitet> oppgittPerioder) {
        for (var i = 0; i < oppgittPerioder.size(); i++) {
            for (var j = i + 1; j < oppgittPerioder.size(); j++) {
                if (overlapper(oppgittPerioder.get(i), oppgittPerioder.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean overlapper(OppgittPeriodeEntitet p1, OppgittPeriodeEntitet p2) {
        return new SimpleLocalDateInterval(p1.getFom(), p1.getTom()).overlapper(new SimpleLocalDateInterval(p2.getFom(), p2.getTom()));
    }

    static List<OppgittPeriodeEntitet> sorterEtterFom(List<OppgittPeriodeEntitet> oppgittePerioder) {
        return oppgittePerioder.stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).collect(Collectors.toList());
    }

    /**
     * Finn første dato fra søknad som ikke er en utsettelse.
     *
     * @param oppgittePerioder
     * @return første dato fra søknad som ikke er en utsettelse.
     */
    static Optional<LocalDate> finnFørsteSøkteUttaksdato(List<OppgittPeriodeEntitet> oppgittePerioder) {
        var sortertePerioder = sorterEtterFom(oppgittePerioder);
        var perioderMedUttak = sortertePerioder
            .stream()
            .filter(p -> Årsak.UKJENT.equals(p.getÅrsak()) || !p.isOpphold())
            .collect(Collectors.toList());

        if(perioderMedUttak.size() > 0) {
            return Optional.of(perioderMedUttak.get(0).getFom());
        }

        return Optional.empty();
    }

    static Optional<LocalDate> finnFørsteSøknadsdato(List<OppgittPeriodeEntitet> perioder) {
        var sortertePerioder = sorterEtterFom(perioder);

        if(sortertePerioder.size() > 0) {
            return Optional.of(sortertePerioder.get(0).getFom());
        }

        return Optional.empty();
    }

    static List<OppgittPeriodeEntitet> slåSammenLikePerioder(List<OppgittPeriodeEntitet> perioder) {
        List<OppgittPeriodeEntitet> resultat = new ArrayList<>();

        var i = 0;
        while (i < perioder.size()) {
            var j = i + 1;
            var slåttSammen = perioder.get(i);
            if (i < perioder.size() - 1) {
                //Hvis ikke hull mellom periodene skal vi se om de er like for å så slå de sammen
                while (j < perioder.size()) {
                    var nestePeriode = perioder.get(j);
                    if (!erHullMellom(slåttSammen.getTom(), nestePeriode.getFom()) && erLikBortsettFraTidsperiode(slåttSammen, nestePeriode)) {
                        slåttSammen = slåSammen(slåttSammen, nestePeriode);
                    } else {
                        break;
                    }
                    j++;
                }
            }
            resultat.add(slåttSammen);
            i = j;
        }
        return resultat;
    }

    private static boolean erLikBortsettFraTidsperiode(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        //begrunnelse ikke viktig å se på
        return Objects.equals(periode1.isArbeidstaker(), periode2.isArbeidstaker()) &&
            Objects.equals(periode1.isFrilanser(), periode2.isFrilanser()) &&
            Objects.equals(periode1.isSelvstendig(), periode2.isSelvstendig()) &&
            Objects.equals(periode1.isFlerbarnsdager(), periode2.isFlerbarnsdager()) &&
            Objects.equals(periode1.isSamtidigUttak(), periode2.isSamtidigUttak()) &&
            Objects.equals(periode1.getArbeidsgiver(), periode2.getArbeidsgiver()) &&
            Objects.equals(periode1.getMorsAktivitet(), periode2.getMorsAktivitet()) &&
            Objects.equals(periode1.isVedtaksperiode(), periode2.isVedtaksperiode()) &&
            Objects.equals(periode1.getPeriodeType(), periode2.getPeriodeType()) &&
            Objects.equals(periode1.getPeriodeVurderingType(), periode2.getPeriodeVurderingType()) &&
            Objects.equals(periode1.getSamtidigUttaksprosent(), periode2.getSamtidigUttaksprosent()) &&
            Objects.equals(periode1.getMottattDato(), periode2.getMottattDato()) &&
            Objects.equals(periode1.getTidligstMottattDato(), periode2.getTidligstMottattDato()) &&
            Objects.equals(periode1.getÅrsak(), periode2.getÅrsak()) &&
            Objects.equals(periode1.getArbeidsprosent(), periode2.getArbeidsprosent());
    }

    private static OppgittPeriodeEntitet slåSammen(OppgittPeriodeEntitet periode1, OppgittPeriodeEntitet periode2) {
        return kopier(periode1, periode1.getFom(), periode2.getTom());
    }

    static boolean erHullMellom(LocalDate date1, LocalDate date2) {
        return Virkedager.plusVirkedager(date1, 1).isBefore(date2);
    }

    static OppgittPeriodeEntitet kopier(OppgittPeriodeEntitet oppgittPeriode, LocalDate nyFom, LocalDate nyTom) {
        if (oppgittPeriode instanceof JusterFordelingTjeneste.JusterPeriodeHull) {
            return new JusterFordelingTjeneste.JusterPeriodeHull(nyFom, nyTom);
        }
        return OppgittPeriodeBuilder.fraEksisterende(oppgittPeriode).medPeriode(nyFom, nyTom).build();
    }

}
