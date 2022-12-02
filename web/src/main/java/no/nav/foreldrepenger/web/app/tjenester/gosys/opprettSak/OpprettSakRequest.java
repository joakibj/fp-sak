package no.nav.foreldrepenger.web.app.tjenester.gosys.opprettSak;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public record OpprettSakRequest(@NotNull @Pattern(regexp = "^(-?[1-9]|[a-z0])[a-z0-9_:-]*$", message = "journalpostId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String journalpostId,
                                @NotNull String behandlingsTema,
                                @NotNull @Pattern(regexp = "^\\d{13}$", message = "aktørId ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String aktørId) {
}