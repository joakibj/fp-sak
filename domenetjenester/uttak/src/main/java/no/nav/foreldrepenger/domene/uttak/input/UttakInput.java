package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;

/** Inputstruktur for uttak tjenester. */
public class UttakInput {

    private BehandlingReferanse behandlingReferanse;
    private Set<BeregningsgrunnlagStatus> beregningsgrunnlagStatuser = Set.of();
    private final InntektArbeidYtelseGrunnlag iayGrunnlag;
    private final YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag;
    private LocalDate søknadMottattDato;
    private LocalDate medlemskapOpphørsdato;
    private Set<BehandlingÅrsakType> behandlingÅrsaker = Set.of();
    private boolean behandlingManueltOpprettet;
    private boolean opplysningerOmDødEndret;
    private boolean finnesAndelerMedGraderingUtenBeregningsgrunnlag;

    public UttakInput(BehandlingReferanse behandlingReferanse,
                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                      YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag) {
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse, "behandlingReferanse");
        this.iayGrunnlag = iayGrunnlag;
        this.ytelsespesifiktGrunnlag = ytelsespesifiktGrunnlag;
    }

    private UttakInput(UttakInput input) {
        this(input.getBehandlingReferanse(), input.getIayGrunnlag(), input.getYtelsespesifiktGrunnlag());
        this.beregningsgrunnlagStatuser = Set.copyOf(input.beregningsgrunnlagStatuser);
        this.søknadMottattDato = input.søknadMottattDato;
        this.medlemskapOpphørsdato = input.medlemskapOpphørsdato;
        this.behandlingÅrsaker = input.behandlingÅrsaker;
        this.behandlingManueltOpprettet = input.behandlingManueltOpprettet;
        this.opplysningerOmDødEndret = input.opplysningerOmDødEndret;
        this.finnesAndelerMedGraderingUtenBeregningsgrunnlag = input.finnesAndelerMedGraderingUtenBeregningsgrunnlag;
    }

    public AktørId getAktørId() {
        return behandlingReferanse.getAktørId();
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }

    public Set<BeregningsgrunnlagStatus> getBeregningsgrunnlagStatuser() {
        return beregningsgrunnlagStatuser;
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return behandlingReferanse.getFagsakYtelseType();
    }

    public InntektArbeidYtelseGrunnlag getIayGrunnlag() {
        return iayGrunnlag;
    }

    public LocalDate getSøknadMottattDato() {
        return søknadMottattDato;
    }

    /** Sjekk fagsakytelsetype før denne kalles. */
    @SuppressWarnings("unchecked")
    public <V extends YtelsespesifiktGrunnlag> V getYtelsespesifiktGrunnlag() {
        return (V) ytelsespesifiktGrunnlag;
    }

    public UttakYrkesaktiviteter getYrkesaktiviteter() {
        return new UttakYrkesaktiviteter(this);
    }

    public Optional<LocalDate> getMedlemskapOpphørsdato() {
        return Optional.ofNullable(medlemskapOpphørsdato);
    }

    public boolean harBehandlingÅrsak(BehandlingÅrsakType behandlingÅrsakType) {
        return behandlingÅrsaker.stream().anyMatch(årsak -> årsak.equals(behandlingÅrsakType));
    }

    public boolean harBehandlingÅrsakRelatertTilDød() {
        return behandlingÅrsaker.stream().anyMatch(årsak -> BehandlingÅrsakType.årsakerRelatertTilDød().contains(årsak));
    }

    public boolean isBehandlingManueltOpprettet() {
        return behandlingManueltOpprettet;
    }

    public boolean isOpplysningerOmDødEndret() {
        return opplysningerOmDødEndret;
    }

    public boolean finnesAndelerMedGraderingUtenBeregningsgrunnlag() {
        return finnesAndelerMedGraderingUtenBeregningsgrunnlag;
    }

    public Set<BehandlingÅrsakType> getBehandlingÅrsaker() {
        return behandlingÅrsaker;
    }

    public UttakInput medBeregningsgrunnlagStatuser(Set<BeregningsgrunnlagStatus> statuser) {
        var newInput = new UttakInput(this);
        newInput.beregningsgrunnlagStatuser = Set.copyOf(statuser);
        return newInput;
    }

    public UttakInput medSøknadMottattDato(LocalDate mottattDato) {
        var newInput = new UttakInput(this);
        newInput.søknadMottattDato = mottattDato;
        return newInput;
    }

    public UttakInput medMedlemskapOpphørsdato(LocalDate opphørsdato) {
        var newInput = new UttakInput(this);
        newInput.medlemskapOpphørsdato = opphørsdato;
        return newInput;
    }

    public UttakInput medBehandlingÅrsaker(Set<BehandlingÅrsakType> behandlingÅrsaker) {
        var newInput = new UttakInput(this);
        newInput.behandlingÅrsaker = behandlingÅrsaker;
        return newInput;
    }

    public UttakInput medBehandlingManueltOpprettet(boolean behandlingManueltOpprettet) {
        var newInput = new UttakInput(this);
        newInput.behandlingManueltOpprettet = behandlingManueltOpprettet;
        return newInput;
    }

    public UttakInput medErOpplysningerOmDødEndret(boolean opplysningerOmDødEndret) {
        var newInput = new UttakInput(this);
        newInput.opplysningerOmDødEndret = opplysningerOmDødEndret;
        return newInput;
    }

    public UttakInput medFinnesAndelerMedGraderingUtenBeregningsgrunnlag(boolean finnesAndelerMedGraderingUtenBeregningsgrunnlag) {
        var newInput = new UttakInput(this);
        newInput.finnesAndelerMedGraderingUtenBeregningsgrunnlag = finnesAndelerMedGraderingUtenBeregningsgrunnlag;
        return newInput;
    }
}
