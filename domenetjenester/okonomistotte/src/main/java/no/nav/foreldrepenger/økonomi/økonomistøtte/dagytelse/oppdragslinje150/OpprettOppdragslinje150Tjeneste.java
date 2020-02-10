package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.OppdragskontrollConstants;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.VurderFeriepengerBeregning;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.OppdragsmottakerInfo;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

public class OpprettOppdragslinje150Tjeneste {

    private static final int INITIAL_TELLER = 100;
    private static final int INITIAL_COUNT = 0;

    private OpprettOppdragslinje150Tjeneste() {
        // skjul default constructor
    }

    public static List<Oppdragslinje150> opprettOppdragslinje150(OppdragInput behandlingInfo, Oppdrag110 oppdrag110,
                                                                 List<TilkjentYtelseAndel> andelListe, Oppdragsmottaker mottaker) {
        List<String> klassekodeListe = KlassekodeUtleder.getKlassekodeListe(andelListe);
        if (mottaker.erBruker() && klassekodeListe.size() > 1) {
            List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode = gruppereAndelerMedKlassekode(andelListe);
            return opprettOppdr150ForBrukerMedFlereKlassekode(behandlingInfo, oppdrag110,
                andelerGruppertMedKlassekode, mottaker, Collections.emptyList());
        }
        return opprettOppdragslinje150(behandlingInfo, oppdrag110, andelListe, mottaker, null);
    }

    public static List<Oppdragslinje150> opprettOppdr150ForBrukerMedFlereKlassekode(OppdragInput behandlingInfo,
                                                                                    Oppdrag110 oppdrag110,
                                                                                    List<List<TilkjentYtelseAndel>> andelerGruppertMedKlassekode,
                                                                                    Oppdragsmottaker mottaker, List<Oppdragslinje150> tidligereOppdr150Liste) {
        List<Oppdragslinje150> oppdrlinje150Liste = new ArrayList<>();
        int teller = INITIAL_TELLER;
        int count = INITIAL_COUNT;
        for (List<TilkjentYtelseAndel> andelListe : andelerGruppertMedKlassekode) {
            List<Long> delYtelseIdListe = new ArrayList<>();
            for (TilkjentYtelseAndel andel : andelListe) {
                List<Oppdrag110> alleTidligereOppdrag110ForMottaker = finnOpprag110ForGittFagsystemId(behandlingInfo, oppdrag110);
                OppdragsmottakerInfo oppdragInfo = new OppdragsmottakerInfo(mottaker, andel, alleTidligereOppdrag110ForMottaker, tidligereOppdr150Liste);
                Oppdragslinje150 oppdragslinje150 = opprettOppdragslinje150FørsteOppdrag(behandlingInfo, oppdragInfo, oppdrag110,
                    delYtelseIdListe, count, teller++);
                int grad = andel.getUtbetalingsgrad().setScale(0, RoundingMode.HALF_EVEN).intValue();
                OpprettOppdragsmeldingerRelatertTil150.opprettGrad170(oppdragslinje150, grad);
                oppdrlinje150Liste.add(oppdragslinje150);
            }
            count = count + andelListe.size();
        }
        long sisteSattDelYtelseId = oppdrlinje150Liste.get(oppdrlinje150Liste.size() - 1).getDelytelseId();
        if (tidligereOppdr150Liste.isEmpty() || VurderFeriepengerBeregning.erFeriepengerEndret(behandlingInfo, tidligereOppdr150Liste, mottaker)) {
            List<Oppdragslinje150> opp150FeriepengerList = OpprettOppdragslinje150FeriepengerTjeneste.lagOppdragslinje150ForFeriepenger(behandlingInfo,
                oppdrag110, mottaker, tidligereOppdr150Liste, sisteSattDelYtelseId);
            oppdrlinje150Liste.addAll(opp150FeriepengerList);
        }
        return oppdrlinje150Liste;
    }

