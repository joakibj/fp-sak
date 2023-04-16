package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.event.BehandlingEnhetEventPubliserer;

@ApplicationScoped
public class BehandlendeEnhetTjeneste {

    private EnhetsTjeneste enhetsTjeneste;
    private BehandlingEnhetEventPubliserer eventPubliserer;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private PersonopplysningRepository personopplysningRepository;
    private HistorikkRepository historikkRepository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    public BehandlendeEnhetTjeneste() {
        // For CDI
    }

    @Inject
    public BehandlendeEnhetTjeneste(EnhetsTjeneste enhetsTjeneste,
                                    BehandlingEnhetEventPubliserer eventPubliserer,
                                    BehandlingRepositoryProvider provider,
                                    FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.enhetsTjeneste = enhetsTjeneste;
        this.eventPubliserer = eventPubliserer;
        this.personopplysningRepository = provider.getPersonopplysningRepository();
        this.fagsakRelasjonRepository = provider.getFagsakRelasjonRepository();
        this.fagsakRepository = provider.getFagsakRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.historikkRepository = provider.getHistorikkRepository();
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    // Alle aktuelle enheter
    public static List<OrganisasjonsEnhet> hentEnhetListe() {
        return EnhetsTjeneste.hentEnhetListe();
    }

    // Brukes ved opprettelse av alle typer behandlinger og oppgaver
    public OrganisasjonsEnhet finnBehandlendeEnhetFor(Fagsak fagsak) {
        var enhet = finnEnhetFor(fagsak);
        return sjekkMotKobletSak(fagsak, enhet);
    }

    public OrganisasjonsEnhet finnBehandlendeEnhetFor(Long fagsakId, String enhetId) {
        return Optional.ofNullable(enhetId).map(e -> EnhetsTjeneste.velgEnhet(e, finnSaksmerking(fagsakId)))
            .orElseGet(() -> finnBehandlendeEnhetFor(fagsakRepository.finnEksaktFagsak(fagsakId)));
    }

    public OrganisasjonsEnhet finnBehandlendeEnhetFra(Behandling behandling) {
        return Optional.ofNullable(EnhetsTjeneste.velgEnhet(behandling.getBehandlendeEnhet(), finnSaksmerking(behandling.getFagsakId())))
            .orElseGet(() -> finnBehandlendeEnhetFor(behandling.getFagsak()));
    }

    public OrganisasjonsEnhet finnBehandlendeEnhetForUkoblet(Fagsak fagsak, String sisteBrukt) {
        return Optional.ofNullable(sisteBrukt).map(e -> EnhetsTjeneste.velgEnhet(e, finnSaksmerking(fagsak.getId())))
            .orElseGet(() -> enhetsTjeneste.hentEnhetSjekkKunAktør(fagsak.getAktørId(), BehandlingTema.fraFagsak(fagsak, null)));
    }

    private OrganisasjonsEnhet finnEnhetFor(Fagsak fagsak) {
        var merking = finnSaksmerking(fagsak);
        var enhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .map(Behandling::getBehandlendeOrganisasjonsEnhet)
            .map(e -> EnhetsTjeneste.velgEnhet(e.enhetId(), merking))
            .orElseGet(() -> enhetsTjeneste.hentEnhetSjekkKunAktør(fagsak.getAktørId(), BehandlingTema.fraFagsak(fagsak, null)));
        return EnhetsTjeneste.velgEnhet(enhet, merking);
    }

    // Brukes for å sjekke om det er behov for å flytte eller endre til spesialenheter etter innhenting av oppdaterte registerdata
    public Optional<OrganisasjonsEnhet> sjekkEnhetEtterEndring(Behandling behandling) {
        var enhet = behandling.getBehandlendeOrganisasjonsEnhet();
        if (enhet.equals(EnhetsTjeneste.getEnhetKlage())) {
            return Optional.empty();
        }
        var oppdatertEnhet = getOrganisasjonsEnhetEtterEndring(behandling, enhet).orElse(enhet);
        var enhetFraKobling = sjekkMotKobletSak(behandling.getFagsak(), oppdatertEnhet);
        return enhet.equals(enhetFraKobling) ? Optional.empty() : Optional.of(enhetFraKobling);
    }

    private FagsakMarkering finnSaksmerking(Fagsak fagsak) {
        return finnSaksmerking(fagsak.getId());
    }

    private FagsakMarkering finnSaksmerking(Long fagsakId) {
        return fagsakEgenskapRepository.finnFagsakMarkering(fagsakId).orElse(null);
    }

    private Optional<OrganisasjonsEnhet> getOrganisasjonsEnhetEtterEndring(Behandling behandling, OrganisasjonsEnhet enhet) {
        var hovedPerson = behandling.getAktørId();
        Set<AktørId> allePersoner = new HashSet<>();

        finnAktørAnnenPart(behandling).ifPresent(allePersoner::add);

        allePersoner.addAll(finnAktørIdFraPersonopplysninger(behandling));

        return getOrganisasjonsEnhetEtterEndring(behandling.getFagsak(), enhet, hovedPerson, allePersoner);
    }

    private Optional<OrganisasjonsEnhet> getOrganisasjonsEnhetEtterEndring(Fagsak fagsak, OrganisasjonsEnhet enhet, AktørId hovedPerson, Set<AktørId> allePersoner) {
        allePersoner.add(hovedPerson);

        var relasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(fagsak);
        relasjon.map(FagsakRelasjon::getFagsakNrEn).map(Fagsak::getAktørId).ifPresent(allePersoner::add);
        relasjon.flatMap(FagsakRelasjon::getFagsakNrTo).map(Fagsak::getAktørId).ifPresent(allePersoner::add);

        return enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhet.enhetId(),
            BehandlingTema.fraFagsak(fagsak, null), hovedPerson, allePersoner, finnSaksmerking(fagsak));
    }


