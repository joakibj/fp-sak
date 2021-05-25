package no.nav.foreldrepenger.web.app.tjenester.formidling.beregningsgrunnlag.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.domene.modell.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = NONE, getterVisibility = NONE, setterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class BeregningsgrunnlagAndelDto {

    @JsonProperty("dagsats")
    @Valid
    @Digits(integer = 8, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("10000.00")
    private Long dagsats;

    @JsonProperty(value = "aktivitetStatus")
    @Valid
    @NotNull
    private AktivitetStatus aktivitetStatus;

    @JsonProperty(value = "bruttoPrÅr")
    @Valid
    @Digits(integer = 8, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal bruttoPrÅr;

    @JsonProperty(value = "avkortetPrÅr")
    @Valid
    @Digits(integer = 8, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("10000000.00")
    private BigDecimal avkortetPrÅr;

    @JsonProperty(value = "erNyIArbeidslivet")
    @Valid
    private Boolean erNyIArbeidslivet;

    @JsonProperty(value = "arbeidsforholdType")
    @Valid
    @NotNull
    private OpptjeningAktivitetType arbeidsforholdType;

    @JsonProperty(value = "beregningsperiodeFom")
    @Valid
    private LocalDate beregningsperiodeFom;

    @JsonProperty(value = "beregningsperiodeTom")
    @Valid
    private LocalDate beregningsperiodeTom;

    @JsonProperty(value = "arbeidsforhold")
    @Valid
    private BgAndelArbeidsforholdDto arbeidsforhold;

    @JsonProperty(value = "erTilkommetAndel")
    @Valid
    private Boolean erTilkommetAndel;

    public BeregningsgrunnlagAndelDto(@Valid @Digits(integer = 8, fraction = 2) @DecimalMin("0.00") @DecimalMax("10000.00") Long dagsats,
                                      @Valid @NotNull AktivitetStatus aktivitetStatus,
                                      @Valid @Digits(integer = 8, fraction = 2) @DecimalMin("0.00") @DecimalMax("10000000.00") BigDecimal bruttoPrÅr,
                                      @Valid @Digits(integer = 8, fraction = 2) @DecimalMin("0.00") @DecimalMax("10000000.00") BigDecimal avkortetPrÅr,
                                      @Valid Boolean erNyIArbeidslivet,
                                      @Valid @NotNull OpptjeningAktivitetType arbeidsforholdType,
                                      @Valid LocalDate beregningsperiodeFom,
                                      @Valid LocalDate beregningsperiodeTom,
                                      @Valid BgAndelArbeidsforholdDto arbeidsforhold,
                                      @Valid Boolean erTilkommetAndel) {
        this.dagsats = dagsats;
        this.aktivitetStatus = aktivitetStatus;
        this.bruttoPrÅr = bruttoPrÅr;
        this.avkortetPrÅr = avkortetPrÅr;
        this.erNyIArbeidslivet = erNyIArbeidslivet;
        this.arbeidsforholdType = arbeidsforholdType;
        this.beregningsperiodeFom = beregningsperiodeFom;
        this.beregningsperiodeTom = beregningsperiodeTom;
        this.arbeidsforhold = arbeidsforhold;
        this.erTilkommetAndel = erTilkommetAndel;
    }

    public Long getDagsats() {
        return dagsats;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public BigDecimal getBruttoPrÅr() {
        return bruttoPrÅr;
    }

    public BigDecimal getAvkortetPrÅr() {
        return avkortetPrÅr;
    }

    public Boolean getErNyIArbeidslivet() {
        return erNyIArbeidslivet;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public LocalDate getBeregningsperiodeFom() {
        return beregningsperiodeFom;
    }

    public LocalDate getBeregningsperiodeTom() {
        return beregningsperiodeTom;
    }

    public BgAndelArbeidsforholdDto getArbeidsforhold() {
        return arbeidsforhold;
    }

    public boolean getErTilkommetAndel() { return  erTilkommetAndel; }
}
