package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.svp;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.FPDateUtil;

@RunWith(CdiRunner.class)
public class FastsettOpptjeningsperiodeStegTest {

    private static final String ORGNR = "100";
    private final int ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING = 28;

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final SvangerskapspengerRepository svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    @BehandlingTypeRef
    @FagsakYtelseTypeRef("SVP")
    private FastsettOpptjeningsperiodeSteg opptjeningsperiodeSvpSteg;
    @Inject
    @BehandlingTypeRef
    @FagsakYtelseTypeRef("SVP")
    private VurderOpptjeningsvilkårSteg vurderOpptjeningsvilkårSteg;
    private LocalDate jordmorsdato = LocalDate.now().minusDays(30);

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_fastsette_opptjeningsperioden_for_SVP_til_28_dager() {
        // Arrange
        var scenario = byggBehandlingScenario();
        Behandling behandling = lagre(scenario);
        lagreSvp(behandling, jordmorsdato);

        Fagsak fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);

        // Assert
        Opptjening opptjening = repositoryProvider.getOpptjeningRepository().finnOpptjening(behandling.getId()).orElseThrow();

        assertThat(opptjening.getFom()).isEqualTo(jordmorsdato.minusDays(28));
        assertThat(opptjening.getTom()).isEqualTo(jordmorsdato.minusDays(1));
        // between tar -> the start date, inclusive, not null & the end date, exclusive, not null
        assertThat(Period.between(opptjening.getFom(), opptjening.getTom().plusDays(1)).getDays()).isEqualTo(ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING);
    }

    @Test
    public void skal_vurdere_opptjeningsvilkåret_for_SVP_til_oppfylt() {
        // Arrange
        var scenario = byggBehandlingScenario();
        Behandling behandling = lagre(scenario);

        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        lagAktørArbeid(aggregatBuilder, behandling.getAktørId(), ORGNR, LocalDate.now().minusYears(1), Tid.TIDENES_ENDE, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        lagreSvp(behandling, jordmorsdato);

        Fagsak fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);


        var lås2 = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst2 = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås2);

        // Act
        vurderOpptjeningsvilkårSteg.utførSteg(kontekst2);

        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        Vilkår opptjeningsvilkåret = behandlingsresultat.getVilkårResultat().getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
            .findFirst()
            .orElseThrow();

        Opptjening opptjening = repositoryProvider.getOpptjeningRepository().finnOpptjening(behandling.getId()).orElseThrow();
        assertThat(opptjeningsvilkåret.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(opptjening.getOpptjentPeriode().getDays()).isEqualTo(ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING);
    }

    @Test
    public void skal_vurdere_opptjeningsvilkåret_for_SVP_til_ikke_oppfylt_når_søker_ikke_har_nok_arbeid() {
        // Arrange
        var scenario = byggBehandlingScenario();
        Behandling behandling = lagre(scenario);
        lagreSvp(behandling, jordmorsdato);

        Fagsak fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);


        var lås2 = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst2 = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås2);

        // Act
        vurderOpptjeningsvilkårSteg.utførSteg(kontekst2);

        Behandlingsresultat behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        Vilkår opptjeningsvilkåret = behandlingsresultat.getVilkårResultat().getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
            .findFirst()
            .orElseThrow();

        assertThat(opptjeningsvilkåret.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.IKKE_OPPFYLT);
    }

    @Test
    public void skal_vurdere_opptjeningsvilkåret_for_SVP_til_oppfylt_når_søker_bare_har_aktivitet() {
        // Arrange
        var scenario = byggBehandlingScenario();
        Behandling behandling = lagre(scenario);
        lagreSvp(behandling, jordmorsdato);

        Fagsak fagsak = behandling.getFagsak();
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        // Simulerer at disse vilkårene har blitt opprettet
        opprettVilkår(List.of(OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET), behandling, lås);
        var kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);
        opptjeningsperiodeSvpSteg.utførSteg(kontekst);


        var lås2 = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst2 = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås2);

        //simuler at aktiviteten har blitt godkjent
        var aktivitetsPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(6));
        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(aktivitetsPeriode, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        godkjennGittArbeidtypeMedPeriode(behandling, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, aktivitetsPeriode);

        // Act
        vurderOpptjeningsvilkårSteg.utførSteg(kontekst2);

        Behandlingsresultat behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        Vilkår opptjeningsvilkåret = behandlingsresultat.getVilkårResultat().getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
            .findFirst()
            .orElseThrow();

        Opptjening opptjening = repositoryProvider.getOpptjeningRepository().finnOpptjening(behandling.getId()).orElseThrow();
        assertThat(opptjeningsvilkåret.getGjeldendeVilkårUtfall()).isEqualTo(VilkårUtfallType.OPPFYLT);
        assertThat(opptjening.getOpptjentPeriode().getDays()).isEqualTo(ANTALL_DAGER_SVANGERSKAP_SKAL_SJEKKE_FOR_OPPTJENING);
    }

    private void godkjennGittArbeidtypeMedPeriode(Behandling behandling, ArbeidType militærEllerSiviltjeneste, DatoIntervallEntitet aktivitetsPeriode) {
        var builder = iayTjeneste.opprettBuilderForSaksbehandlet(behandling.getId());
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        var builderY = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(militærEllerSiviltjeneste);
        var aktivitetsAvtaleBuilder = builderY.getAktivitetsAvtaleBuilder(aktivitetsPeriode, true);
        builderY.leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);
        aktørArbeidBuilder.leggTilYrkesaktivitet(builderY);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private AbstractTestScenario<?> byggBehandlingScenario() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        leggTilSøker(scenario);
        return scenario;
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, NavBrukerKjønn.KVINNE, Region.UDEFINERT)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void opprettVilkår(List<VilkårType> typeList, Behandling behandling, BehandlingLås skriveLås) {
        // Opprett Vilkårsresultat med vilkårne som skal vurderes, og sett dem som ikke vurdert
        var behandlingsresultat = behandling.getBehandlingsresultat();
        var vilkårBuilder = behandlingsresultat != null
            ? VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat())
            : VilkårResultat.builder();
        typeList
            .forEach(vilkårType -> vilkårBuilder.leggTilVilkår(vilkårType, VilkårUtfallType.IKKE_VURDERT));
        VilkårResultat vilkårResultat = vilkårBuilder.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, skriveLås);
    }

    private void lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                                LocalDate fom, LocalDate tom, ArbeidType arbeidType) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
            .getAktørArbeidBuilder(aktørId);
        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var yrkesaktivitetBuilder = aktørArbeidBuilder
            .getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.
            medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(virksomhetOrgnr));

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
    }

    private void lagreSvp(Behandling behandling, LocalDate jordmorsdato) {
        SvpTilretteleggingEntitet tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medIngenTilrettelegging(jordmorsdato)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(FPDateUtil.nå())
            .medKopiertFraTidligereBehandling(false)
            .build();
        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }
}
