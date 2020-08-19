package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.typer.Beløp;

@ApplicationScoped
public class BeregningTilInputTjeneste {

    private static final String UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER = "Utvikler-feil: skal ha beregningsgrunnlag her";
    private static final Supplier<IllegalStateException> INGEN_BG_EXCEPTION_SUPPLIER = () -> new IllegalStateException(UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER);
    private static final MonthDay ENDRING_AV_GRUNNBELØP = MonthDay.of(5, 1);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private KalkulusKonfigInjecter kalkulusKonfigInjecter;
    private BehandlingRepository behandlingRepository;


    public BeregningTilInputTjeneste() {
        // CDI
    }

    @Inject
    public BeregningTilInputTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                     KalkulusKonfigInjecter kalkulusKonfigInjecter,
                                     BehandlingRepository behandlingRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.kalkulusKonfigInjecter = kalkulusKonfigInjecter;
        this.behandlingRepository = behandlingRepository;
    }

    public BeregningsgrunnlagInput lagInputMedVerdierFraBeregning(BeregningsgrunnlagInput input) {
        return lagInputMedBeregningsgrunnlag(input);
    }

    private BeregningsgrunnlagInput lagInputMedBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getBehandlingReferanse().getBehandlingId();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEntitetOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId);
        BeregningsgrunnlagInput newInput = lagInputMedGrunnbeløpSatser(input);
        if (grunnlagEntitetOpt.isPresent()) {
            BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = grunnlagEntitetOpt.get();
            BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlagEntitet.getBeregningsgrunnlag()
                .orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
            var ref = oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(input.getBehandlingReferanse(), grunnlagEntitet.getGjeldendeAktiviteter(), beregningsgrunnlag);
            newInput = newInput
                .medBehandlingReferanse(ref)
                .medBeregningsgrunnlagGrunnlag(BehandlingslagerTilKalkulusMapper.mapGrunnlag(grunnlagEntitet, input.getInntektsmeldinger()));
        }
        kalkulusKonfigInjecter.leggTilKonfigverdier(input);
        kalkulusKonfigInjecter.leggTilFeatureToggles(input);
        Optional<BeregningsgrunnlagGrunnlagEntitet> førsteFastsatteGrunnlagEtterEndringAvG = finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(behandlingId);
        newInput = førsteFastsatteGrunnlagEtterEndringAvG.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(BeregningsgrunnlagEntitet::getGrunnbeløp)
            .map(Beløp::getVerdi)
            .map(newInput::medUregulertGrunnbeløp)
            .orElse(newInput);
        return lagBeregningsgrunnlagHistorikk(newInput);
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagHistorikk(BeregningsgrunnlagInput input) {
        BeregningsgrunnlagTilstand[] tilstander = BeregningsgrunnlagTilstand.values();
        for (BeregningsgrunnlagTilstand tilstand : tilstander) {
            Optional<BeregningsgrunnlagGrunnlagEntitet> sisteBg = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(input.getBehandlingReferanse().getBehandlingId(), input.getBehandlingReferanse().getOriginalBehandlingId(), tilstand);
            sisteBg.ifPresent(gr -> input.leggTilBeregningsgrunnlagIHistorikk(BehandlingslagerTilKalkulusMapper.mapGrunnlag(gr, input.getInntektsmeldinger()),
                no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand.fraKode(tilstand.getKode())));
        }
        return input;
    }

    private BeregningsgrunnlagInput lagInputMedGrunnbeløpSatser(BeregningsgrunnlagInput input) {
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(GrunnbeløpTjeneste.class, input.getFagsakYtelseType()).orElseThrow();
        List<Grunnbeløp> grunnbeløpSatser = tjeneste.mapGrunnbeløpSatser();
        Integer antallGrunnbeløpMilitærHarKravPå = tjeneste.finnAntallGrunnbeløpMilitærHarKravPå();
        BeregningsgrunnlagInput newInput = input.medGrunnbeløpsatser(grunnbeløpSatser);
        newInput.getYtelsespesifiktGrunnlag()
            .setGrunnbeløpMilitærHarKravPå(antallGrunnbeløpMilitærHarKravPå);
        return newInput;
    }


    private BehandlingReferanse oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(BehandlingReferanse ref,
                                                                                          BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                                          BeregningsgrunnlagEntitet beregningsgrunnlag) {
        LocalDate skjæringstidspunktOpptjening = beregningAktivitetAggregat.getSkjæringstidspunktOpptjening();
        LocalDate førsteUttaksdato = ref.getFørsteUttaksdato();
        LocalDate skjæringstidspunktBeregning = beregningsgrunnlag.getSkjæringstidspunkt();
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunktOpptjening)
            .medFørsteUttaksdato(førsteUttaksdato)
            .medSkjæringstidspunktBeregning(skjæringstidspunktBeregning).build();
        return ref.medSkjæringstidspunkt(skjæringstidspunkt);
    }

    private Optional<BeregningsgrunnlagGrunnlagEntitet> finnFørsteFastsatteGrunnlagEtterEndringAvGrunnbeløp(Long behandlingId) {
        Long fagsakId = behandlingRepository.hentBehandling(behandlingId).getFagsak().getId();
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId);
        return behandlinger.stream()
            .filter(b -> b.getStatus().erFerdigbehandletStatus())
            .map(kobling -> beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(kobling.getId(), BeregningsgrunnlagTilstand.FASTSATT))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(gr -> MonthDay.from(gr.getBeregningsgrunnlag().orElseThrow(() -> new IllegalStateException("Skal ha beregningsgrunnlag"))
                .getSkjæringstidspunkt()).isAfter(ENDRING_AV_GRUNNBELØP))
            .min(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt));
    }

}
