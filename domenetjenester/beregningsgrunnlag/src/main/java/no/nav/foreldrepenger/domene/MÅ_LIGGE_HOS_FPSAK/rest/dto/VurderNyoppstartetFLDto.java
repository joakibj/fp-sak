package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto;

import javax.validation.constraints.NotNull;

public class VurderNyoppstartetFLDto {

    @NotNull
    private Boolean erNyoppstartetFL;

    VurderNyoppstartetFLDto() {
        // For Jackson
    }

    public VurderNyoppstartetFLDto(Boolean erNyoppstartetFL) { // NOSONAR
        this.erNyoppstartetFL = erNyoppstartetFL;
    }

    public void setErNyoppstartetFL(Boolean erNyoppstartetFL) {
        this.erNyoppstartetFL = erNyoppstartetFL;
    }

    public Boolean erErNyoppstartetFL() {
        return erNyoppstartetFL;
    }
}
