package no.nav.foreldrepenger.behandling.steg.iverksettevedtak.svp;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.iverksettevedtak.IverksetteVedtakStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.domene.vedtak.impl.OpprettProsessTaskIverksettKlage;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-003") //Klage
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class IverksetteVedtakStegKlage extends IverksetteVedtakStegFelles {

    IverksetteVedtakStegKlage() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegKlage(BehandlingRepositoryProvider repositoryProvider,
                                       OpprettProsessTaskIverksettKlage opprettProsessTaskIverksettKlage) {
        super(repositoryProvider, opprettProsessTaskIverksettKlage);
    }

    @Override
    protected Optional<Venteårsak> kanBegynneIverksetting(Behandling behandling) {
        return Optional.empty();
    }

    @Override
    protected void førIverksetting(Behandling behandling, BehandlingVedtak behandlingVedtak) {

    }
}
