package no.nav.foreldrepenger.mottak.sakogenhet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class KobleSakerTjeneste {

    private Logger LOG = LoggerFactory.getLogger(KobleSakerTjeneste.class);

    private PersoninfoAdapter personinfoAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    KobleSakerTjeneste() {
        //For CDI
    }

    @Inject
    public KobleSakerTjeneste(BehandlingRepositoryProvider provider,
                              PersoninfoAdapter personinfoAdapter,
                              FamilieHendelseTjeneste familieHendelseTjeneste,
                              FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.personopplysningRepository = provider.getPersonopplysningRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.fagsakRepository = provider.getFagsakRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
    }
    /*
     * Hva: Koble innkommende sak med annen sak som angår samme barnekull
     * Utfordring: Kjøres før registerinnhenting - mangler registerversjon av FH og PO
     * Hvorfor så tidlig: For å unngå å sende Infobrev når annenpart allerede har søkt.
     *
     * Problematikk 1: Har partene oppgitt hverandre? Har bare denne oppgitt annenpart eller er bruker oppgitt som annenpart i annen sak
     * Problematikk 2: Forsinket registrering av far/medmor. Potensielt ulike aktører i søknad og freg
     * Problematikk 3: Søknadsfeil - hvem som er annenpart og oppgitt fødsels/termindato
     */
    public Optional<Fagsak> finnRelatertFagsakDersomRelevant(Behandling behandling) {
        Fagsak fagsak = behandling.getFagsak();
        FagsakRelasjon fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak).orElse(null);
        // Allerede koblet
        if (fagsakRelasjon != null && fagsakRelasjon.getFagsakNrTo().isPresent()) {
            HendelserFeil.FACTORY.fagsakAlleredeKoblet(fagsak.getSaksnummer()).log(LOG);
            return Optional.empty();
        }

        // Mangler familiehendelseinformasjon og mulighet for å finne samme barnekull.
        FamilieHendelseGrunnlagEntitet grunnlag = familieHendelseTjeneste.finnAggregat(behandling.getId()).orElse(null);
        if (grunnlag == null || FamilieHendelseType.UDEFINERT.equals(grunnlag.getGjeldendeVersjon().getType())) {
            HendelserFeil.FACTORY.familiehendelseUtenDato(fagsak.getSaksnummer()).log(LOG);
            return Optional.empty();
        }

        // Finner saker å koble med ut fra oppgitt annen part og fra FREG-data om registrerte foreldre
        AktørId annenPart = personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(behandling.getId())
            .map(OppgittAnnenPartEntitet::getAktørId).orElse(null);
        List<LocalDateInterval> fødselsIntervaller = familieHendelseTjeneste.forventetFødselsIntervaller(BehandlingReferanse.fra(behandling));
        Set<AktørId> andreForeldreForBarn = finnAndreRegistrerteForeldreForBarnekull(fagsak.getAktørId(), fødselsIntervaller);
        List<Fagsak> aktuelleFagsaker = finnMuligeFagsakerForKobling(behandling, grunnlag, annenPart, andreForeldreForBarn);

        // Håndter utfall
        if (aktuelleFagsaker.isEmpty()) {
            if (!RelasjonsRolleType.erMor(behandling.getFagsak().getRelasjonsRolleType())) {
                LOG.info("KobleSak: Finner ikke sak å koble med for ikke-mor i sak {} rolle {}", behandling.getFagsak().getSaksnummer(),
                    behandling.getFagsak().getRelasjonsRolleType().getKode());
            }
            return Optional.empty();
        } else if (aktuelleFagsaker.size() > 1) {
            String kandidater = aktuelleFagsaker.stream().map(f -> f.getSaksnummer().getVerdi()).collect(Collectors.joining(", "));
            HendelserFeil.FACTORY.flereMuligeFagsakerÅKobleTil(fagsak.getSaksnummer(), kandidater).log(LOG);
            return Optional.empty();
        }
        return Optional.of(aktuelleFagsaker.get(0));

    }

    public Optional<FagsakRelasjon> finnFagsakRelasjonDersomOpprettet(Behandling behandling) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak());
    }

    public void oppdaterFagsakRelasjonMedDekningsgrad(Fagsak fagsak, Optional<FagsakRelasjon> relasjon, Dekningsgrad dekningsgrad) {
        fagsakRelasjonTjeneste.opprettEllerOppdaterRelasjon(fagsak, relasjon, dekningsgrad);
    }

    public void kobleRelatertFagsakHvisDetFinnesEn(Behandling behandling) {
        final Optional<FagsakRelasjon> eksisterendeRelasjon = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        if (eksisterendeRelasjon.flatMap(FagsakRelasjon::getFagsakNrTo).isPresent()) {
            return;
        }
        final Optional<Fagsak> potensiellFagsak = finnRelatertFagsakDersomRelevant(behandling);
        potensiellFagsak.ifPresent(fagsak -> fagsakRelasjonTjeneste.kobleFagsaker(fagsak, behandling.getFagsak(), behandling));
    }

    private Set<AktørId> finnAndreRegistrerteForeldreForBarnekull(AktørId aktørId, List<LocalDateInterval> intervaller) {
        // Hent fødsler i intervall og deretter kjerneinfo m/familierelasjoner for disse
        return personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(aktørId, intervaller).stream()
            .flatMap(b -> personinfoAdapter.finnAktørIdForForeldreTil(b.getIdent()).stream())
            .filter(a -> !aktørId.equals(a))
            .collect(Collectors.toSet());
    }

    private List<Fagsak> finnMuligeFagsakerForKobling(Behandling behandling, FamilieHendelseGrunnlagEntitet grunnlag, AktørId annenPart, Set<AktørId> andreForeldreForBarn) {
        // Finner saker der bruker fra behandling er oppgitt som annen part
        Set<Long> fagsakerSomRefererer = new HashSet<>(personopplysningRepository.fagsakerMedOppgittAnnenPart(behandling.getAktørId()));
        // Henter fagsaker for oppgitt annen part og for foreldre til brukers barn i aktuelt kull
        List<Fagsak> aktuelleFagsaker = new ArrayList<>(fagsakRepository.hentForBrukerMulti(andreForeldreForBarn));
        if (annenPart != null && !andreForeldreForBarn.contains(annenPart)) {
            aktuelleFagsaker.addAll(fagsakRepository.hentForBruker(annenPart));
        }
        Set<Long> fagsakerAndreForeldre = aktuelleFagsaker.stream().map(Fagsak::getId).collect(Collectors.toSet());
        fagsakerSomRefererer.stream()
            .filter(fid -> !fagsakerAndreForeldre.contains(fid))
            .map(fagsakRepository::finnEksaktFagsak)
            .forEach(aktuelleFagsaker::add);
        // Finner ukoblete saker, siste behandling og filtrerer på innkommende sak sitt FH-grunnlag
        var ytelseType = behandling.getFagsakYtelseType();
        List<Behandling> filtrertGrunnlag = aktuelleFagsaker.stream()
            .filter(f -> ytelseType.equals(f.getYtelseType()))
            .filter(f -> !behandling.getAktørId().equals(f.getAktørId()))
            .filter(f -> fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(f).flatMap(FagsakRelasjon::getFagsakNrTo).isEmpty())
            .flatMap(f -> behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(f.getId()).stream())
            .filter(b -> sammeFamilieHendelse(grunnlag, b))
            .collect(Collectors.toList());
        if (filtrertGrunnlag.size() > 1) {
            // Studere logger og vurdere å legge inn heuristikker
            // Eksempelvis prioritere saker der man har oppgitt hverandre - eller foreldre fra registrerte barn fra FREG
            LOG.info("KobleSak: Flere saker å koble med for fagsakId {} behandlinger {}", behandling.getFagsak().getId(),
                filtrertGrunnlag.stream().map(Behandling::getId).collect(Collectors.toList()));
        }
        return filtrertGrunnlag.stream().map(Behandling::getFagsak).collect(Collectors.toList());
    }

    private boolean sammeFamilieHendelse(FamilieHendelseGrunnlagEntitet grunnlag, Behandling aktuellBehandling) {
        var grunnlagAktuellBehandling = familieHendelseTjeneste.finnAggregat(aktuellBehandling.getId());
        return grunnlagAktuellBehandling.filter(g -> familieHendelseTjeneste.matcherGrunnlagene(grunnlag, g)).isPresent();
    }

}
