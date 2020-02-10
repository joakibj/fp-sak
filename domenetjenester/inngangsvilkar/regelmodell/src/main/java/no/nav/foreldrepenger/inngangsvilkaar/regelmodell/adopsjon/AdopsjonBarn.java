package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon;

import java.time.LocalDate;

public class AdopsjonBarn {

    private LocalDate foedselsdato;

    public AdopsjonBarn(LocalDate foedselsdato) {
        this.foedselsdato = foedselsdato;
    }

    public LocalDate getFoedselsdato() {
        return foedselsdato;
    }
}
