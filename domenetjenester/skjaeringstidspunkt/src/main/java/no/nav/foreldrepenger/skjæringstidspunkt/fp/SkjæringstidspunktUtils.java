package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.SoekerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class SkjæringstidspunktUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkjæringstidspunktUtils.class);

    private Period grenseverdiFør;
    private Period grenseverdiEtter;
    private Period opptjeningsperiode;
    private Period tidligsteUttakFørFødselPeriode;

    SkjæringstidspunktUtils() {
        // CDI
    }

    /**
     *
     * @param opptjeningsperiode - Opptjeningsperiode lengde før skjæringstidspunkt
     * @param tidligsteUttakFørFødselPeriode -
     * @param grenseverdiAvvikFør - Maks avvik før/etter STP for registerinnhenting før justering av perioden
     * @param grenseverdiAvvikEtter
     */
    @Inject
    public SkjæringstidspunktUtils(@KonfigVerdi(value = "fp.opptjeningsperiode.lengde", defaultVerdi = "P10M") Period opptjeningsperiode,
                                   @KonfigVerdi(value = "fp.uttak.tidligst.før.fødsel", defaultVerdi = "P12W") Period tidligsteUttakFørFødselPeriode,
                                   @KonfigVerdi(value = "fp.registerinnhenting.avvik.periode.før", defaultVerdi = "P4M") Period grenseverdiAvvikFør,
                                   @KonfigVerdi(value = "fp.registerinnhenting.avvik.periode.etter", defaultVerdi = "P1Y") Period grenseverdiAvvikEtter) {
        this.grenseverdiFør = grenseverdiAvvikFør;
        this.grenseverdiEtter = grenseverdiAvvikEtter;
        this.opptjeningsperiode = opptjeningsperiode;
        this.tidligsteUttakFørFødselPeriode = tidligsteUttakFørFødselPeriode;
    }

    LocalDate utledSkjæringstidspunktRegisterinnhenting(FamilieHendelseGrunnlagEntitet familieHendelseAggregat) {
        final LocalDate gjeldendeHendelseDato = familieHendelseAggregat.getGjeldendeVersjon().getGjelderFødsel()
            ? familieHendelseAggregat.finnGjeldendeFødselsdato()
            : familieHendelseAggregat.getGjeldendeVersjon().getSkjæringstidspunkt();
        final LocalDate oppgittHendelseDato = familieHendelseAggregat.getSøknadVersjon().getSkjæringstidspunkt();

        if (erEndringIPerioden(oppgittHendelseDato, gjeldendeHendelseDato)) {
            LOGGER.info("STP registerinnhenting endring i perioden for fhgrunnlag {}", familieHendelseAggregat.getId());
            return gjeldendeHendelseDato;
        }
        return oppgittHendelseDato;
    }

    private boolean erEndringIPerioden(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        if (bekreftetSkjæringstidspunkt == null) {
            return false;
        }
        return vurderEndringFør(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiFør)
            || vurderEndringEtter(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiEtter);
    }

    private boolean vurderEndringEtter(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiEtter) {
        final Period avstand = Period.between(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiEtter);
    }

    private boolean vurderEndringFør(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiFør) {
        final Period avstand = Period.between(bekreftetSkjæringstidspunkt, oppgittSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiFør);
    }

    private static boolean størreEnn(Period period, Period sammenligning) {
        return tilDager(period) > tilDager(sammenligning);
    }

    private static int tilDager(Period period) {
        return period.getDays() + (period.getMonths() * 30) + ((period.getYears() * 12) * 30);
    }

    LocalDate utledSkjæringstidspunktFraBehandling(Behandling behandling, LocalDate førsteUttaksDato,
                                                   Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag) {

        if (familieHendelseGrunnlag.isPresent()) {
            final FamilieHendelseGrunnlagEntitet fhGrunnlag = familieHendelseGrunnlag.get();
            final FagsakÅrsak årsak = finnFagsakÅrsak(fhGrunnlag.getGjeldendeVersjon());
            final SoekerRolle rolle = finnFagsakSøkerRolle(behandling);

            final LocalDate gjeldendeHendelseDato = fhGrunnlag.getGjeldendeVersjon().getGjelderFødsel() ? fhGrunnlag.finnGjeldendeFødselsdato()
                : fhGrunnlag.getGjeldendeVersjon().getSkjæringstidspunkt();
            final Optional<LocalDate> gjeldendeTerminDato = fhGrunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

            final LocalDate gjeldendeSkjæringstidspunkt = evaluerSkjæringstidspunktOpptjening(behandling.getId(), årsak, rolle, gjeldendeHendelseDato,
                gjeldendeTerminDato, førsteUttaksDato);
            return gjeldendeSkjæringstidspunkt;
        }
        return førsteUttaksDato;
    }

    private LocalDate evaluerSkjæringstidspunktOpptjening(Long behandlingId, FagsakÅrsak årsak, SoekerRolle rolle, LocalDate hendelseDato,
                                                          Optional<LocalDate> terminDato, LocalDate førsteUttaksdato) {
        OpptjeningsperiodeGrunnlag grunnlag = new OpptjeningsperiodeGrunnlag();

        grunnlag.setFagsakÅrsak(årsak);
        grunnlag.setSøkerRolle(rolle);
        if (grunnlag.getFagsakÅrsak() == null || grunnlag.getSøkerRolle() == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: Finner ikke årsak(" + grunnlag.getFagsakÅrsak() + ")/rolle(" + grunnlag.getSøkerRolle() + ") for behandling:" + behandlingId);
        }

        grunnlag.setHendelsesDato(hendelseDato);
        terminDato.ifPresent(grunnlag::setTerminDato);

        if (grunnlag.getHendelsesDato() == null) {
            grunnlag.setHendelsesDato(grunnlag.getTerminDato());
            if (grunnlag.getHendelsesDato() == null) {
                throw new IllegalArgumentException("Utvikler-feil: Finner ikke hendelsesdato for behandling:" + behandlingId);
            }
        }

        grunnlag.setTidligsteUttakFørFødselPeriode(tidligsteUttakFørFødselPeriode);
        grunnlag.setPeriodeLengde(opptjeningsperiode);
        grunnlag.setFørsteUttaksDato(førsteUttaksdato);

        final RegelFastsettOpptjeningsperiode fastsettPeriode = new RegelFastsettOpptjeningsperiode();
        final OpptjeningsPeriode periode = new OpptjeningsPeriode();
        fastsettPeriode.evaluer(grunnlag, periode);

        return periode.getOpptjeningsperiodeTom().plusDays(1);
    }

    // TODO(Termitt): Håndtere MMOR, SAMB mm.
    private SoekerRolle finnFagsakSøkerRolle(Behandling behandling) {
        RelasjonsRolleType relasjonsRolleType = behandling.getRelasjonsRolleType();
        if (RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            return SoekerRolle.MORA;
        }
        if (RelasjonsRolleType.UDEFINERT.equals(relasjonsRolleType) || RelasjonsRolleType.BARN.equals(relasjonsRolleType)) {
            return null;
        }
        return SoekerRolle.FARA;
    }

    private FagsakÅrsak finnFagsakÅrsak(FamilieHendelseEntitet gjeldendeVersjon) {
        final FamilieHendelseType type = gjeldendeVersjon.getType();
        if (gjeldendeVersjon.getGjelderFødsel()) {
            return FagsakÅrsak.FØDSEL;
        } else if (FamilieHendelseType.ADOPSJON.equals(type)) {
            return FagsakÅrsak.ADOPSJON;
        } else if (FamilieHendelseType.OMSORG.equals(type)) {
            return FagsakÅrsak.OMSORG;
        }
        return null;
    }
}
