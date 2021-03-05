package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Oppdrag;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragKjedeFortsettelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.OppdragLinje;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.MottakerOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ØkonomistøtteUtils;

public class OppdragMapper {

    private final Input input;
    private final String fnrBruker;
    private final String ansvarligSaksbehandler;
    private final OverordnetOppdragKjedeOversikt tidligereOppdrag;

    public OppdragMapper(String fnrBruker, OverordnetOppdragKjedeOversikt tidligereOppdrag, Input input) {
        this.fnrBruker = fnrBruker;
        this.tidligereOppdrag = tidligereOppdrag;
        this.input = input;
        this.ansvarligSaksbehandler = input.getAnsvarligSaksbehandler() != null
            ? input.getAnsvarligSaksbehandler()
            : "VL";
    }

    public void mapTilOppdrag110(Oppdrag oppdrag, Oppdragskontroll oppdragskontroll) {
        Oppdrag110.Builder builder = Oppdrag110.builder()
            .medOppdragskontroll(oppdragskontroll)
            .medKodeEndring(utledKodeEndring(oppdrag))
            .medKodeFagomrade(oppdrag.getKodeFagområde())
            .medFagSystemId(Long.parseLong(oppdrag.getFagsystemId().toString()))
            .medOppdragGjelderId(fnrBruker)
            .medSaksbehId(ansvarligSaksbehandler)
            .medAvstemming(Avstemming.ny());

        if (oppdrag.getBetalingsmottaker() == Betalingsmottaker.BRUKER && !oppdragErTilNyMottaker(oppdrag) && !erOpphørForMottaker(oppdrag)) {
            builder.medOmpostering116(opprettOmpostering116(oppdrag, input.brukInntrekk()));
        }

        Oppdrag110 oppdrag110 = builder.build();

        LocalDate maxdatoRefusjon = getMaxdatoRefusjon(oppdrag);

        for (Map.Entry<KjedeNøkkel, OppdragKjedeFortsettelse> entry : oppdrag.getKjeder().entrySet()) {
            for (OppdragLinje oppdragLinje : entry.getValue().getOppdragslinjer()) {
                mapTilOppdragslinje150(oppdrag110, entry.getKey(), oppdragLinje, maxdatoRefusjon, input.getVedtaksdato(), input.getBehandlingId());
            }
        }
    }

    private boolean oppdragErTilNyMottaker(Oppdrag oppdrag) {
        return !tidligereOppdrag.getBetalingsmottakere().contains(oppdrag.getBetalingsmottaker());
    }

    public KodeEndring utledKodeEndring(Oppdrag oppdrag) {
        if (oppdragErTilNyMottaker(oppdrag)) {
            return KodeEndring.NY;
        }
        return KodeEndring.ENDRING;
    }

