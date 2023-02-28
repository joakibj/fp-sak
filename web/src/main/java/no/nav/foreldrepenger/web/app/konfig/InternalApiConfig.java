package no.nav.foreldrepenger.web.app.konfig;


import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import no.nav.foreldrepenger.web.app.healthchecks.HealthCheckRestService;
import no.nav.foreldrepenger.web.app.metrics.PrometheusRestService;

/**
 * Konfigurer Prometheus og Healthchecks
 */
@ApplicationPath(InternalApiConfig.INTERNAL_URI)
public class InternalApiConfig extends Application {

    public static final String INTERNAL_URI = "/internal";

    public InternalApiConfig() {
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(HealthCheckRestService.class, PrometheusRestService.class);
    }
}
