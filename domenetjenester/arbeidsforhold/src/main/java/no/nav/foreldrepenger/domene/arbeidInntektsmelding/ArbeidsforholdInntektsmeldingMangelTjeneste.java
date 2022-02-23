package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ArbeidsforholdInntektsmeldingMangelTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdInntektsmeldingMangelTjeneste.class);

    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInntektsmeldingsMangelUtleder arbeidsforholdInntektsmeldingsMangelUtleder;


    public ArbeidsforholdInntektsmeldingMangelTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdInntektsmeldingMangelTjeneste(ArbeidsforholdValgRepository arbeidsforholdValgRepository,
                                                       ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste,
                                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       ArbeidsforholdInntektsmeldingsMangelUtleder arbeidsforholdInntektsmeldingsMangelUtleder) {
        this.arbeidsforholdValgRepository = arbeidsforholdValgRepository;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdInntektsmeldingsMangelUtleder = arbeidsforholdInntektsmeldingsMangelUtleder;
    }

    public void lagreManglendeOpplysningerVurdering(BehandlingReferanse behandlingReferanse, ManglendeOpplysningerVurderingDto dto) {
        ryddBortManuelleArbeidsforholdVedBehov(behandlingReferanse, dto);

        var arbeidsforholdMedMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
        var entitet = ArbeidsforholdInntektsmeldingMangelMapper.mapManglendeOpplysningerVurdering(dto, arbeidsforholdMedMangler);
        arbeidsforholdValgRepository.lagre(entitet, behandlingReferanse.getBehandlingId());
    }

    private void ryddBortManuelleArbeidsforholdVedBehov(BehandlingReferanse behandlingReferanse, ManglendeOpplysningerVurderingDto dto) {
        var eksisterendeInfo = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingUuid()).getArbeidsforholdInformasjon();
        if (eksisterendeInfo.map(info -> ArbeidsforholdInntektsmeldingRyddeTjeneste.arbeidsforholdSomMåRyddesBortVedNyttValg(dto, info)).orElse(false)) {
            var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingReferanse.getBehandlingId());
            informasjonBuilder.fjernOverstyringerSomGjelder(ArbeidsforholdInntektsmeldingMangelMapper.lagArbeidsgiver(dto.getArbeidsgiverIdent()));
            arbeidsforholdTjeneste.lagreOverstyring(behandlingReferanse.getBehandlingId(), behandlingReferanse.getAktørId(), informasjonBuilder);
        }
    }

    public void lagreManuelleArbeidsforhold(BehandlingReferanse behandlingReferanse, ManueltArbeidsforholdDto dto) {
        var arbeidsforholdMedMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
        var eksisterendeValg = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.getBehandlingId());
        var valgSomMåRyddesBort = ArbeidsforholdInntektsmeldingRyddeTjeneste.valgSomMåRyddesBortVedOpprettelseAvArbeidsforhold(dto, eksisterendeValg);

        valgSomMåRyddesBort.ifPresent(arbeidsforholdValg -> arbeidsforholdValgRepository.fjernValg(arbeidsforholdValg));

        var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingReferanse.getBehandlingId());
        ArbeidsforholdInformasjonBuilder oppdatertBuilder = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, arbeidsforholdMedMangler, informasjonBuilder);
        arbeidsforholdTjeneste.lagreOverstyring(behandlingReferanse.getBehandlingId(), behandlingReferanse.getAktørId(), oppdatertBuilder);
    }

    public List<ArbeidsforholdInntektsmeldingMangel> utledManglerPåArbeidsforholdInntektsmelding(BehandlingReferanse behandlingReferanse) {
        return arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
    }

    public List<ArbeidsforholdValg> hentArbeidsforholdValgForSak(BehandlingReferanse behandlingReferanse) {
        return arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.getBehandlingId());
    }

    /**
     * Tjeneste for å rydde vekk valg som ikke er relevant i behandlingen etter kopiering fra forrige behandling.
     * @param ref
     */
    public void ryddVekkUgyldigeValg(BehandlingReferanse ref) {
        var valgPåBehandlingen = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(ref.getBehandlingId());
        var manglerPåBehandlingen = utledManglerPåArbeidsforholdInntektsmelding(ref);
        var valgSomMåDeaktiveres = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeValgSomErGjort(valgPåBehandlingen, manglerPåBehandlingen);
        valgSomMåDeaktiveres.forEach(valg -> {
            LOG.info("Deaktiverer valg som ikke lenger er gyldig: {}", valg);
            arbeidsforholdValgRepository.fjernValg(valg);
        });
    }

    /**
     * Tjeneste for å rydde vekk overstyringer som ikke lenger er mulig å gjøre (overgang fra gammelt aksjonspunkt 5080)
     * @param ref
     */
    public void ryddVekkUgyldigeArbeidsforholdoverstyringer(BehandlingReferanse ref) {
        var manglerPåBehandlingen = utledManglerPåArbeidsforholdInntektsmelding(ref);
        var overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId()).getArbeidsforholdOverstyringer();
        var overstyringerSomMåFjernes = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeOverstyringer(manglerPåBehandlingen, overstyringer);
        if (!overstyringerSomMåFjernes.isEmpty()) {
            var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(ref.getBehandlingId());
            overstyringerSomMåFjernes.forEach(os -> {
                LOG.info("Fjerner overstyring som ikke lenger er gyldig: {}", os);
                informasjonBuilder.fjernOverstyringerSomGjelder(os.getArbeidsgiver());
            });
            arbeidsforholdTjeneste.lagreOverstyring(ref.getBehandlingId(), ref.getAktørId(), informasjonBuilder);
        }
    }
}
