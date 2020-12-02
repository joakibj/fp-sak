package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.gjenopptak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKobling;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.TaskStatus;

public class AutomatiskGjenopptagelseTjenesteTest {

    private AutomatiskGjenopptagelseTjeneste tjeneste; // objektet vi tester

    private ProsessTaskRepository mockProsessTaskRepository;

    private BehandlingKandidaterRepository mockBehandlingKandidaterRepository;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setup() {
        mockProsessTaskRepository = mock(ProsessTaskRepository.class);
        mockBehandlingKandidaterRepository = mock(BehandlingKandidaterRepository.class);
        oppgaveBehandlingKoblingRepository = mock(OppgaveBehandlingKoblingRepository.class);
        behandlingRepository = mock(BehandlingRepository.class);
        tjeneste = new AutomatiskGjenopptagelseTjeneste(mockBehandlingKandidaterRepository, oppgaveBehandlingKoblingRepository, behandlingRepository,
                mockProsessTaskRepository);
    }

    @Test
    public void skal_ha_0_arg_ctor_for_cdi() {

        new AutomatiskGjenopptagelseTjeneste();
    }

    @Test
    public void skal_lage_prosess_tasks_for_behandlinger_som_skal_gjenopptas() {

        final String gruppe = "55";

        // Arrange

        Behandling behandling1 = lagMockBehandling();
        final AktørId aktørId1 = behandling1.getAktørId();
        final Long fagsakId1 = behandling1.getFagsakId();
        final Long behandlingId1 = behandling1.getId();

        Behandling behandling2 = lagMockBehandling();
        final AktørId aktørId2 = behandling2.getAktørId();
        final Long fagsakId2 = behandling2.getFagsakId();
        final Long behandlingId2 = behandling2.getId();

        List<Behandling> list = List.of(behandling1, behandling2);

        when(mockBehandlingKandidaterRepository.finnBehandlingerForAutomatiskGjenopptagelse()).thenReturn(list);

        List<ProsessTaskData> faktiskeProsessTaskDataListe = new ArrayList<>();
        doAnswer((Answer<Void>) invocation -> {
            ProsessTaskData data = (ProsessTaskData) invocation.getArguments()[0];
            if (data.getGruppe() == null) {
                data.setGruppe(gruppe);
            }
            faktiskeProsessTaskDataListe.add(data);
            return null;
        }).when(mockProsessTaskRepository).lagre(any(ProsessTaskData.class));

        // Act

        tjeneste.gjenopptaBehandlinger();

        // Assert

        assertThat(faktiskeProsessTaskDataListe).hasSize(2);

        ProsessTaskData faktiskProsessTaskData1 = faktiskeProsessTaskDataListe.get(0);
        assertThat(faktiskProsessTaskData1.getAktørId()).isEqualTo(aktørId1.getId());
        assertThat(faktiskProsessTaskData1.getFagsakId()).isEqualTo(fagsakId1);
        assertThat(faktiskProsessTaskData1.getBehandlingId()).isEqualTo(behandlingId1.toString());
        assertThat(faktiskProsessTaskData1.getGruppe()).isEqualTo(gruppe);
        assertThat(faktiskProsessTaskData1.getSekvens()).isEqualTo("1");
        assertThat(faktiskProsessTaskData1.getPriority()).isEqualTo(100);

        ProsessTaskData faktiskProsessTaskData2 = faktiskeProsessTaskDataListe.get(1);
        assertThat(faktiskProsessTaskData2.getAktørId()).isEqualTo(aktørId2.getId());
        assertThat(faktiskProsessTaskData2.getFagsakId()).isEqualTo(fagsakId2);
        assertThat(faktiskProsessTaskData2.getBehandlingId()).isEqualTo(behandlingId2.toString());
        assertThat(faktiskProsessTaskData2.getGruppe()).isEqualTo(gruppe);
        assertThat(faktiskProsessTaskData2.getSekvens()).isEqualTo("1");
        assertThat(faktiskProsessTaskData2.getPriority()).isEqualTo(100);
    }

