package no.nav.foreldrepenger.mottak.publiserer.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.kafka.KafkaProperties;


@ApplicationScoped
public class DialogHendelseProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DialogHendelseProducer.class);

    private Producer<String, String> producer;
    private String topic;

    public DialogHendelseProducer() {
    }

    @Inject
    public DialogHendelseProducer(@KonfigVerdi("kafka.inntektsmelding.innsendinghendelse.topic") String topic) {
        this.topic = topic;
        this.producer = new KafkaProducer<>(KafkaProperties.forProducer());
    }

    public void sendJsonMedNøkkel(String nøkkel, String json) {
        LOG.info("Sender inntektsmelding på topic={}", topic);
        runProducerWithSingleJson(new ProducerRecord<>(topic, nøkkel, json));
    }

    private void runProducerWithSingleJson(ProducerRecord<String, String> record) {
        try {
            var recordMetadata = producer.send(record).get();
            LOG.info("Sendte melding til {}, partition {}, offset {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw kafkaPubliseringException(e);
        } catch (Exception e) {
            throw kafkaPubliseringException(e);
        }
    }

    private IntegrasjonException kafkaPubliseringException(Exception e) {
        return new IntegrasjonException("FP-HENDELSE-925475", "Uventet feil ved sending til Kafka, topic " + topic, e);
    }

}
