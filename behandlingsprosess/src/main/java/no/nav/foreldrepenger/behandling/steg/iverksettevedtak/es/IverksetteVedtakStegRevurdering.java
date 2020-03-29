package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegYtelseFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-004") // Revurdering
@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class IverksetteVedtakStegRevurdering extends IverksetteVedtakStegYtelseFelles {

    IverksetteVedtakStegRevurdering() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegRevurdering(BehandlingRepositoryProvider repositoryProvider,
                                             @FagsakYtelseTypeRef("ES") OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                             VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse) {
        super(repositoryProvider, opprettProsessTaskIverksett, tidligereBehandlingUnderIverksettelse);
    }
}
