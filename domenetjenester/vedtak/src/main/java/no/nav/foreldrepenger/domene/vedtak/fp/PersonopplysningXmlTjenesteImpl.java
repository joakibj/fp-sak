package no.nav.foreldrepenger.domene.vedtak.fp;

import static no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil.harAleneomsorg;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.YtelseStørrelse;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlFelles;
import no.nav.foreldrepenger.domene.vedtak.xml.PersonopplysningXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Addresse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.DokumentasjonPeriode;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.FamilieHendelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Familierelasjon;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Inntekt;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Inntektspost;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Medlemskap;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.PersonopplysningerForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.RelatertYtelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Terminbekreftelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Verge;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.Virksomhet;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseStorrelse;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.Foedsel;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.PersonIdentifiserbar;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class PersonopplysningXmlTjenesteImpl extends PersonopplysningXmlTjeneste {
    private final no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory personopplysningBaseObjectFactory = new no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory();
    private final ObjectFactory personopplysningObjectFactory = new ObjectFactory();

    private FamilieHendelseRepository familieHendelseRepository;
    private MedlemskapRepository medlemskapRepository;
    private VergeRepository vergeRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private VirksomhetRepository virksomhetRepository;
    private PersonopplysningXmlFelles personopplysningFellesTjeneste;

    public PersonopplysningXmlTjenesteImpl() {
        // For CDI
    }

    @Inject
    public PersonopplysningXmlTjenesteImpl(PersonopplysningXmlFelles fellesTjeneste,
                                                     BehandlingRepositoryProvider provider,
                                                     KodeverkRepository kodeverkRepository,
                                                     PersonopplysningTjeneste personopplysningTjeneste,
                                                     InntektArbeidYtelseTjeneste iayTjeneste,
                                                     YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                                     VergeRepository vergeRepository,
                                                     VirksomhetRepository virksomhetRepository) {
        super(personopplysningTjeneste, kodeverkRepository);
        this.personopplysningFellesTjeneste = fellesTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.virksomhetRepository = virksomhetRepository;
        this.familieHendelseRepository = provider.getFamilieHendelseRepository();
        this.medlemskapRepository = provider.getMedlemskapRepository();
        this.vergeRepository = vergeRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public Object lagPersonopplysning(PersonopplysningerAggregat personopplysningerAggregat, Long behandlingId, AktørId aktørId,
                                      Skjæringstidspunkt skjæringstidspunkter) {
        PersonopplysningerForeldrepenger personopplysninger = personopplysningObjectFactory.createPersonopplysningerForeldrepenger();
        familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId).ifPresent(familieHendelseGrunnlag -> {
            setVerge(behandlingId, personopplysninger);
            setMedlemskapsperioder(behandlingId, personopplysninger);
            setFamiliehendelse(personopplysninger, familieHendelseGrunnlag);
        });
        LocalDate skjæringstidspunkt = skjæringstidspunkter.getUtledetSkjæringstidspunkt();
        setAdresse(personopplysninger, personopplysningerAggregat);
        setDokumentasjonsperioder(behandlingId, personopplysninger);
        setInntekter(behandlingId, personopplysninger, skjæringstidspunkt);
        setBruker(personopplysninger, personopplysningerAggregat);
        setFamilierelasjoner(personopplysninger, personopplysningerAggregat);
        setRelaterteYtelser(behandlingId, aktørId, personopplysninger, skjæringstidspunkt);

        return personopplysningObjectFactory.createPersonopplysningerForeldrepenger(personopplysninger);
    }

    private void setFamiliehendelse(PersonopplysningerForeldrepenger personopplysninger, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        FamilieHendelse familieHendelse = personopplysningObjectFactory.createFamilieHendelse();
        setFoedsel(familieHendelse, familieHendelseGrunnlag);
        setAdopsjon(familieHendelse, familieHendelseGrunnlag);
        setTerminbekreftelse(familieHendelse, familieHendelseGrunnlag);
        personopplysninger.setFamiliehendelse(familieHendelse);
    }

    private void setRelaterteYtelser(Long behandlingId, AktørId aktørId, PersonopplysningerForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
            .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);
        var ytelser = ytelseFilter.getFiltrertYtelser();
        if (!ytelser.isEmpty()) {
            PersonopplysningerForeldrepenger.RelaterteYtelser relaterteYtelser = personopplysningObjectFactory
                .createPersonopplysningerForeldrepengerRelaterteYtelser();
            ytelser.stream().forEach(ytelse -> relaterteYtelser.getRelatertYtelse().add(konverterFraDomene(ytelse)));
            personopplysninger.setRelaterteYtelser(relaterteYtelser);
        }
    }

    private RelatertYtelse konverterFraDomene(Ytelse ytelse) {
        RelatertYtelse relatertYtelse = personopplysningObjectFactory.createRelatertYtelse();
        relatertYtelse.setBehandlingstema(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getBehandlingsTema()));
        relatertYtelse.setKilde(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getKilde()));
        Optional.ofNullable(ytelse.getPeriode())
            .ifPresent(periode -> relatertYtelse.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getFomDato(), periode.getTomDato())));
        Optional.ofNullable(ytelse.getSaksnummer())
            .ifPresent(saksnummer -> relatertYtelse.setSaksnummer(VedtakXmlUtil.lagStringOpplysning(saksnummer.getVerdi())));
        relatertYtelse.setStatus(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getStatus()));
        relatertYtelse.setTemaUnderkategori(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getBehandlingsTema()));
        relatertYtelse.setType(VedtakXmlUtil.lagKodeverksOpplysning(ytelse.getRelatertYtelseType()));

        setYtelseAnvist(relatertYtelse, ytelse.getYtelseAnvist());
        setYtelsesgrunnlag(relatertYtelse, ytelse.getYtelseGrunnlag());
        setYtelsesStørrelse(relatertYtelse, ytelse.getYtelseGrunnlag());
        return relatertYtelse;
    }

    private void setYtelsesStørrelse(RelatertYtelse relatertYtelseKontrakt, Optional<YtelseGrunnlag> ytelseGrunnlagDomene) {
        if (ytelseGrunnlagDomene.isPresent()) {
            YtelseGrunnlag ytelseGrunnlag = ytelseGrunnlagDomene.get();
            List<YtelseStorrelse> ytelseStorrelser = ytelseGrunnlag.getYtelseStørrelse().stream().map(ys -> konverterFraDomene(ys))
                .collect(Collectors.toList());
            relatertYtelseKontrakt.getYtelsesstorrelse().addAll(ytelseStorrelser);
        }
    }

    private YtelseStorrelse konverterFraDomene(YtelseStørrelse domene) {
        YtelseStorrelse kontrakt = personopplysningObjectFactory.createYtelseStorrelse();
        var virk = domene.getOrgnr().flatMap(orgnr -> virksomhetRepository.hent(orgnr));
        virk.ifPresent(virksomhet -> kontrakt.setVirksomhet(tilVirksomhet(virksomhet)));
        kontrakt.setBeloep(VedtakXmlUtil.lagDecimalOpplysning(domene.getBeløp().getVerdi()));
        kontrakt.setHyppighet(VedtakXmlUtil.lagKodeverksOpplysning(domene.getHyppighet()));
        return kontrakt;
    }

    private Virksomhet tilVirksomhet(no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet domene) {
        Virksomhet kontrakt = personopplysningObjectFactory.createVirksomhet();
        kontrakt.setNavn(VedtakXmlUtil.lagStringOpplysning(domene.getNavn()));
        kontrakt.setOrgnr(VedtakXmlUtil.lagStringOpplysning(domene.getOrgnr()));
        return kontrakt;
    }

    private void setYtelsesgrunnlag(RelatertYtelse relatertYtelseKontrakt, Optional<YtelseGrunnlag> ytelseGrunnlagDomene) {
        Optional<no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseGrunnlag> ytelseGrunnlagOptional = ytelseGrunnlagDomene
            .map(yg -> konverterFraDomene(yg));
        ytelseGrunnlagOptional.ifPresent(ytelseGrunnlag -> {
            relatertYtelseKontrakt.setYtelsesgrunnlag(ytelseGrunnlag);
        });
    }

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseGrunnlag konverterFraDomene(YtelseGrunnlag domene) {
        no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseGrunnlag kontrakt = personopplysningObjectFactory.createYtelseGrunnlag();
        domene.getArbeidskategori()
            .ifPresent(arbeidskategori -> kontrakt.setArbeidtype(VedtakXmlUtil.lagKodeverksOpplysning(arbeidskategori)));
        domene.getDekningsgradProsent().ifPresent(dp -> kontrakt.setDekningsgradprosent(VedtakXmlUtil.lagDecimalOpplysning(dp.getVerdi())));

        domene.getGraderingProsent()
            .ifPresent(graderingsProsent -> kontrakt.setGraderingprosent(VedtakXmlUtil.lagDecimalOpplysning(graderingsProsent.getVerdi())));
        if (domene.getInntektsgrunnlagProsent().isPresent() && domene.getInntektsgrunnlagProsent().get().getVerdi() != null) {
            kontrakt.setInntektsgrunnlagprosent(VedtakXmlUtil.lagDecimalOpplysning(domene.getInntektsgrunnlagProsent().get().getVerdi()));
        }

        return kontrakt;
    }

    private void setYtelseAnvist(RelatertYtelse relatertYtelseKontrakt, Collection<YtelseAnvist> ytelseAnvistDomene) {
        List<no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseAnvist> alleYtelserAnvist = ytelseAnvistDomene.stream()
            .map(ytelseAnvist -> konverterFraDomene(ytelseAnvist)).collect(Collectors.toList());
        relatertYtelseKontrakt.getYtelseanvist().addAll(alleYtelserAnvist);
    }

    private no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseAnvist konverterFraDomene(YtelseAnvist domene) {
        no.nav.vedtak.felles.xml.vedtak.personopplysninger.fp.v2.YtelseAnvist kontrakt = personopplysningObjectFactory.createYtelseAnvist();
        domene.getBeløp().ifPresent(beløp -> kontrakt.setBeloep(VedtakXmlUtil.lagDecimalOpplysning(beløp.getVerdi())));
        domene.getDagsats().ifPresent(dagsats -> kontrakt.setDagsats(VedtakXmlUtil.lagDecimalOpplysning(dagsats.getVerdi())));
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(domene.getAnvistFOM(), domene.getAnvistTOM()));
        domene.getUtbetalingsgradProsent().ifPresent(prosent -> kontrakt.setUtbetalingsgradprosent(VedtakXmlUtil.lagDecimalOpplysning(prosent.getVerdi())));

        return kontrakt;
    }

    private void setFamilierelasjoner(PersonopplysningerForeldrepenger personopplysninger, PersonopplysningerAggregat aggregat) {
        final Map<AktørId, PersonopplysningEntitet> aktørPersonopplysningMap = aggregat.getAktørPersonopplysningMap();
        final List<PersonRelasjonEntitet> tilPersoner = aggregat.getSøkersRelasjoner().stream()
            .filter(r -> aktørPersonopplysningMap.get(r.getTilAktørId()) != null)
            .collect(Collectors.toList());
        if (!tilPersoner.isEmpty()) {
            PersonopplysningerForeldrepenger.Familierelasjoner familierelasjoner = personopplysningObjectFactory
                .createPersonopplysningerForeldrepengerFamilierelasjoner();
            personopplysninger.setFamilierelasjoner(familierelasjoner);
            tilPersoner.forEach(relasjon -> personopplysninger.getFamilierelasjoner().getFamilierelasjon()
                .add(lagRelasjon(relasjon, aktørPersonopplysningMap.get(relasjon.getTilAktørId()), aggregat)));
        }
    }

    private Familierelasjon lagRelasjon(PersonRelasjonEntitet relasjon, PersonopplysningEntitet tilPerson, PersonopplysningerAggregat aggregat) {
        Familierelasjon familierelasjon = personopplysningObjectFactory.createFamilierelasjon();
        PersonIdentifiserbar person = personopplysningFellesTjeneste.lagBruker(aggregat, tilPerson);
        familierelasjon.setTilPerson(person);
        familierelasjon.setRelasjon(VedtakXmlUtil.lagKodeverksOpplysning(relasjon.getRelasjonsrolle()));
        return familierelasjon;
    }

    private void setDokumentasjonsperioder(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger) {
        PersonopplysningerForeldrepenger.Dokumentasjonsperioder dokumentasjonsperioder = personopplysningObjectFactory
            .createPersonopplysningerForeldrepengerDokumentasjonsperioder();

        ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandlingId).ifPresent(aggregat -> {
            leggTilPerioderMedAleneomsorg(aggregat, dokumentasjonsperioder);
            aggregat.getPerioderUttakDokumentasjon().ifPresent(
                uttakDokumentasjon -> dokumentasjonsperioder.getDokumentasjonperiode().addAll(lagDokumentasjonPerioder(uttakDokumentasjon.getPerioder())));
            aggregat.getPerioderUtenOmsorg()
                .ifPresent(utenOmsorg -> dokumentasjonsperioder.getDokumentasjonperiode().addAll(lagDokumentasjonPerioder(utenOmsorg.getPerioder())));
            aggregat.getPerioderAnnenforelderHarRett().ifPresent(
                annenforelderHarRett -> dokumentasjonsperioder.getDokumentasjonperiode().addAll(lagDokumentasjonPerioder(annenforelderHarRett.getPerioder())));
            personopplysninger.setDokumentasjonsperioder(dokumentasjonsperioder);
        });
    }

    private void leggTilPerioderMedAleneomsorg(YtelseFordelingAggregat aggregat,
                                               PersonopplysningerForeldrepenger.Dokumentasjonsperioder dokumentasjonsperioder) {
        if (harAleneomsorg(aggregat)) {
            dokumentasjonsperioder.getDokumentasjonperiode()
                .addAll(lagDokumentasjonPerioder(List.of(new PeriodeAleneOmsorgEntitet(LocalDate.now(), LocalDate.now()))));
        }
    }

    private List<? extends DokumentasjonPeriode> lagDokumentasjonPerioder(List<? extends no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.DokumentasjonPeriodeEntitet<?>> perioder) {
        List<DokumentasjonPeriode> result = new ArrayList<>();
        perioder.stream().forEach(periode -> {
            DokumentasjonPeriode dokumentasjonPeriode = personopplysningObjectFactory.createDokumentasjonPeriode();
            dokumentasjonPeriode.setDokumentasjontype(VedtakXmlUtil.lagKodeverksOpplysning(periode.getDokumentasjonType()));
            dokumentasjonPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periode.getPeriode().getFomDato(), periode.getPeriode().getTomDato()));
            result.add(dokumentasjonPeriode);
        });
        return result;
    }

    private void setInntekter(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger, LocalDate skjæringstidspunkt) {

        iayTjeneste.finnGrunnlag(behandlingId).ifPresent(grunnlag -> {
            Collection<AktørInntekt> aktørInntekt = grunnlag.getAlleAktørInntektFraRegister();
            if (aktørInntekt != null) {
                var inntekter = personopplysningObjectFactory.createPersonopplysningerForeldrepengerInntekter();
                aktørInntekt.forEach(inntekt -> {
                    var filter = new InntektFilter(inntekt).før(skjæringstidspunkt).filterPensjonsgivende();
                    inntekter.getInntekt().addAll(lagInntekt(inntekt.getAktørId(), filter));
                    personopplysninger.setInntekter(inntekter);
                });
            }
        });

    }

    private Collection<? extends Inntekt> lagInntekt(AktørId aktørId, InntektFilter filter) {
        List<Inntekt> inntektList = new ArrayList<>();
        List<Inntektspost> inntektspostList = new ArrayList<>();

        filter.forFilter((inntekt, inntektsposter) -> {
            Inntekt inntektXML = personopplysningObjectFactory.createInntekt();
            inntektsposter.forEach(inntektspost -> {
                Inntektspost inntektspostXML = personopplysningObjectFactory.createInntektspost();
                if (inntekt.getArbeidsgiver() != null) {
                    inntektXML.setArbeidsgiver(VedtakXmlUtil.lagStringOpplysning(inntekt.getArbeidsgiver().getIdentifikator()));
                }
                inntektspostXML.setBeloep(VedtakXmlUtil.lagDoubleOpplysning(inntektspost.getBeløp().getVerdi().doubleValue()));
                var periode = VedtakXmlUtil.lagPeriodeOpplysning(inntektspost.getPeriode().getFomDato(), inntektspost.getPeriode().getTomDato());
                inntektspostXML.setPeriode(periode);
                inntektspostXML.setYtelsetype(VedtakXmlUtil.lagStringOpplysning(inntektspost.getInntektspostType().getKode()));
                inntektXML.getInntektsposter().add(inntektspostXML);
                inntektXML.setMottaker(VedtakXmlUtil.lagStringOpplysning(aktørId.getId()));
                inntektspostList.add(inntektspostXML);
            });
            inntektList.add(inntektXML);
        });
        return inntektList;
    }

    private void setBruker(PersonopplysningerForeldrepenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        PersonIdentifiserbar person = personopplysningFellesTjeneste.lagBruker(personopplysningerAggregat, personopplysningerAggregat.getSøker());
        personopplysninger.setBruker(person);
    }

    private void setAdresse(PersonopplysningerForeldrepenger personopplysninger, PersonopplysningerAggregat personopplysningerAggregat) {
        final PersonopplysningEntitet personopplysning = personopplysningerAggregat.getSøker();
        List<PersonAdresseEntitet> opplysningAdresser = personopplysningerAggregat.getAdresserFor(personopplysning.getAktørId());
        if (opplysningAdresser != null) {
            opplysningAdresser.forEach(adresse -> personopplysninger.getAdresse().add(lagAdresse(personopplysning, adresse)));
        }
    }

    private Addresse lagAdresse(PersonopplysningEntitet personopplysning, PersonAdresseEntitet adresseFraBehandling) {
        Addresse adresse = personopplysningObjectFactory.createAddresse();
        adresse.setAdressetype(VedtakXmlUtil.lagKodeverksOpplysning(adresseFraBehandling.getAdresseType()));
        adresse.setAddresselinje1(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje1()));
        if (adresseFraBehandling.getAdresselinje2() != null) {
            adresse.setAddresselinje2(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje2()));
        }
        if (adresseFraBehandling.getAdresselinje3() != null) {
            adresse.setAddresselinje3(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje3()));
        }
        if (adresseFraBehandling.getAdresselinje4() != null) {
            adresse.setAddresselinje4(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getAdresselinje4()));
        }
        adresse.setLand(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getLand()));
        adresse.setMottakersNavn(VedtakXmlUtil.lagStringOpplysning(personopplysning.getNavn()));
        adresse.setPostnummer(VedtakXmlUtil.lagStringOpplysning(adresseFraBehandling.getPostnummer()));
        return adresse;
    }

    private void setFoedsel(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet gjeldendeFamiliehendelse = familieHendelseGrunnlag
            .getGjeldendeVersjon();
        if (Arrays.asList(FamilieHendelseType.FØDSEL, FamilieHendelseType.TERMIN).contains(gjeldendeFamiliehendelse.getType())) {
            Foedsel fødsel = personopplysningBaseObjectFactory.createFoedsel();
            fødsel.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(gjeldendeFamiliehendelse.getAntallBarn()));
            gjeldendeFamiliehendelse.getFødselsdato().ifPresent(fDato -> VedtakXmlUtil.lagDateOpplysning(fDato).ifPresent(fødsel::setFoedselsdato));
            familieHendelse.setFoedsel(fødsel);
        }
    }

    private void setAdopsjon(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        familieHendelseGrunnlag.getGjeldendeAdopsjon().ifPresent(adopsjonhendelse -> {
            Adopsjon adopsjon = personopplysningObjectFactory.createAdopsjon();
            if (adopsjonhendelse.getErEktefellesBarn() != null) {
                adopsjon.setErEktefellesBarn(VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.getErEktefellesBarn()));
            }
            familieHendelseGrunnlag.getGjeldendeBarna().forEach(aBarn -> adopsjon.getAdopsjonsbarn().add(leggTilAdopsjonsbarn(aBarn)));
            if (adopsjonhendelse.getAdoptererAlene() != null) {
                adopsjon.setAdoptererAlene(VedtakXmlUtil.lagBooleanOpplysning(adopsjonhendelse.getAdoptererAlene()));
            }
            if (adopsjonhendelse.getOmsorgsovertakelseDato() != null) {
                VedtakXmlUtil.lagDateOpplysning(adopsjonhendelse.getOmsorgsovertakelseDato()).ifPresent(adopsjon::setOmsorgsovertakelsesdato);
            }
            familieHendelse.setAdopsjon(adopsjon);
        });
    }

    private Adopsjon.Adopsjonsbarn leggTilAdopsjonsbarn(UidentifisertBarn aBarn) {
        Adopsjon.Adopsjonsbarn adopsjonsbarn = personopplysningObjectFactory.createAdopsjonAdopsjonsbarn();
        VedtakXmlUtil.lagDateOpplysning(aBarn.getFødselsdato()).ifPresent(adopsjonsbarn::setFoedselsdato);
        return adopsjonsbarn;
    }

    private void setTerminbekreftelse(FamilieHendelse familieHendelse, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag.getGjeldendeVersjon().getType().equals(FamilieHendelseType.TERMIN)) {
            familieHendelseGrunnlag.getGjeldendeTerminbekreftelse().ifPresent(terminbekreftelseFraBehandling -> {
                Terminbekreftelse terminbekreftelse = personopplysningObjectFactory.createTerminbekreftelse();
                terminbekreftelse.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(familieHendelseGrunnlag.getGjeldendeAntallBarn()));
                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getUtstedtdato()).ifPresent(terminbekreftelse::setUtstedtDato);
                VedtakXmlUtil.lagDateOpplysning(terminbekreftelseFraBehandling.getTermindato()).ifPresent(terminbekreftelse::setTermindato);
                familieHendelse.setTerminbekreftelse(terminbekreftelse);
            });
        }
    }

    private void setMedlemskapsperioder(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger) {
        medlemskapRepository.hentMedlemskap(behandlingId).ifPresent(medlemskapAggregat -> {
            Medlemskap medlemskap = personopplysningObjectFactory.createMedlemskap();
            personopplysninger.setMedlemskap(medlemskap);

            medlemskapAggregat.getRegistrertMedlemskapPerioder()
                .forEach(medlemskapPeriode -> personopplysninger.getMedlemskap().getMedlemskapsperiode()
                    .add(personopplysningFellesTjeneste.lagMedlemskapPeriode(medlemskapPeriode)));
        });
    }

    private void setVerge(Long behandlingId, PersonopplysningerForeldrepenger personopplysninger) {
        vergeRepository.hentAggregat(behandlingId).ifPresent(vergeAggregat -> {
            vergeAggregat.getVerge().ifPresent(vergeFraBehandling -> {
                Verge verge = personopplysningObjectFactory.createVerge();
                if( vergeFraBehandling.getVergeOrganisasjon().isPresent()){
                    verge.setNavn(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getNavn()));
                    verge.setOrganisasjonsnummer(VedtakXmlUtil.lagStringOpplysning( vergeFraBehandling.getVergeOrganisasjon().get().getOrganisasjonsnummer()));
                }
                else {
                    Optional<AktørId> aktørId = vergeAggregat.getAktørId();
                    if (aktørId.isPresent()) {
                        verge.setNavn(VedtakXmlUtil.lagStringOpplysning(personopplysningFellesTjeneste.hentVergeNavn(aktørId.get())));
                    }
                }
                verge.setVergetype(VedtakXmlUtil.lagKodeverksOpplysning(vergeFraBehandling.getVergeType()));
                verge.setGyldighetsperiode(VedtakXmlUtil.lagPeriodeOpplysning(vergeFraBehandling.getGyldigFom(), vergeFraBehandling.getGyldigTom()));
                personopplysninger.setVerge(verge);
            });
        });
    }

}
