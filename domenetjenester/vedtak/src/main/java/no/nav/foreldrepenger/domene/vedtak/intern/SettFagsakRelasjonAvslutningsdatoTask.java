package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLåsRepository;
import no.nav.foreldrepenger.behandlingslager.task.FagsakRelasjonProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("iverksetteVedtak.fagsakRelasjonAvsluttningsdato")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class SettFagsakRelasjonAvslutningsdatoTask extends FagsakRelasjonProsessTask {

    private Instance<FagsakRelasjonAvslutningsdatoOppdaterer> fagsakRelasjonAvslutningsdatoOppdaterer;

    SettFagsakRelasjonAvslutningsdatoTask() {
        // for CDI proxy
    }

    @Inject
    public SettFagsakRelasjonAvslutningsdatoTask(FagsakLåsRepository fagsakLåsRepository, FagsakRelasjonLåsRepository relasjonLåsRepository, FagsakRelasjonRepository fagsakRelasjonRepository,
                                                 @Any Instance<FagsakRelasjonAvslutningsdatoOppdaterer> fagsakRelasjonAvsluttningsdatoOppdaterer) {
        super(fagsakLåsRepository, relasjonLåsRepository, fagsakRelasjonRepository);
        this.fagsakRelasjonAvslutningsdatoOppdaterer = fagsakRelasjonAvsluttningsdatoOppdaterer;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Optional<FagsakRelasjon> relasjon, FagsakRelasjonLås relasjonLås, Optional<FagsakLås> fagsak1Lås, Optional<FagsakLås> fagsak2Lås) {
        var fagsakId = prosessTaskData.getFagsakId();
        if(relasjon.isPresent()){
            var ytelseType = relasjon.get().getFagsakNrEn().getYtelseType();
            var fagsakRelasjonAvslutningsdatoOppdaterer = FagsakYtelseTypeRef.Lookup.find(this.fagsakRelasjonAvslutningsdatoOppdaterer, ytelseType)
                .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner av FagsakRelasjonAvslutningsdatoOppdaterer funnet for ytelse: " + ytelseType.getKode()));
            fagsakRelasjonAvslutningsdatoOppdaterer.oppdaterFagsakRelasjonAvsluttningsdato(relasjon.get(), fagsakId, relasjonLås, fagsak1Lås, fagsak2Lås);
        }
    }
}
