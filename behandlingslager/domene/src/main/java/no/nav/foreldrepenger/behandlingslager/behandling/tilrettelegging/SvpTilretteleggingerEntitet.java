package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity
@Table(name = "SVP_TILRETTELEGGINGER")
public class SvpTilretteleggingerEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SVP_TILRETTELEGGINGER")
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "tilrettelegginger")
    @BatchSize(size = 25)
    private List<SvpTilretteleggingEntitet> tilretteleggingListe = new ArrayList<>();

    public List<SvpTilretteleggingEntitet> getTilretteleggingListe() {
        return Collections.unmodifiableList(tilretteleggingListe);
    }

    public static class Builder {

        private List<SvpTilretteleggingEntitet> tilretteleggingListe = new ArrayList<>();

        public SvpTilretteleggingerEntitet build() {
            SvpTilretteleggingerEntitet entitet = new SvpTilretteleggingerEntitet();
            for (SvpTilretteleggingEntitet tilrettelegging : this.tilretteleggingListe) {
                SvpTilretteleggingEntitet svpTilretteleggingEntitet = new SvpTilretteleggingEntitet(tilrettelegging, entitet);
                entitet.tilretteleggingListe.add(svpTilretteleggingEntitet);
            }
            return entitet;
        }

        public Builder medTilretteleggingListe(List<SvpTilretteleggingEntitet> tilretteleggingListe) {
            this.tilretteleggingListe = tilretteleggingListe;
            return this;
        }
    }
}
