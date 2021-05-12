package no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktKode;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.Redirect;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@ApplicationScoped
@Transactional
@Path(AksjonspunktRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class AksjonspunktRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String AKSJONSPUNKT_OVERSTYR_PART_PATH = "/aksjonspunkt/overstyr";
    public static final String AKSJONSPUNKT_OVERSTYR_PATH = BASE_PATH + AKSJONSPUNKT_OVERSTYR_PART_PATH;
    private static final String AKSJONSPUNKT_PART_PATH = "/aksjonspunkt";
    public static final String AKSJONSPUNKT_PATH = BASE_PATH + AKSJONSPUNKT_PART_PATH;
    private static final String AKSJONSPUNKT_V2_PART_PATH = "/aksjonspunkt-v2";
    public static final String AKSJONSPUNKT_V2_PATH = BASE_PATH + AKSJONSPUNKT_V2_PART_PATH;
    private static final String AKSJONSPUNKT_RISIKO_PART_PATH = "/aksjonspunkt/risiko";
    public static final String AKSJONSPUNKT_RISIKO_PATH = BASE_PATH + AKSJONSPUNKT_RISIKO_PART_PATH;
    private static final String AKSJONSPUNKT_KONTROLLER_REVURDERING_PART_PATH = "/aksjonspunkt/kontroller-revurdering";
    public static final String AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH = BASE_PATH + AKSJONSPUNKT_KONTROLLER_REVURDERING_PART_PATH;

    private AksjonspunktTjeneste applikasjonstjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsutredningTjeneste behandlingutredningTjeneste;
    private TotrinnTjeneste totrinnTjeneste;

    public AksjonspunktRestTjeneste() {
        // Bare for RESTeasy
    }

    @Inject
    public AksjonspunktRestTjeneste(
        AksjonspunktTjeneste aksjonpunktApplikasjonTjeneste,
        BehandlingRepository behandlingRepository,
        BehandlingsutredningTjeneste behandlingutredningTjeneste, TotrinnTjeneste totrinnTjeneste) {

        this.applikasjonstjeneste = aksjonpunktApplikasjonTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingutredningTjeneste = behandlingutredningTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(AKSJONSPUNKT_PART_PATH)
    @Operation(description = "Hent aksjonspunter for en behandling", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Set.class), schema = @Schema(implementation = AksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getAksjonspunkter(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
        @NotNull @QueryParam("behandlingId") @Valid BehandlingIdDto behandlingIdDto) { // NOSONAR
        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var ttVurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
        var dto = AksjonspunktDtoMapper.lagAksjonspunktDto(behandling, ttVurderinger);
        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(AKSJONSPUNKT_V2_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent aksjonspunter for en behandling", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Set.class), schema = @Schema(implementation = AksjonspunktDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getAksjonspunkter(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getAksjonspunkter(new BehandlingIdDto(uuidDto));
    }

    @GET
    @Path(AKSJONSPUNKT_RISIKO_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent risikoaksjonspunkt for en behandling", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AksjonspunktDto.class), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response getRisikoAksjonspunkt(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var dto = AksjonspunktDtoMapper.lagAksjonspunktDto(behandling, Collections.emptyList())
                .stream()
                .filter(ap -> AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE.equals(ap.getDefinisjon().getKode()))
                .findFirst()
                .orElse(null);

        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(dto).cacheControl(cc).build();
    }

    @GET
    @Path(AKSJONSPUNKT_KONTROLLER_REVURDERING_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har behandling åpent kontroller revurdering aksjonspunkt", tags = "aksjonspunkt", responses = {
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Boolean.class), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response erKontrollerRevurderingAksjonspunktÅpent(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var harÅpentAksjonspunkt = behandling
                .harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
        var cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        return Response.ok(harÅpentAksjonspunkt).cacheControl(cc).build();
    }

    /**
     * Håndterer prosessering av aksjonspunkt og videre behandling.
     * <p>
     * MERK: Det skal ikke ligge spesifikke sjekker som avhenger av status på
     * behanlding, steg eller knytning til spesifikke aksjonspunkter idenne
     * tjenesten.
     *
     */
    @POST
    @Path(AKSJONSPUNKT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre endringer gitt av aksjonspunktene og rekjør behandling fra gjeldende steg", tags = "aksjonspunkt")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response bekreft(@TilpassetAbacAttributt(supplierClass = BekreftetAbacDataSupplier.class)
            @Parameter(description = "Liste over aksjonspunkt som skal bekreftes, inklusiv data som trengs for å løse de.") @Valid BekreftedeAksjonspunkterDto apDto) { // NOSONAR

        var bekreftedeAksjonspunktDtoer = apDto.getBekreftedeAksjonspunktDtoer();

        var behandlingId = apDto.getBehandlingId().getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(apDto.getBehandlingId().getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling, apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getBekreftedeAksjonspunktDtoer());

        applikasjonstjeneste.bekreftAksjonspunkter(bekreftedeAksjonspunktDtoer, behandling.getId());

        return Redirect.tilBehandlingPollStatus(behandling.getUuid());
    }

    /**
     * Oppretting og prosessering av aksjonspunkt som har type overstyringspunkt.
     * <p>
     * MERK: Det skal ikke ligge spesifikke sjekker som avhenger av status på
     * behanlding, steg eller knytning til spesifikke aksjonspunkter idenne
     * tjenesten.
     */
    @POST
    @Path(AKSJONSPUNKT_OVERSTYR_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Overstyrer stegene", tags = "aksjonspunkt")
    @BeskyttetRessurs(action = UPDATE, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public Response overstyr(@TilpassetAbacAttributt(supplierClass = OverstyrtAbacDataSupplier.class)
        @Parameter(description = "Liste over overstyring aksjonspunkter.") @Valid OverstyrteAksjonspunkterDto apDto) { // NOSONAR

        var behandlingIdDto = apDto.getBehandlingId();

        var behandlingId = behandlingIdDto.getBehandlingId();
        var behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());

        behandlingutredningTjeneste.kanEndreBehandling(behandling, apDto.getBehandlingVersjon());

        validerBetingelserForAksjonspunkt(behandling, apDto.getOverstyrteAksjonspunktDtoer());

        applikasjonstjeneste.overstyrAksjonspunkter(apDto.getOverstyrteAksjonspunktDtoer(), behandling.getId());

        return Redirect.tilBehandlingPollStatus(behandling.getUuid());
    }

    private static void validerBetingelserForAksjonspunkt(Behandling behandling, Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        // TODO (FC): skal ikke ha spesfikke pre-conditions inne i denne tjenesten
        // (sjekk på status FATTER_VEDTAK). Se
        // om kan håndteres annerledes.
        if (behandling.getStatus().equals(BehandlingStatus.FATTER_VEDTAK) && !erFatteVedtakAkpt(aksjonspunktDtoer)) {
            throw new FunksjonellException("FP-760743",
                String.format("Det kan ikke akseptere endringer siden totrinnsbehandling er startet og behandlingen med behandlingId: %s er hos beslutter", behandling.getId()),
                "Avklare med beslutter");
        }
    }

    private static boolean erFatteVedtakAkpt(Collection<? extends AksjonspunktKode> aksjonspunktDtoer) {
        return aksjonspunktDtoer.size() == 1 &&
                aksjonspunktDtoer.iterator().next().getKode().equals(AksjonspunktDefinisjon.FATTER_VEDTAK.getKode());
    }

    public static class BekreftetAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (BekreftedeAksjonspunkterDto) obj;
            var abac = AbacDataAttributter.opprett();

            if(req.getBehandlingId().getBehandlingId() != null) {
                abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, req.getBehandlingId().getBehandlingId());
            } else if (req.getBehandlingId().getBehandlingUuid() != null) {
                abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingId().getBehandlingUuid());
            }

            req.getBekreftedeAksjonspunktDtoer().forEach(apDto -> {
                abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, apDto.getKode());
                if (apDto instanceof AvklarVergeDto avklarVergeDto && avklarVergeDto.getFnr() != null) {
                    abac.leggTil(AppAbacAttributtType.FNR, avklarVergeDto.getFnr());
                }
                if (apDto instanceof ManuellRegistreringDto manuellRegistreringDto && manuellRegistreringDto.getAnnenForelder() != null && manuellRegistreringDto.getAnnenForelder().getFoedselsnummer() != null) {
                    abac.leggTil(AppAbacAttributtType.FNR, manuellRegistreringDto.getAnnenForelder().getFoedselsnummer());
                }
            });
            return abac;
        }
    }

    public static class OverstyrtAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (OverstyrteAksjonspunkterDto) obj;
            var abac = AbacDataAttributter.opprett();

            if (req.getBehandlingId().getBehandlingId() != null) {
                abac.leggTil(AppAbacAttributtType.BEHANDLING_ID, req.getBehandlingId().getBehandlingId());
            } else if (req.getBehandlingId().getBehandlingUuid() != null) {
                abac.leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingId().getBehandlingUuid());
            }
            req.getOverstyrteAksjonspunktDtoer().forEach(apDto -> abac.leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, apDto.getKode()));
            return abac;
        }
    }
}
