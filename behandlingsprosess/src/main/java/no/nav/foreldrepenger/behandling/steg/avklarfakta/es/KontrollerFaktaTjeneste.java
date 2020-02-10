package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.StartpunktRef;

@FagsakYtelseTypeRef("ES")
@BehandlingTypeRef
@StartpunktRef
@ApplicationScoped
class KontrollerFaktaTjeneste extends KontrollerFaktaTjenesteImpl {

    protected KontrollerFaktaTjeneste() {
        // for CDI proxy
    }

    @Inject
    protected KontrollerFaktaTjeneste(KontrollerFaktaUtledereTjenesteImpl utlederTjeneste,
                                      BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(utlederTjeneste, behandlingskontrollTjeneste);
    }

}
