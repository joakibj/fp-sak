package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

record SvpSak(String saksnummer, String aktørId, FamilieHendelse familieHendelse) implements Sak {

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + '}';
    }
}
