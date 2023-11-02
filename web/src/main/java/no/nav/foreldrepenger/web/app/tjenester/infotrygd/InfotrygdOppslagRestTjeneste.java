package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdFPGrunnlag;
import no.nav.foreldrepenger.mottak.vedtak.rest.InfotrygdSvpGrunnlag;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SokefeltDto;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.pdl.HentIdenterQueryRequest;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.IdentInformasjonResponseProjection;
import no.nav.pdl.IdentlisteResponseProjection;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.person.Persondata;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(InfotrygdOppslagRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class InfotrygdOppslagRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InfotrygdOppslagRestTjeneste.class);

    private static final LocalDate FOM = LocalDate.of(2000, 1,1);

    static final String BASE_PATH = "/infotrygd";
    private static final String INFOTRYGD_SOK_PART_PATH = "/sok";
    public static final String INFOTRYGD_SOK_PATH = BASE_PATH + INFOTRYGD_SOK_PART_PATH;

    private Persondata pdlKlient;
    private InfotrygdFPGrunnlag foreldrepenger;
    private InfotrygdSvpGrunnlag svangerskapspenger;

    public InfotrygdOppslagRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public InfotrygdOppslagRestTjeneste(Persondata pdlKlient,
                                        InfotrygdFPGrunnlag foreldrepenger,
                                        InfotrygdSvpGrunnlag svangerskapspenger) {
        this.pdlKlient = pdlKlient;
        this.foreldrepenger = foreldrepenger;
        this.svangerskapspenger = svangerskapspenger;
    }

    @POST
    @Path(INFOTRYGD_SOK_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Søk etter utbetalinger i Infotrygd for fødselsnummer", tags = "infotrygd",
        summary = "Oversikt over utbetalinger knyttet til en bruker kan søkes via fødselsnummer eller d-nummer.")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response sokInfotrygd(@TilpassetAbacAttributt(supplierClass = SøkeFeltAbacDataSupplier.class)
        @Parameter(description = "Søkestreng kan være aktørId, fødselsnummer eller D-nummer.") @Valid SokefeltDto søkestreng) {
        var trimmed = søkestreng.getSearchString() != null ? søkestreng.getSearchString().trim() : "";
        var ident = PersonIdent.erGyldigFnr(trimmed) || AktørId.erGyldigAktørId(trimmed) ? trimmed : null;
        if (ident == null) {
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
        }
        Set<String> identer = new LinkedHashSet<>();
        if (PersonIdent.erGyldigFnr(ident)) {
            identer.add(ident);
        }
        identer.addAll(finnAlleHistoriskeFødselsnummer(ident));
        if (identer.isEmpty()) {
            return Response.status(HttpURLConnection.HTTP_NOT_FOUND).build();
        }
        List<Grunnlag> grunnlagene = new ArrayList<>();
        identer.forEach(i -> {
            // Ser på Dtos senere. Først må vi utforske litt innhold via swagger.
            grunnlagene.addAll(foreldrepenger.hentGrunnlagFailSoft(i, FOM, LocalDate.now()));
            grunnlagene.addAll(svangerskapspenger.hentGrunnlagFailSoft(i, FOM, LocalDate.now()));
        });
        return Response.ok(grunnlagene).build();
    }

    private List<String> finnAlleHistoriskeFødselsnummer(String inputIdent) {
        var request = new HentIdenterQueryRequest();
        request.setIdent(inputIdent);
        request.setGrupper(List.of(IdentGruppe.FOLKEREGISTERIDENT));
        request.setHistorikk(Boolean.TRUE);
        var projection = new IdentlisteResponseProjection()
            .identer(new IdentInformasjonResponseProjection().ident());

        try {
            var identliste = pdlKlient.hentIdenter(request, projection);
            return identliste.getIdenter().stream().map(IdentInformasjon::getIdent).toList();
        } catch (VLException v) {
            if (Persondata.PDL_KLIENT_NOT_FOUND_KODE.equals(v.getKode())) {
                return List.of();
            }
            throw v;
        } catch (ProcessingException e) {
            throw e.getCause() instanceof SocketTimeoutException ? new IntegrasjonException("FP-723618", "PDL timeout") : e;
        }
    }

    public static class SøkeFeltAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (SokefeltDto) obj;
            var attributter = AbacDataAttributter.opprett();
            var søkestring = req.getSearchString() != null ? req.getSearchString().trim() : "";
            if (søkestring.length() == 13 /* guess - aktørId */) {
                attributter.leggTil(AppAbacAttributtType.AKTØR_ID, søkestring)
                    .leggTil(AppAbacAttributtType.SAKER_FOR_AKTØR, søkestring);
            } else if (søkestring.length() == 11 /* guess - FNR */) {
                attributter.leggTil(AppAbacAttributtType.FNR, søkestring);
            }
            return attributter;
        }
    }


}
