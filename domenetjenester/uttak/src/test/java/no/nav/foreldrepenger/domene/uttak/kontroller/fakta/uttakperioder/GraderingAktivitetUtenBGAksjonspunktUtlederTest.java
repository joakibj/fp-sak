package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;


import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class GraderingAktivitetUtenBGAksjonspunktUtlederTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());
    private GraderingAktivitetUtenBGAksjonspunktUtleder utleder = new GraderingAktivitetUtenBGAksjonspunktUtleder();

    @Test
    public void skalUtledeAksjonspunktHvisDetFinnesAndelerUtenBeregningsgrunnlag() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.now(), LocalDate.now())
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medErArbeidstaker(true)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsprosent(BigDecimal.TEN)
            .build();
        var søknadsperioder = List.of(søknadsperiode);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(søknadsperioder, false));
        var behandling = scenario.lagre(repositoryProvider);

        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null)
            .medFinnesAndelerMedGraderingUtenBeregningsgrunnlag(true);
        var resultat = utleder.utledAksjonspunkterFor(input);
        assertThat(resultat).containsExactly(AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG);
    }
}
