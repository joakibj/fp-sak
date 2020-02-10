package no.nav.foreldrepenger.web.app.tjenester.behandling;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.net.URISyntaxException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.ProsessTaskGruppeIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.UtvidetBehandlingDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

/**
 * Annotasjonen @Path("") fungerer ikke alltid etter oppgradering til fp-felles 1.3.1-20191209145544-0d1eda9.
 * For å komme videre med oppgradering til nyere felles-versjoner innføres dette som et midlertidig hack.
 * Hensikten er å få splittet opp BehandlingRestTjeneste i unike rot-nivåer igjen,
 * slik at det er mulig å angi ikke-tom @Path-annotasjon på klassen.
 *
 * Langsiktig løsning som fjerner hacket er skissert i TFP-2237
 * */
@ApplicationScoped
@Transaction
@Path(BehandlingRestTjenestePathHack1.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class BehandlingRestTjenestePathHack1 {

    static final String BASE_PATH = "/behandling";
    private static final String BEHANDLING_PART_PATH = "";
    public static final String BEHANDLING_PATH = BASE_PATH + BEHANDLING_PART_PATH;
    private static final String STATUS_PART_PATH = "/status";
    public static final String STATUS_PATH = BASE_PATH + STATUS_PART_PATH;

    public BehandlingRestTjenestePathHack1() {
        // for resteasy
    }

    private BehandlingRestTjeneste behandlingRestTjeneste;

    @Inject
    public BehandlingRestTjenestePathHack1(BehandlingRestTjeneste behandlingRestTjeneste) {
        this.behandlingRestTjeneste = behandlingRestTjeneste;
    }

    @GET
    @Path(STATUS_PART_PATH)
    @Operation(description = "Url for å polle på behandling mens behandlingprosessen pågår i bakgrunnen(asynkront)",
        summary = ("Returnerer link til enten samme (hvis ikke ferdig) eller redirecter til /behandlinger dersom asynkrone operasjoner er ferdig."),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer Status",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AsyncPollingStatus.class)
                )
            ),
            @ApiResponse(responseCode = "303",
                description = "Behandling tilgjenglig (prosesstasks avsluttet)",
                headers = @Header(name = HttpHeaders.LOCATION)
            ),
            @ApiResponse(responseCode = "418",
                description = "ProsessTasks har feilet",
                headers = @Header(name = HttpHeaders.LOCATION),
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AsyncPollingStatus.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingMidlertidigStatus(
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto,
        @QueryParam("gruppe") @Valid ProsessTaskGruppeIdDto gruppeDto
    ) throws URISyntaxException {
        return behandlingRestTjeneste.hentBehandlingMidlertidigStatus(new BehandlingIdDto(uuidDto), gruppeDto);
    }

    @GET
    @Path(BEHANDLING_PART_PATH)
    @Operation(description = "Hent behandling gitt id",
        summary = ("Returnerer behandlingen som er tilknyttet id. Dette er resultat etter at asynkrone operasjoner er utført."),
        tags = "behandlinger",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer Behandling",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = UtvidetBehandlingDto.class)
                )
            )
        }
    )
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentBehandlingResultat(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return behandlingRestTjeneste.hentBehandlingResultat(new BehandlingIdDto(uuidDto));
    }
}
