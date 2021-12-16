package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.RisikoklassifiseringEntitet;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.RisikovurderingTjeneste;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalGruppeWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;

public class KontrollDtoTjenesteTest {

    private KontrollDtoTjeneste kontrollDtoTjeneste;

    private RisikovurderingTjeneste risikovurderingTjeneste = mock(RisikovurderingTjeneste.class);

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        var scenarioKlage = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenarioKlage.lagMocked();
        kontrollDtoTjeneste = new KontrollDtoTjeneste(risikovurderingTjeneste);
    }

    @Test
    public void skal_teste_at_dto_lages_korrekt_når_resultatet_mangler() {
        // Arrange
        when(risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling)).thenReturn(Optional.empty());

        // Act
        var kontrollresultatDto = kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(kontrollresultatDto).isPresent();
        assertThat(kontrollresultatDto.get().kontrollresultat()).isEqualTo(Kontrollresultat.IKKE_KLASSIFISERT);
    }

    @Test
    public void skal_teste_at_dto_lages_korrekt_når_resultatet_viser_ikke_høy() {
        // Arrange
        when(risikovurderingTjeneste.hentRisikoklassifiseringForBehandling(behandling.getId()))
                .thenReturn(Optional.of(lagEntitet(Kontrollresultat.IKKE_HØY, FaresignalVurdering.UDEFINERT)));

        // Act
        var kontrollresultatDto = kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(kontrollresultatDto).isPresent();
        assertThat(kontrollresultatDto.get().kontrollresultat()).isEqualTo(Kontrollresultat.IKKE_HØY);
        assertThat(kontrollresultatDto.get().medlFaresignaler()).isNull();
        assertThat(kontrollresultatDto.get().iayFaresignaler()).isNull();

    }

    @Test
    public void skal_teste_at_dto_lages_korrekt_når_resultatet_viser_høy() {
        // Arrange
        var faresignaler = Arrays.asList("Grunn en", "Grunn to", "Grunn tre", "Grunn 4", "Grunn 5");
        when(risikovurderingTjeneste.finnKontrollresultatForBehandling(behandling))
                .thenReturn(Optional.of(lagFaresignalWrapper(Kontrollresultat.HØY, faresignaler)));
        when(risikovurderingTjeneste.hentRisikoklassifiseringForBehandling(behandling.getId()))
                .thenReturn(Optional.of(lagEntitet(Kontrollresultat.HØY, FaresignalVurdering.UDEFINERT)));

        // Act
        var kontrollresultatDto = kontrollDtoTjeneste.lagKontrollresultatForBehandling(behandling);

        // Assert
        assertThat(kontrollresultatDto).isPresent();
        assertThat(kontrollresultatDto.get().kontrollresultat()).isEqualTo(Kontrollresultat.HØY);
        assertThat(kontrollresultatDto.get().medlFaresignaler()).isNotNull();
        assertThat(kontrollresultatDto.get().medlFaresignaler().faresignaler()).containsAll(faresignaler);
        assertThat(kontrollresultatDto.get().iayFaresignaler()).isNotNull();
        assertThat(kontrollresultatDto.get().iayFaresignaler().faresignaler()).containsAll(faresignaler);
    }

    private RisikoklassifiseringEntitet lagEntitet(Kontrollresultat kontrollresultat, FaresignalVurdering faresignalVurdering) {
        return RisikoklassifiseringEntitet.builder()
                .medFaresignalVurdering(faresignalVurdering)
                .medKontrollresultat(kontrollresultat)
                .buildFor(behandling.getId());
    }

    private FaresignalWrapper lagFaresignalWrapper(Kontrollresultat kontrollresultat, List<String> faresignaler) {
        if (!faresignaler.isEmpty()) {
            var iayBuilder = new FaresignalGruppeWrapper(faresignaler);
            var medlBuilder = new FaresignalGruppeWrapper(faresignaler);
            return new FaresignalWrapper(kontrollresultat, null, medlBuilder, iayBuilder);
        }
        return new FaresignalWrapper(kontrollresultat, null, null, null);
    }
}
