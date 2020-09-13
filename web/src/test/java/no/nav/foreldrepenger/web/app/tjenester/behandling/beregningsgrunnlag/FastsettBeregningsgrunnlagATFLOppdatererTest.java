package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.FastsettBeregningsgrunnlagATFLHistorikkTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;

public class FastsettBeregningsgrunnlagATFLOppdatererTest {
    private FastsettBeregningsgrunnlagATFLOppdaterer oppdaterer;

    @Mock
    private FastsettBGTidsbegrensetArbeidsforholdHistorikkTjeneste historikkTidsbegrenset;

    @Mock
    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste historikk;

    @Mock
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Mock
    private Behandling behandling;

    @Mock
    private Fagsak fagsak;

    @Mock
    private Aksjonspunkt ap;

    @Mock
    private BeregningHåndterer beregningHåndterer;

    @Mock
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;

    @Mock
    private BeregningsgrunnlagInputFelles beregningsgrunnlagInputFelles;

    @Mock
    private BeregningsgrunnlagInput input;

    @Before
    public void setup() {
        initMocks(this);
        when(behandling.getFagsak()).thenReturn(fagsak);
        oppdaterer = new FastsettBeregningsgrunnlagATFLOppdaterer(beregningsgrunnlagTjeneste, historikk, historikkTidsbegrenset, beregningsgrunnlagInputTjeneste, beregningHåndterer);
    }

    @Test
    public void skal_håndtere_overflødig_fastsett_tidsbegrenset_arbeidsforhold_aksjonspunkt() {
        //Arrange
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(anyLong()))
            .thenReturn(BeregningsgrunnlagEntitet.builder().medSkjæringstidspunkt(LocalDate.now()).build());

        when(behandling.getÅpentAksjonspunktMedDefinisjonOptional(any())).thenReturn(Optional.of(ap));
        when(ap.getAksjonspunktDefinisjon()).thenReturn(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        when(ap.erOpprettet()).thenReturn(true);
        when(beregningsgrunnlagInputTjeneste.getTjeneste(any())).thenReturn(beregningsgrunnlagInputFelles);
        when(beregningsgrunnlagInputFelles.lagInput(any(BehandlingReferanse.class))).thenReturn(input);

        //Dto
        FastsettBeregningsgrunnlagATFLDto dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.emptyList(), null);
        // Act
        var resultat = oppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, ap, dto));

        //Assert
        assertThat(resultat.getEkstraAksjonspunktResultat()).hasSize(1);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getElement1().getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        assertThat(resultat.getEkstraAksjonspunktResultat().get(0).getElement2()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }
}
