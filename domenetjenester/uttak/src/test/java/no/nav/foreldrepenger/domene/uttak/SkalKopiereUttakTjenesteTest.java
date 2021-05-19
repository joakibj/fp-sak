package no.nav.foreldrepenger.domene.uttak;


import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_HENDELSE_DØD_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_SATS_REGULERING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

class SkalKopiereUttakTjenesteTest {

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    public void endret_arbeid_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), true, true)).isFalse();
    }

    @Test
    public void endret_inntektsmelding_sammen_med_søknad_skal_ikke_kopiere() {
        assertThat(
            skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING, RE_ENDRING_FRA_BRUKER), false, true)).isFalse();
    }

    @Test
    public void endret_inntektsmelding_skal_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, true)).isTrue();
    }

    @Test
    public void endret_inntektsmelding_men_årsak_om_død_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_HENDELSE_DØD_BARN), false, true)).isFalse();
    }

    @Test
    public void endret_inntektsmelding_men_opplysninger_om_død_endret_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, true, true)).isFalse();
    }

    @Test
    public void endret_inntektsmelding_og_g_reg_skal_kopiere() {
        assertThat(
            skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING, RE_SATS_REGULERING), false, true)).isTrue();
    }

    @Test
    public void endret_inntektsmelding_i_førstegangsbehandling_skal_ikke_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_ENDRET_INNTEKTSMELDING), false, false)).isFalse();
    }

    @Test
    public void g_reg_skal_kopiere() {
        assertThat(skalKopiereStegResultat(Set.of(RE_SATS_REGULERING), false, true)).isTrue();
    }

    @Test
    public void saksbehandler_har_ikke_avklart_startdato_skal_kopiere() {
        var uttakInput = lagInput(Set.of(RE_ENDRET_INNTEKTSMELDING), true, false);
        var tjeneste = opprettTjeneste(false);

        //null = ikke avklart i denne behandlingen
        settFørsteUttaksdato(null, uttakInput.getBehandlingReferanse());
        assertThat(tjeneste.skalKopiereStegResultat(uttakInput)).isTrue();
    }

    @Test
    public void saksbehandler_har_avklart_startdato_skal_ikke_kopiere() {
        var uttakInput = lagInput(Set.of(RE_ENDRET_INNTEKTSMELDING), true, false);
        var tjeneste = opprettTjeneste(false);

        settFørsteUttaksdato(LocalDate.now(), uttakInput.getBehandlingReferanse());
        assertThat(tjeneste.skalKopiereStegResultat(uttakInput)).isFalse();
    }

    private void settFørsteUttaksdato(LocalDate førsteUttaksdato, BehandlingReferanse behandlingReferanse) {
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var behandlingId = behandlingReferanse.getBehandlingId();
        var yfa = ytelsesFordelingRepository.opprettBuilder(behandlingId);
        var avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(førsteUttaksdato)
            .medJustertEndringsdato(LocalDate.now())
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, yfa.medAvklarteDatoer(avklarteDatoer).build());
    }

    private boolean skalKopiereStegResultat(Set<BehandlingÅrsakType> årsaker,
                                            boolean arbeidEndret,
                                            boolean erRevurdering) {
        return skalKopiereStegResultat(årsaker, arbeidEndret, erRevurdering, false);
    }

    private boolean skalKopiereStegResultat(Set<BehandlingÅrsakType> årsaker,
                                            boolean arbeidEndret,
                                            boolean erRevurdering,
                                            boolean opplysningerOmDødEndret) {
        var input = lagInput(årsaker, erRevurdering, opplysningerOmDødEndret);
        var tjeneste = opprettTjeneste(arbeidEndret);
        return tjeneste.skalKopiereStegResultat(input);
    }

    private SkalKopiereUttakTjeneste opprettTjeneste(boolean arbeidEndret) {
        var relevanteArbeidsforholdTjeneste = mock(RelevanteArbeidsforholdTjeneste.class);
        when(relevanteArbeidsforholdTjeneste.arbeidsforholdRelevantForUttakErEndretSidenForrigeBehandling(
            any())).thenReturn(arbeidEndret);
        return new SkalKopiereUttakTjeneste(relevanteArbeidsforholdTjeneste,
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()));
    }

    private UttakInput lagInput(Set<BehandlingÅrsakType> årsaker,
                                boolean erRevurdering,
                                boolean opplysningerOmDødEndret) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        if (erRevurdering) {
            scenario.medOriginalBehandling(ScenarioMorSøkerForeldrepenger.forFødsel().lagre(repositoryProvider),
                årsaker);
        }
        var behandling = scenario.lagre(repositoryProvider);
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null)
            .medErOpplysningerOmDødEndret(opplysningerOmDødEndret)
            .medBehandlingÅrsaker(årsaker);
    }
}
