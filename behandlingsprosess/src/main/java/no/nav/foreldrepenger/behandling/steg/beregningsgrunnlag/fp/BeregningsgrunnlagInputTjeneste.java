package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class BeregningsgrunnlagInputTjeneste extends BeregningsgrunnlagInputFelles {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;

    protected BeregningsgrunnlagInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
            InntektArbeidYtelseTjeneste iayTjeneste,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            AndelGraderingTjeneste andelGraderingTjeneste,
            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
            BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste, InntektsmeldingTjeneste inntektsmeldingTjeneste,
            KalkulusKonfigInjecter kalkulusKonfigInjecter) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste, andelGraderingTjeneste,
                opptjeningForBeregningTjeneste, inntektsmeldingTjeneste, kalkulusKonfigInjecter);
        this.fagsakRelasjonRepository = Objects.requireNonNull(behandlingRepositoryProvider.getFagsakRelasjonRepository(),
                "fagsakRelasjonRepository");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var saksnummer = ref.getSaksnummer();
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);
        var dekningsgrad = fagsakRelasjon.map(FagsakRelasjon::getGjeldendeDekningsgrad)
                .orElseThrow(() -> new IllegalStateException("Mangler FagsakRelasjon#dekningsgrad for behandling: " + ref));
        boolean kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);
        return new ForeldrepengerGrunnlag(dekningsgrad.getVerdi(), kvalifisererTilBesteberegning);
    }
}
