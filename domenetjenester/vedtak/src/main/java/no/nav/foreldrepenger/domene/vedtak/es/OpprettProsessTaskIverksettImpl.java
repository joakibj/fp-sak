package no.nav.foreldrepenger.domene.vedtak.es;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksettFelles;
import no.nav.foreldrepenger.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.foreldrepenger.domene.vedtak.intern.SendVedtaksbrevTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOppgaveTilbakekrevingTask;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class OpprettProsessTaskIverksettImpl extends OpprettProsessTaskIverksettFelles {

    OpprettProsessTaskIverksettImpl() {
        // for CDI proxy
    }

    @Inject
    public OpprettProsessTaskIverksettImpl(ProsessTaskRepository prosessTaskRepository, OppgaveTjeneste oppgaveTjeneste) {
        super(prosessTaskRepository, oppgaveTjeneste);
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling, List<String> inititellTaskNavn) {
        ProsessTaskGruppe taskData;
        ProsessTaskData avsluttBehandling = new ProsessTaskData(AvsluttBehandlingTask.TASKTYPE);
        Optional<ProsessTaskData> avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling, behandling.erRevurdering() ? OppgaveÅrsak.REVURDER : OppgaveÅrsak.BEHANDLE_SAK, false);
        ProsessTaskData sendVedtaksbrev = new ProsessTaskData(SendVedtaksbrevTask.TASKTYPE);

        ProsessTaskData vurderOgSendØkonomiOppdrag = new ProsessTaskData(VurderOgSendØkonomiOppdragTask.TASKTYPE);
        ProsessTaskData vedtakTilDatavarehus = new ProsessTaskData(VEDTAK_TIL_DATAVAREHUS_TASK);
        taskData = new ProsessTaskGruppe();

        List<ProsessTaskData> parallelle = new ArrayList<>();
        parallelle.add(sendVedtaksbrev);
        parallelle.add(vurderOgSendØkonomiOppdrag);
        avsluttOppgave.ifPresent(parallelle::add);
        taskData.addNesteParallell(parallelle);
        taskData.addNesteSekvensiell(vedtakTilDatavarehus);
        taskData.addNesteSekvensiell(avsluttBehandling);

        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        taskData.setCallIdFraEksisterende();

        prosessTaskRepository.lagre(taskData);

        // Opprettes som egen task da den er uavhengig av de andre
        prosessTaskRepository.lagre(opprettTaskVurderOppgaveTilbakekrevingES(behandling));
    }

    private ProsessTaskData opprettTaskVurderOppgaveTilbakekrevingES(Behandling behandling) {
        ProsessTaskData vurderOppgaveTilbakekreving = new ProsessTaskData(VurderOppgaveTilbakekrevingTask.TASKTYPE);
        vurderOppgaveTilbakekreving.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        vurderOppgaveTilbakekreving.setCallIdFraEksisterende();
        return vurderOppgaveTilbakekreving;
    }
}
