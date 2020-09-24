package no.nav.foreldrepenger.web.server.abac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeUgyldigInput;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journalposttyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.sikkerhet.abac.AbacAttributtSamling;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;
import no.nav.vedtak.sikkerhet.abac.NavAbacCommonAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;

@ExtendWith(MockitoExtension.class)
public class PdpRequestBuilderTest {

    private static final String DUMMY_ID_TOKEN = "dummyheader.dymmypayload.dummysignaturee";

    private static final Long FAGSAK_ID = 10001L;
    private static final Long FAGSAK_ID_2 = 10002L;
    private static final Long BEHANDLING_ID = 333L;
    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("444");
    private static final JournalpostId JOURNALPOST_ID_UGYLDIG = new JournalpostId("555");
    private static final String SAKSNUMMER = "7777";

    private static final AktørId AKTØR_0 = AktørId.dummy();
    private static final AktørId AKTØR_1 = AktørId.dummy();
    private static final AktørId AKTØR_2 = AktørId.dummy();

    private static final String PERSON_0 = "00000000000";

    @Mock
    private PipRepository pipRepository;
    @Mock
    private AktørConsumerMedCache aktørConsumer;

    private AppPdpRequestBuilderImpl requestBuilder;

    @BeforeEach
    public void beforeEach() {
        requestBuilder = new AppPdpRequestBuilderImpl(pipRepository, aktørConsumer);

    }

    @Test
    public void skal_hente_saksstatus_og_behandlingsstatus_når_behandlingId_er_input() throws Exception {
        AbacAttributtSamling attributter = byggAbacAttributtSamling().leggTil(AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_ID, BEHANDLING_ID));