    Oppdragslinje150 mapTilOppdragslinje150(Oppdrag110 oppdrag110, KjedeNøkkel kjedeNøkkel, OppdragLinje linje, LocalDate maxdatoRefusjon, LocalDate vedtaksdato, Long behandlingId) {
        var builder = Oppdragslinje150.builder()
            .medOppdrag110(oppdrag110)
            .medDelytelseId(Long.valueOf(linje.getDelytelseId().toString()))
            .medKodeKlassifik(kjedeNøkkel.getKlassekode())
            .medVedtakFomOgTom(linje.getPeriode().getFom(), linje.getPeriode().getTom())
            .medSats(Sats.på(linje.getSats().getSats()))
            .medTypeSats(TypeSats.fraKode(linje.getSats().getSatsType().getKode()))
            .medVedtakId(vedtaksdato.toString());

        if (linje.erOpphørslinje()) {
            builder.medKodeEndringLinje(KodeEndringLinje.ENDRING);
            builder.medKodeStatusLinje(KodeStatusLinje.OPPHØR);
            builder.medDatoStatusFom(linje.getOpphørFomDato());
        } else {
            builder.medKodeEndringLinje(KodeEndringLinje.NY);
            if (linje.getRefDelytelseId() != null) {
                builder.medRefDelytelseId(Long.valueOf(linje.getRefDelytelseId().toString()));
                builder.medRefFagsystemId(Long.valueOf(linje.getRefDelytelseId().getFagsystemId().toString()));
            }
        }
        if (kjedeNøkkel.getBetalingsmottaker() == Betalingsmottaker.BRUKER) {
            builder.medUtbetalesTilId(fnrBruker);
        }

        if (linje.getUtbetalingsgrad() != null) {
            builder.medUtbetalingsgrad(Utbetalingsgrad.prosent(linje.getUtbetalingsgrad().getUtbetalingsgrad()));
        }

        Oppdragslinje150 oppdragslinje150 = builder.build();

        if (kjedeNøkkel.getBetalingsmottaker() instanceof Betalingsmottaker.ArbeidsgiverOrgnr) {
            Betalingsmottaker.ArbeidsgiverOrgnr mottaker = (Betalingsmottaker.ArbeidsgiverOrgnr) kjedeNøkkel.getBetalingsmottaker();
            Refusjonsinfo156.builder()
                .medMaksDato(maxdatoRefusjon)
                .medDatoFom(vedtaksdato)
                .medRefunderesId(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getOrgnr()))
                .medOppdragslinje150(oppdragslinje150)
                .build();
        }
        return oppdragslinje150;
    }

    private Ompostering116 opprettOmpostering116(Oppdrag oppdrag, boolean brukInntrekk) {
        Ompostering116.Builder ompostering116Builder = new Ompostering116.Builder()
            .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
            .medOmPostering(brukInntrekk);
        if (brukInntrekk) {
            ompostering116Builder.medDatoOmposterFom(finnDatoOmposterFom(oppdrag));
        }
        return ompostering116Builder.build();
    }

    private LocalDate finnDatoOmposterFom(Oppdrag oppdrag) {
        LocalDate endringsdato = oppdrag.getEndringsdato();
        LocalDate korrigeringsdato = hentFørsteUttaksdato(oppdrag);
        return korrigeringsdato.isAfter(endringsdato)
            ? korrigeringsdato
            : endringsdato;
    }

    public LocalDate hentFørsteUttaksdato(Oppdrag nyttOppdrag) {
        MottakerOppdragKjedeOversikt tidligerOppdragForMottaker = tidligereOppdrag.filter(nyttOppdrag.getBetalingsmottaker());
        MottakerOppdragKjedeOversikt utvidetMedNyttOppdrag = tidligerOppdragForMottaker.utvidMed(nyttOppdrag);
        LocalDate førsteUtbetalingsdato = hentFørsteUtbetalingsdato(tidligerOppdragForMottaker);
        if (førsteUtbetalingsdato != null) {
            return førsteUtbetalingsdato;
        }
        return hentFørsteUtbetalingsdato(utvidetMedNyttOppdrag);
    }

    private LocalDate hentFørsteUtbetalingsdato(MottakerOppdragKjedeOversikt oppdrag) {
        LocalDate førsteUtetalingsdato = null;
        for (Map.Entry<KjedeNøkkel, OppdragKjede> entry : oppdrag.getKjeder().entrySet()) {
            KjedeNøkkel nøkkel = entry.getKey();
            if (nøkkel.getKlassekode().gjelderFeriepenger()) {
                continue;
            }
            OppdragKjede kjede = entry.getValue();
            List<YtelsePeriode> perioder = kjede.tilYtelse().getPerioder();
            if (!perioder.isEmpty()) {
                YtelsePeriode førstePeriode = perioder.get(0);
                LocalDate fom = førstePeriode.getPeriode().getFom();
                if (førsteUtetalingsdato == null || fom.isBefore(førsteUtetalingsdato)) {
                    førsteUtetalingsdato = fom;
                }
            }
        }
        return førsteUtetalingsdato;
    }


    private LocalDate getMaxdatoRefusjon(Oppdrag nyttOppdrag) {
        MottakerOppdragKjedeOversikt tidligerOppdragForMottaker = tidligereOppdrag.filter(nyttOppdrag.getBetalingsmottaker());
        MottakerOppdragKjedeOversikt utvidetMedNyttOppdrag = tidligerOppdragForMottaker.utvidMed(nyttOppdrag);
        LocalDate sisteUtbetalingsdato = hentSisteUtbetalingsdato(utvidetMedNyttOppdrag);
        if (sisteUtbetalingsdato != null) {
            return sisteUtbetalingsdato;
        }
        //usikker på hvorfor... men ved opphør brukes siste utbetalingsdato for forrige oppdrag
        return hentSisteUtbetalingsdato(tidligerOppdragForMottaker);
    }

    private LocalDate hentSisteUtbetalingsdato(MottakerOppdragKjedeOversikt oppdrag) {
        LocalDate sisteUtbetalingsdato = null;
        for (Map.Entry<KjedeNøkkel, OppdragKjede> entry : oppdrag.getKjeder().entrySet()) {
            KjedeNøkkel nøkkel = entry.getKey();
            if (nøkkel.getKlassekode().gjelderFeriepenger()) {
                continue;
            }
            OppdragKjede kjede = entry.getValue();
            List<YtelsePeriode> perioder = kjede.tilYtelse().getPerioder();
            if (!perioder.isEmpty()) {
                YtelsePeriode sistePeriode = perioder.get(perioder.size() - 1);
                LocalDate tom = sistePeriode.getPeriode().getTom();
                if (sisteUtbetalingsdato == null || tom.isAfter(sisteUtbetalingsdato)) {
                    sisteUtbetalingsdato = tom;
                }
            }
        }
        return sisteUtbetalingsdato;
    }

    private boolean erOpphørForMottaker(Oppdrag nyttOppdrag) {
        MottakerOppdragKjedeOversikt tidligerOppdragForMottaker = tidligereOppdrag.filter(nyttOppdrag.getBetalingsmottaker());
        MottakerOppdragKjedeOversikt inklNyttOppdrag = tidligerOppdragForMottaker.utvidMed(nyttOppdrag);
        for (OppdragKjede kjede : inklNyttOppdrag.getKjeder().values()) {
            if (!kjede.tilYtelse().getPerioder().isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
