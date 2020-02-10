package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsOvertakelse;

public class AvklartDataAdopsjonDto extends FamiliehendelseDto implements OmsorgsOvertakelse {

    private Boolean mannAdoptererAlene;
    private Boolean ektefellesBarn;
    private LocalDate omsorgsovertakelseDato;
    private LocalDate ankomstNorge;

    @Valid
    @Size(max = 9)
    private Map<Integer, LocalDate> adopsjonFodelsedatoer;

    public AvklartDataAdopsjonDto() {
        super(SøknadType.ADOPSJON);
    }

    @Override
    public LocalDate getOmsorgsovertakelseDato() {
        return omsorgsovertakelseDato;
    }

    void setOmsorgsovertakelseDato(LocalDate omsorgsovertakelseDato) {
        this.omsorgsovertakelseDato = omsorgsovertakelseDato;
    }

    public Map<Integer, LocalDate> getAdopsjonFodelsedatoer() {
        return adopsjonFodelsedatoer;
    }

    void setAdopsjonFodelsedatoer(Map<Integer, LocalDate> adopsjonFodelsedatoer) {
        this.adopsjonFodelsedatoer = adopsjonFodelsedatoer;
    }

    public Boolean getEktefellesBarn() {
        return ektefellesBarn;
    }

    void setEktefellesBarn(Boolean ektefellesBarn) {
        this.ektefellesBarn = ektefellesBarn;
    }

    public Boolean getMannAdoptererAlene() {
        return mannAdoptererAlene;
    }

    void setMannAdoptererAlene(Boolean mannAdoptererAlene) {
        this.mannAdoptererAlene = mannAdoptererAlene;
    }

    public LocalDate getAnkomstNorge() {
        return ankomstNorge;
    }

    void setAnkomstNorge(LocalDate ankomstNorge) {
        this.ankomstNorge = ankomstNorge;
    }
}
