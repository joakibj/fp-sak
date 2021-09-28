package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulator.output.BeregningAvklaringsbehovResultat;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningVenteårsak;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BeregningAksjonspunktResultatMapperTest {


    @Test
    public void tester_mapping_av_vanlig_aksjonspunkt() {
        var beregningAp = BeregningAvklaringsbehovResultat.opprettFor(AvklaringsbehovDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
        AksjonspunktResultat ap = BeregningAksjonspunktResultatMapper.map(beregningAp);
        assertThat(ap.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
        assertThat(ap.getVenteårsak()).isNull();
    }

    @Test
    public void tester_mapping_av_aksjonspunkt_med_vent() {
        LocalDateTime frist = LocalDateTime.now();
        var beregningAp = BeregningAvklaringsbehovResultat.opprettMedFristFor(AvklaringsbehovDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST,
            BeregningVenteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST,
            frist);
        AksjonspunktResultat ap = BeregningAksjonspunktResultatMapper.map(beregningAp);
        assertThat(ap.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST);
        assertThat(ap.getVenteårsak()).isEqualTo(Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST);
        assertThat(ap.getFrist()).isEqualTo(frist);
    }

}