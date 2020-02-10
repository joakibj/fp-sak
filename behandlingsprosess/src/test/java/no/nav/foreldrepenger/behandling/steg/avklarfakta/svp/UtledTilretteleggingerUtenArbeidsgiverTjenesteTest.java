package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class UtledTilretteleggingerUtenArbeidsgiverTjenesteTest {

    @Test
    public void skal_filtrer_bort_tilrettelegginger_med_arbeidsgiver(){

        // Arrange
        var tilrettelegging_1 = new SvpTilretteleggingEntitet.Builder().medArbeidsgiver(Arbeidsgiver.virksomhet("123")).build();
        var tilrettelegging_2 = new SvpTilretteleggingEntitet.Builder().medArbeidsgiver(null).build();
        var tilrettelegging_3 = new SvpTilretteleggingEntitet.Builder().medArbeidsgiver(null).build();
        var tilrettelegging_4 = new SvpTilretteleggingEntitet.Builder().medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy())).build();
        var tilrettelegginger = List.of(tilrettelegging_1, tilrettelegging_2, tilrettelegging_3, tilrettelegging_4);

        // Act
        var result = UtledNyeTilretteleggingerTjeneste.utled(tilrettelegginger);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getArbeidsgiver()).isEmpty();
        assertThat(result.get(1).getArbeidsgiver()).isEmpty();

    }

}
