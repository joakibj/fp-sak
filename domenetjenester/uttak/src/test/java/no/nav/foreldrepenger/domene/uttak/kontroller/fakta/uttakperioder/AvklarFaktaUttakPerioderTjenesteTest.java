package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AvklarFaktaUttakPerioderTjenesteTest extends EntityManagerAwareTest {

    private UttakRepositoryProvider repositoryProvider;
    private AvklarFaktaUttakPerioderTjeneste tjeneste;

    @BeforeEach
    public void setup() {
        repositoryProvider = new UttakRepositoryProvider(getEntityManager());
        tjeneste = new AvklarFaktaUttakPerioderTjeneste(repositoryProvider.getYtelsesFordelingRepository());
    }

    @Test
    public void skal_hente_kontroller_fakta_perioder_med_bekreftelse() {
        var fom = LocalDate.of(2020, 10, 10);
        var søknadsperiode = OppgittPeriodeBuilder.ny()
            .medÅrsak(UtsettelseÅrsak.SYKDOM)
            .medPeriode(fom, LocalDate.of(2020, 11, 11))
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(søknadsperiode), true))
            .lagre(repositoryProvider);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag(fom));
        var resultat = tjeneste.hentKontrollerFaktaPerioder(input);

        assertThat(resultat.getPerioder()).hasSize(1);
        assertThat(resultat.getPerioder().get(0).erBekreftet()).isFalse();
    }

    @Test
    public void skal_returnere_tom_liste_uten_ytelsefordeling() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .lagre(repositoryProvider);
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, fpGrunnlag(LocalDate.of(2020, 10, 10)));
        var resultat = tjeneste.hentKontrollerFaktaPerioder(input);

        assertThat(resultat.getPerioder()).isEmpty();
    }

    private ForeldrepengerGrunnlag fpGrunnlag(LocalDate termindato) {
        var søknadHendelse = FamilieHendelse.forFødsel(termindato, null, List.of(), 1);
        return new ForeldrepengerGrunnlag().medFamilieHendelser(new FamilieHendelser().medSøknadHendelse(søknadHendelse));
    }

}
