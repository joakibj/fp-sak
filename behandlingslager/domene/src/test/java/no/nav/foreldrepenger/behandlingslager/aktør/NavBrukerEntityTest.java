package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.time.Month.JANUARY;
import static no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn.KVINNE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class NavBrukerEntityTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Test
    public void skal_lagre_og_hente_søker() {
        Repository repository = repoRule.getRepository();

        Assertions.assertThat(repository.hentAlle(NavBruker.class)).isEmpty();

        AktørId aktørId = AktørId.dummy();
        NavBruker søker = NavBruker.opprettNy(
            new Personinfo.Builder()
                .medAktørId(aktørId)
                .medPersonIdent(new PersonIdent("12345678901"))
                .medNavn("Kari Nordmann")
                .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                .medNavBrukerKjønn(KVINNE)
                .medForetrukketSpråk(Språkkode.NB)
                .build());

        repository.lagre(søker);
        repository.flush();

        List<NavBruker> søkere = repository.hentAlle(NavBruker.class);
        assertThat(søkere).hasSize(1);
        assertThat(søkere.get(0).getAktørId()).isEqualTo(aktørId);
    }
}
