package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

public class FødselsVilkårDocTest {

    @Test
    public void test_documentation() {
        var vilkår = new FødselsvilkårMor().getSpecification();
        var digraph = new RuleDescriptionDigraph(vilkår.ruleDescription());

        @SuppressWarnings("unused") var json = digraph.toJson();

//        System.out.println(json);
    }

    private static final String gammelJson = """
        {
            "soekersKjonn" : "KVINNE",
            "bekreftetFoedselsdato" : null,
            "antallBarn" : 1,
            "bekreftetTermindato" : "2021-05-20",
            "soekerRolle" : null,
            "dagensdato" : "2021-04-22",
            "erMorForSykVedFødsel" : false,
            "erSøktOmTermin" : true,
            "erTerminBekreftelseUtstedtEtterXUker" : true
        }
        """;

    @Test
    public void kanDeserialisereGammeltFormat() throws JsonProcessingException {
        var gsource = new FødselsvilkårGrunnlag(RegelKjønn.KVINNE, null, LocalDate.of(2021,4,22),
            null, LocalDate.of(2021,5,20), 1,
            false, false, true,
            false, true);
        var grunnlag = deserialiser(gammelJson);
        assertThat(grunnlag).isEqualTo(gsource);
    }

    @Test
    public void kanSerialisereDeserialisereNyttFormat() throws JsonProcessingException {
        var gsource = new FødselsvilkårGrunnlag(RegelKjønn.MANN, RegelSøkerRolle.FARA, LocalDate.now().minusWeeks(1),
            null, LocalDate.now().plusMonths(1), 1,
            false, false, true,
            true, true);
        var serialisert = DefaultJsonMapper.toJson(gsource);
        var grunnlag = deserialiser(serialisert);
        assertThat(grunnlag).isEqualTo(gsource);
    }

    private FødselsvilkårGrunnlag deserialiser(String s) throws JsonProcessingException {
        return DefaultJsonMapper.getObjectMapper().readValue(s, FødselsvilkårGrunnlag.class);
    }
}
