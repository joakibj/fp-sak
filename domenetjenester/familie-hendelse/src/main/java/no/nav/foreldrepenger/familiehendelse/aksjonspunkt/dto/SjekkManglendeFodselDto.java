package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE)
public class SjekkManglendeFodselDto extends BekreftetAksjonspunktDto {

    @NotNull
    private Boolean dokumentasjonForeligger;

    private boolean brukAntallBarnITps;

    @Valid
    @NotNull
    @Size(min = 1, max = 9)
    private List<UidentifisertBarnDto> uidentifiserteBarn;

    SjekkManglendeFodselDto() { // NOSONAR
        //For Jackson
    }

    public SjekkManglendeFodselDto(String begrunnelse, Boolean dokumentasjonForeligger, boolean brukAntallBarnITps,
        List<UidentifisertBarnDto> uidentifiserteBarn) { // NOSONAR
        super(begrunnelse);
        this.dokumentasjonForeligger = dokumentasjonForeligger;
        this.brukAntallBarnITps = brukAntallBarnITps;
        this.uidentifiserteBarn = new ArrayList<>(uidentifiserteBarn);
    }

    public Boolean getDokumentasjonForeligger() {
        return dokumentasjonForeligger;
    }

    public boolean isBrukAntallBarnITps() {
        return Boolean.FALSE.equals(dokumentasjonForeligger) || brukAntallBarnITps;
    }

    public List<UidentifisertBarnDto> getUidentifiserteBarn() {
        return uidentifiserteBarn;
    }

}
