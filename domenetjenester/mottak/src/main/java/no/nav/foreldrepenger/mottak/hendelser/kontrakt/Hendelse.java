package no.nav.foreldrepenger.mottak.hendelser.kontrakt;

public abstract class Hendelse {

    private String hendelseKode;

    protected Hendelse(String hendelseKode) {
        this.hendelseKode = hendelseKode;
    }

    public String getHendelseKode() {
        return hendelseKode;
    }
}
