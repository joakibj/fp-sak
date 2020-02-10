package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;

public class PeriodeDto {

    private LocalDate periodeFom;
    private LocalDate periodeTom;

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public void setPeriodeFom(LocalDate periodeFom) {
        this.periodeFom = periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public void setPeriodeTom(LocalDate periodeTom) {
        this.periodeTom = periodeTom;
    }
}
