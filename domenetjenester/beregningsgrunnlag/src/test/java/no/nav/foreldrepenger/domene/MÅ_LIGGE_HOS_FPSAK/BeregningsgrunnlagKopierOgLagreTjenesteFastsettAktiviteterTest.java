package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static java.util.Optional.empty;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.kalkulator.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.gradering.AktivitetGradering;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling.ScenarioForeldrepenger;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetHandlingType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BeregningsgrunnlagKopierOgLagreTjenesteFastsettAktiviteterTest {


    private static final String ORG_NUMMER = "915933149";
    private static final String ORG_NUMMER2 = "915933148";

    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(ORG_NUMMER);
    private static final Arbeidsgiver VIRKSOMHET2 = Arbeidsgiver.virksomhet(ORG_NUMMER2);

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final RepositoryProvider repositoryProvider = new RepositoryProvider(repoRule.getEntityManager());
    private final FakeUnleash unleash = new FakeUnleash();
    private final BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();


    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingReferanse behandlingReferanse;

    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Before
    public void setUp() {
        KalkulusKonfigInjecter kalkulusKonfigInjecter = new KalkulusKonfigInjecter(5, unleash);
        BeregningTilInputTjeneste beregningTilInputTjeneste = new BeregningTilInputTjeneste(beregningsgrunnlagRepository, kalkulusKonfigInjecter, behandlingRepository);
        behandlingReferanse = lagBehandlingReferanse();
        beregningsgrunnlagKopierOgLagreTjeneste = new BeregningsgrunnlagKopierOgLagreTjeneste(beregningsgrunnlagRepository, beregningsgrunnlagTjeneste, beregningTilInputTjeneste);
    }

    @Test
    public void skal_kunne_kjøre_fastsett_aktiviteter_første_gang_med_aksjonspunkt() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGr = lagIAYGrunnlagForArbeidOgVentelønnVartpenger();
        BeregningsgrunnlagInput input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr, lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger());

        // Act
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(bgMedAktiviteter.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPRETTET);
        assertThat(ap.size()).isEqualTo(1);
    }

    @Test
    public void skal_kunne_kjøre_fastsett_aktiviteter_andre_gang_med_aksjonspunkt_uten_endringer_i_input() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGr = lagIAYGrunnlagForArbeidOgVentelønnVartpenger();
        BeregningsgrunnlagInput input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr, lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger());

        // Act: kjør første gang
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap.size()).isEqualTo(1);

        // Arrange: lag bekreftet aggregat
        lagreSaksbehandletFjernArbeidOgDeaktiver(bgMedAktiviteter);

        // Act: kjør andre gang
        List<BeregningAksjonspunktResultat> ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedSaksbehandlet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedSaksbehandlet).isPresent();
        assertThat(ap2.size()).isEqualTo(1);
        assertThat(bgMedSaksbehandlet.get().getSaksbehandletAktiviteter()).isPresent();
        assertThat(bgMedSaksbehandlet.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
    }

    @Test
    public void skal_ikkje_kopiere_om_man_ikkje_har_aksjonspunkt_eller_overstyring_i_input() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGr = lagIAYGrunnlagForToArbeidsforhold();
        BeregningsgrunnlagInput input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr, lagOpptjeningAktiviteterMedToArbeidsforhold());

        // Act: kjør første gang
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap.size()).isEqualTo(0);

        // Act: kjør andre gang
        List<BeregningAksjonspunktResultat> ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter2 = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter2).isPresent();
        assertThat(ap2.size()).isEqualTo(0);
        assertThat(bgMedAktiviteter2.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPRETTET);
    }

    @Test
    public void skal_kunne_kjøre_fastsett_aktiviteter_andre_gang_med_aksjonspunkt_med_endringer_i_input() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGr = lagIAYGrunnlagForArbeidOgVentelønnVartpenger();
        BeregningsgrunnlagInput input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr, lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger());

        // Act: kjør første gang
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap.size()).isEqualTo(1);

        // Arrange: lag bekreftet aggregat
        lagreSaksbehandletFjernArbeidOgDeaktiver(bgMedAktiviteter);

        // Endre input
        InntektArbeidYtelseGrunnlag iayGr2 = lagIAYGrunnlagForToArbeidsforholdOgVentelønnVartpenger();
        BeregningsgrunnlagInput input2 = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr2, lagOpptjeningAktiviteterMedToArbeidsforholdOgVentelønnVartpenger());

        // Act: kjør andre gang
        List<BeregningAksjonspunktResultat> ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input2);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgUtenSaksbehandlet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgUtenSaksbehandlet).isPresent();
        assertThat(ap2.size()).isEqualTo(1);
        assertThat(bgUtenSaksbehandlet.get().getSaksbehandletAktiviteter()).isEmpty();
        assertThat(bgUtenSaksbehandlet.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPRETTET);
    }

    @Test
    public void skal_ta_vare_på_overstyringer_for_andre_kjøring_med_endringer_i_input() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGr = lagIAYGrunnlagForToArbeidsforhold();
        BeregningsgrunnlagInput input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr, lagOpptjeningAktiviteterMedToArbeidsforhold());

        // Act: kjør første gang
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap.size()).isEqualTo(0);

        // Arrange: lag overstyrt aggregat
        lagreOverstyrtFjernEttArbeidsforholdOgDeaktiver(bgMedAktiviteter);

        // Endre input
        InntektArbeidYtelseGrunnlag iayGr2 = lagIAYGrunnlagForToArbeidsforholdOgVentelønnVartpenger();
        BeregningsgrunnlagInput input2 = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr2, lagOpptjeningAktiviteterMedToArbeidsforholdOgVentelønnVartpenger());

        // Act: kjør andre gang
        List<BeregningAksjonspunktResultat> ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input2);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedOverstyring = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedOverstyring).isPresent();
        assertThat(ap2.size()).isEqualTo(0);
        assertThat(bgMedOverstyring.get().getOverstyring()).isPresent();
        assertThat(bgMedOverstyring.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.OPPRETTET);
    }

    @Test
    public void skal_ta_vare_på_overstyringer_for_andre_kjøring_uten_endringer_i_input() {
        // Arrange
        InntektArbeidYtelseGrunnlag iayGr = lagIAYGrunnlagForToArbeidsforhold();
        BeregningsgrunnlagInput input = lagBeregningsgrunnlagInput(behandlingReferanse, iayGr, lagOpptjeningAktiviteterMedToArbeidsforhold());

        // Act: kjør første gang
        List<BeregningAksjonspunktResultat> ap = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedAktiviteter).isPresent();
        assertThat(ap.size()).isEqualTo(0);

        // Arrange: lag overstyrt aggregat
        lagreOverstyrtFjernEttArbeidsforholdOgDeaktiver(bgMedAktiviteter);

        // Act: kjør andre gang
        List<BeregningAksjonspunktResultat> ap2 = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);

        // Assert
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedOverstyring = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(
            behandlingReferanse.getBehandlingId());
        assertThat(bgMedOverstyring).isPresent();
        assertThat(ap2.size()).isEqualTo(0);
        assertThat(bgMedOverstyring.get().getOverstyring()).isPresent();
        assertThat(bgMedOverstyring.get().getBeregningsgrunnlagTilstand()).isEqualTo(BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
    }

    private void lagreSaksbehandletFjernArbeidOgDeaktiver(Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter) {
        BeregningsgrunnlagGrunnlagBuilder saksbehandletGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(bgMedAktiviteter)
            .medSaksbehandletAktiviteter(BeregningAktivitetAggregatEntitet.builder()
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .leggTilAktivitet(BeregningAktivitetEntitet.builder()
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.VENTELØNN_VARTPENGER)
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusMonths(1)))
                    .build())
                .build());
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getBehandlingId(), saksbehandletGrunnlag, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        beregningsgrunnlagRepository.deaktiverBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId());
    }

    private void lagreOverstyrtFjernEttArbeidsforholdOgDeaktiver(Optional<BeregningsgrunnlagGrunnlagEntitet> bgMedAktiviteter) {
        BeregningsgrunnlagGrunnlagBuilder saksbehandletGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(bgMedAktiviteter)
            .medOverstyring(BeregningAktivitetOverstyringerEntitet.builder()
                .leggTilOverstyring(BeregningAktivitetOverstyringEntitet.builder()
                    .medArbeidsgiver(VIRKSOMHET)
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                    .medHandling(BeregningAktivitetHandlingType.BENYTT)
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusMonths(1)))
                    .build())
                .leggTilOverstyring(BeregningAktivitetOverstyringEntitet.builder()
                    .medArbeidsgiver(VIRKSOMHET2)
                    .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEID)
                    .medHandling(BeregningAktivitetHandlingType.IKKE_BENYTT)
                    .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusMonths(1)))
                    .build())
                .build());
        beregningsgrunnlagRepository.lagre(behandlingReferanse.getBehandlingId(), saksbehandletGrunnlag, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        beregningsgrunnlagRepository.deaktiverBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId());
    }

    private BehandlingReferanse lagBehandlingReferanse() {
        return ScenarioForeldrepenger.nyttScenario().lagre(repositoryProvider)
            .medSkjæringstidspunkt(
            Skjæringstidspunkt.builder()
                .medFørsteUttaksdato(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
                .medSkjæringstidspunktBeregning(SKJÆRINGSTIDSPUNKT)
                .medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .build()
        );
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagForToArbeidsforholdOgVentelønnVartpenger() {
        YrkesaktivitetBuilder arbeid = lagArbeidYA(VIRKSOMHET);
        YrkesaktivitetBuilder arbeid2 = lagArbeidYA(VIRKSOMHET2);
        InntektArbeidYtelseAggregatBuilder oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(empty())
                .medAktørId(behandlingReferanse.getAktørId())
                .leggTilYrkesaktivitet(arbeid2)
                .leggTilYrkesaktivitet(arbeid));

        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjening(lagVentelønnVartpengerOppgittOpptjening())
            .medData(oppdatere)
            .medInntektsmeldinger(List.of(lagInntektsmelding(VIRKSOMHET), lagInntektsmelding(VIRKSOMHET2)))
            .build();
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagForArbeidOgVentelønnVartpenger() {
        YrkesaktivitetBuilder arbeid = lagArbeidYA(VIRKSOMHET);
        InntektArbeidYtelseAggregatBuilder oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(empty())
                .medAktørId(behandlingReferanse.getAktørId())
                .leggTilYrkesaktivitet(arbeid));

        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medOppgittOpptjening(lagVentelønnVartpengerOppgittOpptjening())
            .medData(oppdatere)
            .medInntektsmeldinger(List.of(lagInntektsmelding(VIRKSOMHET)))
            .build();
    }

    private InntektArbeidYtelseGrunnlag lagIAYGrunnlagForToArbeidsforhold() {
        YrkesaktivitetBuilder arbeid = lagArbeidYA(VIRKSOMHET);
        YrkesaktivitetBuilder arbeid2 = lagArbeidYA(VIRKSOMHET2);
        InntektArbeidYtelseAggregatBuilder oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER)
            .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(empty())
                .medAktørId(behandlingReferanse.getAktørId())
                .leggTilYrkesaktivitet(arbeid)
                .leggTilYrkesaktivitet(arbeid2));
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(oppdatere)
            .medInntektsmeldinger(List.of(lagInntektsmelding(VIRKSOMHET), lagInntektsmelding(VIRKSOMHET2)))
            .build();
    }

    private Inntektsmelding lagInntektsmelding(Arbeidsgiver virksomhet) {
        return InntektsmeldingBuilder.builder().medBeløp(BigDecimal.TEN).medArbeidsgiver(virksomhet).build();
    }


    private YrkesaktivitetBuilder lagArbeidYA(Arbeidsgiver virksomhet) {
        return YrkesaktivitetBuilder.oppdatere(empty())
            .medArbeidsgiver(virksomhet)
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusMonths(10)))
            )
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusMonths(10)))
                .medProsentsats(BigDecimal.valueOf(100))
            )
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
    }

    private OppgittOpptjeningBuilder lagVentelønnVartpengerOppgittOpptjening() {
        return OppgittOpptjeningBuilder.ny()
            .leggTilAnnenAktivitet(new OppgittAnnenAktivitet(
                DatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1), SKJÆRINGSTIDSPUNKT.plusMonths(1)),
                ArbeidType.VENTELØNN_VARTPENGER));
    }

    private BeregningsgrunnlagInput lagBeregningsgrunnlagInput(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGr, OpptjeningAktiviteterDto opptjeningAktiviteter) {
        InntektArbeidYtelseGrunnlagDto iayGrunnlag = IAYMapperTilKalkulus.mapGrunnlag(iayGr);
        no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingReferanse behandlingReferanse1 = MapBehandlingRef.mapRef(behandlingReferanse);
        return new BeregningsgrunnlagInput(behandlingReferanse1, iayGrunnlag,
            opptjeningAktiviteter,
            AktivitetGradering.INGEN_GRADERING,
            List.of(),
            new ForeldrepengerGrunnlag(100, false));
    }

    private OpptjeningAktiviteterDto lagOpptjeningAktiviteterMedArbeidOgVentelønnVartpenger() {
        return new OpptjeningAktiviteterDto(List.of(lagOpptjeningAktivitetArbeid(ORG_NUMMER), lagOpptjeningAktivitetVentelønnVartpenger()));
    }

    private OpptjeningAktiviteterDto lagOpptjeningAktiviteterMedToArbeidsforholdOgVentelønnVartpenger() {
        return new OpptjeningAktiviteterDto(List.of(
            lagOpptjeningAktivitetArbeid(ORG_NUMMER),
            lagOpptjeningAktivitetArbeid(ORG_NUMMER2),
            lagOpptjeningAktivitetVentelønnVartpenger()));
    }

    private OpptjeningAktiviteterDto lagOpptjeningAktiviteterMedToArbeidsforhold() {
        return new OpptjeningAktiviteterDto(List.of(
            lagOpptjeningAktivitetArbeid(ORG_NUMMER),
            lagOpptjeningAktivitetArbeid(ORG_NUMMER2)));
    }

    private OpptjeningAktiviteterDto.OpptjeningPeriodeDto lagOpptjeningAktivitetArbeid(String orgNummer) {
        return OpptjeningAktiviteterDto.nyPeriode(no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktivitetType.ARBEID,
            new Periode(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT), orgNummer, null, InternArbeidsforholdRefDto.nyRef());
    }

    private OpptjeningAktiviteterDto.OpptjeningPeriodeDto lagOpptjeningAktivitetVentelønnVartpenger() {
        return OpptjeningAktiviteterDto.nyPeriode(no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktivitetType.VENTELØNN_VARTPENGER,
            new Periode(SKJÆRINGSTIDSPUNKT.minusMonths(10), SKJÆRINGSTIDSPUNKT));
    }


}
