package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "VergeGrunnlag")
@Table(name = "GR_VERGE")
public class VergeGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_VERGE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @OneToOne
    @JoinColumn(name = "verge_id", updatable = false, unique = true)
    private VergeEntitet verge;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public VergeGrunnlagEntitet() {
        // defualt tom entitet
    }

    VergeGrunnlagEntitet(Long behandlingId, VergeEntitet verge) {
        this.behandlingId = behandlingId;
        this.verge = verge;
    }

    VergeEntitet getVerge() {
        return verge;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof VergeGrunnlagEntitet)) {
            return false;
        }

        VergeGrunnlagEntitet that = (VergeGrunnlagEntitet) obj;
        return Objects.equals(this.behandlingId, that.behandlingId)
            && Objects.equals(this.verge, that.verge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.behandlingId, this.verge);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<id=" + this.id //$NON-NLS-1$
            + ", verge=" + this.verge //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
    }

    VergeAggregat tilAggregat() {
        return new VergeAggregat(this.verge);
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandling(Long behandlingId) {
        this.behandlingId = behandlingId;
    }
}
