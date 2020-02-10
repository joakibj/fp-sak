package no.nav.foreldrepenger.behandlingslager.hendelser;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class HendelseHåndteringRepository {

    private EntityManager entityManager;

    HendelseHåndteringRepository() {
        // CDI
    }

    @Inject
    public HendelseHåndteringRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Fagsak> hentFagsakerSomHarAktørIdSomBarn(AktørId aktørId) {
        TypedQuery<Fagsak> query = entityManager.createQuery(
            "select f from Fagsak f " +
                "inner join Behandling b on b.fagsak = f " +
                "inner join PersonopplysningGrunnlagEntitet gr on gr.behandlingId = b.id " +
                "inner join PersonInformasjon poi on gr.registrertePersonopplysninger = poi " +
                "inner join PersonopplysningRelasjon por on por.personopplysningInformasjon = poi " +
                "where por.relasjonsrolle = :relasjonsRolle " +
                "and por.tilAktørId = (:aktørId) " +
                "and gr.aktiv = :aktiv " +
                "and f.fagsakStatus != :fagsakStatus " +
                "and f.ytelseType = :ytelseType ", //$NON-NLS-1$
            Fagsak.class);
        query.setParameter("relasjonsRolle", RelasjonsRolleType.BARN);
        query.setParameter("aktørId", aktørId);
        query.setParameter("aktiv", true);
        query.setParameter("fagsakStatus", FagsakStatus.AVSLUTTET);
        query.setParameter("ytelseType", FagsakYtelseType.FORELDREPENGER);
        return query.getResultList();
    }

}