        lenient().when(pipRepository.fagsakIdForJournalpostId(Collections.singleton(JOURNALPOST_ID))).thenReturn(Collections.singleton(FAGSAK_ID));
        lenient().when(pipRepository.hentAktørIdKnyttetTilFagsaker(Collections.singleton(FAGSAK_ID))).thenReturn(Collections.singleton(AKTØR_1));
        String behandligStatus = BehandlingStatus.OPPRETTET.getKode();
        String ansvarligSaksbehandler = "Z123456";
        String fagsakStatus = FagsakStatus.UNDER_BEHANDLING.getKode();
        lenient().when(pipRepository.hentDataForBehandling(BEHANDLING_ID)).thenReturn(
                Optional.of(new PipBehandlingsData(behandligStatus, ansvarligSaksbehandler, BigDecimal.valueOf(FAGSAK_ID), fagsakStatus)));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_1.getId());
        assertThat(request.getString(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_ANSVARLIG_SAKSBEHANDLER)).isEqualTo(ansvarligSaksbehandler);
        assertThat(request.getString(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_BEHANDLINGSSTATUS))
                .isEqualTo(AbacBehandlingStatus.OPPRETTET.getEksternKode());
        assertThat(request.getString(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_SAKSSTATUS))
                .isEqualTo(AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());
    }

    @Test
    public void skal_angi_aktørId_gitt_journalpost_id_som_input() throws HentKjerneJournalpostListeUgyldigInput {
        AbacAttributtSamling attributter = byggAbacAttributtSamling().leggTil(AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.JOURNALPOST_ID, JOURNALPOST_ID.getVerdi()));
        final HentKjerneJournalpostListeResponse mockJournalResponse = initJournalMockResponse(false);

        lenient().when(pipRepository.fagsakIdForJournalpostId(Collections.singleton(JOURNALPOST_ID))).thenReturn(Collections.singleton(FAGSAK_ID));
        lenient().when(pipRepository.fagsakIdForSaksnummer(Collections.singleton(SAKSNUMMER))).thenReturn(Collections.singleton(FAGSAK_ID));
        lenient().when(pipRepository.hentAktørIdKnyttetTilFagsaker(Collections.singleton(FAGSAK_ID))).thenReturn(Collections.singleton(AKTØR_1));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_1.getId());
    }

    @Test
    public void skal_hente_fnr_fra_alle_tilknyttede_saker_når_det_kommer_inn_søk_etter_saker_for_fnr() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKER_MED_FNR, PERSON_0));

        when(aktørConsumer.hentAktørIdForPersonIdentSet(any())).thenReturn(Collections.singleton(AKTØR_0.getId()));

        Set<Long> fagsakIder = new HashSet<>();
        fagsakIder.add(FAGSAK_ID);
        fagsakIder.add(FAGSAK_ID_2);
        when(pipRepository.fagsakIderForSøker(Collections.singleton(AKTØR_0))).thenReturn(fagsakIder);
        Set<AktørId> aktører = new HashSet<>();
        aktører.add(AKTØR_0);
        aktører.add(AKTØR_1);
        aktører.add(AKTØR_2);
        when(pipRepository.hentAktørIdKnyttetTilFagsaker(fagsakIder)).thenReturn(aktører);

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_0.getId(),
                AKTØR_1.getId(), AKTØR_2.getId());
    }

    @Test
    public void skal_bare_sende_fnr_vider_til_pdp() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.FNR, PERSON_0));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR)).containsOnly(PERSON_0);
    }

    @Test
    public void skal_ta_inn_aksjonspunkt_id_og_sende_videre_aksjonspunkt_typer() throws Exception {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, "0000")
                .leggTil(AppAbacAttributtType.AKSJONSPUNKT_KODE, "0001"));

        Set<String> koder = new HashSet<>();
        koder.add("0000");
        koder.add("0001");
        Set<String> svar = new HashSet<>();
        svar.add("Overstyring");
        svar.add("Manuell");
        Mockito.when(pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(koder)).thenReturn(svar);

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_AKSJONSPUNKT_TYPE)).containsOnly("Overstyring", "Manuell");
    }

    @Test
    public void skal_slå_opp_og_sende_videre_fnr_når_aktør_id_er_input() throws Exception {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, AKTØR_1.getId()));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_1.getId());
    }

    @Test
    public void skal_ikke_godta_at_det_sendes_inn_fagsak_id_og_behandling_id_som_ikke_stemmer_overens() throws Exception {

        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.FAGSAK_ID, 123L));
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.BEHANDLING_ID, 1234L));

        when(pipRepository.hentDataForBehandling(1234L)).thenReturn(Optional.of(
                new PipBehandlingsData(BehandlingStatus.OPPRETTET.getKode(), "Z1234", BigDecimal.valueOf(666), FagsakStatus.OPPRETTET.getKode())));

        var e = assertThrows(ManglerTilgangException.class, () -> requestBuilder.lagPdpRequest(attributter));
        assertThat(e.getMessage()).contains("Ugyldig input. Ikke samsvar mellom behandlingId 1234 og fagsakId [123]");

    }

    private AbacAttributtSamling byggAbacAttributtSamling() {
        AbacAttributtSamling attributtSamling = AbacAttributtSamling.medJwtToken(DUMMY_ID_TOKEN);
        attributtSamling.setActionType(BeskyttetRessursActionAttributt.READ);
        attributtSamling.setResource(FPSakBeskyttetRessursAttributt.FAGSAK);
        return attributtSamling;
    }

    private HentKjerneJournalpostListeResponse initJournalMockResponse(boolean utgått) {
        HentKjerneJournalpostListeResponse response = new HentKjerneJournalpostListeResponse();
        Journalpost dummy = new Journalpost();
        dummy.setJournalpostId(JOURNALPOST_ID.getVerdi());
        dummy.setJournaltilstand(utgått ? Journaltilstand.UTGAAR : Journaltilstand.ENDELIG);
        Journalposttyper retning = new Journalposttyper();
        retning.setValue("I");
        dummy.setJournalposttype(retning);
        response.getJournalpostListe().add(dummy);
        return response;
    }

}