    public static List<Oppdragslinje150> opprettOppdragslinje150(OppdragInput behandlingInfo, Oppdrag110 nyOppdrag110,
                                                                 List<TilkjentYtelseAndel> andelerListe, Oppdragsmottaker mottaker,
                                                                 Oppdragslinje150 sisteOppdr150) {
        List<Oppdragslinje150> oppdrlinje150Liste = new ArrayList<>();
        List<Long> delYtelseIdListe = new ArrayList<>();

        int teller = INITIAL_TELLER;
        List<Oppdragslinje150> tidligereOppdr150Liste = sisteOppdr150 != null ? Collections.singletonList(sisteOppdr150) : Collections.emptyList();
        for (TilkjentYtelseAndel andel : andelerListe) {
            List<Oppdrag110> alleTidligereOppdrag110ForMottaker = finnOpprag110ForGittFagsystemId(behandlingInfo, nyOppdrag110);
            OppdragsmottakerInfo oppdragInfo = new OppdragsmottakerInfo(mottaker, andel, alleTidligereOppdrag110ForMottaker, tidligereOppdr150Liste);
            Oppdragslinje150 oppdragslinje150 = opprettOppdragslinje150FørsteOppdrag(behandlingInfo, oppdragInfo, nyOppdrag110, delYtelseIdListe, teller++);
            int grad = andel.getUtbetalingsgrad().setScale(0, RoundingMode.HALF_EVEN).intValue();
            OpprettOppdragsmeldingerRelatertTil150.opprettGrad170(oppdragslinje150, grad);
            if (!mottaker.erBruker()) {
                LocalDate maksDato = FinnMottakerInfoITilkjentYtelse.finnSisteDagMedUtbetalingTilMottaker(behandlingInfo, mottaker);
                OpprettOppdragsmeldingerRelatertTil150.opprettRefusjonsinfo156(behandlingInfo, oppdragslinje150, mottaker, maksDato);
            }
            oppdrlinje150Liste.add(oppdragslinje150);
        }
        long sisteSattDelYtelseId = delYtelseIdListe.get(delYtelseIdListe.size() - 1);
        if (tidligereOppdr150Liste.isEmpty() || VurderFeriepengerBeregning.erFeriepengerEndret(behandlingInfo, tidligereOppdr150Liste, mottaker)) {
            List<Oppdragslinje150> opp150FeriepengerList = OpprettOppdragslinje150FeriepengerTjeneste.lagOppdragslinje150ForFeriepenger(behandlingInfo,
                nyOppdrag110, mottaker, tidligereOppdr150Liste, sisteSattDelYtelseId);
            OpprettOppdragslinje150FeriepengerTjeneste.kobleAndreMeldingselementerTilOpp150NyFeriepenger(behandlingInfo, opp150FeriepengerList, mottaker);
            oppdrlinje150Liste.addAll(opp150FeriepengerList);
        }
        return oppdrlinje150Liste;
    }

    private static Oppdragslinje150 opprettOppdragslinje150FørsteOppdrag(OppdragInput behandlingInfo, OppdragsmottakerInfo oppdragInfo, Oppdrag110 nyOppdrag110,
                                                                         List<Long> delYtelseIdListe, int teller) {
        return opprettOppdragslinje150FørsteOppdrag(behandlingInfo, oppdragInfo, nyOppdrag110,
            delYtelseIdListe, INITIAL_COUNT, teller);
    }

    private static Oppdragslinje150 opprettOppdragslinje150FørsteOppdrag(OppdragInput behandlingInfo, OppdragsmottakerInfo oppdragInfo, Oppdrag110 nyOppdrag110,
                                                                         List<Long> delYtelseIdListe, int count, int teller) {

        Oppdragslinje150.Builder oppdragslinje150Builder = opprettOppdragslinje150Builder(behandlingInfo, oppdragInfo, nyOppdrag110);

        List<Oppdragslinje150> tidligereOppdr150Liste = oppdragInfo.getTidligereOppdr150MottakerListe();
        if (tidligereOppdr150Liste.isEmpty()) {
            UtledDelytelseOgFagsystemIdI150.settRefDelytelseOgFagsystemId(nyOppdrag110, delYtelseIdListe, count, teller, oppdragslinje150Builder);
        } else {
            int antallIter = teller - (INITIAL_TELLER + count);
            UtledDelytelseOgFagsystemIdI150.settRefDelytelseOgFagsystemId(oppdragInfo, nyOppdrag110, oppdragslinje150Builder, delYtelseIdListe, antallIter);
        }
        return oppdragslinje150Builder.build();
    }

