package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FORESLÅTT;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@CdiDbAwareTest
class UttakStegImplTest {

    private static final String ORGNR = "123";
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private static final AktørId AKTØRID = AktørId.dummy();

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    @Inject
    private FamilieHendelseRepository familieHendelseRepository;
    @Inject
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Inject
    private SøknadRepository søknadRepository;
    @Inject
    private FpUttakRepository fpUttakRepository;
    @Inject
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private PersonopplysningRepository personopplysningRepository;
    @Inject
    private BehandlingLåsRepository behandlingLåsRepository;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private UttakStegImpl steg;

    private Behandling opprettBehandling() {
        var fagsak = opprettFagsak();
        var behandling = byggBehandlingForElektroniskSøknadOmFødsel(fagsak, LocalDate.now(), LocalDate.now());
        byggArbeidForBehandling(behandling);
        opprettUttaksperiodegrense(LocalDate.now(), behandling);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        var fagsak = FagsakBuilder.nyForeldrepengerForMor()
                .medSaksnummer(new Saksnummer("1234"))
                .medBrukerAktørId(AKTØRID)
                .build();
        fagsakRepository.opprettNy(fagsak);
        fagsakRelasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        return fagsak;
    }

    @Test
    void skal_utføre_uten_aksjonspunkt_når_det_ikke_er_noe_som_skal_fastsettes_manuelt() {
        var behandling = opprettBehandling();

        opprettPersonopplysninger(behandling);

        // Act
        var behandleStegResultat = steg.utførSteg(kontekst(behandling));

        assertThat(behandleStegResultat).isNotNull();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandlingskontrollKontekst kontekst(Behandling behandling) {
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    void skal_ha_aksjonspunkt_når_resultat_må_manuelt_fastsettes_her_pga_tomt_på_konto() {
        var behandling = opprettBehandling();

        var dekningsgrad = OppgittDekningsgradEntitet.bruk100();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOverstyrtFordeling(søknad4ukerFPFF())
            .medOppgittDekningsgrad(dekningsgrad);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        opprettPersonopplysninger(behandling);

        // Act
        var behandleStegResultat = steg.utførSteg(kontekst(behandling));

        assertThat(behandleStegResultat).isNotNull();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER);
    }

    @Test
    void skalBeregneStønadskontoVedFørsteBehandlingForFørsteForelder() {
        var fagsakForFar = FagsakBuilder.nyForeldrepengesak(RelasjonsRolleType.FARA)
                .medSaksnummer(new Saksnummer("12345"))
                .build();
        fagsakRepository.opprettNy(fagsakForFar);

        var farsBehandling = byggBehandlingForElektroniskSøknadOmFødsel(fagsakForFar, LocalDate.now(), LocalDate.now());
        byggArbeidForBehandling(farsBehandling);
        opprettUttaksperiodegrense(LocalDate.now(), farsBehandling);
        opprettPersonopplysninger(farsBehandling);

        var behandling = opprettBehandling();
        var fagsak = fagsakRepository.finnEksaktFagsak(behandling.getFagsakId());
        fagsakRelasjonRepository.kobleFagsaker(fagsak, fagsakForFar, behandling);

        opprettPersonopplysninger(behandling);

        var yfa = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(OppgittRettighetEntitet.aleneomsorg())
            .medOverstyrtRettighet(OppgittRettighetEntitet.aleneomsorg());
        ytelsesFordelingRepository.lagre(behandling.getId(), yfa.build());

        // Act -- behandler mors behandling først
        steg.utførSteg(kontekst(behandling));
        var morsFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak());

        // Assert - stønadskontoer skal ha blitt opprettet
        assertThat(morsFagsakRelasjon.getGjeldendeStønadskontoberegning()).isPresent();
        var førsteStønadskontoberegning = morsFagsakRelasjon.getGjeldendeStønadskontoberegning().get();

        // Act -- kjører steget på nytt for mor
        steg.utførSteg(kontekst(behandling));
        morsFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak());

        // Assert -- fortsatt innenfor første behandling -- skal beregne stønadskontoer på nytt men lagring gir samme id
        assertThat(morsFagsakRelasjon.getGjeldendeStønadskontoberegning()).isPresent();
        var andreStønadskontoberegning = morsFagsakRelasjon.getGjeldendeStønadskontoberegning().get();
        assertThat(andreStønadskontoberegning.getId()).isEqualTo(førsteStønadskontoberegning.getId());

