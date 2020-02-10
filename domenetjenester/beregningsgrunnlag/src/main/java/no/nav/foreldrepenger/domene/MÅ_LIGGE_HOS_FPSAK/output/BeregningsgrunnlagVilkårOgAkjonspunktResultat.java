package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.output;

import java.util.List;

import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;

public class BeregningsgrunnlagVilkårOgAkjonspunktResultat {
    private List<BeregningAksjonspunktResultat> aksjonspunkter;
    private Boolean vilkårOppfylt;
    private String regelEvalueringVilkårVurdering;
    private String regelInputVilkårVurdering;

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat(List<BeregningAksjonspunktResultat> aksjonspunktResultatListe) {
        this.aksjonspunkter = aksjonspunktResultatListe;
    }

    public List<BeregningAksjonspunktResultat> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public Boolean getVilkårOppfylt() {
        return vilkårOppfylt;
    }

    public void setVilkårOppfylt(Boolean vilkårOppfylt, String regelEvalueringVilkårVurdering, String regelInputVilkårVurdering) {
        this.regelEvalueringVilkårVurdering = regelEvalueringVilkårVurdering;
        this.regelInputVilkårVurdering = regelInputVilkårVurdering;
        this.vilkårOppfylt = vilkårOppfylt;
    }

    public String getRegelEvalueringVilkårVurdering() {
        return regelEvalueringVilkårVurdering;
    }

    public String getRegelInputVilkårVurdering() {
        return regelInputVilkårVurdering;
    }
}
