package no.nav.foreldrepenger.web.server.jetty;

import no.nav.vedtak.sikkerhet.ContextPathHolder;

public class JettyWebKonfigurasjon {

    private static final String CONTEXT_PATH = "/fpsak";

    private Integer serverPort;

    // Hvis du føler for å omstrukturere Jetty-klassene så sørg for at CPHolder
    // blir kalt så tidlig som mulig i oppstarten av Jetty og helst kun en gang ....
    // Cookies: bruker path "/" når man instansierer CPH uten å angi cookiepath
    public JettyWebKonfigurasjon(int serverPort) {
        this.serverPort = serverPort;
        ContextPathHolder.instance(CONTEXT_PATH);
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getContextPath() {
        return CONTEXT_PATH;
    }
}
