package no.nav.foreldrepenger.domene.uttak.fakta.aktkrav;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AktivitetskravPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.input.Annenpart;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
public class KontrollerAktivitetskravAksjonspunktUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(KontrollerAktivitetskravAksjonspunktUtleder.class);

    private static final Set<UtsettelseÅrsak> BFHR_MED_AKTIVITETSKRAV = Set.of(UtsettelseÅrsak.ARBEID, UtsettelseÅrsak.FERIE,
        UtsettelseÅrsak.SYKDOM, UtsettelseÅrsak.INSTITUSJON_BARN, UtsettelseÅrsak.INSTITUSJON_SØKER);

    private static final Set<UttakPeriodeType> UTTAK_MED_AKTIVITETSKRAV = Set.of(UttakPeriodeType.FELLESPERIODE, UttakPeriodeType.FORELDREPENGER);

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    @Inject
    public KontrollerAktivitetskravAksjonspunktUtleder(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                       ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
    }

    KontrollerAktivitetskravAksjonspunktUtleder() {
        //CDI
    }

    public List<AksjonspunktDefinisjon> utledFor(UttakInput uttakInput) {
        return utledFor(uttakInput, false);
    }

    public List<AksjonspunktDefinisjon> utledFor(UttakInput uttakInput, boolean logg150) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = uttakInput.getYtelsespesifiktGrunnlag();
        if (skalKontrollereAktivitetskrav(uttakInput.getBehandlingReferanse(),
            ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse(), ytelsespesifiktGrunnlag, logg150)) {
            return List.of(AksjonspunktDefinisjon.KONTROLLER_AKTIVITETSKRAV);
        }
        return List.of();
    }

    public static SkalKontrollereAktiviteskravResultat skalKontrollereAktivitetskrav(BehandlingReferanse behandlingReferanse,
                                                                                     OppgittPeriodeEntitet periode,
                                                                                     YtelseFordelingAggregat ytelseFordelingAggregat,
                                                                                     FamilieHendelse familieHendelse,
                                                                                     boolean annenForelderHarRett,
                                                                                     List<ForeldrepengerUttakPeriode> annenpartFullMK,
                                                                                     boolean logg150) {
        if (helePeriodenErHelg(periode) || erMor(behandlingReferanse) || UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat) ||
            familieHendelse.erStebarnsadopsjon() || Set.of(MorsAktivitet.UFØRE, MorsAktivitet.IKKE_OPPGITT).contains(periode.getMorsAktivitet()) ||
            ytelseFordelingAggregat.getGjeldendeEndringsdatoHvisEksisterer().isEmpty()) {
            return ikkeKontrollerer();
        }
        var harKravTilAktivitet = !periode.isFlerbarnsdager() &&
            (UTTAK_MED_AKTIVITETSKRAV.contains(periode.getPeriodeType()) || bareFarHarRettOgSøkerUtsettelse(periode, annenForelderHarRett));
        if (!harKravTilAktivitet) {
            return ikkeKontrollerer();
        }
        // Pgf 14-12 andre ledd - samtidig 100% MK + <= 50% Fellesperiode -> ikke aktivitetskrav. To be elaborated further ....
        if (logg150 && erTilfelleAv150ProsentSamtidig(behandlingReferanse, periode, annenpartFullMK)) {
            LOG.info("Aktivitetskravutleder behandling {} periode fom {} dekkes av full MK", behandlingReferanse.behandlingId(), periode.getFom());
            //return ikkeKontrollerer();
        }

        var avklaring = finnAvklartePerioderSomDekkerSøknadsperiode(periode, ytelseFordelingAggregat);
        return new SkalKontrollereAktiviteskravResultat(true, avklaring);
    }

    private static boolean helePeriodenErHelg(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) == 0;
    }

    private static SkalKontrollereAktiviteskravResultat ikkeKontrollerer() {
        return new SkalKontrollereAktiviteskravResultat(false, Set.of());
    }

    private static boolean bareFarHarRettOgSøkerUtsettelse(OppgittPeriodeEntitet periode,
                                                           boolean annenForelderHarRett) {
        // Reglene sjekker ikke aktivitetskrav hvis tiltak nav eller hv
        return !annenForelderHarRett && (BFHR_MED_AKTIVITETSKRAV.contains(periode.getÅrsak()) ||
            (UtsettelseÅrsak.FRI.equals(periode.getÅrsak()) && MorsAktivitet.forventerDokumentasjon(periode.getMorsAktivitet())));
    }

    private static Set<AktivitetskravPeriodeEntitet> finnAvklartePerioderSomDekkerSøknadsperiode(OppgittPeriodeEntitet periode,
                                                                                                 YtelseFordelingAggregat ytelseFordelingAggregat) {
        var avklartePerioder = finnRelevanteAvklartePerioder(ytelseFordelingAggregat);
        var dekkendeAvklartePerioder = new HashSet<AktivitetskravPeriodeEntitet>();
        var dato = periode.getFom();
        do {
            if (!erHelg(dato)) {
                var dekkendeAvklartPeriode = finnDekkendePeriode(dato, avklartePerioder);
                if (dekkendeAvklartPeriode.isEmpty()) {
                    return Set.of();
                }
                dekkendeAvklartePerioder.add(dekkendeAvklartPeriode.get());
            }
            dato = dato.plusDays(1);
        } while (!dato.isAfter(periode.getTom()));
        return dekkendeAvklartePerioder;
    }

    private static List<AktivitetskravPeriodeEntitet> finnRelevanteAvklartePerioder(YtelseFordelingAggregat ytelseFordelingAggregat) {
        if (ytelseFordelingAggregat.getSaksbehandledeAktivitetskravPerioder().isPresent()) {
            return ytelseFordelingAggregat.getSaksbehandledeAktivitetskravPerioder().get().getPerioder();
        }
        return ytelseFordelingAggregat.getOpprinneligeAktivitetskravPerioder()
            .stream()
            .flatMap(perioder -> perioder.getPerioder().stream())
            .flatMap(p -> {
                //Behandlinger med nye oppgitte perioder (endringssøknader) må avklare aktivitetskrav uansett avklaringer gjort i tidligere behandlinger
                if (inneholderNyePerioder(ytelseFordelingAggregat)) {
                    return fjernPerioderEtterDato(p, ytelseFordelingAggregat.getGjeldendeEndringsdato()).stream();
                }
                return Stream.of(p);
            })
            .collect(Collectors.toList());
    }

    private static boolean inneholderNyePerioder(YtelseFordelingAggregat ytelseFordelingAggregat) {
        return ytelseFordelingAggregat.getOppgittFordeling() != null && !ytelseFordelingAggregat.getOppgittFordeling()
            .getOppgittePerioder()
            .isEmpty();
    }

    private static List<AktivitetskravPeriodeEntitet> fjernPerioderEtterDato(AktivitetskravPeriodeEntitet periode,
                                                                             LocalDate dato) {
        if (periode.getTidsperiode().inkluderer(dato) && !periode.getTidsperiode().getFomDato().isEqual(dato)) {
            return List.of(new AktivitetskravPeriodeEntitet(periode.getTidsperiode().getFomDato(), dato.minusDays(1),
                periode.getAvklaring(), periode.getBegrunnelse()));
        }
        if (!periode.getTidsperiode().getFomDato().isBefore(dato)) {
            return List.of();
        }
        return List.of(periode);
    }

    private static boolean erHelg(LocalDate dato) {
        return dato.getDayOfWeek() == DayOfWeek.SATURDAY || dato.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private static Optional<AktivitetskravPeriodeEntitet> finnDekkendePeriode(LocalDate dato,
                                                                              List<AktivitetskravPeriodeEntitet> avklartePerioder) {
        return avklartePerioder.stream().filter(p -> p.getTidsperiode().inkluderer(dato)).findFirst();
    }

    private boolean skalKontrollereAktivitetskrav(BehandlingReferanse behandlingReferanse,
                                                  FamilieHendelse familieHendelse,
                                                  ForeldrepengerGrunnlag fpGrunnlag, boolean logg150) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingReferanse.behandlingId());
        var annenpartUttak = fpGrunnlag.getAnnenpart().map(Annenpart::gjeldendeVedtakBehandlingId)
            .flatMap(apVedtak -> foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(apVedtak));
        var annenpartFullMK = annenpartsHundreprosentMødrekvote(annenpartUttak);
        var annenforelderRett = UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, annenpartUttak);
        return ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder().stream().anyMatch(p -> {
            var resultat = skalKontrollereAktivitetskrav(behandlingReferanse, p, ytelseFordelingAggregat,
                familieHendelse, annenforelderRett, annenpartFullMK, logg150);
            return resultat.kravTilAktivitet() && !resultat.isAvklart();
        });
    }

    private static boolean erMor(BehandlingReferanse behandlingReferanse) {
        return RelasjonsRolleType.erMor(behandlingReferanse.relasjonRolle());
    }

    public static List<ForeldrepengerUttakPeriode> annenpartsHundreprosentMødrekvote(Optional<ForeldrepengerUttak> uttak) {
        return uttak.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(List.of()).stream()
            .filter(KontrollerAktivitetskravAksjonspunktUtleder::periodeErHundreprosentMødrekvote)
            .toList();
    }

    private static boolean periodeErHundreprosentMødrekvote(ForeldrepengerUttakPeriode periode) {
        return periode.getAktiviteter().stream()
            .allMatch(a -> StønadskontoType.MØDREKVOTE.equals(a.getTrekkonto()) && a.getUtbetalingsgrad().compareTo(Utbetalingsgrad.HUNDRED) >= 0);
    }

    private static boolean erTilfelleAv150ProsentSamtidig(BehandlingReferanse ref, OppgittPeriodeEntitet periode, List<ForeldrepengerUttakPeriode> annenpartFullMK) {
        var samtidigEllerGradert = Optional.ofNullable(periode.getSamtidigUttaksprosent())
            .or(() -> Optional.ofNullable(periode.getArbeidsprosent())
                .map(ap -> BigDecimal.valueOf(100).subtract(ap)) // Antar at samtidiguttak% = 100 - Arbeidsprosent
                .map(SamtidigUttaksprosent::new));
        if (UttakPeriodeType.FELLESPERIODE.equals(periode.getPeriodeType()) && !annenpartFullMK.isEmpty() &&
            samtidigEllerGradert.filter(pct -> pct.compareTo(new SamtidigUttaksprosent(50)) <= 0).isPresent()) {
            var dekkesAvFullMK = dekkesavSamtidigfullMK(annenpartFullMK, periode);
            if (dekkesAvFullMK) {
                return true;
            } else {
                LOG.info("Aktivitetskravutleder behandling {} periode fom {} ikke dekket av MK", ref.behandlingId(), periode.getFom());
            }
        }
        return false;
    }

    private static boolean dekkesavSamtidigfullMK(List<ForeldrepengerUttakPeriode> annenpartFullMK, OppgittPeriodeEntitet periode) {
        var segmenter = annenpartFullMK.stream()
            .filter(p -> periode.isSamtidigUttak() || p.isSamtidigUttak())
            .map(p -> new LocalDateSegment<>(p.getFom(), VirkedagUtil.fredagLørdagTilSøndag(p.getTom()), Boolean.TRUE))
            .toList();
        if (segmenter.isEmpty()) {
            return false;
        }
        var oppgittIntervall = new LocalDateInterval(periode.getFom(), periode.getTom());
        return new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch).compress().toSegments().stream()
            .anyMatch(seg -> seg.getLocalDateInterval().contains(oppgittIntervall));
    }

}
