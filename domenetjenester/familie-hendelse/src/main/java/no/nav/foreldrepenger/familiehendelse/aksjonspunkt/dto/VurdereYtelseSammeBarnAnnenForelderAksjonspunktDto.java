package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE_KODE)
public class VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto extends BekreftetAksjonspunktDto implements AvslagbartAksjonspunktDto {


    @NotNull
    private Boolean erVilkarOk;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    public VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto() { // NOSONAR
        // for jackson
    }

    public VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto(String begrunnelse, Boolean erVilkarOk) {  // NOSONAR
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
    }

    @Override
    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public void setErVilkarOk(Boolean erVilkarOk) {
        this.erVilkarOk = erVilkarOk;
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }

    public void setAvslagskode(String avslagskode) {
        this.avslagskode = avslagskode;
    }

}
