package no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger;

import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "PleiepengerGrunnlag")
@Table(name = "GR_PLEIEPENGER")
public class PleiepengerGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_PLEIEPENGER")
    private Long id;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "psb_perioder_id", updatable = false)
    private PleiepengerPerioderEntitet perioderMedInnleggelse;

    PleiepengerGrunnlagEntitet() {
    }

    PleiepengerGrunnlagEntitet(PleiepengerGrunnlagEntitet behandlingsgrunnlag) {
        behandlingsgrunnlag.getPerioderMedInnleggelse().ifPresent(perioder -> this.perioderMedInnleggelse = perioder);
    }

    Long getId() {
        return id;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Optional<PleiepengerPerioderEntitet> getPerioderMedInnleggelse() {
        return Optional.ofNullable(perioderMedInnleggelse);
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PleiepengerGrunnlagEntitet that = (PleiepengerGrunnlagEntitet) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
            Objects.equals(perioderMedInnleggelse, that.perioderMedInnleggelse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, perioderMedInnleggelse);
    }

    public static class Builder {

        private PleiepengerGrunnlagEntitet kladd;

        private Builder(PleiepengerGrunnlagEntitet kladd) {
            this.kladd = kladd;
        }

        private static Builder nytt() {
            return new Builder(new PleiepengerGrunnlagEntitet());
        }

        private static Builder oppdatere(PleiepengerGrunnlagEntitet kladd) {
            return new Builder(new PleiepengerGrunnlagEntitet(kladd));
        }

        public static Builder oppdatere(Optional<PleiepengerGrunnlagEntitet> kladd) {
            return kladd.map(Builder::oppdatere).orElseGet(Builder::nytt);
        }

        public Builder medInnleggelsePerioder(PleiepengerPerioderEntitet.Builder perioder) {
            this.kladd.perioderMedInnleggelse = perioder.build();
            return this;
        }

        public PleiepengerGrunnlagEntitet build() {
            return this.kladd;
        }
    }
}