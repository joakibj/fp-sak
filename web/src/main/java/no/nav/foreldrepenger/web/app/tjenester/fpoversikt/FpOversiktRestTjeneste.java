package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("fpoversikt")
@ApplicationScoped
@Transactional
public class FpOversiktRestTjeneste {

    private FpOversiktDtoTjeneste dtoTjeneste;

    @Inject
    public FpOversiktRestTjeneste(FpOversiktDtoTjeneste dtoTjeneste) {
        this.dtoTjeneste = dtoTjeneste;
    }

    FpOversiktRestTjeneste() {
        //CDI
    }

    @GET
    @Path("sak")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent sak for bruk i fpoversikt", tags = "fpoversikt")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Sak hentSak(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class) @NotNull @Parameter(description = "Saksnummer for fagsak") @QueryParam("saksnummer") @Valid SaksnummerDto saksnummerDto) {
        return dtoTjeneste.hentSak(saksnummerDto.getVerdi());
    }
}
