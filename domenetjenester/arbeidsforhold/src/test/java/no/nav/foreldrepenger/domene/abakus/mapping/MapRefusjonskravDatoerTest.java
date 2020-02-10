package no.nav.foreldrepenger.domene.abakus.mapping;


import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoerDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;

public class MapRefusjonskravDatoerTest {

    @Test
    public void skal_mappe_refusjonskravdato() {
        // Arrange
        LocalDate førsteInnsendingAvRefusjonskrav = LocalDate.now().minusDays(10);
        LocalDate førsteDagMedRefusjonskrav = LocalDate.now().minusDays(20);
        String orgnr = KUNSTIG_ORG;
        RefusjonskravDatoerDto dto = new RefusjonskravDatoerDto(List.of(new RefusjonskravDatoDto(new Organisasjon(orgnr),
            førsteInnsendingAvRefusjonskrav, førsteDagMedRefusjonskrav)));

        // Act
        List<RefusjonskravDato> refusjonskravDatoer = MapRefusjonskravDatoer.map(dto);

        // Assert
        assertThat(refusjonskravDatoer.size()).isEqualTo(1);
        assertThat(refusjonskravDatoer.get(0).getArbeidsgiver()).isEqualTo(Arbeidsgiver.virksomhet(orgnr));
        assertThat(refusjonskravDatoer.get(0).getFørsteDagMedRefusjonskrav().get()).isEqualTo(førsteDagMedRefusjonskrav);
        assertThat(refusjonskravDatoer.get(0).getFørsteInnsendingAvRefusjonskrav()).isEqualTo(førsteInnsendingAvRefusjonskrav);
    }
}
