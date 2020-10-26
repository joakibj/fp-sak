package no.nav.foreldrepenger.behandlingslager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.ManagedType;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.dbstoette.Databaseskjemainitialisering;

/** Lagt til web for å sjekke orm filer fra alle moduler. */
public class SjekkCollectionsOrderedIEntiteterTest {

    private static final EntityManagerFactory entityManagerFactory;

    static {
        try {
            Databaseskjemainitialisering.settJdniOppslag();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default");
    }

    private static Collection<Class<?>> parameters() {

        Set<Class<?>> baseEntitetSubklasser = getEntityClasses(BaseEntitet.class::isAssignableFrom);
        Set<Class<?>> entityKlasser = getEntityClasses(c -> c.isAnnotationPresent(Entity.class));

        Collection<Class<?>> params = new HashSet<>(baseEntitetSubklasser);
        assertThat(params).isNotEmpty();

        params.addAll(entityKlasser);
        assertThat(params).isNotEmpty();

        return params;
    }

    public static Set<Class<?>> getEntityClasses(Predicate<Class<?>> filter) {
        Set<ManagedType<?>> managedTypes = entityManagerFactory.getMetamodel().getManagedTypes();
        return managedTypes.stream().map(javax.persistence.metamodel.Type::getJavaType).filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .filter(filter).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void sjekk_alle_lister_er_ordered(Class<?> entityClass) {
        for (Field f : entityClass.getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(f.getType())) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    ParameterizedType paramType = (ParameterizedType) f.getGenericType();
                    Class<?> cls = (Class<?>) paramType.getActualTypeArguments()[0];
                    assumeTrue(IndexKey.class.isAssignableFrom(cls));
                    assertThat(IndexKey.class).as(f + " definerer en liste i " + entityClass.getSimpleName()).isAssignableFrom(cls);
                }
            }
        }
    }

}
