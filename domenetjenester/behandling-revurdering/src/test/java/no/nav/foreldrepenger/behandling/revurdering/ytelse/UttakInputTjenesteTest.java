package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class UttakInputTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private UttakInputTjeneste tjeneste;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Test
    public void skal_hente_behandlingsårsaker_fra_behandling() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);
        var årsak = BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, årsak, false)
            .medDefaultOppgittFordeling(LocalDate.of(2019, 11, 6))
            .medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(revurdering);

        assertThat(resultat.isBehandlingManueltOpprettet()).isFalse();
        assertThat(resultat.harBehandlingÅrsak(årsak)).isTrue();
    }

    @Test
    public void skal_sette_om_behandling_er_manuelt_behandlet() {
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);
        var revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT, true)
            .medDefaultOppgittFordeling(LocalDate.of(2019, 11, 6))
            .medDefaultSøknadTerminbekreftelse()
            .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(revurdering);

        assertThat(resultat.isBehandlingManueltOpprettet()).isTrue();
    }

    @Test
    public void skal_sette_om_opplysninger_om_død_er_endret_hvis_det_er_endringer() {
        var behandlingMedEndretOpplysningerOmDød = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultOppgittFordeling(LocalDate.of(2019, 1, 1))
            .lagre(repositoryProvider);
        var personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Optional<PersonInformasjonEntitet> registerVersjon = personopplysningRepository.hentPersonopplysninger(behandlingMedEndretOpplysningerOmDød.getId()).getRegisterVersjon();
        PersonInformasjonBuilder builder = PersonInformasjonBuilder.oppdater(registerVersjon, PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(behandlingMedEndretOpplysningerOmDød.getAktørId()).medDødsdato(LocalDate.now()));
        personopplysningRepository.lagre(behandlingMedEndretOpplysningerOmDød.getId(), builder);

        var resultat = tjeneste.lagInput(behandlingMedEndretOpplysningerOmDød.getId());

        assertThat(resultat.isOpplysningerOmDødEndret()).isTrue();
    }

    @Test
    public void skal_sette_om_opplysninger_om_død_er_endret_hvis_det_er_ingen_endringer() {
        var behandlingUtenEndringIOpplysninger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultOppgittFordeling(LocalDate.of(2019, 1, 1))
            .lagre(repositoryProvider);

        var resultat = tjeneste.lagInput(behandlingUtenEndringIOpplysninger.getId());

        assertThat(resultat.isOpplysningerOmDødEndret()).isFalse();
    }
}
