package no.nav.foreldrepenger.batch;

import java.util.Map;

import no.nav.foreldrepenger.batch.feil.UnknownArgumentsReceivedVLBatchException;

public abstract class BatchArguments {

    public BatchArguments(Map<String, String> arguments) {
        arguments.entrySet().removeIf(it -> settParameterVerdien(it.getKey(), it.getValue()));

        if (!arguments.entrySet().isEmpty()) {
            throw new UnknownArgumentsReceivedVLBatchException("FP-959814",
                "Ukjente job argumenter " + arguments.keySet());
        }
    }

    public abstract boolean settParameterVerdien(String key, String value);

    /**
     * @return sant / usant om argumentene er semtantisk korrekte
     */
    public abstract boolean isValid();
}
