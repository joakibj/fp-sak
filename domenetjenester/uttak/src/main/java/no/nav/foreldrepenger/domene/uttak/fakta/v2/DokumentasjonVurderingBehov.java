package no.nav.foreldrepenger.domene.uttak.fakta.v2;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;

public record DokumentasjonVurderingBehov(OppgittPeriodeEntitet oppgittPeriode, Behov behov, DokumentasjonVurdering vurdering) {

    public boolean måVurderes() {
        return behov != null && vurdering == null;
    }

    public record Behov(Behov.Type type, Behov.Årsak årsak) {
        public enum Type {
            UTSETTELSE,
            OVERFØRING,
            UTTAK,
        }

        public enum UtsettelseÅrsak implements Årsak {
            INNLEGGELSE_SØKER,
            INNLEGGELSE_BARN,
            HV_ØVELSE,
            NAV_TILTAK,
            SYKDOM_SØKER,
        }

        public enum OverføringÅrsak implements Årsak {
            INNLEGGELSE_ANNEN_FORELDER,
            SYKDOM_ANNEN_FORELDER,
            BARE_SØKER_RETT,
            ALENEOMSORG,
        }

        public enum UttakÅrsak implements Årsak {
            TIDLIG_OPPSTART_FAR,
            AKTIVITETSKRAV_ARBEID,
            AKTIVITETSKRAV_UTDANNING,
            AKTIVITETSKRAV_KVALPROG,
            AKTIVITETSKRAV_INTROPROG,
            AKTIVITETSKRAV_TRENGER_HJELP,
            AKTIVITETSKRAV_INNLAGT,
            AKTIVITETSKRAV_ARBEID_OG_UTDANNING,
            AKTIVITETSKRAV_IKKE_OPPGITT,
        }

        public interface Årsak {}
    }
}
