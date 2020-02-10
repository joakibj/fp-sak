package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;

import java.util.ArrayList;
import java.util.List;

public final class OmfordelRevurderingsandelerMedReferanser {

    private OmfordelRevurderingsandelerMedReferanser()  {
        // SKjuler default
    }

    public static List<EndringIBeregningsresultat> omfordel(BRNøkkelMedAndeler revurderingNøkkelMedAndeler, BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        List<EndringIBeregningsresultat> list = new ArrayList<>();

        for (BeregningsresultatAndel revurderingAndel : revurderingNøkkelMedAndeler.getAndelerSomHarReferanse()) {
            int reberegnetDagsats = getReberegnetDagsats(revurderingAndel,  revurderingNøkkelMedAndeler, originalNøkkelMedAndeler);
            if (erDagsatsEndret(revurderingAndel, reberegnetDagsats)) {
                EndringIBeregningsresultat endring = new EndringIBeregningsresultat(revurderingAndel, reberegnetDagsats);
                list.add(endring);
            }
        }
        return list;
    }

    private static int getReberegnetDagsats(BeregningsresultatAndel revurderingAndel, BRNøkkelMedAndeler revurderingNøkkelMedAndeler, BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        int originalBrukersDagsats = originalNøkkelMedAndeler.getBrukersAndelMedReferanse(revurderingAndel.getArbeidsforholdRef())
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        int reberegnetDagsats;
        if (revurderingAndel.erBrukerMottaker()) {
            int revurderingArbeidsgiverDagsats = revurderingNøkkelMedAndeler.getArbeidsgiversAndelMedReferanse(revurderingAndel.getArbeidsforholdRef())
                .map(BeregningsresultatAndel::getDagsats)
                .orElse(0);
            reberegnetDagsats = OmfordelDagsats.beregnDagsatsBruker(revurderingAndel.getDagsats(), revurderingArbeidsgiverDagsats, originalBrukersDagsats);
        } else {
            int revurderingBrukersDagsats = revurderingNøkkelMedAndeler.getBrukersAndelMedReferanse(revurderingAndel.getArbeidsforholdRef())
                .map(BeregningsresultatAndel::getDagsats)
                .orElse(0);
            reberegnetDagsats = OmfordelDagsats.beregnDagsatsArbeidsgiver(revurderingAndel.getDagsats(), revurderingBrukersDagsats, originalBrukersDagsats);
        }
        return reberegnetDagsats;
    }

    private static boolean erDagsatsEndret(BeregningsresultatAndel andel, int omberegnetDagsats) {
        return omberegnetDagsats != andel.getDagsats();
    }

}
