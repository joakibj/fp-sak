package no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

public class SaksnummerJournalpostDto implements AbacDto {

    @NotNull
    @QueryParam("saksnummer")
    @Digits(integer = 18, fraction = 0)
    private String saksnummer;

    @NotNull
    @QueryParam("journalpostid")
    @Digits(integer = 18, fraction = 0)
    private String journalpostId;

    public SaksnummerJournalpostDto(@NotNull String saksnummer, @NotNull String journalpostId) {
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
    }

    public SaksnummerJournalpostDto() {
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett()
            .leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer)
            .leggTil(AppAbacAttributtType.EKSISTERENDE_JOURNALPOST_ID, journalpostId);
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getJournalpostId() {
        return journalpostId;
    }
}
