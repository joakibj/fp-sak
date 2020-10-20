package no.nav.foreldrepenger.domene.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

//Trengs en omskriving hvis testene skal kunne kjøres i parallell
@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FeedRepositoryTest extends EntityManagerAwareTest {

    private static final String TYPE2 = "type2";
    private static final String TYPE1 = "type1";
    private static final String AKTØR_ID = "1000000000";
    private static final String AKTØR_ID_2 = "1000000001";
    private static final String KILDE_ID = "kildeId";
    private static final String PAYLOAD = "{\"hello\": \"world\"}";

    private FeedRepository feedRepository;
    private FpVedtakUtgåendeHendelse hendelseAvType1MedAktørId1MedSek1;
    private FpVedtakUtgåendeHendelse hendelseAvType1MedAktørId2MedSek2;
    private FpVedtakUtgåendeHendelse hendelseAvType2MedAktørId1MedSek3;
    private FpVedtakUtgåendeHendelse hendelseMedHøyestSeksvensnummerogKilde;

    @BeforeEach
    void setUp() {
        feedRepository = new FeedRepository(getEntityManager());
    }

    private void lagreHendelser() {
        if (hendelseAvType1MedAktørId1MedSek1 == null) {
            hendelseAvType1MedAktørId1MedSek1 = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId(AKTØR_ID).type(TYPE1).build();
            var nesteSekvensnummer = nesteSeksvensnummer();
            hendelseAvType1MedAktørId1MedSek1.setSekvensnummer(nesteSekvensnummer);
            feedRepository.lagre(hendelseAvType1MedAktørId1MedSek1);
        }

        if (hendelseAvType1MedAktørId2MedSek2 == null) {
            hendelseAvType1MedAktørId2MedSek2 = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId(AKTØR_ID_2).type(TYPE1).build();
            hendelseAvType1MedAktørId2MedSek2.setSekvensnummer(nesteSeksvensnummer());
            feedRepository.lagre(hendelseAvType1MedAktørId2MedSek2);
        }

        if (hendelseAvType2MedAktørId1MedSek3 == null) {
            hendelseAvType2MedAktørId1MedSek3 = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId(AKTØR_ID).type(TYPE2).build();
            hendelseAvType2MedAktørId1MedSek3.setSekvensnummer(nesteSeksvensnummer());
            feedRepository.lagre(hendelseAvType2MedAktørId1MedSek3);
        }

        if (hendelseMedHøyestSeksvensnummerogKilde == null) {
            hendelseMedHøyestSeksvensnummerogKilde = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD).aktørId("1000000002")
                .type("type3").kildeId(KILDE_ID).build();
            hendelseMedHøyestSeksvensnummerogKilde.setSekvensnummer(nesteSeksvensnummer());
            feedRepository.lagre(hendelseMedHøyestSeksvensnummerogKilde);
        }
    }

    private long nesteSeksvensnummer() {
        return feedRepository.hentNesteSekvensnummer(FpVedtakUtgåendeHendelse.class);
    }

    @Test
    public void skal_lagre_fp_hendelse() {
        lagreHendelser();
        FpVedtakUtgåendeHendelse utgåendeHendelse = byggUtgåendeFpHendelse();
        long id = feedRepository.lagre(utgåendeHendelse);
        assertThat(id).isGreaterThanOrEqualTo(1);
        Optional<UtgåendeHendelse> utgåendeHendelse1 = feedRepository.hentUtgåendeHendelse(id);

        assertThat(utgåendeHendelse1.get()).isNotNull();
        assertThat(utgåendeHendelse1.get().getId()).isEqualTo(id);
        assertThat(utgåendeHendelse1.get().getSekvensnummer()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    public void skal_lagre_svp_hendelse() {
        lagreHendelser();
        SvpVedtakUtgåendeHendelse utgåendeHendelse = byggUtgåendeSvpHendelse();
        long id = feedRepository.lagre(utgåendeHendelse);
        assertThat(id).isGreaterThanOrEqualTo(1);
        Optional<UtgåendeHendelse> utgåendeHendelse1 = feedRepository.hentUtgåendeHendelse(id);

        assertThat(utgåendeHendelse1.get()).isNotNull();
        assertThat(utgåendeHendelse1.get().getId()).isEqualTo(id);
        assertThat(utgåendeHendelse1.get().getSekvensnummer()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    public void skal_returnere_true_hvis_hendelse_med_kilde_id_eksisterer() {
        lagreHendelser();
        assertThat(feedRepository.harHendelseMedKildeId(FpVedtakUtgåendeHendelse.class, KILDE_ID)).isTrue();
    }

    @Test
    public void skal_returnere_false_hvis_hendelse_med_kilde_id_ikke_eksisterer() {
        lagreHendelser();
        assertThat(feedRepository.harHendelseMedKildeId(FpVedtakUtgåendeHendelse.class, "eksisterer_ikke")).isFalse();
    }

    @Test
    public void skal_lagre_hendelse_flushe_sjekke_om_kilde_eksisterer() {
        lagreHendelser();
        assertThat(feedRepository.harHendelseMedKildeId(FpVedtakUtgåendeHendelse.class, "ny_kilde")).isFalse();
        FpVedtakUtgåendeHendelse utgåendeHendelse = FpVedtakUtgåendeHendelse.builder().payload(PAYLOAD)
            .aktørId("1000000002")
            .type("type3")
            .kildeId("ny_kilde")
            .build();
        feedRepository.lagre(utgåendeHendelse);
        assertThat(feedRepository.harHendelseMedKildeId(FpVedtakUtgåendeHendelse.class, "ny_kilde")).isTrue();
    }

    @Test
    public void skal_hente_hendelser_med_type1() {
        lagreHendelser();
        List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder()
                    .medSisteLestSekvensId(hendelseAvType1MedAktørId1MedSek1.getSekvensnummer() - 1)
                    .medType(TYPE1)
                    .medMaxAntall(100L).build());

        assertThat(alle).contains(hendelseAvType1MedAktørId1MedSek1, hendelseAvType1MedAktørId2MedSek2);
    }

    @Test
    public void skal_hente_alle_hendelser_med_sekvens_id_større_enn_sist_lest() {
        lagreHendelser();
        List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(hendelseAvType1MedAktørId2MedSek2.getSekvensnummer() - 1)
                    .medMaxAntall(100L).build());

        assertThat(alle).contains(hendelseAvType1MedAktørId2MedSek2, hendelseAvType2MedAktørId1MedSek3, hendelseMedHøyestSeksvensnummerogKilde);
    }

    @Test
    public void skal_returnerer_tom_liste_hvis_result_set_er_tom() {
        lagreHendelser();
        List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(nesteSeksvensnummer() + 100).medMaxAntall(100L).build());

        assertThat(alle).isEmpty();
    }

    @Test
    public void skal_hente_alle_hendelser_med_aktør_id() {
        lagreHendelser();
        List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(0L).medAktørId(AKTØR_ID_2).medMaxAntall(100L).build());

        assertThat(alle).containsOnly(hendelseAvType1MedAktørId2MedSek2);
    }

    @Test
    public void skal_hente_max_antall_1() {
        lagreHendelser();
        List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(hendelseAvType1MedAktørId1MedSek1.getSekvensnummer() - 1)
                    .medMaxAntall(1L).build());

        assertThat(alle).containsOnly(hendelseAvType1MedAktørId1MedSek1);
    }

    @Test
    public void skal_hente_max_antall_4_med_hopp_i_sekvensnummer() {
        lagreHendelser();
        List<FpVedtakUtgåendeHendelse> alle = feedRepository.hentUtgåendeHendelser(FpVedtakUtgåendeHendelse.class,
                new HendelseCriteria.Builder().medSisteLestSekvensId(hendelseAvType1MedAktørId1MedSek1.getSekvensnummer() - 1)
                    .medMaxAntall(4L).build());

        assertThat(alle).containsOnly(hendelseAvType1MedAktørId1MedSek1, hendelseAvType1MedAktørId2MedSek2,
                hendelseAvType2MedAktørId1MedSek3, hendelseMedHøyestSeksvensnummerogKilde);
    }

    private static FpVedtakUtgåendeHendelse byggUtgåendeFpHendelse() {
        return FpVedtakUtgåendeHendelse.builder()
                .payload(PAYLOAD)
                .aktørId(AKTØR_ID)
                .type(TYPE1)
                .build();
    }

    private static SvpVedtakUtgåendeHendelse byggUtgåendeSvpHendelse() {
        return SvpVedtakUtgåendeHendelse.builder()
                .payload(PAYLOAD)
                .aktørId(AKTØR_ID)
                .type(TYPE1)
                .build();
    }
}