    @Test
    public void skal_lage_prosess_tasks_for_behandlinger_som_skal_gjenopplives() {

        final String gruppe = "55";

        // Arrange

        Behandling behandling1 = lagMockBehandling();
        final AktørId aktørId1 = behandling1.getAktørId();
        final Long fagsakId1 = behandling1.getFagsakId();
        final Long behandlingId1 = behandling1.getId();

        Behandling behandling2 = lagMockBehandling();
        final AktørId aktørId2 = behandling2.getAktørId();
        final Long fagsakId2 = behandling2.getFagsakId();
        final Long behandlingId2 = behandling2.getId();

        List<Behandling> list = List.of(behandling1, behandling2);

        when(mockBehandlingKandidaterRepository.finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt()).thenReturn(list);

        List<ProsessTaskData> faktiskeProsessTaskDataListe = new ArrayList<>();
        doAnswer((Answer<Void>) invocation -> {
            ProsessTaskData data = (ProsessTaskData) invocation.getArguments()[0];
            if (data.getGruppe() == null) {
                data.setGruppe(gruppe);
            }
            faktiskeProsessTaskDataListe.add(data);
            return null;
        }).when(mockProsessTaskRepository).lagre(any(ProsessTaskData.class));

        // Act

        tjeneste.gjenopplivBehandlinger();

        // Assert

        assertThat(faktiskeProsessTaskDataListe).hasSize(2);

        ProsessTaskData faktiskProsessTaskData1 = faktiskeProsessTaskDataListe.get(0);
        assertThat(faktiskProsessTaskData1.getAktørId()).isEqualTo(aktørId1.getId());
        assertThat(faktiskProsessTaskData1.getFagsakId()).isEqualTo(fagsakId1);
        assertThat(faktiskProsessTaskData1.getBehandlingId()).isEqualTo(behandlingId1.toString());
        assertThat(faktiskProsessTaskData1.getGruppe()).isEqualTo(gruppe);
        assertThat(faktiskProsessTaskData1.getSekvens()).isEqualTo("1");
        assertThat(faktiskProsessTaskData1.getPriority()).isEqualTo(100);

        ProsessTaskData faktiskProsessTaskData2 = faktiskeProsessTaskDataListe.get(1);
        assertThat(faktiskProsessTaskData2.getAktørId()).isEqualTo(aktørId2.getId());
        assertThat(faktiskProsessTaskData2.getFagsakId()).isEqualTo(fagsakId2);
        assertThat(faktiskProsessTaskData2.getBehandlingId()).isEqualTo(behandlingId2.toString());
        assertThat(faktiskProsessTaskData2.getGruppe()).isEqualTo(gruppe);
        assertThat(faktiskProsessTaskData2.getSekvens()).isEqualTo("1");
        assertThat(faktiskProsessTaskData2.getPriority()).isEqualTo(100);
    }

    @Test
    public void skal_hente_statuser_for_gjenopptaBehandling_gruppe() {
        // Arrange
        final TaskStatus status1 = new TaskStatus(ProsessTaskStatus.FERDIG, new BigDecimal(1));
        final TaskStatus status2 = new TaskStatus(ProsessTaskStatus.FEILET, new BigDecimal(1));
        final List<TaskStatus> statusListFromRepo = List.of(status1, status2);
        when(mockProsessTaskRepository.finnStatusForTaskIGruppe(same(GjenopptaBehandlingTask.TASKTYPE), anyString())).thenReturn(statusListFromRepo);

        // Act
        List<TaskStatus> statusListFromSvc = tjeneste.hentStatusForGjenopptaBehandlingGruppe("gruppa");

        // Assert
        assertThat(statusListFromSvc).containsExactly(status1, status2);
    }

    @Test
    public void skal_lage_prosess_tasks_for_behandlinger_som_skal_oppdateres() {

        final String gruppe = "55";

        // Arrange

        Behandling behandling1 = lagMockBehandling();
        final AktørId aktørId1 = behandling1.getAktørId();
        final Long fagsakId1 = behandling1.getFagsakId();
        final Long behandlingId1 = behandling1.getId();
        OppgaveBehandlingKobling obk1 = lagMockOppgave(behandling1, OppgaveÅrsak.BEHANDLE_SAK);

        Behandling behandling2 = lagMockBehandling();
        final Long behandlingId2 = behandling2.getId();
        OppgaveBehandlingKobling obk2 = lagMockOppgave(behandling2, OppgaveÅrsak.REVURDER);

        List<OppgaveBehandlingKobling> list = List.of(obk1, obk2);

        when(oppgaveBehandlingKoblingRepository.hentUferdigeOppgaverOpprettetTidsrom(any(), any(), anySet())).thenReturn(list);
        when(behandlingRepository.hentBehandling(behandlingId1)).thenReturn(behandling1);
        when(behandlingRepository.hentBehandling(behandlingId2)).thenReturn(behandling2);

        List<ProsessTaskData> faktiskeProsessTaskDataListe = new ArrayList<>();
        doAnswer((Answer<Void>) invocation -> {
            ProsessTaskData data = (ProsessTaskData) invocation.getArguments()[0];
            if (data.getGruppe() == null) {
                data.setGruppe(gruppe);
            }
            faktiskeProsessTaskDataListe.add(data);
            return null;
        }).when(mockProsessTaskRepository).lagre(any(ProsessTaskData.class));

        // Act

        tjeneste.oppdaterBehandlingerFraOppgaveFrist();

        // Assert

        assertThat(faktiskeProsessTaskDataListe).hasSize(2);

        ProsessTaskData faktiskProsessTaskData1 = faktiskeProsessTaskDataListe.get(0);
        assertThat(faktiskProsessTaskData1.getAktørId()).isEqualTo(aktørId1.getId());
        assertThat(faktiskProsessTaskData1.getFagsakId()).isEqualTo(fagsakId1);
        assertThat(faktiskProsessTaskData1.getBehandlingId()).isEqualTo(behandlingId1.toString());
        assertThat(faktiskProsessTaskData1.getGruppe()).isEqualTo(gruppe);
        assertThat(faktiskProsessTaskData1.getSekvens()).isEqualTo("1");
        assertThat(faktiskProsessTaskData1.getPriority()).isEqualTo(100);
    }

    private Behandling lagMockBehandling() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = scenario.lagMocked();
        return behandling;
    }

    private OppgaveBehandlingKobling lagMockOppgave(Behandling behandling, OppgaveÅrsak type) {
        return new OppgaveBehandlingKobling(type, null, null, behandling.getId());
    }
}
