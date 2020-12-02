package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling.forceOppdaterBehandlingSteg;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personstatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Statsborgerskap;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

@CdiDbAwareTest
public class KontrollerFaktaRevurderingStegImplTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    @Any
    private KontrollerFaktaRevurderingStegImpl steg;

    @Test
    public void skal_fjerne_aksjonspunkter_som_er_utledet_før_startpunktet() {
        var behandling = opprettRevurdering();
        Fagsak fagsak = behandling.getFagsak();
        // Arrange
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);
        behandling.setStartpunkt(StartpunktType.UTTAKSVILKÅR);

        // Act
        List<AksjonspunktDefinisjon> aksjonspunkter = steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        assertThat(aksjonspunkter).doesNotContain(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD);
    }

    @Test
    public void skal_ikke_fjerne_aksjonspunkter_som_er_utledet_etter_startpunktet() {
        var behandling = opprettRevurdering();
        Fagsak fagsak = behandling.getFagsak();
        // Arrange
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        List<AksjonspunktDefinisjon> aksjonspunkter = steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD);
        // Må verifisere at startpunkt er før aksjonpunktet for at assert ovenfor skal
        // ha mening
        assertThat(behandling.getStartpunkt()).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    @Test
    public void må_nullstille_fordelingsperiode_hvis_ikke_er_endringssøknad() {
        var behandling = opprettRevurdering();
        Fagsak fagsak = behandling.getFagsak();
        // Arrange
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregatHvisEksisterer(behandling.getId());
        assertThat(ytelseFordelingAggregat).isPresent();
        YtelseFordelingAggregat aggregat = ytelseFordelingAggregat.get();
        assertThat(aggregat.getOppgittFordeling()).isNotNull();
        assertThat(aggregat.getOppgittFordeling().getOppgittePerioder()).isEmpty();
        assertThat(aggregat.getOppgittFordeling().getErAnnenForelderInformert()).isTrue();
    }

    @Test
    public void må_ikke_nullstille_fordelingsperiode_hvis_er_revurdering_med_førstegangssøknad_uten_uttak() {
        var søknadsperiode = OppgittPeriodeBuilder.ny()
                .medPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 5, 5))
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();
        var avslåttFørstegangsbehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.fraEksisterende(søknadsperiode).build()), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                .medDefaultOppgittTilknytning()
                .lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(avslåttFørstegangsbehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG)
                .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.fraEksisterende(søknadsperiode).build()), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                .medDefaultOppgittTilknytning()
                .lagre(repositoryProvider);

        var kontekst = new BehandlingskontrollKontekst(revurdering.getFagsakId(), revurdering.getAktørId(),
                behandlingRepository.taSkriveLås(revurdering.getId()));
        steg.utførSteg(kontekst);

        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregat(revurdering.getId());
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getErAnnenForelderInformert()).isTrue();
    }

    @Test
    public void må_ikke_nullstille_fordelingsperiode_hvis_er_revurdering_av_revurdering_uten_uttak() {
        var avslåttFørstegangsbehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .lagre(repositoryProvider);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
                .medPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 5, 5))
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();
        var avslåttRevurdering1 = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(avslåttFørstegangsbehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG)
                .medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT))
                .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                // Behandling avslås i inngangsvilkår, derfor ikke noe uttak
                .lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(avslåttRevurdering1, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG)
                .medFordeling(new OppgittFordelingEntitet(List.of(OppgittPeriodeBuilder.fraEksisterende(søknadsperiode).build()), true))
                .medFødselAdopsjonsdato(List.of(LocalDate.of(2020, 1, 1)))
                .medDefaultOppgittTilknytning()
                .lagre(repositoryProvider);

        var kontekst = new BehandlingskontrollKontekst(revurdering.getFagsakId(), revurdering.getAktørId(),
                behandlingRepository.taSkriveLås(revurdering.getId()));
        steg.utførSteg(kontekst);

        var ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregat(revurdering.getId());
        assertThat(ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
    }

    @Test
    public void må_ikke_nullstille_fordelingsperiode_hvis_er_endringssøknad() {
        var behandling = opprettRevurdering();
        LocalDate fom = LocalDate.now();
        LocalDate tom = LocalDate.now().plusWeeks(30);

        Behandling revurdering = opprettRevurderingPgaEndringsSøknad(behandling, fom, tom);

        Fagsak fagsak = revurdering.getFagsak();
        // Arrange
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = repositoryProvider.getYtelsesFordelingRepository()
                .hentAggregatHvisEksisterer(revurdering.getId());
        assertThat(ytelseFordelingAggregat).isPresent();
        YtelseFordelingAggregat aggregat = ytelseFordelingAggregat.get();
        assertThat(aggregat.getOppgittFordeling()).isNotNull();
        assertThat(aggregat.getOppgittFordeling().getOppgittePerioder()).isNotEmpty();
        assertThat(aggregat.getOppgittFordeling().getOppgittePerioder()).size().isEqualTo(1);
        assertThat(aggregat.getOppgittFordeling().getOppgittePerioder().get(0).getFom()).isEqualTo(fom);
        assertThat(aggregat.getOppgittFordeling().getOppgittePerioder().get(0).getTom()).isEqualTo(tom);
        assertThat(aggregat.getOppgittFordeling().getErAnnenForelderInformert()).isTrue();
    }

    private Behandling opprettRevurderingPgaEndringsSøknad(Behandling originalBehandling, LocalDate fom, LocalDate tom) {
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(originalBehandling, LocalDate.now().minusYears(1), LocalDate.now(),
                false);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        revurderingScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());
        revurderingScenario.medDefaultOppgittTilknytning();

        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);
        // Legg til fordelingsperiode
        OppgittPeriodeEntitet foreldrepenger = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medPeriode(fom, tom)
                .build();
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(Collections.singletonList(foreldrepenger), true);
        repositoryProvider.getYtelsesFordelingRepository().lagre(revurdering.getId(), fordeling);

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);

        return revurdering;
    }

    @Test
    public void skal_utlede_startpunkt_dersom_uttaksplan_på_original_behandling_mangler() {
        // Arrange
        Behandling revurdering = opprettRevurderingPgaBerørtBehandling();
        Fagsak fagsak = revurdering.getFagsak();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        Behandling behandlingEtterSteg = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(behandlingEtterSteg.getStartpunkt()).isEqualTo(StartpunktType.UTTAKSVILKÅR);
    }

    @Test
    public void skal_sette_startpunkt_inngangsvilkår_for_manuelt_opprettet_revurdering() {
        var behandling = opprettRevurdering();
        // Arrange
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(List.of(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKERS_REL));
        behandling.getBehandlingÅrsaker().add(builder.medManueltOpprettet(true).buildFor(behandling).get(0));

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLER_FAKTA);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        Fagsak fagsak = behandling.getFagsak();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        steg.utførSteg(kontekst).getAksjonspunktListe();

        // Assert
        Behandling behandlingEtterSteg = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandlingEtterSteg.getStartpunkt()).isEqualTo(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT);
    }

    private Behandling opprettRevurderingPgaBerørtBehandling() {
        var førstegangsbehandling = opprettFørstegangsbehandling(new Behandlingsresultat.Builder());
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(førstegangsbehandling, LocalDate.now().minusYears(1), LocalDate.now(),
                false);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        revurderingScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());
        revurderingScenario.medDefaultOppgittTilknytning();

        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));
        Behandling revurdering = revurderingScenario.lagre(repositoryProvider);
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(Collections.emptyList(), true);
        repositoryProvider.getYtelsesFordelingRepository().lagre(revurdering.getId(), fordeling);

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(revurdering, BehandlingStegType.KONTROLLER_FAKTA);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, behandlingLås);

        return revurdering;
    }

    private Behandling opprettFørstegangsbehandling(Behandlingsresultat.Builder behandlingsresultat) {
        LocalDate fødselsdato = LocalDate.now().minusYears(20);
        AktørId aktørId = AktørId.dummy();

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .medBehandlingsresultat(behandlingsresultat)
                .medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA);

        AktørId søkerAktørId = førstegangScenario.getDefaultBrukerAktørId();

        PersonInformasjon personInformasjon = førstegangScenario
                .opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.SAMBOER).statsborgerskap(Landkoder.USA)
                .build();

        førstegangScenario.medRegisterOpplysninger(personInformasjon);

        førstegangScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));

        var personopplysningBuilder = førstegangScenario.opprettBuilderForRegisteropplysninger();
        personopplysningBuilder.leggTilPersonopplysninger(
                Personopplysning.builder().aktørId(aktørId).sivilstand(SivilstandType.GIFT)
                        .fødselsdato(fødselsdato).brukerKjønn(NavBrukerKjønn.KVINNE).navn("Marie Curie")
                        .region(Region.UDEFINERT))
                .leggTilAdresser(
                        PersonAdresse.builder()
                                .adresselinje1("dsffsd 13").aktørId(aktørId).land("USA")
                                .adresseType(AdresseType.POSTADRESSE_UTLAND)
                                .periode(fødselsdato, LocalDate.now()))
                .leggTilPersonstatus(
                        Personstatus.builder().aktørId(aktørId).personstatus(PersonstatusType.UTVA)
                                .periode(fødselsdato, LocalDate.now()))
                .leggTilStatsborgerskap(
                        Statsborgerskap.builder().aktørId(aktørId)
                                .periode(fødselsdato, LocalDate.now())
                                .region(Region.UDEFINERT)
                                .statsborgerskap(Landkoder.USA));

        førstegangScenario.medRegisterOpplysninger(personopplysningBuilder.build());
        førstegangScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());

        return førstegangScenario.lagre(repositoryProvider);
    }

    private Behandling opprettRevurdering() {
        LocalDate fødselsdato = LocalDate.now().minusYears(20);
        AktørId aktørId = AktørId.dummy();

        ScenarioMorSøkerForeldrepenger førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .medBehandlingStegStart(BehandlingStegType.KONTROLLER_FAKTA)
                .medUttak(new UttakResultatPerioderEntitet());

        førstegangScenario.medDefaultOppgittTilknytning();

        AktørId søkerAktørId = førstegangScenario.getDefaultBrukerAktørId();

        PersonInformasjon personInformasjon = førstegangScenario
                .opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .kvinne(søkerAktørId, SivilstandType.SAMBOER).statsborgerskap(Landkoder.USA)
                .build();

        førstegangScenario.medRegisterOpplysninger(personInformasjon);

        førstegangScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));

        var personopplysningBuilder = førstegangScenario.opprettBuilderForRegisteropplysninger();
        personopplysningBuilder.leggTilPersonopplysninger(
                Personopplysning.builder().aktørId(aktørId).sivilstand(SivilstandType.GIFT)
                        .fødselsdato(fødselsdato).brukerKjønn(NavBrukerKjønn.KVINNE).navn("Marie Curie")
                        .region(Region.UDEFINERT))
                .leggTilAdresser(
                        PersonAdresse.builder()
                                .adresselinje1("dsffsd 13").aktørId(aktørId).land("USA")
                                .adresseType(AdresseType.POSTADRESSE_UTLAND)
                                .periode(fødselsdato, LocalDate.now()))
                .leggTilPersonstatus(
                        Personstatus.builder().aktørId(aktørId).personstatus(PersonstatusType.UTVA)
                                .periode(fødselsdato, LocalDate.now()))
                .leggTilStatsborgerskap(
                        Statsborgerskap.builder().aktørId(aktørId)
                                .periode(fødselsdato, LocalDate.now())
                                .region(Region.UDEFINERT)
                                .statsborgerskap(Landkoder.USA));

        førstegangScenario.medRegisterOpplysninger(personopplysningBuilder.build());
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medFørsteUttaksdato(LocalDate.now())
                .build();
        førstegangScenario.medAvklarteUttakDatoer(avklarteUttakDatoer);

        Behandling originalBehandling = førstegangScenario.lagre(repositoryProvider);
        // Legg til Uttaksperiodegrense -> dessverre ikke tilgjengelig i scenariobygger
        BehandlingLås lås = behandlingRepository.taSkriveLås(originalBehandling);
        behandlingRepository.lagre(originalBehandling, lås);
        Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(originalBehandling.getBehandlingsresultat())
                .medFørsteLovligeUttaksdag(LocalDate.now())
                .medMottattDato(LocalDate.now())
                .build();
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(originalBehandling.getId(), uttaksperiodegrense);
        // Legg til Opptjeningsperidoe -> dessverre ikke tilgjengelig i scenariobygger
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(originalBehandling, LocalDate.now().minusYears(1), LocalDate.now(),
                false);
        // Legg til fordelingsperiode
        OppgittPeriodeEntitet foreldrepenger = OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(20))
                .build();
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(Collections.singletonList(foreldrepenger), true);
        Long orgBehandlingId = originalBehandling.getId();
        repositoryProvider.getYtelsesFordelingRepository().lagre(orgBehandlingId, fordeling);

        ScenarioMorSøkerForeldrepenger revurderingScenario = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .medRegisterOpplysninger(personopplysningBuilder.build())
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        revurderingScenario.medDefaultOppgittTilknytning();

        revurderingScenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());

        revurderingScenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(10));
        var behandling = revurderingScenario.lagre(repositoryProvider);
        // kopierer ytelsefordeling grunnlag
        repositoryProvider.getYtelsesFordelingRepository().kopierGrunnlagFraEksisterendeBehandling(originalBehandling.getId(), behandling.getId());

        // Nødvendig å sette aktivt steg for KOFAK revurdering
        forceOppdaterBehandlingSteg(behandling, BehandlingStegType.KONTROLLER_FAKTA);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        return behandling;
    }
}
