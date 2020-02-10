package no.nav.foreldrepenger.web.app.soap.sak.tjeneste;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.feil.Feil;

public class UkjentPersonException extends TekniskException {
    public UkjentPersonException(Feil feil) {
        super(feil);
    }
}