    private static Oppdragslinje150.Builder opprettOppdragslinje150Builder(OppdragInput behandlingInfo, OppdragsmottakerInfo oppdragInfo, Oppdrag110 oppdrag110) {
        TilkjentYtelseAndel andel = oppdragInfo.getTilkjentYtelseAndel();
        Oppdragsmottaker mottaker = oppdragInfo.getMottaker();

        LocalDate vedtakFom = andel.getOppdragPeriodeFom();
        LocalDate vedtakTom = andel.getOppdragPeriodeTom();
        String kodeKlassifik = KlassekodeUtleder.utled(andel);
        int dagsats = andel.getDagsats();

        Oppdragslinje150.Builder oppdragslinje150Builder = Oppdragslinje150.builder();
        settFellesFelterIOppdr150(behandlingInfo, oppdragslinje150Builder, false, false);
        oppdragslinje150Builder.medKodeKlassifik(kodeKlassifik)
            .medOppdrag110(oppdrag110)
            .medVedtakFomOgTom(vedtakFom, vedtakTom)
            .medSats(dagsats);
        if (mottaker.erBruker()) {
            oppdragslinje150Builder.medUtbetalesTilId(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getId()));
        }
        return oppdragslinje150Builder;
    }

    private static List<Oppdrag110> finnOpprag110ForGittFagsystemId(OppdragInput behandlingInfo, Oppdrag110 oppdrag110) {
        return behandlingInfo.getAlleTidligereOppdrag110()
            .stream()
            .filter(tidligere110 -> tidligere110.getFagsystemId() == oppdrag110.getFagsystemId())
            .collect(Collectors.toList());
    }

    public static List<TilkjentYtelseAndel> hentForrigeTilkjentYtelseAndeler(OppdragInput behandlingInfo) {
        return behandlingInfo.getForrigeTilkjentYtelsePerioder()
            .stream()
            .sorted(Comparator.comparing(TilkjentYtelsePeriode::getFom))
            .map(TilkjentYtelsePeriode::getTilkjentYtelseAndeler)
            .flatMap(List::stream)
            .filter(a -> a.getDagsats() > 0)
            .collect(Collectors.toList());
    }

    public static boolean finnesFlereKlassekodeIForrigeOppdrag(OppdragInput behandlingInfo) {
        List<Oppdragslinje150> tidligereOpp150Liste = TidligereOppdragTjeneste.hentTidligereGjeldendeOppdragslinje150(
            behandlingInfo, false);

        return tidligereOpp150Liste.stream().map(Oppdragslinje150::getKodeKlassifik).distinct().count() > 1L;
    }

    public static List<List<TilkjentYtelseAndel>> gruppereAndelerMedKlassekode(List<TilkjentYtelseAndel> andelListe) {
        Map<String, List<TilkjentYtelseAndel>> andelPrKlassekodeMap = new LinkedHashMap<>();
        List<String> klassekodeListe = KlassekodeUtleder.getKlassekodeListe(andelListe);
        for (String klassekode : klassekodeListe) {
            List<TilkjentYtelseAndel> andelerMedSammeKlassekode = andelListe.stream()
                .filter(andel -> KlassekodeUtleder.utled(andel).equals(klassekode))
                .sorted(Comparator.comparing(TilkjentYtelseAndel::getOppdragPeriodeFom))
                .collect(Collectors.toList());
            andelPrKlassekodeMap.put(klassekode, andelerMedSammeKlassekode);
        }
        return new ArrayList<>(andelPrKlassekodeMap.values());
    }

    public static void settFellesFelterIOppdr150(OppdragInput behandlingInfo, Oppdragslinje150.Builder oppdr150Builder, boolean gjelderOpphør, boolean gjelderFeriepenger) {
        LocalDate vedtaksdato = behandlingInfo.getVedtaksdato();
        String kodeEndringLinje = gjelderOpphør ? OppdragskontrollConstants.KODE_ENDRING_LINJE_ENDRING : OppdragskontrollConstants.KODE_ENDRING_LINJE_NY;
        String typeSats = gjelderFeriepenger ? OppdragskontrollConstants.TYPE_SATS_FERIEPENGER : OppdragskontrollConstants.TYPE_SATS_DAG;
        if (gjelderOpphør) {
            oppdr150Builder.medKodeStatusLinje(OppdragskontrollConstants.KODE_STATUS_LINJE_OPPHØR);
        }
        oppdr150Builder.medKodeEndringLinje(kodeEndringLinje)
            .medVedtakId(vedtaksdato.toString())
            .medFradragTillegg(OppdragskontrollConstants.FRADRAG_TILLEGG)
            .medBrukKjoreplan(OppdragskontrollConstants.BRUK_KJOREPLAN)
            .medSaksbehId(behandlingInfo.getAnsvarligSaksbehandler())
            .medHenvisning(behandlingInfo.getBehandlingId())
            .medTypeSats(typeSats);
    }
}
