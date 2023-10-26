package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.uttak.*;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeAktivitetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPerioderDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UttakPerioderDtoTjeneste {
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public UttakPerioderDtoTjeneste(ForeldrepengerUttakTjeneste uttakTjeneste,
                                    RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                    YtelsesFordelingRepository ytelsesFordelingRepository,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    BehandlingVedtakRepository behandlingVedtakRepository) {
        this.uttakTjeneste = uttakTjeneste;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    public UttakPerioderDtoTjeneste() {
        // For CDI
    }

    public UttakResultatPerioderDto mapFra(Behandling behandling) {
        return mapFra(behandling, false, false);
    }

    public UttakResultatPerioderDto mapFra(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        return mapFra(behandling, skjæringstidspunkt.kreverSammenhengendeUttak(), skjæringstidspunkt.utenMinsterett());
    }

    private UttakResultatPerioderDto mapFra(Behandling behandling, boolean kreverSammenhengendeUttak, boolean utenMinsterett) {
        var ytelseFordeling = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());

        final List<UttakResultatPeriodeDto> annenpartUttaksperioder;
        final Optional<ForeldrepengerUttak> annenpartUttak;
        var annenpartBehandling = annenpartBehandling(behandling);
        if (annenpartBehandling.isPresent()) {
            annenpartUttak = uttakTjeneste.hentUttakHvisEksisterer(annenpartBehandling.get().getId());
            if (annenpartUttak.isPresent()) {
                annenpartUttaksperioder = annenpartUttak.map(u -> {
                    var annenpartBehandlingId = annenpartBehandling.orElseThrow().getId();
                    return finnUttakResultatPerioder(u, annenpartBehandlingId);
                }).orElse(List.of());
            } else {
                annenpartUttaksperioder = List.of();
            }
        } else {
            annenpartUttaksperioder = List.of();
            annenpartUttak = Optional.empty();
        }

        var perioderSøker = finnUttakResultatPerioderSøker(behandling.getId());
        var filter = new UttakResultatPerioderDto.FilterDto(kreverSammenhengendeUttak, utenMinsterett,
            RelasjonsRolleType.erMor(behandling.getRelasjonsRolleType()));
        var annenForelderHarRett = ytelseFordeling.map(yf -> UttakOmsorgUtil.harAnnenForelderRett(yf, annenpartUttak)).orElse(false);
        var aleneomsorg = ytelseFordeling.map(UttakOmsorgUtil::harAleneomsorg).orElse(false);
        var annenForelderRettEØS = ytelseFordeling.map(UttakOmsorgUtil::avklartAnnenForelderHarRettEØS).orElse(false);
        var oppgittAnnenForelderRettEØS = ytelseFordeling.map(UttakOmsorgUtil::oppgittAnnenForelderRettEØS).orElse(false);
        return new UttakResultatPerioderDto(perioderSøker,
            annenpartUttaksperioder, annenForelderHarRett, aleneomsorg,
            annenForelderRettEØS, oppgittAnnenForelderRettEØS, filter);
    }

    private Optional<Behandling> annenpartBehandling(Behandling søkersBehandling) {
        if (harVedtak(søkersBehandling)) {
            return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeBehandlingPåVedtakstidspunkt(søkersBehandling);
        }

        return relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(søkersBehandling.getFagsak().getSaksnummer());
    }

    private boolean harVedtak(Behandling søkersBehandling) {
        return behandlingVedtakRepository.hentForBehandlingHvisEksisterer(søkersBehandling.getId()).isPresent();
    }

    private List<UttakResultatPeriodeDto> finnUttakResultatPerioderSøker(Long behandling) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandling).map(uttak -> finnUttakResultatPerioder(uttak, behandling)).orElse(List.of());
    }

    private List<UttakResultatPeriodeDto> finnUttakResultatPerioder(ForeldrepengerUttak uttakResultat, Long behandling) {
        var gjeldenePerioder = uttakResultat.getGjeldendePerioder();

        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandling);
        List<UttakResultatPeriodeDto> list = new ArrayList<>();
        for (var entitet : gjeldenePerioder) {
            var periode = map(entitet, iayGrunnlag);
            list.add(periode);
        }

        return sortedByFom(list);
    }

    private UttakResultatPeriodeDto map(ForeldrepengerUttakPeriode periode,
                                        Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        var dto = new UttakResultatPeriodeDto.Builder()
            .medTidsperiode(periode.getFom(), periode.getTom())
            .medManuellBehandlingÅrsak(periode.getManuellBehandlingÅrsak())
            .medUtsettelseType(periode.getUtsettelseType())
            .medPeriodeResultatType(periode.getResultatType())
            .medBegrunnelse(periode.getBegrunnelse())
            .medPeriodeResultatÅrsak(periode.getResultatÅrsak())
            .medFlerbarnsdager(periode.isFlerbarnsdager())
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medSamtidigUttaksprosent(periode.getSamtidigUttaksprosent())
            .medGraderingInnvilget(periode.isGraderingInnvilget())
            .medGraderingAvslåttÅrsak(periode.getGraderingAvslagÅrsak())
            .medOppholdÅrsak(periode.getOppholdÅrsak())
            .medPeriodeType(periode.getSøktKonto())
            .medMottattDato(periode.getMottattDato())
            .medTidligstMottattDato(periode.getTidligstMottatttDato())
            .build();

        for (var aktivitet : periode.getAktiviteter()) {
            dto.leggTilAktivitet(map(aktivitet, inntektArbeidYtelseGrunnlag, periode.isOpprinneligSendtTilManuellBehandling()));
        }
        return dto;
    }

    private List<UttakResultatPeriodeDto> sortedByFom(List<UttakResultatPeriodeDto> list) {
        return list
            .stream()
            .sorted(Comparator.comparing(UttakResultatPeriodeDto::getFom))
            .toList();
    }

    private UttakResultatPeriodeAktivitetDto map(ForeldrepengerUttakPeriodeAktivitet aktivitet,
                                                 Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag,
                                                 boolean opprinneligSendtTilManuellBehandling) {
        var builder = new UttakResultatPeriodeAktivitetDto.Builder()
            .medProsentArbeid(aktivitet.getArbeidsprosent())
            .medGradering(aktivitet.isSøktGraderingForAktivitetIPeriode())
            .medTrekkdager(aktivitet.getTrekkdager())
            .medStønadskontoType(aktivitet.getTrekkonto())
            .medUttakArbeidType(aktivitet.getUttakArbeidType());
        mapArbeidsforhold(aktivitet, builder, inntektArbeidYtelseGrunnlag);
        if (!opprinneligSendtTilManuellBehandling) {
            builder.medUtbetalingsgrad(aktivitet.getUtbetalingsgrad());
        }
        return builder.build();
    }

    private void mapArbeidsforhold(ForeldrepengerUttakPeriodeAktivitet aktivitet,
                                   UttakResultatPeriodeAktivitetDto.Builder builder,
                                   Optional<InntektArbeidYtelseGrunnlag> inntektArbeidYtelseGrunnlag) {
        var arbeidsgiverOptional = aktivitet.getUttakAktivitet().getArbeidsgiver();
        var arbeidsgiverReferanse = arbeidsgiverOptional.map(Arbeidsgiver::getIdentifikator).orElse(null);
        var ref = aktivitet.getArbeidsforholdRef();
        if (ref != null && inntektArbeidYtelseGrunnlag.isPresent() && inntektArbeidYtelseGrunnlag.get().getArbeidsforholdInformasjon().isPresent()
            && arbeidsgiverOptional.isPresent()) {
            var eksternArbeidsforholdId = inntektArbeidYtelseGrunnlag.orElseThrow()
                .getArbeidsforholdInformasjon()
                .orElseThrow()
                .finnEkstern(arbeidsgiverOptional.get(), ref);
            builder.medArbeidsforhold(ref, eksternArbeidsforholdId.getReferanse(), arbeidsgiverReferanse);
        } else {
            builder.medArbeidsforhold(null, null, arbeidsgiverReferanse);
        }
    }
}
