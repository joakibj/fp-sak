package no.nav.foreldrepenger.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class Personinfo {

    private AktørId aktørId;
    private String navn;
    private PersonIdent personIdent;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private PersonstatusType personstatus;
    private NavBrukerKjønn kjønn;
    private Set<Familierelasjon> familierelasjoner = Collections.emptySet();
    private Region region;
    private String diskresjonskode;
    private Språkkode foretrukketSpråk;
    private Landkoder landkode;

    private List<Adresseinfo> adresseInfoList = new ArrayList<>();
    private SivilstandType sivilstand;

    private Personinfo() {
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public String getNavn() {
        return navn;
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public int getAlder() {
        return (int) ChronoUnit.YEARS.between(fødselsdato, LocalDate.now());
    }

    public Set<Familierelasjon> getFamilierelasjoner() {
        return Collections.unmodifiableSet(familierelasjoner);
    }

    public boolean erKvinne() {
        return kjønn.equals(NavBrukerKjønn.KVINNE);
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public Region getRegion() {
        return region;
    }

    public Språkkode getForetrukketSpråk() {
        return foretrukketSpråk;
    }

    public String getDiskresjonskode() {
        return diskresjonskode;
    }

    public List<Adresseinfo> getAdresseInfoList() {
        return adresseInfoList;
    }

    public SivilstandType getSivilstandType() {
        return sivilstand;
    }

    public Landkoder getLandkode() {
        return landkode;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<aktørId=" + aktørId + ">"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static class Builder {
        private Personinfo personinfoMal;

        public Builder() {
            personinfoMal = new Personinfo();
        }

        public Builder medAktørId(AktørId aktørId) {
            personinfoMal.aktørId = aktørId;
            return this;
        }

        public Builder medNavn(String navn) {
            personinfoMal.navn = navn;
            return this;
        }

        public Builder medPersonIdent(PersonIdent fnr) {
            personinfoMal.personIdent = fnr;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            personinfoMal.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            personinfoMal.dødsdato = dødsdato;
            return this;
        }

        public Builder medPersonstatusType(PersonstatusType personstatus) {
            personinfoMal.personstatus = personstatus;
            return this;
        }

        public Builder medNavBrukerKjønn(NavBrukerKjønn kjønn) {
            personinfoMal.kjønn = kjønn;
            return this;
        }

        public Builder medFamilierelasjon(Set<Familierelasjon> familierelasjon) {
            personinfoMal.familierelasjoner = familierelasjon;
            return this;
        }

        public Builder medRegion(Region region) {
            personinfoMal.region = region;
            return this;
        }

        public Builder medDiskresjonsKode(String diskresjonsKode) {
            personinfoMal.diskresjonskode = diskresjonsKode;
            return this;
        }

        public Builder medForetrukketSpråk(Språkkode språk) {
            personinfoMal.foretrukketSpråk = språk;
            return this;
        }

        public Builder medAdresseInfoList(List<Adresseinfo> adresseinfoArrayList) {
            personinfoMal.adresseInfoList = adresseinfoArrayList;
            return this;
        }

        public Builder medSivilstandType(SivilstandType sivilstandType) {
            personinfoMal.sivilstand = sivilstandType;
            return this;
        }

        public Builder medLandkode(Landkoder landkode) {
            personinfoMal.landkode = landkode;
            return this;
        }

        public Personinfo build() {
            requireNonNull(personinfoMal.aktørId, "Navbruker må ha aktørId"); //$NON-NLS-1$
            requireNonNull(personinfoMal.personIdent, "Navbruker må ha fødselsnummer"); //$NON-NLS-1$
            requireNonNull(personinfoMal.navn, "Navbruker må ha navn"); //$NON-NLS-1$
            requireNonNull(personinfoMal.fødselsdato, "Navbruker må ha fødselsdato"); //$NON-NLS-1$
            requireNonNull(personinfoMal.kjønn, "Navbruker må ha kjønn"); //$NON-NLS-1$
            return personinfoMal;
        }

    }

}
