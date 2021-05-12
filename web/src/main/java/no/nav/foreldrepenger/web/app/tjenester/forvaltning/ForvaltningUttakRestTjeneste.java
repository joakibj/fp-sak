package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningUttak")
@ApplicationScoped
@Transactional
public class ForvaltningUttakRestTjeneste {

    private ForvaltningUttakTjeneste forvaltningUttakTjeneste;

    @Inject
    public ForvaltningUttakRestTjeneste(ForvaltningUttakTjeneste forvaltningUttakTjeneste) {
        this.forvaltningUttakTjeneste = forvaltningUttakTjeneste;
    }

    public ForvaltningUttakRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/leggTilUttakPåOpphørtFpBehandling")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Legg til uttak på opphørt behandling. Alle periodene avslås.", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response leggTilOpphørUttakPåOpphørtFpBehandling(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Objects.requireNonNull(dto.getBehandlingUUID(), "Støtter bare UUID");

        if (!forvaltningUttakTjeneste.erFerdigForeldrepengerBehandlingSomHarFørtTilOpphør(dto.getBehandlingUUID())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Behandlingen må være type foreldrepenger, avsluttet og ført til oppfør")
                    .build();
        }

        forvaltningUttakTjeneste.lagOpphørtUttaksresultat(dto.getBehandlingUUID());
        return Response.noContent().build();
    }

    @POST
    @Path("/beregn-kontoer")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Beregner kontoer basert på data fra behandlingen. Husk å revurdere begge foreldre", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response beregnKontoer(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Objects.requireNonNull(dto.getBehandlingUUID(), "Støtter bare UUID");
        forvaltningUttakTjeneste.beregnKontoer(dto.getBehandlingUUID());
        return Response.noContent().build();
    }

    @POST
    @Path("/endre-annen-forelder-rett")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Endrer resultat av AP om annen forelder har rett", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response endreAnnenForelderRett(@BeanParam @Valid ForvaltningBehandlingIdDto dto,
            @QueryParam(value = "harRett") @Valid Boolean harRett) {
        Objects.requireNonNull(dto.getBehandlingUUID(), "Støtter bare UUID");

        forvaltningUttakTjeneste.endreAnnenForelderHarRett(dto.getBehandlingUUID(), harRett);
        return Response.noContent().build();
    }
}
