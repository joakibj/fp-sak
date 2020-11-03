package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragApplikasjonTjeneste;

@CdiDbAwareTest
public class SimulerOppdragApplikasjonTjenesteTest {
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    private SimulerOppdragApplikasjonTjeneste simulerOppdragApplikasjonTjeneste;

    @Test
    public void simulerOppdrag_uten_behandling_vedtak_FP(EntityManager em) {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(repositoryProvider);
        em.persist(behandling.getBehandlingsresultat());
        var beregningsresultat = opprettBeregningsresultat();
        LocalDate fom = LocalDate.of(2018, 9, 1);
        LocalDate tom = LocalDate.of(2018, 9, 30);
        var beregningsresultatPeriode = opprettBeregningsresultatPeriode(beregningsresultat, fom, tom);
        opprettBeregningsresultatAndel(beregningsresultatPeriode);
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Act
        var resultat = simulerOppdragApplikasjonTjeneste.simulerOppdrag(behandling.getId(), 0L);

        // Assert
        assertThat(resultat).hasSize(1);
    }

    private BeregningsresultatEntitet opprettBeregningsresultat() {
        return BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
    }

    private BeregningsresultatPeriode opprettBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(fom, tom)
                .build(beregningsresultat);
    }

    private BeregningsresultatAndel opprettBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode) {
        return BeregningsresultatAndel.builder()
                .medBrukerErMottaker(true)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(KUNSTIG_ORG))
                .medArbeidsforholdRef(InternArbeidsforholdRef.nyRef())
                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
                .medStillingsprosent(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medDagsats(1000)
                .medDagsatsFraBg(1000)
                .build(beregningsresultatPeriode);
    }
}
