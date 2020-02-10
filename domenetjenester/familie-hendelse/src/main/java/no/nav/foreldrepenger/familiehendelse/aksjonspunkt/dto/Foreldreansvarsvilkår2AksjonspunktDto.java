package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.vedtak.util.InputValideringRegex;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD_KODE)
public class Foreldreansvarsvilkår2AksjonspunktDto extends BekreftetAksjonspunktDto implements AvslagbartAksjonspunktDto{


    @NotNull
    private Boolean erVilkarOk;

    @Size(min = 1, max = 100)
    @Pattern(regexp = InputValideringRegex.KODEVERK)
    private String avslagskode;

    Foreldreansvarsvilkår2AksjonspunktDto() { // NOSONAR
        //For Jackson
    }

    public Foreldreansvarsvilkår2AksjonspunktDto(String begrunnelse, Boolean erVilkarOk, String avslagskode) { // NOSONAR
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.avslagskode = avslagskode;
    }

    @Override
    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    @Override
    public String getAvslagskode() {
        return avslagskode;
    }


}
