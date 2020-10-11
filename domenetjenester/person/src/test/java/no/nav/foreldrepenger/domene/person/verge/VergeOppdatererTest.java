package no.nav.foreldrepenger.domene.person.verge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.AvklarVergeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(MockitoExtension.class)

public class VergeOppdatererTest {

    private static final AksjonspunktDefinisjon AKSJONSPUNKT_DEF = AksjonspunktDefinisjon.AVKLAR_VERGE;

    @Mock
    private HistorikkTjenesteAdapter historikkTjeneste;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private NavBrukerRepository navBrukerRepository;

    private NavBruker vergeBruker;
    private Personinfo pInfo;

    @BeforeEach
    public void oppsett() {

        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        @SuppressWarnings("unused")
        Behandling behandling = scenario.lagMocked();

        pInfo = new Personinfo.Builder()
            .medNavn("Verger Vergusen")
            .medAktørId(AktørId.dummy())
            .medPersonIdent(new PersonIdent(new FiktiveFnr().nesteKvinneFnr()))
            .medFødselsdato(LocalDate.now().minusYears(33))
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medForetrukketSpråk(Språkkode.NB)
            .build();

        vergeBruker = NavBruker.opprettNy(pInfo);

        lenient().when(personinfoAdapter.hentAktørForFnr(Mockito.any())).thenReturn(Optional.of(AktørId.dummy()));
        lenient().when(navBrukerRepository.hent(Mockito.any())).thenReturn(Optional.of(vergeBruker));
    }

    @Test
    public void lagre_verge() {
        new VergeBuilder()
            .medVergeType(VergeType.BARN)
            .medBruker(vergeBruker)
            .build();
    }

    @Test
    public void skal_generere_historikkinnslag_ved_bekreftet() {
        // Behandling
        var behandling = opprettBehandling();
        AvklarVergeDto dto = opprettDtoVerge();
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        new VergeOppdaterer(historikkTjeneste,
            personinfoAdapter, mock(VergeRepository.class), mock(NavBrukerRepository.class)).oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt.orElse(null), dto));

        // Verifiserer HistorikkinnslagDto
        ArgumentCaptor<Historikkinnslag> historikkCapture = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkTjeneste).lagInnslag(historikkCapture.capture());
        Historikkinnslag historikkinnslag = historikkCapture.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.REGISTRER_OM_VERGE);
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SAKSBEHANDLER);
        HistorikkinnslagDel del = historikkinnslag.getHistorikkinnslagDeler().get(0);
        assertThat(del.getSkjermlenke()).as("skjermlenke").hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.FAKTA_OM_VERGE.getKode()));
        assertThat(del.getHendelse()).as("hendelse").hasValueSatisfying(hendelse -> assertThat(hendelse.getNavn()).as("navn").isEqualTo(HistorikkinnslagType.REGISTRER_OM_VERGE.getKode()));
    }

    private AvklarVergeDto opprettDtoVerge() {
        AvklarVergeDto dto = new AvklarVergeDto();
        dto.setNavn("Navn");
        dto.setFnr("12345678901");
        dto.setGyldigFom(LocalDate.now().minusDays(10));
        dto.setGyldigTom(LocalDate.now().plusDays(10));
        dto.setVergeType(VergeType.BARN);
        return dto;
    }

    private Behandling opprettBehandling() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknad();
        scenario.leggTilAksjonspunkt(AKSJONSPUNKT_DEF, BehandlingStegType.KONTROLLER_FAKTA);
        scenario.lagMocked();

        return scenario.getBehandling();
    }

}

