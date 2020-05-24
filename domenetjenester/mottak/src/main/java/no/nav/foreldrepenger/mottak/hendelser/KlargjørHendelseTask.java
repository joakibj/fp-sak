package no.nav.foreldrepenger.mottak.hendelser;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.kontrakter.abonnent.HendelseDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

@ApplicationScoped
@ProsessTask(KlargjørHendelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class KlargjørHendelseTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "hendelser.klargjoering";
    public static final String PROPERTY_HENDELSE_TYPE = "hendelseType";
    public static final String PROPERTY_UID = "hendelseUid";

    private ForretningshendelseMottak forretningshendelseMottak;

    KlargjørHendelseTask() {
        // for CDI proxy
    }

    @Inject
    public KlargjørHendelseTask(ForretningshendelseMottak forretningshendelseMottak) {
        this.forretningshendelseMottak = forretningshendelseMottak;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        ForretningshendelseType hendelseType = ForretningshendelseType.fraKode(prosessTaskData.getPropertyValue(PROPERTY_HENDELSE_TYPE));
        HendelseDto hendelse = JsonMapper.fromJson(prosessTaskData.getPayloadAsString(), HendelseDto.class);
        forretningshendelseMottak.mottaForretningshendelse(hendelseType, hendelse);
    }
}
