package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.FaktaOmBeregningTilfelleRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.VurderLønnsendringDto;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_LØNNSENDRING")
public class VurderLønnsendringHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (!dto.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING)) {
            return;
        }
        VurderLønnsendringDto lønnsendringDto = dto.getVurdertLonnsendring();
        Boolean opprinneligVerdiErLønnsendring = hentOpprinneligVerdiErLønnsendring(forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        lagHistorikkinnslag(lønnsendringDto, opprinneligVerdiErLønnsendring, tekstBuilder);
    }


    private Boolean hentOpprinneligVerdiErLønnsendring(Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return forrigeBg.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(bgAndelArbeidsforhold -> bgAndelArbeidsforhold.erLønnsendringIBeregningsperioden() != null)
            .map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden)
            .findFirst()
            .orElse(null);
    }

    private void lagHistorikkinnslag(VurderLønnsendringDto dto, Boolean opprinneligVerdiErLønnsendring, HistorikkInnslagTekstBuilder tekstBuilder) {
        if (!dto.erLønnsendringIBeregningsperioden().equals(opprinneligVerdiErLønnsendring)) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.LØNNSENDRING_I_PERIODEN, opprinneligVerdiErLønnsendring, dto.erLønnsendringIBeregningsperioden());
        }
    }
}
