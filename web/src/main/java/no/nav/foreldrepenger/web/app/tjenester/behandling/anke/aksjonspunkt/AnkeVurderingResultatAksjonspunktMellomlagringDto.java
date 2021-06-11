package no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class AnkeVurderingResultatAksjonspunktMellomlagringDto  {

    @Pattern(regexp = InputValideringRegex.KODEVERK)
    @JsonProperty("kode")
    private String kode;

    @Valid
    @NotNull
    @JsonProperty("behandlingUuid")
    private UUID behandlingUuid;

    @ValidKodeverk
    @JsonProperty("ankeVurdering")
    private AnkeVurdering ankeVurdering;

    @Size(max = 2000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("begrunnelse")
    private String begrunnelse;

    @Size(max = 100000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    @JsonProperty("fritekstTilBrev")
    private String fritekstTilBrev;

    @ValidKodeverk
    @JsonProperty("ankeOmgjoerArsak")
    private AnkeOmgjørÅrsak ankeOmgjoerArsak;

    @ValidKodeverk
    @JsonProperty("ankeVurderingOmgjoer")
    private AnkeVurderingOmgjør ankeVurderingOmgjoer;

    @JsonProperty("vedtakBehandlingUuid")
    @Valid
    private UUID påAnketBehandlingUuid;

    @JsonProperty("erIkkeAnkerPart")
    private boolean erIkkeAnkerPart;

    @JsonProperty("erFristIkkeOverholdt")
    private boolean erFristIkkeOverholdt;

    @JsonProperty("erIkkeKonkret")
    private boolean erIkkeKonkret;

    @JsonProperty("erIkkeSignert")
    private boolean erIkkeSignert;

    @JsonProperty("erSubsidiartRealitetsbehandles")
    private boolean erSubsidiartRealitetsbehandles;

    public AnkeVurderingResultatAksjonspunktMellomlagringDto() { // NOSONAR
        // For Jackson
    }

    public AnkeVurderingResultatAksjonspunktMellomlagringDto( // NOSONAR
                                                              String kode,
                                                              UUID behandlingUuid,
                                                              String begrunnelse,
                                                              AnkeVurdering ankeVurdering,
                                                              AnkeOmgjørÅrsak ankeOmgjoerArsak,
                                                              String fritekstTilBrev,
                                                              AnkeVurderingOmgjør ankeVurderingOmgjoer,
                                                              boolean erSubsidiartRealitetsbehandles,
                                                              UUID påAnketBehandlingUuid,
                                                              boolean erIkkeAnkerPart,
                                                              boolean erFristIkkeOverholdt,
                                                              boolean erIkkeKonkret,
                                                              boolean erIkkeSignert) {
        this.kode = kode;
        this.behandlingUuid = behandlingUuid;
        this.begrunnelse = begrunnelse;
        this.ankeVurdering = ankeVurdering;
        this.fritekstTilBrev = fritekstTilBrev;
        this.ankeOmgjoerArsak = ankeOmgjoerArsak;
        this.ankeVurderingOmgjoer = ankeVurderingOmgjoer;
        this.erSubsidiartRealitetsbehandles = erSubsidiartRealitetsbehandles;
        this.påAnketBehandlingUuid = påAnketBehandlingUuid;
        this.erIkkeAnkerPart = erIkkeAnkerPart;
        this.erFristIkkeOverholdt = erFristIkkeOverholdt;
        this.erIkkeKonkret = erIkkeKonkret;
        this.erIkkeSignert = erIkkeSignert;
    }

    public AnkeVurdering getAnkeVurdering() {
        return ankeVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstTilBrev() {
        return fritekstTilBrev;
    }

    public AnkeOmgjørÅrsak getAnkeOmgjoerArsak() {
        return ankeOmgjoerArsak;
    }

    public AnkeVurderingOmgjør getAnkeVurderingOmgjoer() {
        return ankeVurderingOmgjoer;
    }

    public boolean erIkkeAnkerPart() {
        return erIkkeAnkerPart;
    }

    public boolean erFristIkkeOverholdt() {
        return erFristIkkeOverholdt;
    }

    public boolean erIkkeKonkret() {
        return erIkkeKonkret;
    }

    public boolean erIkkeSignert() {
        return erIkkeSignert;
    }

    public boolean erSubsidiartRealitetsbehandles() {
        return erSubsidiartRealitetsbehandles;
    }

    public String getKode() {
        return kode;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public UUID hentPåAnketBehandlingUuid() {
        return påAnketBehandlingUuid;
    }
}
