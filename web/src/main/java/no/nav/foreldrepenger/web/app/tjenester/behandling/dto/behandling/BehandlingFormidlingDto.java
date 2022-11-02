package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.web.app.rest.ResourceLink;

/**
 * Dataobjekt som skal brukes som respons ut mot fp-formidling.
 * Skal populere formidlingRessurser med formidling-spesifikke ressurslenker.
 */
public class BehandlingFormidlingDto extends BehandlingDto {

    /**
     * REST HATEOAS - pekere på resttjenester som skal brukes for å populere brev.
     * Skal erstatte links feltet på BehandlingDto for brev.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty("formidlingRessurser")
    private List<ResourceLink> formidlingRessurser = new ArrayList<>();

    @JsonProperty("harAvklartAnnenForelderRett")
    private Boolean harAvklartAnnenForelderRett;

    public List<ResourceLink> getFormidlingRessurser() {
        return formidlingRessurser;
    }

    public void leggTilFormidlingRessurs(ResourceLink link) {
        this.formidlingRessurser.add(link);
    }

    public void setFormidlingRessurser(List<ResourceLink> formidlingRessurser) {
        this.formidlingRessurser = formidlingRessurser;
    }

    public Boolean getHarAvklartAnnenForelderRett() {
        return harAvklartAnnenForelderRett;
    }

    public void setHarAvklartAnnenForelderRett(Boolean harAvklartAnnenForelderRett) {
        this.harAvklartAnnenForelderRett = harAvklartAnnenForelderRett;
    }
}
