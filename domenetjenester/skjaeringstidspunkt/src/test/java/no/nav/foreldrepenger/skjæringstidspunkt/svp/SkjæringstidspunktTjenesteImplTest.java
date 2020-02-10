package no.nav.foreldrepenger.skjæringstidspunkt.svp;


import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SkjæringstidspunktTjenesteImplTest {

    @Test
    public void skal_utlede_skjæringstidspunktet() {
        LocalDate forventetResultat = LocalDate.of(2019, 7, 10);

        SkjæringstidspunktTjenesteImpl tjeneste = new SkjæringstidspunktTjenesteImpl();
        SvpGrunnlagEntitet.Builder svpGrunnlagEntitet = new SvpGrunnlagEntitet.Builder();
        SvpTilretteleggingEntitet.Builder svp = new SvpTilretteleggingEntitet.Builder();
        svp.medBehovForTilretteleggingFom(forventetResultat);
        svp.medDelvisTilrettelegging(forventetResultat, BigDecimal.valueOf(50));
        svp.medDelvisTilrettelegging(LocalDate.of(2019, 9, 17), BigDecimal.valueOf(30));
        svp.medHelTilrettelegging(LocalDate.of(2019, 11, 1));
        svp.medIngenTilrettelegging(LocalDate.of(2019, 11, 25));

        SvpTilretteleggingEntitet tilretteleggingEntitet = svp.build();
        svpGrunnlagEntitet.medOpprinneligeTilrettelegginger(List.of(tilretteleggingEntitet));
        svpGrunnlagEntitet.medBehandlingId(1337L);

        LocalDate dag = tjeneste.utledBasertPåGrunnlag(svpGrunnlagEntitet.build());

        assertThat(dag).isEqualTo(forventetResultat);
    }
}