    private Optional<AktørId> finnAktørAnnenPart(Behandling behandling) {
        return personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandling.getId()).map(OppgittAnnenPartEntitet::getAktørId);
    }

    private Set<AktørId> finnAktørIdFraPersonopplysninger(Behandling behandling) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
            .flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon)
            .map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .map(PersonopplysningEntitet::getAktørId)
            .collect(Collectors.toSet());
    }

    // Sjekk om enhet skal endres etter kobling av fagsak. Andre fagsak vil arve enhet fra første i relasjon, med mindre det er diskresjonskoder. empty() betyr ingen endring
    public Optional<OrganisasjonsEnhet> endretBehandlendeEnhetEtterFagsakKobling(Behandling behandling) {

        var eksisterendeEnhet = EnhetsTjeneste.velgEnhet(behandling.getBehandlendeOrganisasjonsEnhet(), finnSaksmerking(behandling.getFagsak()));
        var nyEnhet = sjekkMotKobletSak(behandling.getFagsak(), eksisterendeEnhet);

        return eksisterendeEnhet.equals(nyEnhet) ? Optional.empty() : Optional.of(nyEnhet);
    }

    private OrganisasjonsEnhet sjekkMotKobletSak(Fagsak sak, OrganisasjonsEnhet enhet) {
        var merking = finnSaksmerking(sak);
        var flyttet = EnhetsTjeneste.velgEnhet(enhet, merking);
        var relasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(sak).orElse(null);
        if (relasjon == null || relasjon.getFagsakNrTo().isEmpty()) {
            return flyttet;
        }
        var relatertSak = relasjon.getRelatertFagsak(sak).get();  // NOSONAR sjekket over
        var relatertEnhet = finnEnhetFor(relatertSak);
        var preferert = EnhetsTjeneste.enhetsPresedens(flyttet, relatertEnhet);
        return EnhetsTjeneste.velgEnhet(preferert, merking);
    }

    // Brukes for å sjekke om behandling skal flyttes etter endringer i NORG2-oppsett
    public Optional<OrganisasjonsEnhet> sjekkOppdatertEnhetEtterReallokering(Behandling behandling) {
        var merking = finnSaksmerking(behandling.getFagsak());
        var enhet = EnhetsTjeneste.velgEnhet(behandling.getBehandlendeOrganisasjonsEnhet(), merking);
        if (enhet.enhetId().equals(behandling.getBehandlendeEnhet())) {
            return Optional.empty();
        }
        return Optional.of(enhet);
    }

    // Returnerer enhetsnummer for NAV Klageinstans
    public static OrganisasjonsEnhet getKlageInstans() {
        return EnhetsTjeneste.getEnhetKlage();
    }

    public static boolean erUtlandsEnhet(Behandling behandling) {
        return EnhetsTjeneste.getEnhetUtland().enhetId().equals(behandling.getBehandlendeEnhet());
    }

    public static boolean erKontrollEnhet(Behandling behandling) {
        return EnhetsTjeneste.getEnhetKontroll().enhetId().equals(behandling.getBehandlendeEnhet());
    }


    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhetUtland(Behandling behandling, HistorikkAktør endretAv, String begrunnelse) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        if (erUtlandsEnhet(behandling)) {
            return;
        }
        if (endretAv != null) {
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, EnhetsTjeneste.getEnhetUtland(), begrunnelse, endretAv);
        }
        behandling.setBehandlendeEnhet(EnhetsTjeneste.getEnhetUtland());
        behandling.setBehandlendeEnhetÅrsak(begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhetKontroll(Behandling behandling, HistorikkAktør endretAv, String begrunnelse) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        if (erKontrollEnhet(behandling)) {
            return;
        }
        if (endretAv != null) {
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, EnhetsTjeneste.getEnhetKontroll(), begrunnelse, endretAv);
        }
        behandling.setBehandlendeEnhet(EnhetsTjeneste.getEnhetKontroll());
        behandling.setBehandlendeEnhetÅrsak(begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    // Oppdaterer behandlende enhet og sikre at dvh oppdateres (via event)
    public void oppdaterBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, HistorikkAktør endretAv, String begrunnelse) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        if (endretAv != null) {
            lagHistorikkInnslagForByttBehandlendeEnhet(behandling, nyEnhet, begrunnelse, endretAv);
        }
        behandling.setBehandlendeEnhet(nyEnhet);
        behandling.setBehandlendeEnhetÅrsak(begrunnelse);

        behandlingRepository.lagre(behandling, lås);
        eventPubliserer.fireEvent(behandling);
    }

    private void lagHistorikkInnslagForByttBehandlendeEnhet(Behandling behandling, OrganisasjonsEnhet nyEnhet, String begrunnelse, HistorikkAktør aktør) {
        var eksisterende = behandling.getBehandlendeOrganisasjonsEnhet();
        var fraMessage = eksisterende != null ? eksisterende.enhetId() + " " + eksisterende.enhetNavn() : "ukjent";
        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BYTT_ENHET)
            .medEndretFelt(HistorikkEndretFeltType.BEHANDLENDE_ENHET,
                fraMessage,
                nyEnhet.enhetId() + " " + nyEnhet.enhetNavn())
            .medBegrunnelse(begrunnelse);

        var innslag = new Historikkinnslag();
        innslag.setAktør(aktør);
        innslag.setType(HistorikkinnslagType.BYTT_ENHET);
        innslag.setBehandlingId(behandling.getId());
        builder.build(innslag);
        historikkRepository.lagre(innslag);
    }
}
