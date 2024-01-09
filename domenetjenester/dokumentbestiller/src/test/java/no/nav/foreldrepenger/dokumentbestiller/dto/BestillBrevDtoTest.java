package no.nav.foreldrepenger.dokumentbestiller.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class BestillBrevDtoTest {

    @Test
    void serdes() {
        var uuid = "5ffbf59b-76d5-4c78-bd27-6c84e0d445a3";
        var behandlingUuid = UUID.fromString(uuid);
        var dokumentMal = DokumentMalType.FRITEKSTBREV;
        var arsak = RevurderingVarslingÅrsak.BARN_IKKE_REGISTRERT_FOLKEREGISTER;

        var brev = new BestillBrevDto(behandlingUuid, dokumentMal, null, arsak);

        var json = DefaultJsonMapper.toJson(brev);

        var etterRoundtrip = DefaultJsonMapper.fromJson(json, BestillBrevDto.class);

        assertEquals(brev.getBrevmalkode(), etterRoundtrip.getBrevmalkode());
        assertEquals(dokumentMal, brev.getBrevmalkode());
        assertEquals(brev.getArsakskode(), etterRoundtrip.getArsakskode());
        assertEquals(arsak, etterRoundtrip.getArsakskode());
        assertEquals(brev.getFritekst(), etterRoundtrip.getFritekst());
        assertNull(etterRoundtrip.getFritekst());
        assertEquals(brev.getBehandlingUuid(), etterRoundtrip.getBehandlingUuid());
        assertEquals(uuid, etterRoundtrip.getBehandlingUuid().toString());
    }

}
