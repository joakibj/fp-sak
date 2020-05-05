package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.overlapp.IdentifiserOverlappendeInfotrygdYtelseTjeneste;

public abstract class IverksetteVedtakStegTilgrensendeFelles extends IverksetteVedtakStegYtelseFelles {

    private IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse;

    protected IverksetteVedtakStegTilgrensendeFelles() {
        // for CDI proxy
    }

    public IverksetteVedtakStegTilgrensendeFelles(BehandlingRepositoryProvider repositoryProvider,
                                                  OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                                  VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                                  IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse) {
        super(repositoryProvider, opprettProsessTaskIverksett, tidligereBehandlingUnderIverksettelse);
        this.identifiserOverlappendeInfotrygdYtelse = identifiserOverlappendeInfotrygdYtelse;
    }

    @Override
    public void etterInngangFørIverksetting(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        identifiserOverlappendeInfotrygdYtelse.vurderOglagreEventueltOverlapp(behandling);
    }
}
