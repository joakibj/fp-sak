package no.nav.foreldrepenger.økonomistøtte.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.økonomistøtte.FinnNyesteOppdragForSak;
import no.nav.foreldrepenger.økonomistøtte.OppdragInputTjeneste;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollEngangsstønadTjeneste;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.OppdragskontrollEngangsstønad;
import no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.adapter.MapBehandlingInfoES;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.NyOppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.ny.toggle.OppdragKjerneimplementasjonToggle;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

@ExtendWith(MockitoExtension.class)
public class SimulerOppdragTjenesteTest extends EntityManagerAwareTest {


    private BehandlingRepositoryProvider repositoryProvider;

    private ØkonomioppdragRepository økonomioppdragRepository;
    private FinnNyesteOppdragForSak finnNyesteOppdragForSak;

    private LegacyESBeregningRepository beregningRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    private SimulerOppdragTjeneste simulerOppdragTjeneste;

    @Mock
    private PersoninfoAdapter tpsTjeneste;
    @Mock
    private OppdragKjerneimplementasjonToggle toggle;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
        finnNyesteOppdragForSak = new FinnNyesteOppdragForSak(økonomioppdragRepository);
        beregningRepository = new LegacyESBeregningRepository(entityManager);
        behandlingVedtakRepository = new BehandlingVedtakRepository(entityManager);
        familieHendelseRepository = new FamilieHendelseRepository(entityManager);
        when(toggle.brukNyImpl(any())).thenReturn(false);

        simulerOppdragTjeneste = new SimulerOppdragTjeneste(mock(OppdragskontrollTjeneste.class), mockTjeneste(), mock(NyOppdragskontrollTjenesteImpl.class), mock(OppdragInputTjeneste.class), toggle);
    }

    private OppdragskontrollTjeneste mockTjeneste() {
        MapBehandlingInfoES mapBehandlingInfo = new MapBehandlingInfoES(finnNyesteOppdragForSak, tpsTjeneste,
            beregningRepository, behandlingVedtakRepository, familieHendelseRepository
        );
        var manager = new OppdragskontrollEngangsstønad(mapBehandlingInfo);

        RevurderingEndring revurderingEndring = mock(RevurderingEndring.class);
        when(revurderingEndring.erRevurderingMedUendretUtfall(any())).thenReturn(false);

        return new OppdragskontrollEngangsstønadTjeneste(repositoryProvider, økonomioppdragRepository, manager, revurderingEndring);
    }

    @Test
    public void simulerOppdrag_uten_behandling_vedtak_ES() {
        // Arrange
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        getEntityManager().persist(behandling.getBehandlingsresultat());

        // Act
        var resultat = simulerOppdragTjeneste.simulerOppdrag(behandling.getId(), behandling.getFagsakYtelseType());

        // Assert
        assertThat(resultat).hasSize(0);

    }
}