        // Act -- kjører steget på nytt for mor
        yfa = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOverstyrtRettighet(OppgittRettighetEntitet.beggeRett());
        ytelsesFordelingRepository.lagre(behandling.getId(), yfa.build());
        steg.utførSteg(kontekst(behandling));
        morsFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak());

        // Assert -- fortsatt innenfor første behandling -- skal beregne stønadskontoer på nytt men lagring gir samme id
        assertThat(morsFagsakRelasjon.getGjeldendeStønadskontoberegning()).isPresent();
        var tredjeStønadskontoberegning = morsFagsakRelasjon.getGjeldendeStønadskontoberegning().get();
        assertThat(tredjeStønadskontoberegning.getId()).isNotEqualTo(førsteStønadskontoberegning.getId());

        // Avslutter mors behandling
        avsluttMedVedtak(behandling, repositoryProvider);

        // Act -- behandler fars behandling, skal ikke opprette stønadskontoer på nytt
        var kontekstForFarsBehandling = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                behandlingRepository.taSkriveLås(farsBehandling));
        steg.utførSteg(kontekstForFarsBehandling);

        var nyLagretFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(fagsakForFar);
        var stønadskontoberegningFar = nyLagretFagsakRelasjon.getGjeldendeStønadskontoberegning().get();

        // Assert
        assertThat(stønadskontoberegningFar.getId()).isEqualTo(tredjeStønadskontoberegning.getId());
    }

    @Test
    void skalBeregneStønadskontoNårDekningsgradErEndret() {

        var fødselsdato = LocalDate.of(2019, 2, 25);
        var fagsak = opprettFagsak();
        var morsFørstegang = byggBehandlingForElektroniskSøknadOmFødsel(fagsak, fødselsdato,
                fødselsdato, OppgittDekningsgradEntitet.bruk80());
        byggArbeidForBehandling(morsFørstegang);
        opprettUttaksperiodegrense(fødselsdato, morsFørstegang);
        opprettPersonopplysninger(morsFørstegang);
        fagsakRelasjonRepository.opprettEllerOppdaterRelasjon(morsFørstegang.getFagsak(),
                Optional.ofNullable(fagsakRelasjonRepository.finnRelasjonFor(morsFørstegang.getFagsak())),
                Dekningsgrad._80);
        var førstegangsKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                behandlingRepository.taSkriveLås(morsFørstegang));

        // Første versjon av kontoer opprettes
        steg.utførSteg(førstegangsKontekst);
        var morsFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(morsFørstegang.getFagsak());
        var førsteStønadskontoberegning = morsFagsakRelasjon.getGjeldendeStønadskontoberegning().get();

        avsluttMedVedtak(morsFørstegang, repositoryProvider);

        // mor oppdaterer dekningsgrad
        var morsRevurdering = opprettRevurdering(morsFørstegang, true, fødselsdato);
        fagsakRelasjonRepository.overstyrDekningsgrad(fagsak, Dekningsgrad._100);

        var revurderingKontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
                behandlingRepository.taSkriveLås(morsRevurdering));
        steg.utførSteg(revurderingKontekst);
        morsFagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(morsRevurdering.getFagsak());

        assertThat(morsFagsakRelasjon.getStønadskontoberegning()).isPresent();
        assertThat(morsFagsakRelasjon.getOverstyrtStønadskontoberegning()).isPresent();
        var overstyrtKontoberegning = morsFagsakRelasjon.getGjeldendeStønadskontoberegning().get();
        assertThat(overstyrtKontoberegning.getId()).isNotEqualTo(førsteStønadskontoberegning.getId());
        assertThat(morsFagsakRelasjon.getStønadskontoberegning().get().getId()).isEqualTo(førsteStønadskontoberegning.getId());
    }

    static void avsluttMedVedtak(Behandling behandling, BehandlingRepositoryProvider repositoryProvider) {
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lagretBehandling = behandlingRepository.hentBehandling(behandling.getId());
        var behandlingsresultat = Behandlingsresultat.opprettFor(lagretBehandling);
        var lås = repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());
        behandlingRepository.lagre(lagretBehandling, lås);
        var vedtak = BehandlingVedtak.builder()
                .medVedtakstidspunkt(LocalDateTime.now())
                .medAnsvarligSaksbehandler("abc")
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medBehandlingsresultat(behandlingsresultat)
                .build();
        repositoryProvider.getBehandlingVedtakRepository().lagre(vedtak, lås);
        lagretBehandling.avsluttBehandling();
        behandlingRepository.lagre(lagretBehandling, lås);
    }

    @Test
    void skalBeregneStønadskontoPåNyttNårFødselErFørUke33() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var terminbekreftelse = FamilieHendelseBuilder
                .oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD)
                .getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.of(2019, 9, 1));
        scenario.medSøknadHendelse().medTerminbekreftelse(terminbekreftelse);
        var fødselsdato = LocalDate.of(2019, 7, 1);
        var periodeBuilder = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fødselsdato, LocalDate.of(2019, 10, 1));
        scenario.medFordeling(new OppgittFordelingEntitet(List.of(periodeBuilder.build()), true));
        scenario.medOppgittRettighet(OppgittRettighetEntitet.beggeRett());
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(fødselsdato).build());
        var førstegangsBehandling = scenario.lagre(repositoryProvider);
        byggArbeidForBehandling(førstegangsBehandling);
        opprettUttaksperiodegrense(fødselsdato, førstegangsBehandling);
        fagsakRelasjonRepository.opprettRelasjon(førstegangsBehandling.getFagsak(), Dekningsgrad._100);

        kjørSteg(førstegangsBehandling);
        avsluttMedVedtak(førstegangsBehandling, repositoryProvider);

        var revurdering = opprettRevurdering(førstegangsBehandling, false, fødselsdato);
        var gjeldendeVersjon = familieHendelseRepository.hentAggregat(revurdering.getId()).getGjeldendeVersjon();
        var hendelse = FamilieHendelseBuilder.oppdatere(Optional.of(gjeldendeVersjon), HendelseVersjonType.SØKNAD);
        hendelse.medFødselsDato(fødselsdato).medAntallBarn(1);
        familieHendelseRepository.lagreRegisterHendelse(revurdering.getId(), hendelse);

        var relasjonFør = fagsakRelasjonRepository.finnRelasjonFor(revurdering.getFagsak());
        assertThat(relasjonFør.getOverstyrtStønadskontoberegning()).isNotPresent();

        kjørSteg(revurdering);
        var relasjonEtter = fagsakRelasjonRepository.finnRelasjonFor(revurdering.getFagsak());

        assertThat(relasjonEtter.getOverstyrtStønadskontoberegning()).isPresent();
    }

    private void kjørSteg(Behandling førstegangsBehandling) {
        var kontekst = new BehandlingskontrollKontekst(førstegangsBehandling.getFagsakId(),
                førstegangsBehandling.getAktørId(), behandlingLåsRepository.taLås(førstegangsBehandling.getId()));
        steg.utførSteg(kontekst);
    }

    private Behandling opprettRevurdering(Behandling tidligereBehandling, boolean endretDekningsgrad, LocalDate fødselsdato) {
        var revurdering = Behandling.fraTidligereBehandling(tidligereBehandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(
                        BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL).medOriginalBehandlingId(tidligereBehandling.getId()))
                .build();
        lagre(revurdering);
        var behandlingId = tidligereBehandling.getId();
        var revurderingId = revurdering.getId();
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        ytelsesFordelingRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurdering);
        familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(behandlingId, revurderingId);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(fødselsdato.minusWeeks(3))
                .medOpprinneligEndringsdato(fødselsdato.minusWeeks(3))
                .build();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(revurderingId)
            .medAvklarteDatoer(avklarteUttakDatoer)
            .build();
        ytelsesFordelingRepository.lagre(revurderingId, ytelseFordelingAggregat);

        var behandlingsresultat = Behandlingsresultat.builder()
                .medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT)
                .medEndretDekningsgrad(endretDekningsgrad)
                .buildFor(revurdering);
        revurdering.setBehandlingresultat(behandlingsresultat);
        lagre(revurdering);

        var vilkårResultat = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.INNVILGET).buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås(revurdering));

        opprettUttaksperiodegrense(LocalDate.now(), revurdering);
        opprettPersonopplysninger(revurdering);

        return revurdering;
    }

    private void lagre(Behandling behandling) {
        behandlingRepository.lagre(behandling, lås(behandling));
    }

    private BehandlingLås lås(Behandling behandling) {
        return behandlingLåsRepository.taLås(behandling.getId());
    }

    private void opprettPersonopplysninger(Behandling behandling) {
        var builder = personopplysningRepository.opprettBuilderForRegisterdata(behandling.getId());
        var personopplysningBuilder = builder.getPersonopplysningBuilder(behandling.getAktørId());
        personopplysningBuilder.medFødselsdato(LocalDate.now().minusYears(20));
        builder.leggTil(personopplysningBuilder);
        personopplysningRepository.lagre(behandling.getId(), builder);
    }

    @Test
    void skal_ha_aktiv_uttak_resultat_etter_tilbakehopp_til_steget() {
        var behandling = opprettBehandling();
        opprettPersonopplysninger(behandling);

        var kontekst = kontekst(behandling);
        steg.utførSteg(kontekst);

        // Act
        steg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, BehandlingStegType.VURDER_UTTAK,
                BehandlingStegType.FATTE_VEDTAK);

        // assert
        var resultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(resultat).isPresent();
    }

    @Test
    void skal_ikke_ha_aktiv_uttak_resultat_etter_tilbakehopp_over_steget() {
        var behandling = opprettBehandling();
        opprettPersonopplysninger(behandling);

        var kontekst = kontekst(behandling);
        steg.utførSteg(kontekst);

        // Act
        steg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_BAKOVER, BehandlingStegType.SØKERS_RELASJON_TIL_BARN,
                BehandlingStegType.FATTE_VEDTAK);

        // assert
        var resultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(resultat.isPresent()).isFalse();
    }

    @Test
    void skal_ikke_ha_aktiv_uttak_resultat_etter_fremoverhopp_over_steget() {
        var behandling = opprettBehandling();
        opprettPersonopplysninger(behandling);

        var kontekst = kontekst(behandling);
        steg.utførSteg(kontekst);

        // Act
        steg.vedTransisjon(kontekst, null, BehandlingSteg.TransisjonType.HOPP_OVER_FRAMOVER, BehandlingStegType.FATTE_VEDTAK,
                BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        // assert
        var resultat = fpUttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        assertThat(resultat.isPresent()).isFalse();
    }

    @Test
    void skal_ha_aksjonspunkt_når_dødsdato_er_registert() {
        var behandling = opprettBehandling();
        opprettPersonopplysninger(behandling);

        var bekreftetHendelse = familieHendelseRepository.opprettBuilderFor(behandling.getId())
            .tilbakestillBarn()
            .medAntallBarn(1)
            .leggTilBarn(LocalDate.now(), LocalDate.now().plusDays(1));
        familieHendelseRepository.lagreRegisterHendelse(behandling.getId(), bekreftetHendelse);

        // Act
        var behandleStegResultat = steg.utførSteg(kontekst(behandling));

        assertThat(behandleStegResultat).isNotNull();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).containsExactlyInAnyOrder(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER,
                AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);

    }

    @Test
    void skal_ha_aksjonspunkt_når_finnes_dødsdato_i_overstyrt_versjon() {
        var behandling = opprettBehandling();
        var bekreftetHendelse = familieHendelseRepository.opprettBuilderFor(behandling.getId()).tilbakestillBarn().medFødselsDato(LocalDate.now());
        familieHendelseRepository.lagreRegisterHendelse(behandling.getId(), bekreftetHendelse);

        var overstyrtHendelse = familieHendelseRepository.opprettBuilderFor(behandling.getId())
            .tilbakestillBarn()
            .leggTilBarn(LocalDate.now(), LocalDate.now().plusDays(1));
        familieHendelseRepository.lagreOverstyrtHendelse(behandling.getId(), overstyrtHendelse);
        opprettPersonopplysninger(behandling);

        // Act
        var behandleStegResultat = steg.utførSteg(kontekst(behandling));

        assertThat(behandleStegResultat).isNotNull();
        assertThat(behandleStegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandleStegResultat.getAksjonspunktListe()).containsExactlyInAnyOrder(AksjonspunktDefinisjon.FASTSETT_UTTAKPERIODER,
                AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD);
    }

    private OppgittFordelingEntitet søknad4ukerFPFF() {
        var fødselsdato = LocalDate.now();
        var periode1 = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
                .medPeriode(fødselsdato.minusWeeks(10), fødselsdato.minusDays(1))
                .medArbeidsgiver(virksomhet())
                .build();
        return new OppgittFordelingEntitet(List.of(periode1), true);
    }

    private Behandling byggBehandlingForElektroniskSøknadOmFødsel(Fagsak fagsak, LocalDate fødselsdato, LocalDate mottattDato) {
        return byggBehandlingForElektroniskSøknadOmFødsel(fagsak, fødselsdato, mottattDato, OppgittDekningsgradEntitet.bruk100());
    }

    private Behandling byggBehandlingForElektroniskSøknadOmFødsel(Fagsak fagsak, LocalDate fødselsdato, LocalDate mottattDato,
            OppgittDekningsgradEntitet oppgittDekningsgrad) {
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();

        behandling.setAnsvarligSaksbehandler("VL");
        var lås = behandlingLåsRepository.taLås(behandling.getId());

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(behandling);
        behandlingRepository.lagre(behandling, lås);

        var vilkårResultat = VilkårResultat.builder().medVilkårResultatType(VilkårResultatType.INNVILGET).buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, lås);

        var søknadHendelse = familieHendelseRepository.opprettBuilderFor(behandling.getId()).medAntallBarn(1).medFødselsDato(fødselsdato);
        familieHendelseRepository.lagre(behandling.getId(), søknadHendelse);

        var bekreftetHendelse = familieHendelseRepository.opprettBuilderFor(behandling.getId()).medAntallBarn(1).medFødselsDato(fødselsdato);
        familieHendelseRepository.lagre(behandling.getId(), bekreftetHendelse);

        OppgittFordelingEntitet fordeling;
        if (fagsak.getRelasjonsRolleType().equals(RelasjonsRolleType.MORA)) {
            var periode0 = OppgittPeriodeBuilder.ny()
                    .medPeriodeType(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
                    .medPeriode(fødselsdato.minusWeeks(3), fødselsdato.minusDays(1))
                    .medArbeidsgiver(virksomhet())
                    .build();

            var periode1 = OppgittPeriodeBuilder.ny()
                    .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                    .medPeriode(fødselsdato, fødselsdato.plusWeeks(6))
                    .medArbeidsgiver(virksomhet())
                    .build();

            var periode2 = OppgittPeriodeBuilder.ny()
                    .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                    .medPeriode(fødselsdato.plusWeeks(6).plusDays(1), fødselsdato.plusWeeks(10))
                    .medArbeidsgiver(virksomhet())
                    .build();

            fordeling = new OppgittFordelingEntitet(List.of(periode0, periode1, periode2), true);
        } else {
            var periodeFK = OppgittPeriodeBuilder.ny()
                    .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                    .medPeriode(fødselsdato.plusWeeks(10).plusDays(1), fødselsdato.plusWeeks(20))
                    .medArbeidsgiver(virksomhet())
                    .build();

            fordeling = new OppgittFordelingEntitet(List.of(periodeFK), true);
        }

        var yfBuilder = YtelseFordelingAggregat.oppdatere(Optional.empty())
            .medOppgittFordeling(fordeling)
            .medOppgittRettighet(OppgittRettighetEntitet.beggeRett())
            .medOppgittDekningsgrad(oppgittDekningsgrad)
            .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder().medJustertEndringsdato(fødselsdato).build());
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        var søknad = new SøknadEntitet.Builder().medSøknadsdato(LocalDate.now()).medMottattDato(mottattDato).medElektroniskRegistrert(true).build();
        søknadRepository.lagreOgFlush(behandling, søknad);
        return behandling;
    }

    private void byggArbeidForBehandling(Behandling behandling) {
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(AKTØRID);
        var yrkesaktivitetBuilder = aktørArbeidBuilder
                .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ARBEIDSFORHOLD_ID, ORGNR, null),
                        ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var fraOgMed = LocalDate.now().minusYears(1);
        var tilOgMed = LocalDate.now().plusYears(10);

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed))
                .medProsentsats(BigDecimal.TEN)
                .medSisteLønnsendringsdato(fraOgMed);

        yrkesaktivitetBuilder
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medArbeidsforholdId(ARBEIDSFORHOLD_ID)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .build();

        var aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        var arbId = InternArbeidsforholdRef.nyRef();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(LocalDate.now())
                .medGrunnbeløp(BigDecimal.TEN)
                .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.ny()
                        .medBeregningsgrunnlagPeriode(LocalDate.now(), LocalDate.now())
                        .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                                .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder()
                                        .medArbeidsforholdRef(arbId)
                                        .medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR)))
                                .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)))
                .build();

        beregningsgrunnlagKopierOgLagreTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, FORESLÅTT);
    }

    private Arbeidsgiver virksomhet() {
        return Arbeidsgiver.virksomhet(ORGNR);
    }

    private void opprettUttaksperiodegrense(LocalDate mottattDato, Behandling behandling) {
        var uttaksperiodegrense = new Uttaksperiodegrense(mottattDato);

        uttaksperiodegrenseRepository.lagre(behandling.getId(), uttaksperiodegrense);
    }
}
