package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class PoststedKodeverkRepository {

    private EntityManager entityManager;

    PoststedKodeverkRepository() {
        // for CDI proxy
    }

    @Inject
    public PoststedKodeverkRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<Poststed> finnPoststed(String postnummer) {
        TypedQuery<Poststed> query = entityManager.createQuery("from Poststed where kode=:kode",
                Poststed.class);
            query.setParameter("kode", postnummer);
            return HibernateVerktøy.hentUniktResultat(query);
    }

}
