package no.nav.foreldrepenger.web.app.tjenester.forvaltning.fpoversikt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/fpoversiktMigrering")
@ApplicationScoped
@Transactional
public class FpoversiktMigreringRestTjeneste {

    private ProsessTaskTjeneste taskTjeneste;

    public FpoversiktMigreringRestTjeneste() {
        // For CDI
    }

    @Inject
    public FpoversiktMigreringRestTjeneste(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    @POST
    @Operation(description = "Oppretter task for migrering", tags = "FORVALTNING-migrering")
    @Path("/opprettTask")
    @Consumes(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    public Response opprettTask(@Valid Interval interval) {
        var sakDato = interval.fom;
        var startDelay = 0;
        var tasks = new ArrayList<ProsessTaskData>();
        while (!sakDato.isAfter(interval.tom)) {
            var task = opprettTaskForDato(sakDato, startDelay);
            tasks.add(task);
            startDelay+=interval.delayBetween();
            sakDato = sakDato.plusDays(1);
        }
        var gruppe = new ProsessTaskGruppe();
        gruppe.addNesteParallell(tasks);
        taskTjeneste.lagre(gruppe);
        return Response.ok().build();
    }

    private static ProsessTaskData opprettTaskForDato(LocalDate dato, int delay) {
        var prosessTaskData = ProsessTaskData.forProsessTask(FpoversiktMigeringBehandlingHendelseTask.class);
        prosessTaskData.setProperty(FpoversiktMigeringBehandlingHendelseTask.DATO_KEY, dato.toString());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskData.setPrioritet(150);
        prosessTaskData.setNesteKjøringEtter(LocalDateTime.now().plusSeconds(delay));
        return prosessTaskData;
    }

    private record Interval(LocalDate fom, LocalDate tom, @Min(0) @Max(60) int delayBetween) implements AbacDto {
        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }
    }
}