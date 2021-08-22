package no.nav.foreldrepenger.mottak.vedtak.rest;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class InfotrygdFPGrunnlag  {

    private static final String DEFAULT_URI = "http://infotrygd-foreldrepenger.default/grunnlag";

    private static final Logger LOG = LoggerFactory.getLogger(InfotrygdFPGrunnlag.class);

    private OidcRestClient restClient;
    private URI uri;
    private String uriString;

    @Inject
    public InfotrygdFPGrunnlag(OidcRestClient restClient, @KonfigVerdi(value = "fpsak.it.fp.grunnlag.url", defaultVerdi = DEFAULT_URI) URI uri) {
        this.restClient = restClient;
        this.uri = uri;
        this.uriString = uri.toString();
    }

    public InfotrygdFPGrunnlag() {
        // CDI
    }


    public List<Grunnlag> hentGrunnlag(String fnr, LocalDate fom, LocalDate tom) {
        try {
            var request = new URIBuilder(uri)
                .addParameter("fnr", fnr)
                .addParameter("fom", konverter(fom))
                .addParameter("tom", konverter(tom)).build();
            var grunnlag = restClient.get(request, Grunnlag[].class);
            return Arrays.asList(grunnlag);
        } catch (Exception e) {
            LOG.info("FPSAK Infotrygd Grunnlag FP - Feil ved oppslag mot {}, returnerer ingen grunnlag", uriString, e);
            //throw InfotrygdRestFeil.FACTORY.feilfratjeneste(uriString).toException();
            return Collections.emptyList();
        }
    }

    private static String konverter(LocalDate dato) {
        return dato.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
