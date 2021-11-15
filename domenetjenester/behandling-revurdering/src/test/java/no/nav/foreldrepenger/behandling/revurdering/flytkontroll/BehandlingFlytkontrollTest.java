package no.nav.foreldrepenger.behandling.revurdering.flytkontroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public class BehandlingFlytkontrollTest {

    private static Long FAGSAK_ID = 1L;
    private static Long FAGSAK_AP_ID = 2L;
    private static Long BEHANDLING_ID = 99L;
    private static Long BERØRT_ID = 98L;

    private BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private BehandlingRevurderingRepository behandlingRevurderingRepository = mock(
            BehandlingRevurderingRepository.class);
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste = mock(BehandlingskontrollTjeneste.class);
    private BehandlingFlytkontroll flytkontroll;
    private Behandling behandling = mock(Behandling.class);
    private Behandling behandlingAnnenPart = mock(Behandling.class);
    private Behandling behandlingBerørt = mock(Behandling.class);
    private Behandling behandlingSammeSak = mock(Behandling.class);
    private Fagsak fagsak = mock(Fagsak.class);
    private Fagsak fagsakAnnenPart = mock(Fagsak.class);

    @BeforeEach
    public void setup() {
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandlingBerørt.getId()).thenReturn(BERØRT_ID);
        when(behandlingBerørt.erRevurdering()).thenReturn(true);
        when(fagsak.getId()).thenReturn(FAGSAK_ID);
        when(fagsakAnnenPart.getId()).thenReturn(FAGSAK_AP_ID);
        when(behandlingRepository.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(behandlingBerørt.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)).thenReturn(true);
        when(behandlingBerørt.harNoenBehandlingÅrsaker(BehandlingÅrsakType.alleTekniskeÅrsaker())).thenReturn(true);
        flytkontroll = new BehandlingFlytkontroll(behandlingRevurderingRepository, behandlingskontrollTjeneste,
                behandlingRepository);
    }

    @Test
    public void fgangSkalIkkeVenteNårUkoblet() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.empty());
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isFalse();
    }

    @Test
    public void fgangSkalIkkeVenteNårKobletIngenBehandlinger() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isFalse();
    }

    @Test
    public void fgangSkalIkkeVenteNårKobletBehandlingErFørUttak() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingskontrollTjeneste.erStegPassert(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isFalse();
    }

    @Test
    public void fgangSkalVenteNårKobletBehandlingErIUttak() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingskontrollTjeneste.erStegPassert(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(true);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isTrue();
    }

    @Test
    public void fgangSkalVenteNårKobletHarBerørt() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingskontrollTjeneste.erStegPassert(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isTrue();
    }

    @Test
    public void revurderingSkalVenteNårSammeSakHarBerørt() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        when(behandlingskontrollTjeneste.erStegPassert(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.uttaksProsessenSkalVente(BEHANDLING_ID)).isTrue();
    }

    @Test
    public void nyrevurderingSkalIkkeVenteNårUkoblet() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.empty());
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
    }

    @Test
    public void nyrevurderingSkalIkkeVenteNårKobletIngenBehandlinger() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
    }

    @Test
    public void nyrevurderingSkalIkkeVenteNårKobletFørstegangBehandlingErFørUttak() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingAnnenPart.erRevurdering()).thenReturn(false);
        when(behandlingskontrollTjeneste.erStegPassert(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
    }

    @Test
    public void nyrevurderingSkalVenteNårKobletRevurderingBehandlingErFørUttak() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingAnnenPart.erRevurdering()).thenReturn(true);
        when(behandlingskontrollTjeneste.erStegPassert(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }

    @Test
    public void nyrevurderingSkalVenteNårKobletFørstegangBehandlingErIUttak() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingAnnenPart));
        when(behandlingAnnenPart.erRevurdering()).thenReturn(false);
        when(behandlingskontrollTjeneste.erStegPassert(behandlingAnnenPart,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(true);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }

    @Test
    public void nyrevurderingSkalVenteNårKobletHarBerørt() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingskontrollTjeneste.erStegPassert(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }

    @Test
    public void nyrevurderingSkalIkkeVenteNårSammesakHarÅpenBehandling() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_ID)).thenReturn(
                List.of(behandlingSammeSak));
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isFalse();
        verifyNoInteractions(behandlingskontrollTjeneste);
    }

    @Test
    public void nyrevurderingSkalVenteNårSammeSakHarBerørt() {
        when(behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak)).thenReturn(Optional.of(fagsakAnnenPart));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_ID)).thenReturn(
                List.of(behandlingBerørt));
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(FAGSAK_AP_ID)).thenReturn(
                Collections.emptyList());
        when(behandlingskontrollTjeneste.erStegPassert(behandlingBerørt,
                StartpunktType.UTTAKSVILKÅR.getBehandlingSteg())).thenReturn(false);
        assertThat(flytkontroll.nyRevurderingSkalVente(fagsak)).isTrue();
    }
}
