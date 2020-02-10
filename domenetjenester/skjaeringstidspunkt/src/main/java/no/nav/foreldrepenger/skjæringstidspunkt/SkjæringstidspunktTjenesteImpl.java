package no.nav.foreldrepenger.skjæringstidspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste engangsstønadTjeneste;
    private SkjæringstidspunktTjeneste foreldrepengerTjeneste;
    private SkjæringstidspunktTjeneste svangerskapspengerTjeneste;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository,
                                          @FagsakYtelseTypeRef("ES") SkjæringstidspunktTjeneste engangsstønadTjeneste,
                                          @FagsakYtelseTypeRef("FP") SkjæringstidspunktTjeneste foreldrepengerTjeneste,
                                          @FagsakYtelseTypeRef("SVP") SkjæringstidspunktTjeneste svangerskapspengerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.engangsstønadTjeneste = engangsstønadTjeneste;
        this.foreldrepengerTjeneste = foreldrepengerTjeneste;
        this.svangerskapspengerTjeneste =svangerskapspengerTjeneste;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erYtelseBehandling()) {
            if (behandling.getFagsakYtelseType().gjelderEngangsstønad()) {
                return engangsstønadTjeneste.getSkjæringstidspunkter(behandlingId);
            } else if (behandling.getFagsakYtelseType().gjelderForeldrepenger()) {
                return foreldrepengerTjeneste.getSkjæringstidspunkter(behandlingId);
            } else if (behandling.getFagsakYtelseType().gjelderSvangerskapspenger()) {
                return svangerskapspengerTjeneste.getSkjæringstidspunkter(behandlingId);
            }
            throw new IllegalStateException("Ukjent ytelse type.");
        } else {
            // returner tom container for andre behandlingtyper
            // (så ser vi om det evt. er noen call paths som kaller på noen form for skjæringstidspunkt)
            return Skjæringstidspunkt.builder().build();
        }
    }

}
