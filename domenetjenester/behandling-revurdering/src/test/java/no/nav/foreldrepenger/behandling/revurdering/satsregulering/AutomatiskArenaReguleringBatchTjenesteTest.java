package no.nav.foreldrepenger.behandling.revurdering.satsregulering;


import static java.time.format.DateTimeFormatter.ofPattern;
import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskArenaReguleringBatchArguments.DATE_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AutomatiskArenaReguleringBatchTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private ProsessTaskRepository prosessTaskRepositoryMock;

    private AutomatiskArenaReguleringBatchTjeneste tjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private LocalDate cutoff;
    private LocalDate nySatsDato;
    AutomatiskArenaReguleringBatchArguments batchArgs;




    @Before
    public void setUp() throws Exception {
        cutoff = AutomatiskArenaReguleringBatchArguments.DATO;
        nySatsDato = cutoff.plusWeeks(3);
        tjeneste = new AutomatiskArenaReguleringBatchTjeneste(repositoryProvider, prosessTaskRepositoryMock);
        Map<String, String> arguments = new HashMap<>();
        arguments.put(AutomatiskArenaReguleringBatchArguments.REVURDER_KEY, "True");
        arguments.put(AutomatiskArenaReguleringBatchArguments.SATS_DATO_KEY, nySatsDato.format(ofPattern(DATE_PATTERN)));
        batchArgs = new AutomatiskArenaReguleringBatchArguments(arguments);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        opprettRevurderingsKandidat(BehandlingStatus.UTREDES, cutoff.minusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusMonths(2));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.minusDays(5));
        String svar = tjeneste.launch(batchArgs);
        assertThat(svar).isEqualTo(AutomatiskArenaReguleringBatchTjeneste.BATCHNAME+"-0");
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, LocalDate uttakFom) {
        LocalDate terminDato = uttakFom.plusWeeks(3);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medSøknadDato(terminDato.minusDays(40));

        scenario.medBekreftetHendelse()
            .medFødselsDato(terminDato)
            .medAntallBarn(1);

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        Whitebox.setInternalState(behandling, "status", status);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(uttakFom)
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.builder(periode)
            .build(beregningsgrunnlag);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        BeregningsresultatEntitet brFP = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
        BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(uttakFom, uttakFom.plusMonths(3))
            .medBeregningsresultatAndeler(Collections.emptyList())
            .build(brFP);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        repoRule.getRepository().flushAndClear();
        return repoRule.getEntityManager().find(Behandling.class, behandling.getId());
    }

}
