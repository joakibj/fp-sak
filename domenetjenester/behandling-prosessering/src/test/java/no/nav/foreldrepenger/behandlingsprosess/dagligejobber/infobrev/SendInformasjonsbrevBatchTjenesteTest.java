package no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev;

import static java.time.format.DateTimeFormatter.ofPattern;
import static no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.SendInformasjonsbrevBatchArguments.DATE_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@CdiDbAwareTest
public class SendInformasjonsbrevBatchTjenesteTest {

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private ProsessTaskTjeneste taskTjenesteMock;

    private SendInformasjonsbrevBatchTjeneste tjeneste;
    private SendInformasjonsbrevOppholdBatchTjeneste tjenesteOpphold;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private InformasjonssakRepository repository;

    private LocalDate fom = LocalDate.now();
    private LocalDate tom = fom.plusDays(3);
    private LocalDate uttakFom = fom.minusWeeks(10);
    LocalDate fomOpphold = LocalDate.now();
    LocalDate tomOpphold = LocalDate.now().plusWeeks(5);
    private LocalDate uttakFomOpphold = fom.minusWeeks(4);
    SendInformasjonsbrevBatchArguments batchArgs;

    @BeforeEach
    public void setUp() throws Exception {
        tjeneste = new SendInformasjonsbrevBatchTjeneste(repository, taskTjenesteMock);
        Map<String, String> arguments = new HashMap<>();
        arguments.put(SendInformasjonsbrevBatchArguments.FOM_KEY, fom.format(ofPattern(DATE_PATTERN)));
        arguments.put(SendInformasjonsbrevBatchArguments.TOM_KEY, tom.format(ofPattern(DATE_PATTERN)));
        batchArgs = new SendInformasjonsbrevBatchArguments(arguments);

        tjenesteOpphold = new SendInformasjonsbrevOppholdBatchTjeneste(repository, taskTjenesteMock);
        Map<String, String> argumentsOpphold = new HashMap<>();
        argumentsOpphold.put(SendInformasjonsbrevBatchArguments.FOM_KEY, fomOpphold.format((ofPattern(DATE_PATTERN))));
        argumentsOpphold.put(SendInformasjonsbrevBatchArguments.TOM_KEY, tomOpphold.format((ofPattern(DATE_PATTERN))));
        batchArgs = new SendInformasjonsbrevBatchArguments(argumentsOpphold);
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.UTREDES, uttakFom, false);
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, uttakFom.minusWeeks(4), false);
        var svar = tjeneste.launch(batchArgs);
        assertThat(svar).isEqualTo(SendInformasjonsbrevBatchTjeneste.BATCHNAVN + "-0");
    }

    @Test
    public void skal_finne_en_sak_til_revurdering(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, uttakFom, false);
        var svar = tjeneste.launch(batchArgs);
        assertThat(svar).isEqualTo(SendInformasjonsbrevBatchTjeneste.BATCHNAVN + "-1");
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering_med_opphold(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.UTREDES, uttakFom, true);
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, uttakFom.minusWeeks(4), true);
        var svar = tjenesteOpphold.launch(batchArgs);
        assertThat(svar).isEqualTo(SendInformasjonsbrevOppholdBatchTjeneste.BATCHNAVN + "-0");
    }

    @Test
    public void skal_finne_en_sak_til_revurdering_med_opphold(EntityManager em) {
        opprettRevurderingsKandidat(em, BehandlingStatus.AVSLUTTET, uttakFomOpphold, true);
        var svar = tjenesteOpphold.launch(batchArgs);
        assertThat(svar).isEqualTo(SendInformasjonsbrevOppholdBatchTjeneste.BATCHNAVN + "-1");
    }

    private Behandling opprettRevurderingsKandidat(EntityManager em, BehandlingStatus status, LocalDate uttakFom, boolean medOpphold) {
        var terminDato = uttakFom.plusWeeks(3);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medSøknadDato(terminDato.minusDays(40));
        scenario.medSøknadAnnenPart().medAktørId(new AktørId("0000000000000")).medNavn("Ola Dunk").build();

        scenario.medBekreftetHendelse()
                .medFødselsDato(terminDato)
                .medAntallBarn(1);

        // Uttak periode 1
        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);

        var uttakFFF = new UttakResultatPeriodeEntitet.Builder(uttakFom, terminDato.minusDays(1))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();

        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
                .build();

        UttakResultatPeriodeAktivitetEntitet.builder(uttakFFF, arbeidsforhold1)
                .medTrekkdager(new Trekkdager(21))
                .medTrekkonto(StønadskontoType.FORELDREPENGER_FØR_FØDSEL)
                .medArbeidsprosent(BigDecimal.TEN).build();

        perioder.leggTilPeriode(uttakFFF);

        // Uttak periode 2
        var uttakMødre = new UttakResultatPeriodeEntitet.Builder(terminDato, terminDato.plusWeeks(6).minusDays(1))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakMødre, arbeidsforhold1)
                .medTrekkdager(new Trekkdager(42))
                .medTrekkonto(StønadskontoType.MØDREKVOTE)
                .medArbeidsprosent(BigDecimal.TEN).build();
        perioder.leggTilPeriode(uttakMødre);

        if (medOpphold) {
            var uttakFelles = new UttakResultatPeriodeEntitet.Builder(terminDato.plusWeeks(6),
                    terminDato.plusWeeks(6).plusDays(8))
                            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                            .medOppholdÅrsak(OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER)
                            .build();
            perioder.leggTilPeriode(uttakFelles);
            scenario.medUttak(perioder);
        } else {
            var uttakFelles = new UttakResultatPeriodeEntitet.Builder(terminDato.plusWeeks(6),
                    terminDato.plusWeeks(6).plusDays(8))
                            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                            .build();
            UttakResultatPeriodeAktivitetEntitet.builder(uttakFelles, arbeidsforhold1)
                    .medTrekkdager(new Trekkdager(7))
                    .medTrekkonto(StønadskontoType.FELLESPERIODE)
                    .medArbeidsprosent(BigDecimal.TEN).build();
            perioder.leggTilPeriode(uttakFelles);
            scenario.medUttak(perioder);
        }

        scenario.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        var behandling = scenario.lagre(repositoryProvider);

        var behandlingsresultat = behandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        em.persist(behandlingsresultat);

        if (BehandlingStatus.AVSLUTTET.equals(status)) {
            behandling.avsluttBehandling();
        }

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        var konto = Stønadskontoberegning.builder()
                .medStønadskonto(Stønadskonto.builder().medStønadskontoType(StønadskontoType.MØDREKVOTE).medMaxDager(75).build())
                .medRegelInput("{ blablabla }").medRegelEvaluering("{ blablabla }");

        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getFagsakRelasjonRepository().lagre(behandling.getFagsak(), behandling.getId(), konto.build());

        em.flush();
        em.clear();
        return em.find(Behandling.class, behandling.getId());
    }

}
