package no.nav.foreldrepenger.domene.modell;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.diff.DiffIgnore;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;


public class BeregningsgrunnlagGrunnlag {

    @DiffIgnore
    private UUID uuid;

    private Beregningsgrunnlag beregningsgrunnlag;
    private BeregningAktivitetAggregat registerAktiviteter;
    private BeregningAktivitetAggregat saksbehandletAktiviteter;
    private BeregningAktivitetOverstyringer overstyringer;
    private BeregningRefusjonOverstyringer refusjonOverstyringer;
    private FaktaAggregat faktaAggregat;
    private boolean aktiv = true;
    private BeregningsgrunnlagTilstand beregningsgrunnlagTilstand;

    public BeregningsgrunnlagGrunnlag() {
    }

    BeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlag grunnlag) {
        grunnlag.getBeregningsgrunnlag().ifPresent(this::setBeregningsgrunnlag);
        this.setRegisterAktiviteter(grunnlag.getRegisterAktiviteter());
        grunnlag.getSaksbehandletAktiviteter().ifPresent(this::setSaksbehandletAktiviteter);
        grunnlag.getOverstyring().ifPresent(this::setOverstyringer);
        grunnlag.getRefusjonOverstyringer().ifPresent(this::setRefusjonOverstyringer);
    }

    public Optional<Beregningsgrunnlag> getBeregningsgrunnlag() {
        return Optional.ofNullable(beregningsgrunnlag);
    }

    public BeregningAktivitetAggregat getRegisterAktiviteter() {
        return registerAktiviteter;
    }

    public Optional<BeregningAktivitetAggregat> getSaksbehandletAktiviteter() {
        return Optional.ofNullable(saksbehandletAktiviteter);
    }

    public Optional<BeregningAktivitetAggregat> getOverstyrteEllerSaksbehandletAktiviteter() {
        Optional<BeregningAktivitetAggregat> overstyrteAktiviteter = getOverstyrteAktiviteter();
        if (overstyrteAktiviteter.isPresent()) {
            return overstyrteAktiviteter;
        }
        return Optional.ofNullable(saksbehandletAktiviteter);
    }

    public Optional<BeregningAktivitetOverstyringer> getOverstyring() {
        return Optional.ofNullable(overstyringer);
    }

    private Optional<BeregningAktivitetAggregat> getOverstyrteAktiviteter() {
        if (overstyringer != null) {
            List<BeregningAktivitet> overstyrteAktiviteter = registerAktiviteter.getBeregningAktiviteter()
                .stream()
                .filter(beregningAktivitet -> beregningAktivitet.skalBrukes(overstyringer))
                .collect(Collectors.toList());
            BeregningAktivitetAggregat.Builder overstyrtBuilder = BeregningAktivitetAggregat.builder()
                .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
            overstyrteAktiviteter.forEach(aktivitet -> {
                BeregningAktivitet kopiert = BeregningAktivitet.builder(aktivitet).build();
                overstyrtBuilder.leggTilAktivitet(kopiert);
            });
            return Optional.of(overstyrtBuilder.build());
        }
        return Optional.empty();
    }

    public BeregningAktivitetAggregat getGjeldendeAktiviteter() {
        return getOverstyrteAktiviteter().or(this::getSaksbehandletAktiviteter).orElse(registerAktiviteter);
    }

    public Optional<FaktaAggregat> getFaktaAggregat() {
        return Optional.ofNullable(faktaAggregat);
    }

    public BeregningAktivitetAggregat getOverstyrteEllerRegisterAktiviteter() {
        Optional<BeregningAktivitetAggregat> overstyrteAktiviteter = getOverstyrteAktiviteter();
        if (overstyrteAktiviteter.isPresent()) {
            return overstyrteAktiviteter.get();
        }
        return registerAktiviteter;
    }

    public BeregningsgrunnlagTilstand getBeregningsgrunnlagTilstand() {
        return beregningsgrunnlagTilstand;
    }

    public boolean getAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBeregningsgrunnlag(Beregningsgrunnlag beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    void setFaktaAggregat(FaktaAggregat faktaAggregat) {
        this.faktaAggregat = faktaAggregat;
    }

    void setRegisterAktiviteter(BeregningAktivitetAggregat registerAktiviteter) {
        this.registerAktiviteter = registerAktiviteter;
    }

    void setSaksbehandletAktiviteter(BeregningAktivitetAggregat saksbehandletAktiviteter) {
        this.saksbehandletAktiviteter = saksbehandletAktiviteter;
    }

    void setBeregningsgrunnlagTilstand(BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        this.beregningsgrunnlagTilstand = beregningsgrunnlagTilstand;
    }

    void setOverstyringer(BeregningAktivitetOverstyringer overstyringer) {
        this.overstyringer = overstyringer;
    }

    public Optional<BeregningRefusjonOverstyringer> getRefusjonOverstyringer() {
        return Optional.ofNullable(refusjonOverstyringer);
    }

    void setRefusjonOverstyringer(BeregningRefusjonOverstyringer refusjonOverstyringer) {
        this.refusjonOverstyringer = refusjonOverstyringer;
    }

    /**
     * Identifisere en immutable instans av grunnlaget unikt og er egnet for utveksling (eks. til abakus eller andre systemer)
     */
    public UUID getEksternReferanse() {
        return uuid;
    }

    public Optional<UUID> getKoblingReferanse() {
        return Optional.empty();
    }
}