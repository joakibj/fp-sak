package no.nav.foreldrepenger.behandling.revurdering.satsregulering;

import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.SatsReguleringUtil.opprettFPSN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.SatsReguleringRepository;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class NæringsdrivendeReguleringSaksutvalgTest {

    @Mock
    private ProsessTaskTjeneste taskTjeneste;
    private GrunnbeløpFinnSakerTask tjeneste;

    private long gammelSats;
    private long nySats;
    private LocalDate cutoff;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        var beregningsresultatRepository = new BeregningsresultatRepository(entityManager);
        nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        tjeneste = new GrunnbeløpFinnSakerTask(new SatsReguleringRepository(entityManager), beregningsresultatRepository, taskTjeneste);
    }

    @Test
    void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        opprettFPSN(em, BehandlingStatus.UTREDES, cutoff.plusDays(5), gammelSats, gammelSats); // Har åpen behandling
        opprettFPSN(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(5), gammelSats, gammelSats * 4); // Uttak før "1/5"

        tjeneste.doTask(SatsReguleringUtil.lagFinnSakerTask("FP", "SN"));

        verifyNoInteractions(taskTjeneste);
    }

    @Test
    void skal_finne_tre_saker_til_revurdering(EntityManager em) {
        var kan1 = opprettFPSN(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), gammelSats, gammelSats); // Skal plukke
        var kan2 = opprettFPSN(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats, gammelSats * 8); // Skal plukke
        var kan3 = opprettFPSN(em, BehandlingStatus.AVSLUTTET, cutoff.plusDays(2), gammelSats, gammelSats * 4); // Skal plukke
        var kan4 = opprettFPSN(em, BehandlingStatus.AVSLUTTET, cutoff.minusDays(2), gammelSats, gammelSats * 2); // Uttak før "1/5"
        var kan5 = opprettFPSN(em, BehandlingStatus.AVSLUTTET, cutoff.plusWeeks(2), nySats, gammelSats * 7); // Har allerede ny G

        tjeneste.doTask(SatsReguleringUtil.lagFinnSakerTask("FP", "SN"));

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(3)).lagre(captor.capture());
        var taskTypeExpected = TaskType.forProsessTask(GrunnbeløpReguleringTask.class);
        assertThat(captor.getValue().taskType()).isEqualTo(taskTypeExpected);

        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan1)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan2)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan3)).isPresent();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan4)).isEmpty();
        assertThat(SatsReguleringUtil.finnTaskFor(captor, kan5)).isEmpty();
    }

}
