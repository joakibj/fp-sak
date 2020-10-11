package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingHistorikk;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkOpplysningType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class RevurderingTjenesteImplTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private HistorikkRepository historikkRepository = spy(repositoryProvider.getHistorikkRepository());
    private Behandling behandling;

    @Inject
    @FagsakYtelseTypeRef("ES")
    private RevurderingEndring revurderingEndringES;
    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingEndring revurderingEndringFP;

    @Inject
    private VergeRepository vergeRepository;

    @Before
    public void setup() {
        opprettRevurderingsKandidat();
    }

    @Test
    public void skal_opprette_historikkinnslag_for_registrert_fødsel() {
        LocalDate fødselsdato = LocalDate.parse("2017-09-04");
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(fødselsdato));
        new RevurderingHistorikk(historikkRepository).opprettHistorikkinnslagForFødsler(behandling, barn);
        ArgumentCaptor<Historikkinnslag> captor = ArgumentCaptor.forClass(Historikkinnslag.class);

        verify(historikkRepository).lagre(captor.capture());
        Historikkinnslag historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.NY_INFO_FRA_TPS);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.VEDTAKSLØSNINGEN);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        Optional<HistorikkinnslagFelt> fodsel = del.getOpplysning(HistorikkOpplysningType.FODSELSDATO);
        Optional<HistorikkinnslagFelt> antallBarn = del.getOpplysning(HistorikkOpplysningType.TPS_ANTALL_BARN);
        assertThat(fodsel).hasValueSatisfying(v -> assertThat(v.getTilVerdi()).isEqualTo("04.09.2017"));
        assertThat(antallBarn).as("antallBarn").hasValueSatisfying(v -> assertThat(v.getTilVerdi()).as("antallBarn.tilVerdi").isEqualTo(Integer.toString(1)));
    }

    @Test
    public void skal_opprette_korrekt_historikkinnslag_for_trillingfødsel_over_2_dager() {
        LocalDate fødselsdato1 = LocalDate.parse("2017-09-04");
        LocalDate fødselsdato2 = LocalDate.parse("2017-09-05");
        List<FødtBarnInfo> barn = new ArrayList<>();
        barn.add(byggBaby(fødselsdato1));
        barn.add(byggBaby(fødselsdato1));
        barn.add(byggBaby(fødselsdato2));

        new RevurderingHistorikk(historikkRepository).opprettHistorikkinnslagForFødsler(behandling, barn);
        ArgumentCaptor<Historikkinnslag> captor = ArgumentCaptor.forClass(Historikkinnslag.class);

        verify(historikkRepository).lagre(captor.capture());
        Historikkinnslag historikkinnslag = captor.getValue();

        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.NY_INFO_FRA_TPS);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.VEDTAKSLØSNINGEN);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        Optional<HistorikkinnslagFelt> fodsel = del.getOpplysning(HistorikkOpplysningType.FODSELSDATO);
        Optional<HistorikkinnslagFelt> antallBarn = del.getOpplysning(HistorikkOpplysningType.TPS_ANTALL_BARN);
        String dateString = dateFormat.format(fødselsdato1) + ", " + dateFormat.format(fødselsdato2);
        assertThat(fodsel).as("fodsel").hasValueSatisfying(v -> assertThat(v.getTilVerdi()).as("fodsel.tilVerdi").isEqualTo(dateString));
        assertThat(antallBarn).as("antallBarn").hasValueSatisfying(v -> assertThat(v.getTilVerdi()).as("antallBarn.tilVerdi").isEqualTo(Integer.toString(3)));
    }

    @Test
    public void skal_opprette_revurdering_for_foreldrepenger() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.medBehandlingstidFrist(LocalDate.now().minusDays(5));
        Behandling behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(), false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);

        final BehandlingskontrollTjenesteImpl behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        RevurderingTjenesteFelles revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        RevurderingTjeneste revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste,
            iayTjeneste, revurderingEndringES, revurderingTjenesteFelles, vergeRepository);

        // Act
        Behandling revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));

        // Assert
        assertThat(revurdering.getFagsak()).isEqualTo(behandlingSomSkalRevurderes.getFagsak());
        assertThat(revurdering.getBehandlingÅrsaker().get(0).getBehandlingÅrsakType()).isEqualTo(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
        assertThat(revurdering.getType()).isEqualTo(BehandlingType.REVURDERING);
        assertThat(revurdering.getAksjonspunkter()).isEmpty();
        assertThat(revurdering.getBehandlingstidFrist()).isNotEqualTo(behandlingSomSkalRevurderes.getBehandlingstidFrist());
    }

    private void opprettRevurderingsKandidat() {

        LocalDate terminDato = LocalDate.now().minusDays(70);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(terminDato).medUtstedtDato(terminDato))
            .medAntallBarn(1);
        scenario.medBekreftetHendelse()
            .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(terminDato).medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);
        behandling = scenario.lagre(repositoryProvider);
    }

    private FødtBarnInfo byggBaby(LocalDate fødselsdato) {
        return new FødtBarnInfo.Builder()
            .medFødselsdato(fødselsdato)
            .medIdent(PersonIdent.fra("12345678901"))
            .build();
    }
}
