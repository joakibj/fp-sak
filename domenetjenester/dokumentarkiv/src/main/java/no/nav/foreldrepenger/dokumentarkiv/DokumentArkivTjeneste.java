package no.nav.foreldrepenger.dokumentarkiv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeUgyldigInput;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.DokumenttypeIder;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumer;

@ApplicationScoped
public class DokumentArkivTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);
    private JournalConsumer journalConsumer;

    private FagsakRepository fagsakRepository;

    private final Set<ArkivFilType> filTyperPdf = byggArkivFilTypeSet();
    private final VariantFormat variantFormatArkiv = VariantFormat.ARKIV;


    DokumentArkivTjeneste() {
        // for CDI proxy
    }

    @Inject
    public DokumentArkivTjeneste(JournalConsumer journalConsumer, FagsakRepository fagsakRepository) {
        this.journalConsumer = journalConsumer;
        this.fagsakRepository = fagsakRepository;
    }

    public byte[] hentDokument(JournalpostId journalpostId, String dokumentId) {
        LOG.info("HentDokument: input parametere journalpostId {} dokumentId {}", journalpostId, dokumentId);
        byte[] pdfFile = new byte[0];
        HentDokumentRequest hentDokumentRequest = new HentDokumentRequest();
        hentDokumentRequest.setJournalpostId(journalpostId.getVerdi());
        hentDokumentRequest.setDokumentId(dokumentId);
        Variantformater variantFormat = new Variantformater();
        variantFormat.setValue(variantFormatArkiv.getOffisiellKode());
        hentDokumentRequest.setVariantformat(variantFormat);

        try {
            HentDokumentResponse hentDokumentResponse = journalConsumer.hentDokument(hentDokumentRequest);
            if (hentDokumentResponse != null && hentDokumentResponse.getDokument() != null) {
                pdfFile = hentDokumentResponse.getDokument();
            }
        } catch (HentDokumentDokumentIkkeFunnet e) {
            throw DokumentArkivTjenesteFeil.FACTORY.hentDokumentIkkeFunnet(e).toException();
        } catch (HentDokumentJournalpostIkkeFunnet e) {
            throw DokumentArkivTjenesteFeil.FACTORY.hentJournalpostIkkeFunnet(e).toException();
        } catch (HentDokumentSikkerhetsbegrensning e) {
            throw DokumentArkivTjenesteFeil.FACTORY.journalUtilgjengeligSikkerhetsbegrensning("hent dokument", e).toException();
        }
        return pdfFile;
    }

    public List<ArkivJournalPost> hentAlleDokumenterForVisning(Saksnummer saksnummer) {
        List<ArkivJournalPost> journalPosterForSak = hentAlleJournalposterForSak(saksnummer);

        List<ArkivJournalPost> journalPosts = new ArrayList<>();

        journalPosterForSak.forEach(jpost -> {
            if (!erDokumentArkivPdf(jpost.getHovedDokument())) {
                jpost.setHovedDokument(null);
            }

            jpost.getAndreDokument().forEach(dok -> {
                if (!erDokumentArkivPdf(jpost.getHovedDokument())) {
                    jpost.getAndreDokument().remove(dok);
                }
            });
        });
        journalPosterForSak.stream()
            .filter(jpost -> jpost.getHovedDokument() != null || !jpost.getAndreDokument().isEmpty())
            .forEach(journalPosts::add);

        return journalPosts;
    }

    private boolean erDokumentArkivPdf(ArkivDokument arkivDokument) {
        for (ArkivDokumentHentbart format : arkivDokument.getTilgjengeligSom()) {
            if (variantFormatArkiv.equals(format.getVariantFormat()) && filTyperPdf.contains(format.getArkivFilType())) {
                return true;
            }
        }
        return false;
    }

    public List<ArkivJournalPost> hentAlleJournalposterForSak(Saksnummer saksnummer) {
        List<ArkivJournalPost> journalPosts = new ArrayList<>();
        doHentKjerneJournalpostListe(saksnummer)
            .map(HentKjerneJournalpostListeResponse::getJournalpostListe).orElse(new ArrayList<>())
            .stream()
            .filter(journalpost -> !Journaltilstand.UTGAAR.equals(journalpost.getJournaltilstand()))
            .forEach(journalpost -> {
                ArkivJournalPost.Builder arkivJournalPost = opprettArkivJournalPost(saksnummer, journalpost);
                journalPosts.add(arkivJournalPost.build());
            });

        return journalPosts;
    }

    public Optional<ArkivJournalPost> hentJournalpostForSak(Saksnummer saksnummer, JournalpostId journalpostId) {
        return doHentKjerneJournalpostListe(saksnummer)
            .map(HentKjerneJournalpostListeResponse::getJournalpostListe).orElse(new ArrayList<>())
            .stream()
            .filter(journalpost -> journalpostId.getVerdi().equals(journalpost.getJournalpostId()))
            .findFirst()
            .map(journalpost -> opprettArkivJournalPost(saksnummer, journalpost).build());
    }

    public Set<DokumentTypeId> hentDokumentTypeIdForSak(Saksnummer saksnummer, LocalDate mottattEtterDato) {
        List<ArkivJournalPost> journalPosts = hentAlleJournalposterForSak(saksnummer).stream()
            .filter(ajp -> Kommunikasjonsretning.INN.equals(ajp.getKommunikasjonsretning()))
            .collect(Collectors.toList());
        Set<DokumentTypeId> alleDTID = new HashSet<>();
        if (LocalDate.MIN.equals(mottattEtterDato)) {
            journalPosts.forEach(jpost -> ekstraherJournalpostDTID(alleDTID, jpost));
        } else {
            journalPosts.stream()
                .filter(jpost -> jpost.getTidspunkt() != null && jpost.getTidspunkt().isAfter(mottattEtterDato.atStartOfDay()))
                .forEach(jpost -> ekstraherJournalpostDTID(alleDTID, jpost));
        }
        return alleDTID;
    }

    private void ekstraherJournalpostDTID(Set<DokumentTypeId> alleDTID, ArkivJournalPost jpost) {
        dokumentTypeFraTittel(jpost.getBeskrivelse()).ifPresent(alleDTID::add);
        ekstraherDokumentDTID(alleDTID, jpost.getHovedDokument());
        jpost.getAndreDokument().forEach(dok -> ekstraherDokumentDTID(alleDTID, dok));
    }

    private void ekstraherDokumentDTID(Set<DokumentTypeId> eksisterende, ArkivDokument dokument) {
        if (dokument == null) {
            return;
        }
        eksisterende.add(dokument.getDokumentType());
        dokumentTypeFraTittel(dokument.getTittel()).ifPresent(eksisterende::add);
        for (ArkivDokumentVedlegg vedlegg : dokument.getInterneVedlegg()) {
            eksisterende.add(vedlegg.getDokumentTypeId());
            dokumentTypeFraTittel(dokument.getTittel()).ifPresent(eksisterende::add);
        }
    }

    private Optional<HentKjerneJournalpostListeResponse> doHentKjerneJournalpostListe(Saksnummer saksnummer) {
        final Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isEmpty()) {
            return Optional.empty();
        }
        HentKjerneJournalpostListeRequest hentKjerneJournalpostListeRequest = new HentKjerneJournalpostListeRequest();

        hentKjerneJournalpostListeRequest.getArkivSakListe().add(lageJournalSak(saksnummer, Fagsystem.GOSYS.getOffisiellKode()));

        try {
            HentKjerneJournalpostListeResponse hentKjerneJournalpostListeResponse = journalConsumer
                .hentKjerneJournalpostListe(hentKjerneJournalpostListeRequest);
            return Optional.of(hentKjerneJournalpostListeResponse);
        } catch (HentKjerneJournalpostListeSikkerhetsbegrensning e) {
            throw DokumentArkivTjenesteFeil.FACTORY.journalUtilgjengeligSikkerhetsbegrensning("hent journalpostliste", e).toException();
        } catch (HentKjerneJournalpostListeUgyldigInput e) {
            throw DokumentArkivTjenesteFeil.FACTORY.journalpostUgyldigInput(e).toException();
        }
    }

    private static Set<ArkivFilType> byggArkivFilTypeSet() {
        final ArkivFilType arkivFilTypePdf = ArkivFilType.PDF;
        final ArkivFilType arkivFilTypePdfa = ArkivFilType.PDFA;
        return new HashSet<>(Arrays.asList(arkivFilTypePdf, arkivFilTypePdfa));
    }

    private ArkivSak lageJournalSak(Saksnummer saksnummer, String fagsystem) {
        ArkivSak journalSak = new ArkivSak();
        journalSak.setArkivSakSystem(fagsystem);
        journalSak.setArkivSakId(saksnummer.getVerdi());
        journalSak.setErFeilregistrert(false);
        return journalSak;
    }

    private ArkivJournalPost.Builder opprettArkivJournalPost(Saksnummer saksnummer, Journalpost journalpost) {
        LocalDateTime tidspunkt = journalpost.getForsendelseJournalfoert() != null ? DateUtil.convertToLocalDateTime(journalpost.getForsendelseJournalfoert())
            : DateUtil.convertToLocalDateTime(journalpost.getForsendelseMottatt());

        ArkivJournalPost.Builder builder = ArkivJournalPost.Builder.ny()
            .medSaksnummer(saksnummer)
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medBeskrivelse(journalpost.getInnhold())
            .medTidspunkt(tidspunkt)
            .medKommunikasjonsretning(Kommunikasjonsretning.fromKommunikasjonsretningCode(journalpost.getJournalposttype().getValue()))
            .medHoveddokument(opprettArkivDokument(journalpost.getHoveddokument()).build());
        journalpost.getVedleggListe().forEach(vedlegg -> builder.leggTillVedlegg(opprettArkivDokument(vedlegg).build()));
        return builder;
    }

    private ArkivDokument.Builder opprettArkivDokument(DetaljertDokumentinformasjon detaljertDokumentinformasjon) {
        ArkivDokument.Builder builder = ArkivDokument.Builder.ny()
            .medDokumentId(detaljertDokumentinformasjon.getDokumentId())
            .medTittel(detaljertDokumentinformasjon.getTittel())
            .medDokumentTypeId(utledDokumentType(detaljertDokumentinformasjon.getDokumentTypeId(), detaljertDokumentinformasjon.getTittel()));
        detaljertDokumentinformasjon.getSkannetInnholdListe().forEach(vedlegg -> {
            builder.leggTilInterntVedlegg(ArkivDokumentVedlegg.Builder.ny()
                .medTittel(vedlegg.getVedleggInnhold())
                .medDokumentTypeId(utledDokumentType(vedlegg.getDokumenttypeId(), vedlegg.getVedleggInnhold()))
                .build());
        });
        detaljertDokumentinformasjon.getDokumentInnholdListe().forEach(innhold -> {
            builder.leggTilTilgjengeligFormat(ArkivDokumentHentbart.Builder.ny()
                .medArkivFilType(
                    innhold.getArkivfiltype() != null ? ArkivFilType.finnForKodeverkEiersKode(innhold.getArkivfiltype().getValue()) : ArkivFilType.UDEFINERT)
                .medVariantFormat(innhold.getVariantformat() != null ? VariantFormat.finnForKodeverkEiersKode(innhold.getVariantformat().getValue())
                    : VariantFormat.UDEFINERT)
                .build());
        });
        return builder;
    }

    private DokumentTypeId utledDokumentType(DokumenttypeIder dokumenttypeIder, String tittel) {
        var fraDokumenttype = dokumenttypeIder != null ? DokumentTypeId.finnForKodeverkEiersKode(dokumenttypeIder.getValue()) : DokumentTypeId.UDEFINERT;
        if (DokumentTypeId.UDEFINERT.equals(fraDokumenttype) || DokumentTypeId.ANNET.equals(fraDokumenttype)) {
            return dokumentTypeFraTittel(tittel).orElse(fraDokumenttype);
        }
        return fraDokumenttype;
    }

    private Optional<DokumentTypeId> dokumentTypeFraTittel(String tittel) {
        if (tittel == null)
            return Optional.empty();
        return Optional.of(DokumentTypeId.finnForKodeverkEiersNavn(tittel));
    }
}
