package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt.AvklarAnnenforelderHarRettOppdaterer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.FørsteUttaksdatoTjenesteImpl;

public class AvklarAnnenforelderHarRettOppdatererTest {

    private static final AksjonspunktDefinisjon AKSONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT;

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider behandlingRepositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private YtelseFordelingTjeneste ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(repositoryRule.getEntityManager()));
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste = mock(HistorikkTjenesteAdapter.class);
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = mock(ArbeidsgiverHistorikkinnslag.class);
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste = new FaktaUttakToTrinnsTjeneste(ytelseFordelingTjeneste);
    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste =
        new KontrollerOppgittFordelingTjeneste(ytelseFordelingTjeneste, behandlingRepositoryProvider,new FørsteUttaksdatoTjenesteImpl(ytelseFordelingTjeneste, behandlingRepositoryProvider.getUttakRepository()));

    @Before
    public void setUp() {
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        faktaUttakHistorikkTjeneste = new FaktaUttakHistorikkTjeneste(lagMockHistory(), arbeidsgiverHistorikkinnslagTjeneste, ytelseFordelingTjeneste, inntektArbeidYtelseTjeneste);
    }

    @Test
    public void skal_opprette_historikkinnslag_ved_endring() {
        //Scenario med avklar fakta annen forelder har rett
        ScenarioMorSøkerForeldrepenger scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF,
            BehandlingStegType.VURDER_UTTAK);
        scenario.lagre(behandlingRepositoryProvider);

        var behandling = AvklarFaktaTestUtil.opprettBehandling(scenario);
        AvklarAnnenforelderHarRettDto dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        oppdaterer().oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        List<HistorikkinnslagDel> historikkinnslagDeler = tekstBuilder.build(historikkinnslag);

        //assert
        assertThat(historikkinnslagDeler).hasSize(1);
        HistorikkinnslagDel del = historikkinnslagDeler.get(0);
        Optional<HistorikkinnslagFelt> rettOpt = del.getEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER);
        assertThat(rettOpt).hasValueSatisfying(rett -> {
            assertThat(rett.getNavn()).isEqualTo(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER.getKode());
            assertThat(rett.getFraVerdi()).isNull();
            assertThat(rett.getTilVerdi()).isEqualTo(HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_RETT.getKode());
        });
        assertThat(del.getSkjermlenke())
            .hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_UTTAK.getKode()));
        assertThat(del.getBegrunnelse())
            .hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("Har rett"));
    }

    @Test
    public void skal_sette_totrinns_ved_avkreft_søkers_opplysning() {
        //Scenario med avklar fakta annen forelder har ikke rett
        ScenarioMorSøkerForeldrepenger scenario = AvklarFaktaTestUtil.opprettScenarioMorSøkerForeldrepenger();
        scenario.leggTilAksjonspunkt(AKSONSPUNKT_DEF, BehandlingStegType.VURDER_UTTAK);
        scenario.lagre(behandlingRepositoryProvider);

        var behandling = AvklarFaktaTestUtil.opprettBehandling(scenario);
        AvklarAnnenforelderHarRettDto dto = AvklarFaktaTestUtil.opprettDtoAvklarAnnenforelderharIkkeRett();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode()).get();

        OppdateringResultat resultat = oppdaterer().oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        //assert
        assertThat(behandling.harAksjonspunktMedType(AKSONSPUNKT_DEF)).isTrue();
        assertThat(resultat.kreverTotrinnsKontroll()).isTrue();
    }

    private AvklarAnnenforelderHarRettOppdaterer oppdaterer() {
        return new AvklarAnnenforelderHarRettOppdaterer(kontrollerOppgittFordelingTjeneste, faktaUttakHistorikkTjeneste, faktaUttakToTrinnsTjeneste);
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        Mockito.when(historikkApplikasjonTjeneste.tekstBuilder()).thenReturn(tekstBuilder);
        return historikkApplikasjonTjeneste;
    }
}
