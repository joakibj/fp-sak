package no.nav.foreldrepenger.domene.opptjening.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

public class OpptjeningAktivitetDto {

    private OpptjeningAktivitetType aktivitetType;
    private LocalDate originalFom;
    private LocalDate originalTom;
    private LocalDate opptjeningFom;
    private LocalDate opptjeningTom;
    private String arbeidsforholdRef;
    private BigDecimal stillingsandel;
    private LocalDate naringRegistreringsdato;
    private Boolean erManueltOpprettet;
    private Boolean erGodkjent;
    private Boolean erEndret;
    private String begrunnelse;
    private Boolean erPeriodeEndret;
    private String arbeidsgiver;  // Et navn
    private String arbeidsgiverReferanse;
    private String arbeidsgiverNavn;
    private String oppdragsgiverOrg;
    private String arbeidsgiverIdentifikator;
    private String privatpersonNavn;
    private LocalDate privatpersonFødselsdato;
    private String utlandskArbeidsgiverNavn;

    public OpptjeningAktivitetDto() {//NOSONAR
        // trengs for deserialisering av JSON
    }

    OpptjeningAktivitetDto(OpptjeningAktivitetType aktivitetType, LocalDate opptjeningFom,
                           LocalDate opptjeningTom) {
        this.aktivitetType = aktivitetType;
        this.opptjeningFom = opptjeningFom;
        this.opptjeningTom = opptjeningTom;
    }

    public LocalDate getOriginalFom() {
        return originalFom;
    }

    public void setOriginalFom(LocalDate originalFom) {
        this.originalFom = originalFom;
    }

    public LocalDate getOriginalTom() {
        return originalTom;
    }

    public void setOriginalTom(LocalDate originalTom) {
        this.originalTom = originalTom;
    }

    public OpptjeningAktivitetType getAktivitetType() {
        return aktivitetType;
    }

    public void setAktivitetType(OpptjeningAktivitetType aktivitetType) {
        this.aktivitetType = aktivitetType;
    }

    public LocalDate getOpptjeningFom() {
        return opptjeningFom;
    }

    public void setOpptjeningFom(LocalDate opptjeningFom) {
        this.opptjeningFom = opptjeningFom;
    }

    public LocalDate getOpptjeningTom() {
        return opptjeningTom;
    }

    public void setOpptjeningTom(LocalDate opptjeningTom) {
        this.opptjeningTom = opptjeningTom;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public String getOppdragsgiverOrg() {
        return oppdragsgiverOrg;
    }

    public void setOppdragsgiverOrg(String oppdragsgiverOrg) {
        this.oppdragsgiverOrg = oppdragsgiverOrg;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public void setArbeidsgiverNavn(String arbeidsgiverNavn) {
        this.arbeidsgiverNavn = arbeidsgiverNavn;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public void setArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
        this.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
    }

    public LocalDate getNaringRegistreringsdato() {
        return naringRegistreringsdato;
    }

    public void setNaringRegistreringsdato(LocalDate naringRegistreringsdato) {
        this.naringRegistreringsdato = naringRegistreringsdato;
    }

    public BigDecimal getStillingsandel() {
        return stillingsandel;
    }

    public void setStillingsandel(BigDecimal stillingsandel) {
        this.stillingsandel = stillingsandel;
    }

    public Boolean getErGodkjent() {
        return erGodkjent;
    }

    public void setErGodkjent(Boolean erGodkjent) {
        this.erGodkjent = erGodkjent;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Boolean getErManueltOpprettet() {
        return erManueltOpprettet;
    }

    public void setErManueltOpprettet(Boolean erManueltOpprettet) {
        this.erManueltOpprettet = erManueltOpprettet;
    }

    public Boolean getErEndret() {
        return erEndret;
    }

    public void setErEndret(Boolean erEndret) {
        this.erEndret = erEndret;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public void setArbeidsforholdRef(String arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public void setErPeriodeEndret(Boolean erPeriodeEndret) {
        this.erPeriodeEndret = erPeriodeEndret;
    }

    public Boolean getErPeriodeEndret() {
        return erPeriodeEndret;
    }

    public String getPrivatpersonNavn() {
        return privatpersonNavn;
    }

    public void setPrivatpersonNavn(String privatpersonNavn) {
        this.privatpersonNavn = privatpersonNavn;
    }

    public LocalDate getPrivatpersonFødselsdato() {
        return privatpersonFødselsdato;
    }

    public void setPrivatpersonFødselsdato(LocalDate privatpersonFødselsdato) {
        this.privatpersonFødselsdato = privatpersonFødselsdato;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    public String getUtlandskArbeidsgiverNavn() {
        return utlandskArbeidsgiverNavn;
    }

    public void setUtlandskArbeidsgiverNavn(String utlandskArbeidsgiverNavn) {
        this.utlandskArbeidsgiverNavn = utlandskArbeidsgiverNavn;
    }
}
