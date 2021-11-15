package no.nav.foreldrepenger.behandlingslager.testutilities.behandling;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerRepository;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling.Builder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapBehandlingsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRegistrertEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personstatus;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Statsborgerskap;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

/**
 * Default test scenario builder for å definere opp testdata med enkle defaults.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt
 * innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne klassen.
 */
public abstract class AbstractTestScenario<S extends AbstractTestScenario<S>> {

    public static final String ADOPSJON = "adopsjon";
    public static final String FØDSEL = "fødsel";
    public static final String TERMINBEKREFTELSE = "terminbekreftelse";
    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private final Map<Long, PersonopplysningGrunnlagEntitet> personopplysningMap = new IdentityHashMap<>();
    private final Map<Long, FamilieHendelseGrunnlagEntitet> familieHendelseAggregatMap = new IdentityHashMap<>();
    private final Map<Long, MedlemskapBehandlingsgrunnlagEntitet> medlemskapgrunnlag = new HashMap<>();
    private final Map<Long, Behandling> behandlingMap = new HashMap<>();
    private ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
    private ArgumentCaptor<Fagsak> fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
    private Behandling behandling;

    private Behandlingsresultat.Builder behandlingresultatBuilder;

    private Fagsak fagsak;
    private SøknadEntitet.Builder søknadBuilder;

    private OppgittAnnenPartBuilder oppgittAnnenPartBuilder;
    private VurdertMedlemskapBuilder vurdertMedlemskapBuilder;
    private BehandlingVedtak.Builder behandlingVedtakBuilder;
    private MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder;
    private BehandlingStegType startSteg;

    private Map<AksjonspunktDefinisjon, BehandlingStegType> aksjonspunktDefinisjoner = new HashMap<>();
    private VilkårResultatType vilkårResultatType = VilkårResultatType.IKKE_FASTSATT;
    private Map<VilkårType, VilkårUtfallType> vilkårTyper = new HashMap<>();
    private List<MedlemskapPerioderEntitet> medlemskapPerioder = new ArrayList<>();
    private Long fagsakId = nyId();
    private LocalDate behandlingstidFrist;
    private LocalDateTime opplysningerOppdatertTidspunkt;
    private String behandlendeEnhet;
    private BehandlingRepository mockBehandlingRepository;
    private FamilieHendelseBuilder søknadHendelseBuilder;
    private FamilieHendelseBuilder bekreftetHendelseBuilder;
    private FamilieHendelseBuilder overstyrtHendelseBuilder;
    private BehandlingVedtak behandlingVedtak;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;
    private OppgittRettighetEntitet oppgittRettighet;
    private OppgittDekningsgradEntitet oppgittDekningsgrad;
    private OppgittFordelingEntitet oppgittFordeling;
    private OppgittFordelingEntitet justertFordeling;
    private AvklarteUttakDatoerEntitet avklarteUttakDatoer;

    // Registret og overstyrt personinfo
    private List<PersonInformasjon> personer;

