package no.nav.foreldrepenger.web.server.jetty;

import java.time.Duration;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import io.micrometer.core.instrument.Metrics;
import no.nav.vedtak.log.metrics.MetricsUtil;

@Provider
@Priority(Priorities.USER)
public class TimingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String STATUS = "status";
    private static final String METHOD = "method";
    private static final String PATH = "path";
    private static final String METRIC_NAME = "rest";
    private static final String COUNTER_NAME = "restantall";
    private static final ThreadLocalTimer TIMER = new ThreadLocalTimer();

    public TimingFilter() {
        MetricsUtil.utvidMedPercentiler(METRIC_NAME, 0.5, 0.95, 0.99);
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        Metrics.timer(METRIC_NAME, PATH, req.getUriInfo().getPath(), METHOD, req.getMethod()).record(Duration.ofMillis(TIMER.stop()));
        Metrics.counter(COUNTER_NAME, METHOD, req.getMethod(), STATUS, String.valueOf(res.getStatus())).increment();
    }

    @Override
    public void filter(ContainerRequestContext req) {
        TIMER.start();
    }

    private static class ThreadLocalTimer extends ThreadLocal<Long> {
        public void start() {
            this.set(System.currentTimeMillis());
        }

        public long stop() {
            return System.currentTimeMillis() - get();
        }

        @Override
        protected Long initialValue() {
            return System.currentTimeMillis();
        }
    }
}
