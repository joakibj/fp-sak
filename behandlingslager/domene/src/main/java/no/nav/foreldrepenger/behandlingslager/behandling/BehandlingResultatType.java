package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingResultatType implements Kodeverdi {

    IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
    INNVILGET("INNVILGET", "Innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    OPPHØR("OPPHØR", "Opphør"),
    HENLAGT_SØKNAD_TRUKKET("HENLAGT_SØKNAD_TRUKKET", "Henlagt, søknaden er trukket"),
    HENLAGT_FEILOPPRETTET("HENLAGT_FEILOPPRETTET", "Henlagt, søknaden er feilopprettet"),
    HENLAGT_BRUKER_DØD("HENLAGT_BRUKER_DØD", "Henlagt, brukeren er død"),
    MERGET_OG_HENLAGT("MERGET_OG_HENLAGT", "Mottatt ny søknad"),
    HENLAGT_SØKNAD_MANGLER("HENLAGT_SØKNAD_MANGLER", "Henlagt søknad mangler"),
    FORELDREPENGER_ENDRET("FORELDREPENGER_ENDRET", "Foreldrepenger er endret"),
    INGEN_ENDRING("INGEN_ENDRING", "Ingen endring"),
    MANGLER_BEREGNINGSREGLER("MANGLER_BEREGNINGSREGLER", "Mangler beregningsregler"),
    
    // Klage
    KLAGE_AVVIST("KLAGE_AVVIST", "Klage er avvist"),
    KLAGE_MEDHOLD("KLAGE_MEDHOLD", "Medhold"),
    KLAGE_YTELSESVEDTAK_OPPHEVET("KLAGE_YTELSESVEDTAK_OPPHEVET", "Ytelsesvedtak opphevet"),
    KLAGE_YTELSESVEDTAK_STADFESTET("KLAGE_YTELSESVEDTAK_STADFESTET", "Ytelsesvedtak stadfestet"),
    HENLAGT_KLAGE_TRUKKET("HENLAGT_KLAGE_TRUKKET", "Henlagt, klagen er trukket"),
    DELVIS_MEDHOLD_I_KLAGE("DELVIS_MEDHOLD_I_KLAGE", "Delvis medhold i klage"),
    HJEMSENDE_UTEN_OPPHEVE("HJEMSENDE_UTEN_OPPHEVE", "Behandlingen er hjemsendt"),
    UGUNST_MEDHOLD_I_KLAGE("UGUNST_MEDHOLD_I_KLAGE", "Ugunst medhold i klage"),
    
    // Anke
    ANKE_AVVIST("ANKE_AVVIST", "Anke er avvist"),
    ANKE_OMGJOER("ANKE_OMGJOER", "Bruker har fått omgjøring i anke"),
    ANKE_OPPHEVE_OG_HJEMSENDE("ANKE_OPPHEVE_OG_HJEMSENDE", "Bruker har fått vedtaket opphevet og hjemsendt i anke"),
    ANKE_YTELSESVEDTAK_STADFESTET("ANKE_YTELSESVEDTAK_STADFESTET", "Anken er stadfestet/opprettholdt"),
    ANKE_DELVIS_OMGJOERING_TIL_GUNST("ANKE_DELVIS_OMGJOERING_TIL_GUNST", "Anke er delvis omgjøring, til gunst"),
    ANKE_TIL_UGUNST("ANKE_TIL_UGUNST", "Gunst omgjør i anke"),
    
    // Innsyn
    INNSYN_INNVILGET("INNSYN_INNVILGET", "Innsynskrav er innvilget"),
    INNSYN_DELVIS_INNVILGET("INNSYN_DELVIS_INNVILGET", "Innsynskrav er delvis innvilget"),
    INNSYN_AVVIST("INNSYN_AVVIST", "Innsynskrav er avvist"),
    HENLAGT_INNSYN_TRUKKET("HENLAGT_INNSYN_TRUKKET", "Henlagt, innsynskrav er trukket"),
    
    ;

    private static final Set<BehandlingResultatType> HENLEGGELSESKODER_FOR_SØKNAD = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, HENLAGT_SØKNAD_MANGLER, MANGLER_BEREGNINGSREGLER)));
    private static final Set<BehandlingResultatType> ALLE_HENLEGGELSESKODER = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(HENLAGT_SØKNAD_TRUKKET, HENLAGT_FEILOPPRETTET, HENLAGT_BRUKER_DØD, HENLAGT_KLAGE_TRUKKET, MERGET_OG_HENLAGT, HENLAGT_SØKNAD_MANGLER, HENLAGT_INNSYN_TRUKKET, MANGLER_BEREGNINGSREGLER)));
    private static final Set<BehandlingResultatType> KLAGE_KODER = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(KLAGE_MEDHOLD, KLAGE_YTELSESVEDTAK_STADFESTET, KLAGE_YTELSESVEDTAK_STADFESTET, KLAGE_AVVIST, DELVIS_MEDHOLD_I_KLAGE, HJEMSENDE_UTEN_OPPHEVE, UGUNST_MEDHOLD_I_KLAGE)));
    private static final Set<BehandlingResultatType> ANKE_KODER = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(ANKE_AVVIST, ANKE_OMGJOER, ANKE_OPPHEVE_OG_HJEMSENDE, ANKE_YTELSESVEDTAK_STADFESTET, ANKE_DELVIS_OMGJOERING_TIL_GUNST, ANKE_TIL_UGUNST)));
    private static final Set<BehandlingResultatType> INNSYN_KODER = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(INNSYN_INNVILGET, INNSYN_DELVIS_INNVILGET, INNSYN_AVVIST)));
    private static final Set<BehandlingResultatType> INNVILGET_KODER = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(INNVILGET, FORELDREPENGER_ENDRET)));

    private static final Map<String, BehandlingResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "BEHANDLING_RESULTAT_TYPE";
    
    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;
    
    private String kode;

    private BehandlingResultatType(String kode) {
        this.kode = kode;
    }

    private BehandlingResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static BehandlingResultatType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingResultatType: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingResultatType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
    
    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }
    
    @Override
    public String getOffisiellKode() {
        return getKode();
    }
    
    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }
    
    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
    
    public static Set<BehandlingResultatType> getAlleHenleggelseskoder() {
        return ALLE_HENLEGGELSESKODER;
    }

    public static Set<BehandlingResultatType> getHenleggelseskoderForSøknad() {
        return HENLEGGELSESKODER_FOR_SØKNAD;
    }

    public static Set<BehandlingResultatType> getKlageKoder() {
        return KLAGE_KODER;
    }

    public static Set<BehandlingResultatType> getAnkeKoder() {
        return ANKE_KODER;
    }

    public static Set<BehandlingResultatType> getInnsynKoder() {
        return INNSYN_KODER;
    }

    public static Set<BehandlingResultatType> getInnvilgetKoder() {
        return INNVILGET_KODER;
    }

    public boolean erHenlagt() {
        return ALLE_HENLEGGELSESKODER.contains(this);
    }

    public static BehandlingResultatType tolkBehandlingResultatType(AnkeVurdering vurdering) {
        switch (vurdering.getKode()) {
            case "ANKE_AVVIS":
                return BehandlingResultatType.ANKE_AVVIST;
            case "ANKE_STADFESTE_YTELSESVEDTAK":
                return BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET;
            case "ANKE_OMGJOER":
                return BehandlingResultatType.ANKE_OMGJOER;
            case "ANKE_OPPHEVE_OG_HJEMSENDE":
                return BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE;
            default:
                return null;
        }
    }

    public static BehandlingResultatType tolkBehandlingResultatType(KlageVurdering vurdering) {
        switch (vurdering.getKode()) {
            case "AVVIS_KLAGE":
                return BehandlingResultatType.KLAGE_AVVIST;
            case "MEDHOLD_I_KLAGE":
                return BehandlingResultatType.KLAGE_MEDHOLD;
            case "OPPHEVE_YTELSESVEDTAK":
                return BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET;
            case "STADFESTE_YTELSESVEDTAK":
                return BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET;
            case "DELVIS_MEDHOLD_I_KLAGE":
                return BehandlingResultatType.DELVIS_MEDHOLD_I_KLAGE;
            case "HJEMSENDE_UTEN_Å_OPPHEVE":
                return BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE;
            case "UGUNST_MEDHOLD_I_KLAGE":
                return BehandlingResultatType.UGUNST_MEDHOLD_I_KLAGE;
            default:
                return null;
        }
    }
    
    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingResultatType, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
