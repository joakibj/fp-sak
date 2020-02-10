package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.felles.PrematurukerUtil;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;

@ApplicationScoped
public class UttakStegBeregnStønadskontoTjeneste {

    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private UttakRepository uttakRepository;

    @Inject
    public UttakStegBeregnStønadskontoTjeneste(UttakRepositoryProvider repositoryProvider,
                                               BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                               DekningsgradTjeneste dekningsgradTjeneste) {
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    UttakStegBeregnStønadskontoTjeneste() {
        //CDI
    }

    /**
     * Beregner og lagrer stønadskontoer hvis preconditions er oppfylt
     */
    BeregningingAvStønadskontoResultat beregnStønadskontoer(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(ref.getSaksnummer());
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();

        //Trenger ikke behandlingslås siden stønadskontoer lagres på fagsakrelasjon.
        if (fagsakRelasjon.getStønadskontoberegning().isEmpty() || !finnesLøpendeInnvilgetFP(fpGrunnlag)) {
            beregnStønadskontoerTjeneste.beregnStønadskontoer(input);
            return BeregningingAvStønadskontoResultat.BEREGNET;
        } else if (dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref) || oppfyllerPrematurUker(fpGrunnlag)){
            beregnStønadskontoerTjeneste.overstyrStønadskontoberegning(input);
            return BeregningingAvStønadskontoResultat.OVERSTYRT;
        }
        return BeregningingAvStønadskontoResultat.INGEN_BEREGNING;
    }

    private boolean oppfyllerPrematurUker(ForeldrepengerGrunnlag fpGrunnlag) {
        var gjeldendeFamilieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        LocalDate fødselsdato = gjeldendeFamilieHendelse.getFødselsdato().orElse(null);
        LocalDate termindato = gjeldendeFamilieHendelse.getTermindato().orElse(null);
        return PrematurukerUtil.oppfyllerKravTilPrematuruker(fødselsdato, termindato, StandardKonfigurasjon.KONFIGURASJON);
    }

    private boolean finnesLøpendeInnvilgetFP(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var originalBehandling = foreldrepengerGrunnlag.getOriginalBehandling();
        if (originalBehandling.isPresent() && erLøpendeInnvilgetFP(originalBehandling.get().getId())) {
            return true;
        }

        if (foreldrepengerGrunnlag.getAnnenpart().isPresent()) {
            var annenpartGjeldendeVedtakBehandlingId = foreldrepengerGrunnlag.getAnnenpart().get().getGjeldendeVedtakBehandlingId();
            return erLøpendeInnvilgetFP(annenpartGjeldendeVedtakBehandlingId);
        }
        return false;
    }

    private boolean erLøpendeInnvilgetFP(Long behandlingId) {
        var uttak = uttakRepository.hentUttakResultatHvisEksisterer(behandlingId);
        if (uttak.isEmpty()) {
            return false;
        }
        return uttak.get().getGjeldendePerioder().getPerioder()
            .stream()
            .anyMatch(this::harTrekkdagerEllerUtbetaling);
    }

    private boolean harTrekkdagerEllerUtbetaling(UttakResultatPeriodeEntitet periode) {
        return periode.getAktiviteter()
            .stream()
            .anyMatch(aktivitet -> aktivitet.getTrekkdager().merEnn0() || aktivitet.getUtbetalingsprosent().compareTo(BigDecimal.ZERO) > 0);
    }

    enum  BeregningingAvStønadskontoResultat {
        BEREGNET, OVERSTYRT, INGEN_BEREGNING
    }
}