    private Behandling originalBehandling;
    private List<BehandlingÅrsakType> behandlingÅrsakTyper;
    private BehandlingRepositoryProvider repositoryProvider;
    private PerioderUtenOmsorgEntitet perioderUtenOmsorg;
    private PerioderAleneOmsorgEntitet perioderMedAleneomsorg;
    private PersonInformasjon.Builder personInformasjonBuilder;
    private UttakResultatPerioderEntitet uttak;
    private boolean manueltOpprettet;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBrukerKjønn kjønn) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBrukerKjønn(kjønn);
    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBrukerKjønn kjønn, AktørId aktørId) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBruker(new NavBrukerBuilder().medAktørId(aktørId).medKjønn(kjønn).build());
    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType brukerRolle,
            NavBruker navBruker) {
        this.fagsakBuilder = FagsakBuilder
                .nyFagsak(fagsakYtelseType, brukerRolle)
                .medSaksnummer(new Saksnummer(nyId() + ""))
                .medBruker(navBruker);
    }

    static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    private BehandlingRepository lagBasicMockBehandlingRepository(BehandlingRepositoryProvider repositoryProvider) {
        var behandlingRepository = mock(BehandlingRepository.class);

        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        var mockFagsakRepository = mockFagsakRepository();
        var mockPersonopplysningRepository = lagMockPersonopplysningRepository();
        var mockMedlemskapRepository = lagMockMedlemskapRepository();
        var familieHendelseRepository = mockFamilieHendelseGrunnlagRepository();
        var søknadRepository = mockSøknadRepository();
        var fagsakLåsRepository = mockFagsakLåsRepository();
        var fagsakRelasjonRepositoryMock = mockFagsakRelasjonRepository();
        var resultatRepository = mockBehandlingresultatRepository();
        var opptjeningRepository = mockOpptjeningRepository();

        var behandlingLåsReposiory = mockBehandlingLåsRepository();

        var behandlingVedtakRepository = mockBehandlingVedtakRepository();
        // ikke ideelt å la mocks returnere mocks, men forenkler enormt mye test kode,
        // forhindrer feil oppsett, så det
        // blir enklere å refactorere

        lenient().when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        lenient().when(repositoryProvider.getFagsakRepository()).thenReturn(mockFagsakRepository);
        lenient().when(repositoryProvider.getPersonopplysningRepository()).thenReturn(mockPersonopplysningRepository);
        lenient().when(repositoryProvider.getFamilieHendelseRepository()).thenReturn(familieHendelseRepository);
        lenient().when(repositoryProvider.getMedlemskapRepository()).thenReturn(mockMedlemskapRepository);
        lenient().when(repositoryProvider.getPleiepengerRepository()).thenReturn(mock(PleiepengerRepository.class));
        lenient().when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        lenient().when(repositoryProvider.getBehandlingVedtakRepository()).thenReturn(behandlingVedtakRepository);
        lenient().when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        lenient().when(repositoryProvider.getBehandlingLåsRepository()).thenReturn(behandlingLåsReposiory);
        lenient().when(repositoryProvider.getFagsakRelasjonRepository()).thenReturn(fagsakRelasjonRepositoryMock);
        lenient().when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(resultatRepository);
        lenient().when(repositoryProvider.getOpptjeningRepository()).thenReturn(opptjeningRepository);

        return behandlingRepository;
    }

    private OpptjeningRepository mockOpptjeningRepository() {
        return mock(OpptjeningRepository.class);
    }

    private BehandlingsresultatRepository mockBehandlingresultatRepository() {
        return new BehandlingsresultatRepository() {
            @Override
            public Optional<Behandlingsresultat> hentHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(mockBehandlingRepository.hentBehandling(behandlingId).getBehandlingsresultat());
            }

            @Override
            public Behandlingsresultat hent(Long behandlingId) {
                return hentHvisEksisterer(behandlingId).orElseThrow();
            }
        };
    }

    private BehandlingLåsRepository mockBehandlingLåsRepository() {
        return new BehandlingLåsRepository() {

            @Override
            public BehandlingLås taLås(Long behandlingId) {
                return new BehandlingLås(behandlingId);
            }

            @Override
            public void oppdaterLåsVersjon(BehandlingLås behandlingLås) {
            }

            @Override
            public BehandlingLås taLås(UUID eksternBehandlingRef) {
                return null;
            }
        };
    }

    private FagsakLåsRepository mockFagsakLåsRepository() {
        return new FagsakLåsRepository() {
            @Override
            public FagsakLås taLås(Long fagsakId) {
                return new FagsakLås(fagsakId) {

                };
            }

            @Override
            public FagsakLås taLås(Fagsak fagsak) {
                return new FagsakLås(fagsak.getId()) {

                };
            }

            @Override
            public void oppdaterLåsVersjon(FagsakLås fagsakLås) {

            }
        };
    }

    private FagsakRelasjonRepository mockFagsakRelasjonRepository() {
        return new FagsakRelasjonRepositoryStub();
    }

    private BehandlingVedtakRepository mockBehandlingVedtakRepository() {
        var behandlingVedtakRepository = mock(BehandlingVedtakRepository.class);
        var behandlingVedtak = mockBehandlingVedtak();
        lenient().when(behandlingVedtakRepository.hentForBehandlingHvisEksisterer(Mockito.any())).thenReturn(Optional.of(behandlingVedtak));

        return behandlingVedtakRepository;
    }

    public BehandlingVedtak mockBehandlingVedtak() {
        if (behandlingVedtak == null) {
            behandlingVedtak = Mockito.mock(BehandlingVedtak.class);
        }
        return behandlingVedtak;
    }

    private SøknadRepository mockSøknadRepository() {
        return new SøknadRepository() {

            private SøknadEntitet søknad;

            @Override
            public Optional<SøknadEntitet> hentSøknadHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(søknad);
            }

            @Override
            public SøknadEntitet hentFørstegangsSøknad(Behandling behandling) {
                return søknad;
            }

            @Override
            public SøknadEntitet hentSøknad(Long behandlingId) {
                return søknad;
            }

            @Override
            public void lagreOgFlush(Behandling behandling, SøknadEntitet søknad1) {
                this.søknad = søknad1;
            }

        };
    }

    public OppgittFordelingEntitet mockOppgittFordeling() {
        return new OppgittFordelingEntitet() {
            @Override
            public List<OppgittPeriodeEntitet> getOppgittePerioder() {
                return Collections.singletonList(OppgittPeriodeBuilder.ny()
                        .medPeriode(LocalDate.now(), LocalDate.now().plusWeeks(6))
                        .medPeriodeType(UttakPeriodeType.MØDREKVOTE).build());
            }

            @Override
            public boolean getErAnnenForelderInformert() {
                return false;
            }
        };
    }

    private FamilieHendelseRepository mockFamilieHendelseGrunnlagRepository() {
        return new FamilieHendelseRepository() {
            @Override
            public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
                return familieHendelseAggregatMap.get(behandlingId);
            }

            @Override
            public Optional<FamilieHendelseGrunnlagEntitet> hentAggregatHvisEksisterer(Long behandlingId) {
                return familieHendelseAggregatMap.entrySet().stream().filter(e -> Objects.equals(behandlingId, e.getKey())).map(e -> e.getValue())
                        .findFirst();
            }

            @Override
            public void lagre(Behandling behandling, FamilieHendelseBuilder hendelseBuilder) {
                var behandlingId = behandling.getId();
                var kladd = hentAggregatHvisEksisterer(behandlingId);
                var builder = FamilieHendelseGrunnlagBuilder.oppdatere(kladd);
                var type = utledTypeFor(kladd);
                switch (type) {
                    case SØKNAD -> builder.medSøknadVersjon(hendelseBuilder);
                    case BEKREFTET -> builder.medBekreftetVersjon(hendelseBuilder);
                    case OVERSTYRT -> builder.medOverstyrtVersjon(hendelseBuilder);
                    default -> throw new IllegalArgumentException("Støtter ikke HendelseVersjonType: " + type);
                }
                familieHendelseAggregatMap.remove(behandlingId);
                familieHendelseAggregatMap.put(behandlingId, builder.build());
            }

            @Override
            public void lagreRegisterHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
                final var kladd = hentAggregatHvisEksisterer(behandling.getId());
                final var aggregatBuilder = FamilieHendelseGrunnlagBuilder.oppdatere(kladd);
                aggregatBuilder.medBekreftetVersjon(hendelse);
                try {
                    var m = FamilieHendelseGrunnlagBuilder.class.getDeclaredMethod("getKladd");
                    m.setAccessible(true);
                    final var invoke = (FamilieHendelseGrunnlagEntitet) m.invoke(aggregatBuilder);
                    if (harOverstyrtTerminOgOvergangTilFødsel(invoke)) {
                        aggregatBuilder.medOverstyrtVersjon(null);
                    }
                    var id = behandling.getId();
                    familieHendelseAggregatMap.remove(id);
                    familieHendelseAggregatMap.put(id, aggregatBuilder.build());
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }

            private boolean harOverstyrtTerminOgOvergangTilFødsel(FamilieHendelseGrunnlagEntitet kladd) {
                return kladd.getHarOverstyrteData() && kladd.getOverstyrtVersjon()
                        .map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT).equals(FamilieHendelseType.TERMIN)
                        && kladd.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT)
                                .equals(FamilieHendelseType.FØDSEL);
            }

            @Override
            public void lagreOverstyrtHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
                final var kladd = hentAggregatHvisEksisterer(behandling.getId());
                final var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(kladd);
                oppdatere.medOverstyrtVersjon(hendelse);
                familieHendelseAggregatMap.remove(behandling.getId());
                familieHendelseAggregatMap.put(behandling.getId(), oppdatere.build());
            }

            @Override
            public void slettAvklarteData(Long behandlingId, BehandlingLås lås) {
                throw new UnsupportedOperationException("Ikke implementert");
            }

            @Override
            public Optional<Long> hentIdPåAktivFamiliehendelse(Long behandlingId) {
                throw new UnsupportedOperationException("Ikke implementert");
            }

            @Override
            public FamilieHendelseGrunnlagEntitet hentGrunnlagPåId(Long aggregatId) {
                throw new UnsupportedOperationException("Ikke implementert");
            }

            @Override
            public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
                final var familieHendelseAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
                final var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);

                var id = nyBehandlingId;
                familieHendelseAggregatMap.remove(id);
                familieHendelseAggregatMap.put(id, oppdatere.build());
            }

            @Override
            public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long gammelBehandlingId, Long nyBehandlingId) {
                final var familieHendelseAggregat = hentAggregatHvisEksisterer(gammelBehandlingId);
                final var oppdatere = FamilieHendelseGrunnlagBuilder.oppdatere(familieHendelseAggregat);
                oppdatere.medOverstyrtVersjon(null);
                oppdatere.medBekreftetVersjon(null);

                var id = nyBehandlingId;
                familieHendelseAggregatMap.remove(id);
                familieHendelseAggregatMap.put(id, oppdatere.build());
            }

            @Override
            public FamilieHendelseBuilder opprettBuilderFor(Behandling behandling) {
                final var aggregatBuilder = FamilieHendelseGrunnlagBuilder
                        .oppdatere(hentAggregatHvisEksisterer(behandling.getId()));
                return opprettBuilderFor(aggregatBuilder);
            }

            FamilieHendelseBuilder opprettBuilderFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat) {
                var type = utledTypeFor(aggregat);

                if (type.equals(HendelseVersjonType.SØKNAD)) {
                    return FamilieHendelseBuilder.oppdatere(aggregat.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon), type);
                }
                if (type.equals(HendelseVersjonType.BEKREFTET)) {
                    return FamilieHendelseBuilder.oppdatere(aggregat.flatMap(FamilieHendelseGrunnlagEntitet::getBekreftetVersjon), type);
                }
                return FamilieHendelseBuilder.oppdatere(aggregat.flatMap(FamilieHendelseGrunnlagEntitet::getOverstyrtVersjon), type);
            }

            private HendelseVersjonType utledTypeFor(Optional<FamilieHendelseGrunnlagEntitet> aggregat) {
                if (aggregat.isPresent()) {
                    if (aggregat.get().getHarOverstyrteData()) {
                        return HendelseVersjonType.OVERSTYRT;
                    }
                    if (aggregat.get().getHarBekreftedeData() || aggregat.get().getSøknadVersjon() != null) {
                        return HendelseVersjonType.BEKREFTET;
                    }
                    if (aggregat.get().getSøknadVersjon() == null) {
                        return HendelseVersjonType.SØKNAD;
                    }
                    throw new IllegalStateException();
                }
                return HendelseVersjonType.SØKNAD;
            }

            private FamilieHendelseBuilder opprettBuilderFor(FamilieHendelseGrunnlagBuilder aggregatBuilder) {
                try {
                    var m = FamilieHendelseGrunnlagBuilder.class.getDeclaredMethod("getKladd");
                    m.setAccessible(true);
                    return opprettBuilderFor(Optional.ofNullable((FamilieHendelseGrunnlagEntitet) m.invoke(aggregatBuilder)));
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    /**
     * Hjelpe metode for å håndtere mock repository.
     */
    public BehandlingRepository mockBehandlingRepository() {
        if (mockBehandlingRepository != null) {
            return mockBehandlingRepository;
        }
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        var behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        lenient().when(behandlingRepository.hentBehandling(Mockito.any(Long.class))).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return behandlingMap.getOrDefault(id, null);
        });
        lenient().when(behandlingRepository.hentBehandling(Mockito.any(UUID.class))).thenAnswer(a -> behandlingMap.entrySet().stream().filter(e -> {
            UUID uuid = a.getArgument(0);
            return Objects.equals(e.getValue().getUuid(), uuid);
        }).findFirst().map(e -> e.getValue()).orElseThrow());
        lenient().when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(Mockito.any())).thenAnswer(a -> {
            return List.copyOf(behandlingMap.values());
        });
        lenient().when(behandlingRepository.finnUnikBehandlingForBehandlingId(Mockito.any())).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return Optional.ofNullable(behandlingMap.getOrDefault(id, null));
        });
        lenient().when(behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(Mockito.any(), Mockito.any(BehandlingType.class)))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    BehandlingType type = a.getArgument(1);
                    return behandlingMap.values().stream().filter(b -> type.equals(b.getType()) && b.getFagsakId().equals(id)).sorted().findFirst();
                });
        lenient().when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(Mockito.any()))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    return behandlingMap.values().stream().filter(b -> BehandlingType.getYtelseBehandlingTyper().contains(b.getType()))
                            .filter(b -> b.getFagsakId().equals(id)).sorted().findFirst();
                });
        lenient().when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(Mockito.any()))
                .thenAnswer(a -> {
                    Long id = a.getArgument(0);
                    return behandlingMap.values().stream()
                            .filter(b -> b.getFagsakId().equals(id) && b.getBehandlingsresultat() != null
                                    && !b.getBehandlingsresultat().isBehandlingHenlagt())
                            .sorted()
                            .findFirst();
                });

        lenient().when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId()) {
            };
        });

        lenient().when(behandlingRepository.hentSistOppdatertTidspunkt(Mockito.any()))
                .thenAnswer(a -> Optional.ofNullable(opplysningerOppdatertTidspunkt));

        lenient().when(behandlingRepository.lagre(behandlingCaptor.capture(), Mockito.any()))
                .thenAnswer((Answer<Long>) invocation -> {
                    Behandling beh = invocation.getArgument(0);
                    var id = beh.getId();
                    if (id == null) {
                        id = nyId();
                        beh.setId(id);
                    }

                    beh.getAksjonspunkter().forEach(punkt -> punkt.setId((nyId())));
                    behandlingMap.put(id, beh);
                    return id;
                });

        mockBehandlingRepository = behandlingRepository;
        return behandlingRepository;
    }

    public BehandlingRepositoryProvider mockBehandlingRepositoryProvider() {
        mockBehandlingRepository();
        return repositoryProvider;
    }

    private MedlemskapRepository lagMockMedlemskapRepository() {
        var dummy = new MedlemskapRepository(null) {
            @Override
            public void lagreOgFlush(Long behandlingId, Optional<MedlemskapBehandlingsgrunnlagEntitet> eksisterendeGrunnlag,
                    MedlemskapBehandlingsgrunnlagEntitet nyttGrunnlag) {
                assert behandlingId != null : "behandlingId er null!";
                medlemskapgrunnlag.put(behandlingId, nyttGrunnlag);
            }

            @Override
            public void lagreMedlemskapRegistrert(MedlemskapRegistrertEntitet ny) {
                // ignore, tracker kun grunnlag for mock
            }

            @Override
            public void lagreOppgittTilknytning(MedlemskapOppgittTilknytningEntitet ny) {
                // ignore, tracker kun grunnlag for mock
            }

            @Override
            public void lagreVurdertMedlemskap(VurdertMedlemskapEntitet ny) {
                // ignore, tracker kun grunnlag for mock
            }

            @Override
            protected BehandlingLås taLås(Long behandlingId) {
                return null;
            }

            @Override
            protected void oppdaterLås(BehandlingLås lås) {
                // NO-OP i mock
            }

            @Override
            public void slettAvklarteMedlemskapsdata(Long behandlingId, BehandlingLås lås) {
                // NO-OP i mock
            }

            @Override
            protected Optional<MedlemskapBehandlingsgrunnlagEntitet> getAktivtBehandlingsgrunnlag(Long behandlingId) {
                assert behandlingId != null : "behandlingId er null!";
                return Optional.ofNullable(medlemskapgrunnlag.get(behandlingId));
            }
        };
        return Mockito.spy(dummy);
    }

    private PersonopplysningRepository lagMockPersonopplysningRepository() {
        return new MockPersonopplysningRepository();
    }

    public FagsakRepository mockFagsakRepository() {
        var fagsakRepository = mock(FagsakRepository.class);
        lenient().when(fagsakRepository.finnEksaktFagsak(Mockito.anyLong())).thenAnswer(a -> fagsak);
        lenient().when(fagsakRepository.finnUnikFagsak(Mockito.anyLong())).thenAnswer(a -> Optional.of(fagsak));
        lenient().when(fagsakRepository.hentSakGittSaksnummer(Mockito.any(Saksnummer.class))).thenAnswer(a -> Optional.of(fagsak));
        lenient().when(fagsakRepository.hentForBruker(Mockito.any(AktørId.class))).thenAnswer(a -> singletonList(fagsak));
        lenient().when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fagsak = invocation.getArgument(0); // NOSONAR
            var id = fagsak.getId();
            if (id == null) {
                id = fagsakId;
                fagsak.setId(id);
                // Whitebox.setInternalState(fagsak, "id", id);
            }
            return id;
        });

        // oppdater fagsakstatus
        Mockito.lenient().doAnswer(invocation -> {
            FagsakStatus status = invocation.getArgument(1);
            fagsak.setStatus(status);
            // Whitebox.setInternalState(fagsak, "fagsakStatus", status);
            return null;
        }).when(fagsakRepository)
                .oppdaterFagsakStatus(eq(fagsakId), Mockito.any(FagsakStatus.class));

        return fagsakRepository;
    }

    public Fagsak lagreFagsak(BehandlingRepositoryProvider repositoryProvider) {
        lagFagsak(repositoryProvider.getFagsakRepository());
        return fagsak;
    }

    public Behandling lagre(BehandlingRepositoryProvider repositoryProvider) {
        build(repositoryProvider);
        return behandling;
    }

    BehandlingRepository lagMockedRepositoryForOpprettingAvBehandlingInternt() {
        if (mockBehandlingRepository != null && behandling != null) {
            return mockBehandlingRepository;
        }
        validerTilstandVedMocking();

        mockBehandlingRepository = mockBehandlingRepository();

        lagre(repositoryProvider);
        return mockBehandlingRepository;
    }

    public Behandling lagMocked() {
        lagMockedRepositoryForOpprettingAvBehandlingInternt();
        return behandling;
    }

    protected void buildAvsluttet(BehandlingRepositoryProvider repositoryProvider) {
        var behandlingBuilder = grunnBuild(repositoryProvider);

        behandling = behandlingBuilder.medAvsluttetDato(LocalDateTime.now()).build();
        var behandlingRepo = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        lagrePersonopplysning(repositoryProvider, behandling);
        behandling.setStatus(BehandlingStatus.AVSLUTTET);
        // Whitebox.setInternalState(behandling, "status", BehandlingStatus.AVSLUTTET);

        var builder = Behandlingsresultat.builder();

        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);
        lagreUttak(repositoryProvider.getFpUttakRepository());
        builder.medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
                .medAvslagsårsak(Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR).buildFor(behandling);

        behandlingRepo.lagre(behandling, lås);
    }

    private void lagrePersonopplysning(BehandlingRepositoryProvider repositoryProvider, Behandling behandling) {
        var personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        var behandlingId = behandling.getId();
        if (oppgittAnnenPartBuilder != null) {
            personopplysningRepository.lagre(behandlingId, oppgittAnnenPartBuilder.build());
        }

        if (personer != null && !personer.isEmpty()) {
            personer.stream().filter(e -> e.getType().equals(PersonopplysningVersjonType.REGISTRERT))
                    .findFirst().ifPresent(e -> lagrePersoninfo(behandling, e, personopplysningRepository));

            personer.stream().filter(a -> a.getType().equals(PersonopplysningVersjonType.OVERSTYRT))
                    .findFirst().ifPresent(b -> {
                        throw new IllegalArgumentException("Overstyrt personinfo er kun legacy");
                    });

        } else {
            var registerInformasjon = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT)
                    .leggTilPersonopplysninger(
                            Personopplysning.builder()
                                    .aktørId(behandling.getAktørId())
                                    .navn("Forelder")
                                    .brukerKjønn(getKjønnFraFagsak())
                                    .fødselsdato(LocalDate.now().minusYears(25))
                                    .sivilstand(SivilstandType.UOPPGITT))
                    .leggTilPersonstatus(Personstatus.builder()
                            .personstatus(PersonstatusType.BOSA)
                            .periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
                            .aktørId(behandling.getAktørId()))
                    .leggTilStatsborgerskap(Statsborgerskap.builder()
                        .aktørId(behandling.getAktørId())
                        .statsborgerskap(Landkoder.NOR)
                        .periode(LocalDate.now().minusYears(20), LocalDate.now().plusYears(10)))
                    .build();
            lagrePersoninfo(behandling, registerInformasjon, personopplysningRepository);
        }
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        Objects.nonNull(behandling);
        Objects.nonNull(personInformasjon);

        if (personInformasjon.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            lagreRegisterPersoninfo(behandling, personInformasjon, repository);
        } else {
            throw new IllegalArgumentException("Overstyrt personopplysning er legacy");
        }
    }

    private void lagreRegisterPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderForRegisterdata(behandling.getId()), personInformasjon, repository);
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjonBuilder personInformasjonBuilder, PersonInformasjon personInformasjon,
            PersonopplysningRepository repository) {
        personInformasjon.getPersonopplysninger().forEach(e -> {
            var builder = personInformasjonBuilder.getPersonopplysningBuilder(e.getAktørId());
            builder.medNavn(e.getNavn())
                    .medFødselsdato(e.getFødselsdato())
                    .medDødsdato(e.getDødsdato())
                    .medKjønn(e.getBrukerKjønn())
                    .medSivilstand(e.getSivilstand());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getAdresser().forEach(e -> {
            var builder = personInformasjonBuilder.getAdresseBuilder(e.getAktørId(), e.getPeriode(),
                    e.getAdresseType());
            builder.medAdresselinje1(e.getAdresselinje1())
                    .medAdresselinje2(e.getAdresselinje2())
                    .medAdresselinje3(e.getAdresselinje3())
                    .medAdresselinje4(e.getAdresselinje4())
                    .medLand(e.getLand())
                    .medPostnummer(e.getPostnummer())
                    .medPoststed(e.getPoststed());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getPersonstatuser().forEach(e -> {
            var builder = personInformasjonBuilder.getPersonstatusBuilder(e.getAktørId(), e.getPeriode());
            builder.medPersonstatus(e.getPersonstatus());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getStatsborgerskap().forEach(e -> {
            var builder = personInformasjonBuilder.getStatsborgerskapBuilder(e.getAktørId(),
                    e.getPeriode(),
                    e.getStatsborgerskap());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getRelasjoner().forEach(e -> {
            var builder = personInformasjonBuilder.getRelasjonBuilder(e.getAktørId(), e.getTilAktørId(),
                    e.getRelasjonsrolle());
            builder.harSammeBosted(e.getHarSammeBosted());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getOpphold().forEach(e -> {
            var builder = personInformasjonBuilder.getOppholdstillatelseBuilder(e.getAktørId(), e.getPeriode()).medOppholdstillatelse(e.getTillatelse());
            personInformasjonBuilder.leggTil(builder);
        });

        repository.lagre(behandling.getId(), personInformasjonBuilder);
    }

    protected void validerTilstandVedMocking() {
        if (startSteg != null) {
            throw new IllegalArgumentException(
                    "Kan ikke sette startSteg ved mocking siden dette krever Kodeverk.  Bruk ManipulerInternBehandling til å justere etterpå.");
        }
    }

    @SuppressWarnings("unchecked")
    public S medSøknadDato(LocalDate søknadsdato) {
        medSøknad().medSøknadsdato(søknadsdato);
        return (S) this;
    }

    private void build(BehandlingRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        var behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();

        if (startSteg != null) {
            InternalManipulerBehandling.forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        leggTilAksjonspunkter(behandling);

        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        var behandlingId = behandling.getId();

        opprettHendelseGrunnlag(repositoryProvider);
        lagrePersonopplysning(repositoryProvider, behandling);
        lagreMedlemskapOpplysninger(repositoryProvider, behandlingId);
        lagreYtelseFordelingOpplysninger(repositoryProvider, behandling);
        lagreSøknad(repositoryProvider);
        // opprett og lagre resulater på behandling
        lagreBehandlingsresultatOgVilkårResultat(repositoryProvider, lås);
        lagreUttak(repositoryProvider.getFpUttakRepository());

        if (this.opplysningerOppdatertTidspunkt != null) {
            behandlingRepository.oppdaterSistOppdatertTidspunkt(this.behandling, this.opplysningerOppdatertTidspunkt);
        }

        // få med behandlingsresultat etc.
        behandlingRepository.lagre(behandling, lås);
    }

    private void leggTilAksjonspunkter(Behandling behandling) {
        aksjonspunktDefinisjoner.forEach(
                (apDef, stegType) -> {
                    if (stegType != null) {
                        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, apDef, stegType);
                    } else {
                        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, apDef);
                    }
                });
    }

    private void lagreSøknad(BehandlingRepositoryProvider repositoryProvider) {
        if (søknadBuilder != null) {
            final var søknadRepository = repositoryProvider.getSøknadRepository();
            søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());
        }
    }

    private void lagreMedlemskapOpplysninger(BehandlingRepositoryProvider repositoryProvider, Long behandlingId) {
        repositoryProvider.getMedlemskapRepository().lagreMedlemskapRegisterOpplysninger(behandlingId, medlemskapPerioder);

        var vurdertMedlemskap = medMedlemskap().build();
        repositoryProvider.getMedlemskapRepository().lagreMedlemskapVurdering(behandlingId, vurdertMedlemskap);
        if (oppgittTilknytningBuilder != null) {
            final var oppgittTilknytning = medOppgittTilknytning().build();
            repositoryProvider.getMedlemskapRepository().lagreOppgittTilkytning(behandlingId, oppgittTilknytning);
        }
    }

    private void lagreYtelseFordelingOpplysninger(BehandlingRepositoryProvider repositoryProvider, Behandling behandling) {
        if (oppgittRettighet == null && oppgittDekningsgrad == null && oppgittFordeling == null
            && avklarteUttakDatoer == null && perioderUtenOmsorg == null && perioderMedAleneomsorg == null
            && justertFordeling == null) {
            return;
        }
        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yf = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittRettighet(oppgittRettighet)
            .medOppgittDekningsgrad(oppgittDekningsgrad)
            .medOppgittFordeling(oppgittFordeling)
            .medJustertFordeling(justertFordeling)
            .medAvklarteDatoer(avklarteUttakDatoer)
            .medPerioderUtenOmsorg(perioderUtenOmsorg)
            .medPerioderAleneOmsorg(perioderMedAleneomsorg);
        ytelsesFordelingRepository.lagre(behandling.getId(), yf.build());
    }

    private FamilieHendelseRepository opprettHendelseGrunnlag(BehandlingRepositoryProvider repositoryProvider) {
        final var grunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        grunnlagRepository.lagre(behandling, medSøknadHendelse());
        if (bekreftetHendelseBuilder != null) {
            grunnlagRepository.lagre(behandling, bekreftetHendelseBuilder);
        }
        if (overstyrtHendelseBuilder != null) {
            grunnlagRepository.lagre(behandling, overstyrtHendelseBuilder);
        }
        resetBuilders();
        return grunnlagRepository;
    }

    private void resetBuilders() {
        medSøknadHendelse(null);
        medBekreftetHendelse(null);
        medOverstyrtHendelse(null);
    }

    private Builder grunnBuild(BehandlingRepositoryProvider repositoryProvider) {
        var fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        Builder behandlingBuilder;
        if (originalBehandling == null) {
            behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        } else {
            behandlingBuilder = Behandling.fraTidligereBehandling(originalBehandling, behandlingType)
                    .medBehandlingÅrsak(BehandlingÅrsak.builder(behandlingÅrsakTyper).medManueltOpprettet(manueltOpprettet)
                            .medOriginalBehandlingId(originalBehandling.getId()));
        }

        if (behandlingstidFrist != null) {
            behandlingBuilder.medBehandlingstidFrist(behandlingstidFrist);
        }

        if (behandlendeEnhet != null) {
            behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(behandlendeEnhet, null));
        }

        return behandlingBuilder;

    }

    protected void lagFagsak(FagsakRepository fagsakRepo) {
        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        if (!Mockito.mockingDetails(fagsakRepo).isMock()) {
            final var entityManager = fagsakRepo.getEntityManager();
            if (entityManager != null) {
                var brukerRepository = new NavBrukerRepository(entityManager);
                final var navBruker = brukerRepository.hent(fagsakBuilder.getBrukerBuilder().getAktørId())
                        .orElseGet(() -> NavBruker.opprettNy(fagsakBuilder.getBrukerBuilder().getAktørId(), Språkkode.NB));
                fagsakBuilder.medBruker(navBruker);
            }
        }
        fagsak = fagsakBuilder.build();
        var fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private NavBrukerKjønn getKjønnFraFagsak() {
        return fagsakBuilder.getBrukerBuilder().getKjønn() != null ? fagsakBuilder.getBrukerBuilder().getKjønn()
                : (RelasjonsRolleType.erMor(fagsakBuilder.getRolle()) || RelasjonsRolleType.erMedmor(fagsakBuilder.getRolle()) ? NavBrukerKjønn.KVINNE
                        : NavBrukerKjønn.MANN);
    }

    private void lagreBehandlingsresultatOgVilkårResultat(BehandlingRepositoryProvider repoProvider, BehandlingLås lås) {
        // opprett og lagre behandlingsresultat med VilkårResultat og BehandlingVedtak
        var behandlingsresultat = (behandlingresultatBuilder == null ? Behandlingsresultat.builderForInngangsvilkår()
                : behandlingresultatBuilder).buildFor(behandling);
        behandlingresultatBuilder = null; // resett

        var inngangsvilkårBuilder = VilkårResultat
                .builderFraEksisterende(behandlingsresultat.getVilkårResultat())
                .medVilkårResultatType(vilkårResultatType);

        vilkårTyper.forEach((vilkårType, vilkårUtfallType) -> {
            inngangsvilkårBuilder.leggTilVilkår(vilkårType, vilkårUtfallType, VilkårUtfallType.IKKE_OPPFYLT.equals(vilkårUtfallType) ? VilkårUtfallMerknad.VM_1019 : VilkårUtfallMerknad.UDEFINERT);
        });

        var vilkårResultat = inngangsvilkårBuilder.buildFor(behandling);

        repoProvider.getBehandlingRepository().lagre(vilkårResultat, lås);

        if (behandlingVedtakBuilder != null) {
            // Må lagre Behandling for at Behandlingsresultat ikke skal være transient når
            // BehandlingVedtak blir lagret:
            repoProvider.getBehandlingRepository().lagre(behandling, lås);
            behandlingVedtak = behandlingVedtakBuilder.medBehandlingsresultat(behandlingsresultat).build();
            repoProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, lås);
        }
    }

    private void lagreUttak(FpUttakRepository fpUttakRepository) {
        if (uttak == null) {
            return;
        }

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttak);
    }

    @SuppressWarnings("unchecked")
    public S medFødselAdopsjonsdato(List<LocalDate> fødselAdopsjonDatoer) {
        for (var localDate : fødselAdopsjonDatoer) {
            medSøknadHendelse().leggTilBarn(localDate);
        }
        return (S) this;
    }

    public Fagsak getFagsak() {
        if (fagsak == null) {
            throw new IllegalStateException("Kan ikke hente Fagsak før denne er bygd");
        }
        return fagsak;
    }

    public AktørId getDefaultBrukerAktørId() {
        return fagsakBuilder.getBrukerBuilder().getAktørId();
    }

    public Behandling getBehandling() {
        if (behandling == null) {
            throw new IllegalStateException("Kan ikke hente Behandling før denne er bygd");
        }
        return behandling;
    }

    @SuppressWarnings("unchecked")
    public S medSaksnummer(Saksnummer saksnummer) {
        fagsakBuilder.medSaksnummer(saksnummer);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medFagsakId(Long id) {
        this.fagsakId = id;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medSøknadHendelse(FamilieHendelseBuilder søknadHendelseBuilder) {
        this.søknadHendelseBuilder = søknadHendelseBuilder;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittTilknytning(MedlemskapOppgittTilknytningEntitet.Builder builder) {
        this.oppgittTilknytningBuilder = builder;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBekreftetHendelse(FamilieHendelseBuilder bekreftetHendelseBuilder) {
        this.bekreftetHendelseBuilder = bekreftetHendelseBuilder;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOverstyrtHendelse(FamilieHendelseBuilder overstyrtHendelseBuilder) {
        this.overstyrtHendelseBuilder = overstyrtHendelseBuilder;
        return (S) this;
    }

    public OppgittAnnenPartBuilder medSøknadAnnenPart() {
        if (oppgittAnnenPartBuilder == null) {
            oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder();
        }
        return oppgittAnnenPartBuilder;
    }

    public FamilieHendelseBuilder medSøknadHendelse() {
        if (søknadHendelseBuilder == null) {
            søknadHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.SØKNAD);
        }
        return søknadHendelseBuilder;
    }

    public BehandlingVedtak.Builder medBehandlingVedtak() {
        if (behandlingVedtakBuilder == null) {
            behandlingVedtakBuilder = BehandlingVedtak.builder()
                    // Setter defaultverdier
                    .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
                    .medAnsvarligSaksbehandler("Nav Navesen");
        }
        return behandlingVedtakBuilder;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingsresultat(Behandlingsresultat.Builder builder) {
        if (behandlingresultatBuilder == null) {
            behandlingresultatBuilder = builder;
        }
        return (S) this;
    }

    public MedlemskapOppgittTilknytningEntitet.Builder medOppgittTilknytning() {
        if (oppgittTilknytningBuilder == null) {
            oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder();
        }
        return oppgittTilknytningBuilder;
    }

    public S medDefaultOppgittTilknytning() {
        if (oppgittTilknytningBuilder == null) {
            oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder();
        }
        var oppholdNorgeSistePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(true)
                .medLand(Landkoder.NOR)
                .medPeriode(
                        LocalDate.now().minusYears(1),
                        LocalDate.now())
                .build();
        var oppholdNorgeNestePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(false)
                .medLand(Landkoder.NOR)
                .medPeriode(
                        LocalDate.now(),
                        LocalDate.now().plusYears(1))
                .build();
        var oppholdNorge = List.of(oppholdNorgeNestePeriode, oppholdNorgeSistePeriode);

        oppgittTilknytningBuilder.medOpphold(oppholdNorge).medOppholdNå(true).medOppgittDato(LocalDate.now());
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medDefaultOppgittDekningsgrad() {
        medOppgittDekningsgrad(OppgittDekningsgradEntitet.bruk100());
        return (S) this;
    }

    public FamilieHendelseBuilder medBekreftetHendelse() {
        if (bekreftetHendelseBuilder == null) {
            bekreftetHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET);
        }
        return bekreftetHendelseBuilder;
    }

    public FamilieHendelseBuilder medOverstyrtHendelse() {
        if (overstyrtHendelseBuilder == null) {
            overstyrtHendelseBuilder = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.OVERSTYRT);
        }
        return overstyrtHendelseBuilder;
    }

    public SøknadEntitet.Builder medSøknad() {
        if (søknadBuilder == null) {
            søknadBuilder = new SøknadEntitet.Builder();
        }
        return søknadBuilder;
    }

    protected void utenSøknad() {
        this.søknadBuilder = null;
    }

    public VurdertMedlemskapBuilder medMedlemskap() {
        if (vurdertMedlemskapBuilder == null) {
            vurdertMedlemskapBuilder = new VurdertMedlemskapBuilder();
        }
        return vurdertMedlemskapBuilder;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S leggTilVilkår(VilkårType vilkårType, VilkårUtfallType vilkårUtfallType) {
        vilkårTyper.put(vilkårType, vilkårUtfallType);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medVilkårResultatType(VilkårResultatType vilkårResultatType) {
        this.vilkårResultatType = vilkårResultatType;
        return (S) this;
    }

    /** @deprecated Fjern uttak fra scenario. Bør bare håndteres i uttak modul. */
    @SuppressWarnings("unchecked")
    @Deprecated
    public S medUttak(UttakResultatPerioderEntitet uttak) {
        if (behandlingresultatBuilder == null) {
            medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår());
        }
        if (behandlingVedtakBuilder == null) {
            medBehandlingVedtak().medVedtakResultatType(VedtakResultatType.INNVILGET);
        }
        this.uttak = uttak;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S leggTilAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        aksjonspunktDefinisjoner.put(apDef, stegType);
        return (S) this;
    }

    public void leggTilMedlemskapPeriode(MedlemskapPerioderEntitet medlemskapPeriode) {
        this.medlemskapPerioder.add(medlemskapPeriode);
    }

    @SuppressWarnings("unchecked")
    public S medDefaultSøknadTerminbekreftelse() {
        final var terminbekreftelse = medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusDays(40))
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(LocalDate.now().minusDays(7));
        medSøknadHendelse()
                .medTerminbekreftelse(terminbekreftelse);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medDefaultBekreftetTerminbekreftelse() {
        final var terminbekreftelse = medBekreftetHendelse().getTerminbekreftelseBuilder()
                .medTermindato(LocalDate.now().plusDays(40))
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(LocalDate.now().minusDays(7));
        medBekreftetHendelse()
                .medTerminbekreftelse(terminbekreftelse);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBruker(AktørId aktørId, NavBrukerKjønn kjønn) {
        fagsakBuilder
                .medBrukerAktørId(aktørId)
                .medBrukerKjønn(kjønn);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBruker(AktørId aktørId) {
        fagsakBuilder
                .medBrukerAktørId(aktørId);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBrukerKjønn(NavBrukerKjønn kjønn) {
        fagsakBuilder
                .medBrukerKjønn(kjønn);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medTilleggsopplysninger(String tilleggsopplysninger) {
        medSøknad().medTilleggsopplysninger(tilleggsopplysninger);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittRettighet(OppgittRettighetEntitet oppgittRettighet) {
        this.oppgittRettighet = oppgittRettighet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOppgittDekningsgrad(OppgittDekningsgradEntitet oppgittDekningsgrad) {
        this.oppgittDekningsgrad = oppgittDekningsgrad;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medFordeling(OppgittFordelingEntitet oppgittFordeling) {
        this.oppgittFordeling = oppgittFordeling;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medJustertFordeling(OppgittFordelingEntitet justertFordeling) {
        this.justertFordeling = justertFordeling;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medAvklarteUttakDatoer(AvklarteUttakDatoerEntitet avklarteUttakDatoer) {
        this.avklarteUttakDatoer = avklarteUttakDatoer;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medPeriodeMedAleneomsorg(PerioderAleneOmsorgEntitet perioderAleneOmsorg) {
        this.perioderMedAleneomsorg = perioderAleneOmsorg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingstidFrist(LocalDate behandlingstidFrist) {
        this.behandlingstidFrist = behandlingstidFrist;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlendeEnhet(String behandlendeEnhet) {
        this.behandlendeEnhet = behandlendeEnhet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOpplysningerOppdatertTidspunkt(LocalDateTime opplysningerOppdatertTidspunkt) {
        this.opplysningerOppdatertTidspunkt = opplysningerOppdatertTidspunkt;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medRegisterOpplysninger(PersonInformasjon personinfo) {
        Objects.nonNull(personinfo);
        if (!personinfo.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            throw new IllegalStateException("Feil versjontype, må være PersonopplysningVersjonType.REGISTRERT");
        }
        if (this.personer == null) {
            this.personer = new ArrayList<>();
            this.personer.add(personinfo);
        }
        return (S) this;
    }

    public PersonInformasjon.Builder opprettBuilderForRegisteropplysninger() {
        if (personInformasjonBuilder == null) {
            personInformasjonBuilder = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT);
        }
        return personInformasjonBuilder;
    }

    public S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        return medOriginalBehandling(originalBehandling, List.of(behandlingÅrsakType), false);
    }

    @SuppressWarnings("unchecked")
    public S medOriginalBehandling(Behandling originalBehandling, List<BehandlingÅrsakType> behandlingÅrsakType, boolean manueltOpprettet) {
        this.originalBehandling = originalBehandling;
        this.behandlingÅrsakTyper = behandlingÅrsakType;
        this.manueltOpprettet = manueltOpprettet;
        this.behandlingType = BehandlingType.REVURDERING;
        return (S) this;
    }

    /**
     * temporær metode til vi får fjernet gammel entitet helt. Gjør en begrenset
     * mapping av Søker data (uten adresse, relasjoner)
     *
     * @deprecated bruk {@link #medRegisterOpplysninger(PersonInformasjon)}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public S medSøker(Personinfo søker) {
        final var builder = opprettBuilderForRegisteropplysninger();
        PersonopplysningPersoninfoAdapter.mapPersonopplysningTilPerson(builder, søker);
        medRegisterOpplysninger(builder.build());
        medBruker(søker.getAktørId(), søker.getKjønn());
        return (S) this;
    }

    private final class MockPersonopplysningRepository extends PersonopplysningRepository {
        @Override
        public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long eksisterendeBehandlingId, Long nyBehandlingId) {
            final var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(eksisterendeBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
            return Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
            if (personopplysningMap.isEmpty() || personopplysningMap.get(behandlingId) == null || !personopplysningMap.containsKey(behandlingId)) {
                throw new IllegalStateException("Fant ingen personopplysninger for angitt behandling");
            }

            return personopplysningMap.getOrDefault(behandlingId, null);
        }

        @Override
        public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
            final var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
                oppdatere.medRegistrertVersjon(builder);
            }
            if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
                throw new IllegalArgumentException("Overstyrt personopplysning er legacy");
            }
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public void lagre(Long behandlingId, OppgittAnnenPartEntitet oppgittAnnenPart) {
            final var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            oppdatere.medOppgittAnnenPart(oppgittAnnenPart);
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public PersonInformasjonBuilder opprettBuilderForRegisterdata(Long behandlingId) {
            final var grunnlag = Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
            return PersonInformasjonBuilder.oppdater(grunnlag.flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon),
                    PersonopplysningVersjonType.REGISTRERT);
        }

        @Override
        public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
            final var oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                    Optional.ofNullable(personopplysningMap.getOrDefault(gammelBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentFørsteVersjonAvPersonopplysninger(Long behandlingId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentGrunnlagPåId(Long aggregatId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }
    }

}
