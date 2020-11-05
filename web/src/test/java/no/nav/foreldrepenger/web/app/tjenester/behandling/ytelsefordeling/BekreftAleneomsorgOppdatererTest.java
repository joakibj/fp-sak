package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public class BekreftAleneomsorgOppdatererTest extends EntityManagerAwareTest {
    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepositoryProvider = new BehandlingRepositoryProvider(entityManager);
        ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(entityManager));
    }

    @Test
    public void skal_generere_historikkinnslag_ved_avklaring_av_aleneomsorg() {
        // Arrange
        boolean oppdatertAleneOmsorg = false;

        // Behandling
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        scenario.medSøknad();
        OppgittRettighetEntitet rettighet = new OppgittRettighetEntitet(false, false, true);
        scenario.medOppgittRettighet(rettighet);
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);

        scenario.lagre(behandlingRepositoryProvider);

        Behandling behandling = scenario.getBehandling();
        // Dto
        BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto dto = new BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto(
            "begrunnelse");
        dto.setAleneomsorg(oppdatertAleneOmsorg);
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        // Act
        new BekreftAleneomsorgOppdaterer(behandlingRepositoryProvider, lagMockHistory(), ytelseFordelingTjeneste) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkinnslagDeler).hasSize(1);
        HistorikkinnslagDel del = historikkinnslagDeler.get(0);
        Optional<HistorikkinnslagFelt> aleneomsorgOpt = del.getEndretFelt(HistorikkEndretFeltType.ALENEOMSORG);
        assertThat(aleneomsorgOpt).hasValueSatisfying(aleneomsorg -> {
            assertThat(aleneomsorg.getNavn()).isEqualTo(HistorikkEndretFeltType.ALENEOMSORG.getKode());
            assertThat(aleneomsorg.getFraVerdi()).isNull();
            assertThat(aleneomsorg.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.IKKE_ALENEOMSORG.getKode());
        });
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
