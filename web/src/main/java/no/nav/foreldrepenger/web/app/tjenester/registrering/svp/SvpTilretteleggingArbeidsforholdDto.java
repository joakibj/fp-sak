package no.nav.foreldrepenger.web.app.tjenester.registrering.svp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
public abstract class SvpTilretteleggingArbeidsforholdDto {

    private LocalDate behovsdato;
    private List<SvpTilretteleggingDto> tilrettelegginger = new ArrayList<>();

    public LocalDate getBehovsdato() {
        return behovsdato;
    }

    public void setBehovsdato(LocalDate behovsdato) {
        this.behovsdato = behovsdato;
    }

    public List<SvpTilretteleggingDto> getTilrettelegginger() {
        return tilrettelegginger;
    }

    public void setTilrettelegginger(List<SvpTilretteleggingDto> tilrettelegginger) {
        this.tilrettelegginger = tilrettelegginger;
    }
}
