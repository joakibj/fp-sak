package no.nav.foreldrepenger.datavarehus.v2;

import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.AnnenForelder;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.Beregning;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.Builder;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.FamilieHendelse;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.ForeldrepengerRettigheter;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.HendelseType;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.LovVersjon;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.RettighetType;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.UtlandsTilsnitt;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.VedtakResultat;
import static no.nav.foreldrepenger.datavarehus.v2.StønadsstatistikkVedtak.YtelseType;

import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.datavarehus.domene.VilkårIkkeOppfylt;
import no.nav.foreldrepenger.datavarehus.tjeneste.BehandlingVedtakDvhMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.saldo.SaldoUtregning;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class StønadsstatistikkTjeneste {

    private static final Period INTERVALL_SAMME_BARN = Period.ofWeeks(6);

    private BehandlingRepository behandlingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakTjeneste fagsakTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private FagsakRepository fagsakRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private LegacyESBeregningRepository legacyESBeregningRepository;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Inject
    public StønadsstatistikkTjeneste(BehandlingRepository behandlingRepository,
                                     FagsakRelasjonRepository fagsakRelasjonRepository,
                                     FagsakTjeneste fagsakTjeneste,
                                     BehandlingVedtakRepository behandlingVedtakRepository,
                                     FagsakEgenskapRepository fagsakEgenskapRepository,
                                     FagsakRepository fagsakRepository,
                                     FamilieHendelseTjeneste familieHendelseTjeneste,
                                     PersonopplysningTjeneste personopplysningTjeneste,
                                     ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                     BeregningsresultatRepository beregningsresultatRepository,
                                     YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                     UttakInputTjeneste uttakInputTjeneste,
                                     StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                     LegacyESBeregningRepository legacyESBeregningRepository,
                                     HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                     InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.fagsakTjeneste = fagsakTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.fagsakRepository = fagsakRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.legacyESBeregningRepository = legacyESBeregningRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    StønadsstatistikkTjeneste() {
        //CDI
    }

    public StønadsstatistikkVedtak genererVedtak(BehandlingReferanse behandlingReferanse) {
        var behandlingId = behandlingReferanse.behandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var vedtak = behandlingVedtakRepository.hentForBehandling(behandlingId);
        var stp = behandlingReferanse.getSkjæringstidspunkt();
        var forrigeBehandlingUuid = behandling.getOriginalBehandlingId().map(id -> behandlingRepository.hentBehandling(id)).map(Behandling::getUuid);
        var utlandMarkering = fagsakEgenskapRepository.finnFagsakMarkering(behandling.getFagsakId()).orElse(FagsakMarkering.NASJONAL);
        var familiehendelse = familieHendelseTjeneste.hentAggregat(behandlingId).getGjeldendeVersjon();

        var lovVersjon = utledLovVersjon(stp);
        var fagsak = behandling.getFagsak();
        var saksnummer = mapSaksnummer(fagsak.getSaksnummer());
        var ytelseType = mapYtelseType(fagsak.getYtelseType());
        var søker = mapAktørId(fagsak.getAktørId());
        var søkersRolle = mapBrukerRolle(fagsak.getRelasjonsRolleType());
        var familieHendelse = mapFamilieHendelse(behandlingReferanse, familiehendelse);


        var builder = new Builder().medLovVersjon(lovVersjon)
            .medSaksnummer(saksnummer)
            .medSøker(søker)
            .medSøkersRolle(søkersRolle)
            .medYtelseType(ytelseType)
            .medBehandlingUuid(behandlingReferanse.behandlingUuid())
            .medForrigeBehandlingUuid(forrigeBehandlingUuid.orElse(null))
            .medSkjæringstidspunkt(stp.getSkjæringstidspunktHvisUtledet().orElse(null))
            .medVedtakstidspunkt(vedtak.getVedtakstidspunkt())
            .medVedtaksresultat(mapVedtaksresultat(vedtak))
            .medVilkårIkkeOppfylt(utledVilkårIkkeOppfylt(vedtak, behandling))
            .medUtlandsTilsnitt(utledUtlandsTilsnitt(utlandMarkering))
            .medAnnenForelder(utledAnnenForelder(behandling, familiehendelse))
            .medFamilieHendelse(familieHendelse)
            .medUtbetalingsreferanse(String.valueOf(behandlingReferanse.behandlingId()));

        if (FagsakYtelseType.FORELDREPENGER.equals(fagsak.getYtelseType())) {
            var rettigheter = utledRettigheter(behandling);
            var foreldrepengerUttaksperioder = mapForeldrepengerUttaksperioder(behandling, rettigheter.rettighetType());
            builder.medUttakssperioder(foreldrepengerUttaksperioder).medForeldrepengerRettigheter(rettigheter);
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            builder.medEngangsstønadInnvilget(utledTilkjentEngangsstønad(behandlingId));
        } else {
            var utbetalingssperioder = mapUtbetalingssperioder(behandling, ytelseType, familieHendelse.hendelseType());
            builder.medUtbetalingssperioder(utbetalingssperioder).medBeregning(utledBeregning(behandling));
        }
        return builder.build();
    }

    private StønadsstatistikkVedtak.Saksrolle mapBrukerRolle(RelasjonsRolleType relasjonsRolleType) {
        return switch (relasjonsRolleType) {
            case FARA -> StønadsstatistikkVedtak.Saksrolle.FAR;
            case MORA -> StønadsstatistikkVedtak.Saksrolle.MOR;
            case MEDMOR -> StønadsstatistikkVedtak.Saksrolle.MEDMOR;
            case UDEFINERT -> StønadsstatistikkVedtak.Saksrolle.UKJENT;
            case EKTE, REGISTRERT_PARTNER, BARN, ANNEN_PART_FRA_SØKNAD -> throw new IllegalStateException("Unexpected value: " + relasjonsRolleType.getKode());
        };
    }

    private Beregning utledBeregning(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return null;
        }
        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandling.getId());
        var skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();

        var bruttoÅrsinntekt = beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .filter(p -> p.getPeriode().inkluderer(skjæringstidspunkt))
            .findFirst()
            .map(BeregningsgrunnlagPeriode::getBruttoPrÅr)
            .orElseThrow();

        var næringOrgNr = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling.getId())
            .flatMap(InntektArbeidYtelseGrunnlag::getOppgittOpptjening)
            .map(OppgittOpptjening::getEgenNæring)
            .orElse(List.of())
            .stream()
            .map(OppgittEgenNæring::getOrgnr)
            .collect(Collectors.toSet());
        return new Beregning(bruttoÅrsinntekt, næringOrgNr);
    }

    private Long utledTilkjentEngangsstønad(Long behandlingId) {
        return legacyESBeregningRepository.getSisteBeregning(behandlingId).map(LegacyESBeregning::getBeregnetTilkjentYtelse).orElse(null);
    }

    private List<StønadsstatistikkUtbetalingPeriode> mapUtbetalingssperioder(Behandling behandling,
                                                                             YtelseType ytelseType,
                                                                             HendelseType hendelseType) {
        var perioder = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(List.of());
        return StønadsstatistikkUtbetalingPeriodeMapper.mapTilkjent(ytelseType, hendelseType, perioder);
    }

    private List<StønadsstatistikkUttakPeriode> mapForeldrepengerUttaksperioder(Behandling behandling, RettighetType rettighetType) {
        return foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId())
            .map(u -> StønadsstatistikkUttakPeriodeMapper.mapUttak(behandling.getRelasjonsRolleType(), rettighetType, u.getGjeldendePerioder()))
            .orElse(List.of());
    }

    private FamilieHendelse mapFamilieHendelse(BehandlingReferanse behandling, FamilieHendelseEntitet familiehendelse) {
        var termindato = familiehendelse.getTermindato().orElse(null);
        var adopsjonsdato = familiehendelse.getGjelderAdopsjon() ? familiehendelse.getAdopsjon()
            .map(AdopsjonEntitet::getOmsorgsovertakelseDato)
            .orElse(null) : null;
        var antallBarn = familiehendelse.getAntallBarn();
        var identifiserteBarn = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(behandling)
            .map(PersonopplysningerAggregat::getBarna)
            .orElse(List.of());
        var barn = identifiserteBarn.size() == familiehendelse.getBarna().size() ? hentIdentifiserteBarn(identifiserteBarn) : hentBarn(
            familiehendelse.getBarna());
        var hendelseType = utledFamilieHendelseType(familiehendelse);

        return new FamilieHendelse(termindato, adopsjonsdato, antallBarn, barn, hendelseType);
    }

    private static HendelseType utledFamilieHendelseType(FamilieHendelseEntitet familiehendelse) {
        if (familiehendelse.getGjelderFødsel()) {
            return HendelseType.FØDSEL;
        }
        return familiehendelse.getType().equals(FamilieHendelseType.ADOPSJON) ? HendelseType.ADOPSJON : HendelseType.OMSORGSOVERTAKELSE;
    }

    private static List<FamilieHendelse.Barn> hentBarn(List<UidentifisertBarn> barna) {
        return barna.stream().map(b -> new FamilieHendelse.Barn(null, b.getFødselsdato(), b.getDødsdato().orElse(null))).toList();
    }

    private static List<FamilieHendelse.Barn> hentIdentifiserteBarn(List<PersonopplysningEntitet> identifiserteBarn) {
        return identifiserteBarn.stream()
            .map(b -> new FamilieHendelse.Barn(mapAktørId(b.getAktørId()), b.getFødselsdato(), b.getDødsdato()))
            .toList();
    }

    private AnnenForelder utledAnnenForelder(Behandling behandling, FamilieHendelseEntitet familiehendelse) {
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        return fagsakRelasjon.flatMap(fr -> fr.getRelatertFagsak(behandling.getFagsak()))
            .map(relatert -> new AnnenForelder(mapAktørId(relatert.getAktørId()), mapSaksnummer(relatert.getSaksnummer()), mapBrukerRolle(relatert.getRelasjonsRolleType())))
            .or(() -> personopplysningTjeneste.hentOppgittAnnenPart(behandling.getId()).map(OppgittAnnenPartEntitet::getAktørId).map(a -> {
                var annenPartsEngangsstønadSak = finnEngangsstønadSak(a, familiehendelse);
                var annenPartsEngangsstønadSaksnummer = annenPartsEngangsstønadSak.map(snr -> mapSaksnummer(snr.getSaksnummer())).orElse(null);
                var annenPartsEngangsstønadSaksrolle = annenPartsEngangsstønadSak.map(Fagsak::getRelasjonsRolleType).map(this::mapBrukerRolle).orElse(null);
                return new AnnenForelder(mapAktørId(a), annenPartsEngangsstønadSaksnummer, annenPartsEngangsstønadSaksrolle);
            }))
            .orElse(null);

    }

    private Optional<Fagsak> finnEngangsstønadSak(AktørId aktørId, FamilieHendelseEntitet familieHendelse) {
        return fagsakRepository.hentForBruker(aktørId)
            .stream()
            .filter(f -> FagsakYtelseType.ENGANGSTØNAD.equals(f.getYtelseType()))
            .filter(f -> behandlingVedtakRepository.hentGjeldendeVedtak(f).isPresent())
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .filter(b -> matcherFamiliehendelseMedSak(familieHendelse, b))
            .findFirst()
            .map(Behandling::getFagsak);
    }

    private boolean matcherFamiliehendelseMedSak(FamilieHendelseEntitet familieHendelse, Behandling behandling) {
        var fhDato = familieHendelse.getSkjæringstidspunkt();
        var egetIntervall = new LocalDateInterval(fhDato.minus(INTERVALL_SAMME_BARN), fhDato.plus(INTERVALL_SAMME_BARN));
        var annenpartIntervall = familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .map(d -> new LocalDateInterval(d.minus(INTERVALL_SAMME_BARN), d.plus(INTERVALL_SAMME_BARN)));

        return annenpartIntervall.filter(i -> i.overlaps(egetIntervall)).isPresent();
    }

    private UtlandsTilsnitt utledUtlandsTilsnitt(FagsakMarkering fagsakMarkering) {
        return switch (fagsakMarkering) {
            case NASJONAL -> UtlandsTilsnitt.NASJONAL;
            case EØS_BOSATT_NORGE -> UtlandsTilsnitt.EØS_BOSATT_NORGE;
            case BOSATT_UTLAND -> UtlandsTilsnitt.BOSATT_UTLAND;
            case SAMMENSATT_KONTROLL, DØD_DØDFØDSEL, SELVSTENDIG_NÆRING -> null;
        };
    }

    private VilkårIkkeOppfylt utledVilkårIkkeOppfylt(BehandlingVedtak vedtak, Behandling behandling) {
        var vilkårIkkeOppfylt = Optional.ofNullable(vedtak.getBehandlingsresultat().getVilkårResultat())
            .map(VilkårResultat::getVilkårene)
            .orElse(List.of())
            .stream()
            .filter(v -> VilkårUtfallType.IKKE_OPPFYLT.equals(v.getGjeldendeVilkårUtfall()))
            .map(Vilkår::getVilkårType)
            .collect(Collectors.toSet());
        return BehandlingVedtakDvhMapper.mapVilkårIkkeOppfylt(vedtak.getVedtakResultatType(), behandling.getFagsakYtelseType(), vilkårIkkeOppfylt);
    }

    private VedtakResultat mapVedtaksresultat(BehandlingVedtak vedtak) {
        return switch (vedtak.getVedtakResultatType()) {

            case INNVILGET -> VedtakResultat.INNVILGET;
            case AVSLAG -> VedtakResultat.AVSLAG;
            case OPPHØR -> VedtakResultat.OPPHØR;
            case VEDTAK_I_KLAGEBEHANDLING, VEDTAK_I_ANKEBEHANDLING, VEDTAK_I_INNSYNBEHANDLING, UDEFINERT ->
                throw new IllegalStateException("Unexpected value: " + vedtak.getVedtakResultatType());
        };
    }

    private LovVersjon utledLovVersjon(Skjæringstidspunkt stp) {
        if (stp.utenMinsterett()) {
            return stp.kreverSammenhengendeUttak() ? LovVersjon.FØRSTE_FPSAK : LovVersjon.FRI_UTSETTELSE;
        }
        return LovVersjon.MINSTERETT_22;
    }

    private ForeldrepengerRettigheter utledRettigheter(Behandling behandling) {
        var fagsak = fagsakTjeneste.finnEksaktFagsak(behandling.getFagsakId());
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(fagsak);

        var gjeldendeStønadskontoberegning = fagsakRelasjon.getGjeldendeStønadskontoberegning();
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var saldoUtregning = stønadskontoSaldoTjeneste.finnSaldoUtregning(uttakInput);
        var konti = gjeldendeStønadskontoberegning.stream()
            .flatMap(b -> b.getStønadskontoer().stream())
            .filter(sk -> sk.getStønadskontoType() != StønadskontoType.FLERBARNSDAGER)
            .map(k -> map(k, saldoUtregning))
            .collect(Collectors.toSet());


        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var rettighetType = utledRettighetType(yfa, konti);
        var flerbarnsdager = gjeldendeStønadskontoberegning.stream()
            .flatMap(b -> b.getStønadskontoer().stream())
            .filter(sk -> sk.getStønadskontoType() == StønadskontoType.FLERBARNSDAGER)
            .findFirst()
            .map(sk -> new ForeldrepengerRettigheter.Trekkdager(sk.getMaxDager()))
            .orElse(null);

        var dekningsgrad = map(fagsakRelasjon.getDekningsgrad());
        return new ForeldrepengerRettigheter(dekningsgrad, rettighetType, konti, flerbarnsdager);
    }

    private static RettighetType utledRettighetType(YtelseFordelingAggregat yfa, Set<ForeldrepengerRettigheter.Stønadskonto> konti) {
        if (konti.stream().anyMatch(k -> k.type().equals(StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER))) {
            return UttakOmsorgUtil.harAleneomsorg(yfa) ? RettighetType.ALENEOMSORG : RettighetType.BARE_SØKER_RETT;
        }
        return UttakOmsorgUtil.avklartAnnenForelderHarRettEØS(yfa) ? RettighetType.BEGGE_RETT_EØS : RettighetType.BEGGE_RETT;
    }

    private StønadsstatistikkVedtak.Dekningsgrad map(Dekningsgrad dekningsgrad) {
        return switch (dekningsgrad.getVerdi()) {
            case 80 -> StønadsstatistikkVedtak.Dekningsgrad.ÅTTI;
            case 100 -> StønadsstatistikkVedtak.Dekningsgrad.HUNDRE;
            default -> throw new IllegalStateException("Unexpected value: " + dekningsgrad.getVerdi());
        };
    }

    private static ForeldrepengerRettigheter.Stønadskonto map(Stønadskonto stønadskonto, SaldoUtregning saldoUtregning) {
        var minsterett = StønadskontoType.FORELDREPENGER.equals(stønadskonto.getStønadskontoType()) ? saldoUtregning.getMaxDagerMinsterett()
            .add(saldoUtregning.getMaxDagerUtenAktivitetskrav())
            .rundOpp() : 0;
        var stønadskontoType = map(stønadskonto.getStønadskontoType());
        var maksdager = map(stønadskonto.getMaxDager());
        var restdager = saldoUtregning.saldoITrekkdager(switch (stønadskonto.getStønadskontoType()) {
            case FELLESPERIODE -> Stønadskontotype.FELLESPERIODE;
            case MØDREKVOTE -> Stønadskontotype.MØDREKVOTE;
            case FEDREKVOTE -> Stønadskontotype.FEDREKVOTE;
            case FORELDREPENGER -> Stønadskontotype.FORELDREPENGER;
            case FLERBARNSDAGER, UDEFINERT -> throw new IllegalStateException("Ukjent " + stønadskonto.getStønadskontoType());
            case FORELDREPENGER_FØR_FØDSEL -> Stønadskontotype.FORELDREPENGER_FØR_FØDSEL;
        }).decimalValue();
        return new ForeldrepengerRettigheter.Stønadskonto(stønadskontoType, maksdager, new ForeldrepengerRettigheter.Trekkdager(restdager),
            new ForeldrepengerRettigheter.Trekkdager(minsterett));
    }

    private static ForeldrepengerRettigheter.Trekkdager map(int maxDager) {
        return new ForeldrepengerRettigheter.Trekkdager(maxDager);
    }

    private static StønadsstatistikkVedtak.StønadskontoType map(StønadskontoType stønadskontoType) {
        return switch (stønadskontoType) {
            case FELLESPERIODE -> StønadsstatistikkVedtak.StønadskontoType.FELLESPERIODE;
            case MØDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.MØDREKVOTE;
            case FEDREKVOTE -> StønadsstatistikkVedtak.StønadskontoType.FEDREKVOTE;
            case FORELDREPENGER -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> StønadsstatistikkVedtak.StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            case FLERBARNSDAGER, UDEFINERT -> throw new IllegalStateException("Unexpected value: " + stønadskontoType);
        };
    }

    private static StønadsstatistikkVedtak.AktørId mapAktørId(AktørId aktørId) {
        return new StønadsstatistikkVedtak.AktørId(aktørId.getId());
    }

    private static YtelseType mapYtelseType(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> YtelseType.ENGANGSSTØNAD;
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + ytelseType);
        };
    }

    private static StønadsstatistikkVedtak.Saksnummer mapSaksnummer(Saksnummer saksnummer) {
        return new StønadsstatistikkVedtak.Saksnummer(saksnummer.getVerdi());
    }
}
