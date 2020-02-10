package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN_KODE)
public class BekreftEktefelleAksjonspunktDto extends BekreftetAksjonspunktDto {


    @NotNull
    private Boolean ektefellesBarn;

    BekreftEktefelleAksjonspunktDto() { // NOSONAR
        //For Jackson
    }

    public BekreftEktefelleAksjonspunktDto(String begrunnelse, Boolean ektefellesBarn) { // NOSONAR
        super(begrunnelse);
        this.ektefellesBarn = ektefellesBarn;
    }


    public Boolean getEktefellesBarn() {
        return ektefellesBarn;
    }

}
