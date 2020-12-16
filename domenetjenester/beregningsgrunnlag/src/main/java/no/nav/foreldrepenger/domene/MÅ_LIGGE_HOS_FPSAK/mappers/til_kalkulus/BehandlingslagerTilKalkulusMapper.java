package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningRefusjonPeriodeDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaAktørDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.FaktaArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulator.modell.typer.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningAktivitetHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.kalkulus.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonPeriodeEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class BehandlingslagerTilKalkulusMapper {


    public static BeregningsgrunnlagDto mapBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlagFraFpsak) {
        BeregningsgrunnlagDto.Builder builder = BeregningsgrunnlagDto.builder();

        //med
        builder.medGrunnbeløp(beregningsgrunnlagFraFpsak.getGrunnbeløp().getVerdi());
        builder.medOverstyring(beregningsgrunnlagFraFpsak.isOverstyrt());
        builder.medSkjæringstidspunkt(beregningsgrunnlagFraFpsak.getSkjæringstidspunkt());
        if (beregningsgrunnlagFraFpsak.getSammenligningsgrunnlag().isPresent()) {
            builder.medSammenligningsgrunnlag(BGMapperTilKalkulus.mapSammenligningsgrunnlag(beregningsgrunnlagFraFpsak.getSammenligningsgrunnlag().get()));
        }

        //lister
        beregningsgrunnlagFraFpsak.getAktivitetStatuser().forEach(beregningsgrunnlagAktivitetStatus -> builder.leggTilAktivitetStatus(BGMapperTilKalkulus.mapAktivitetStatus(beregningsgrunnlagAktivitetStatus)));
        beregningsgrunnlagFraFpsak.getBeregningsgrunnlagPerioder().forEach(beregningsgrunnlagPeriode -> builder.leggTilBeregningsgrunnlagPeriode(BGMapperTilKalkulus.mapBeregningsgrunnlagPeriode(beregningsgrunnlagPeriode)));
        builder.leggTilFaktaOmBeregningTilfeller(beregningsgrunnlagFraFpsak.getFaktaOmBeregningTilfeller().stream().map(fakta -> FaktaOmBeregningTilfelle.fraKode(fakta.getKode())).collect(Collectors.toList()));
        beregningsgrunnlagFraFpsak.getSammenligningsgrunnlagPrStatusListe().forEach(sammenligningsgrunnlagPrStatus -> builder.leggTilSammenligningsgrunnlag(BGMapperTilKalkulus.mapSammenligningsgrunnlagMedStatus(sammenligningsgrunnlagPrStatus)));

        return builder.build();
    }

    public static BeregningRefusjonOverstyringerDto mapRefusjonOverstyring(BeregningRefusjonOverstyringerEntitet refusjonOverstyringerFraFpsak) {
        BeregningRefusjonOverstyringerDto.Builder dtoBuilder = BeregningRefusjonOverstyringerDto.builder();

        refusjonOverstyringerFraFpsak.getRefusjonOverstyringer().forEach(beregningRefusjonOverstyring -> {
            List<BeregningRefusjonPeriodeDto> refusjonsperioder = beregningRefusjonOverstyring.getRefusjonPerioder().stream()
                .map(BehandlingslagerTilKalkulusMapper::mapRefusjonperiode)
                .collect(Collectors.toList());
            BeregningRefusjonOverstyringDto dto = new BeregningRefusjonOverstyringDto(
                IAYMapperTilKalkulus.mapArbeidsgiver(beregningRefusjonOverstyring.getArbeidsgiver()),
                beregningRefusjonOverstyring.getFørsteMuligeRefusjonFom().orElse(null),
                refusjonsperioder);
            dtoBuilder.leggTilOverstyring(dto);
        });
        return dtoBuilder.build();
    }

    public static BeregningRefusjonPeriodeDto mapRefusjonperiode(BeregningRefusjonPeriodeEntitet refusjonPeriodeEntitet) {
        return new BeregningRefusjonPeriodeDto(refusjonPeriodeEntitet.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(refusjonPeriodeEntitet.getArbeidsforholdRef()), refusjonPeriodeEntitet.getStartdatoRefusjon());
    }


    public static BeregningAktivitetAggregatDto mapSaksbehandletAktivitet(BeregningAktivitetAggregatEntitet saksbehandletAktiviteterFraFpsak) {
        BeregningAktivitetAggregatDto.Builder dtoBuilder = BeregningAktivitetAggregatDto.builder();
        dtoBuilder.medSkjæringstidspunktOpptjening(saksbehandletAktiviteterFraFpsak.getSkjæringstidspunktOpptjening());
        saksbehandletAktiviteterFraFpsak.getBeregningAktiviteter().forEach(mapAktivitet(dtoBuilder));
        return dtoBuilder.build();
    }

    private static Consumer<BeregningAktivitetEntitet> mapAktivitet(BeregningAktivitetAggregatDto.Builder dtoBuilder) {
        return beregningAktivitet -> {
            BeregningAktivitetDto.Builder builder = BeregningAktivitetDto.builder();
            builder.medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef()));
            builder.medArbeidsgiver(beregningAktivitet.getArbeidsgiver() == null ? null : IAYMapperTilKalkulus.mapArbeidsgiver(beregningAktivitet.getArbeidsgiver()));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(beregningAktivitet.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(beregningAktivitet.getPeriode()));
            dtoBuilder.leggTilAktivitet(builder.build());
        };
    }

    private static Intervall mapDatoIntervall(ÅpenDatoIntervallEntitet periode) {
        return periode.getTomDato() == null ? Intervall.fraOgMed(periode.getFomDato()) : Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    public static BeregningAktivitetOverstyringerDto mapAktivitetOverstyring(BeregningAktivitetOverstyringerEntitet beregningAktivitetOverstyringerFraFpsak) {
        BeregningAktivitetOverstyringerDto.Builder dtoBuilder = BeregningAktivitetOverstyringerDto.builder();
        beregningAktivitetOverstyringerFraFpsak.getOverstyringer().forEach(overstyring -> {
            BeregningAktivitetOverstyringDto.Builder builder = BeregningAktivitetOverstyringDto.builder();
            builder.medArbeidsforholdRef(overstyring.getArbeidsforholdRef() == null ? null : IAYMapperTilKalkulus.mapArbeidsforholdRef(overstyring.getArbeidsforholdRef()));
            overstyring.getArbeidsgiver().ifPresent(arbeidsgiver -> builder.medArbeidsgiver(IAYMapperTilKalkulus.mapArbeidsgiver(arbeidsgiver)));
            builder.medHandling(overstyring.getHandling() == null ? null : BeregningAktivitetHandlingType.fraKode(overstyring.getHandling().getKode()));
            builder.medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(overstyring.getOpptjeningAktivitetType().getKode()));
            builder.medPeriode(mapDatoIntervall(overstyring.getPeriode()));
            dtoBuilder.leggTilOverstyring(builder.build());
        });
        return dtoBuilder.build();
    }

    public static BeregningsgrunnlagGrunnlagDto mapGrunnlag(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagFraFpsak) {
        BeregningsgrunnlagGrunnlagDtoBuilder oppdatere = BeregningsgrunnlagGrunnlagDtoBuilder.oppdatere(Optional.empty());

        beregningsgrunnlagFraFpsak.getBeregningsgrunnlag().ifPresent(beregningsgrunnlagDto -> oppdatere.medBeregningsgrunnlag(mapBeregningsgrunnlag(beregningsgrunnlagDto)));
        beregningsgrunnlagFraFpsak.getOverstyring().ifPresent(beregningAktivitetOverstyringerDto -> oppdatere.medOverstyring(mapAktivitetOverstyring(beregningAktivitetOverstyringerDto)));
        oppdatere.medRegisterAktiviteter(mapRegisterAktiviteter(beregningsgrunnlagFraFpsak.getRegisterAktiviteter()));
        beregningsgrunnlagFraFpsak.getSaksbehandletAktiviteter().ifPresent(beregningAktivitetAggregatDto -> oppdatere.medSaksbehandletAktiviteter(mapSaksbehandletAktivitet(beregningAktivitetAggregatDto)));
        beregningsgrunnlagFraFpsak.getRefusjonOverstyringer().ifPresent(beregningRefusjonOverstyringerDto -> oppdatere.medRefusjonOverstyring(mapRefusjonOverstyring(beregningRefusjonOverstyringerDto)));
        beregningsgrunnlagFraFpsak.getBeregningsgrunnlag().flatMap(BehandlingslagerTilKalkulusMapper::mapFaktaAggregat).ifPresent(oppdatere::medFaktaAggregat);
        return oppdatere.build(BeregningsgrunnlagTilstand.fraKode(beregningsgrunnlagFraFpsak.getBeregningsgrunnlagTilstand().getKode()));
    }

    private static BeregningAktivitetAggregatDto mapRegisterAktiviteter(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        BeregningAktivitetAggregatDto.Builder builder = BeregningAktivitetAggregatDto.builder();
        builder.medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getBeregningAktiviteter().forEach(mapAktivitet(builder));
        return builder.build();
    }

    public static Optional<FaktaAggregatDto> mapFaktaAggregat(BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        // I fakta om beregning settes alle faktaavklaringer på første periode og vi kan derfor bruke denne til å hente ut avklart fakta
        var førstePeriode = beregningsgrunnlagEntitet.getBeregningsgrunnlagPerioder().get(0);
        var andeler = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        FaktaAggregatDto.Builder faktaAggregatBuilder = FaktaAggregatDto.builder();
        mapFaktaArbeidsforhold(andeler).forEach(faktaAggregatBuilder::erstattEksisterendeEllerLeggTil);
        mapFaktaAktør(andeler, beregningsgrunnlagEntitet.getFaktaOmBeregningTilfeller())
            .ifPresent(faktaAggregatBuilder::medFaktaAktør);
        return faktaAggregatBuilder.manglerFakta() ? Optional.empty() : Optional.of(faktaAggregatBuilder.build());
    }

    private static List<FaktaArbeidsforholdDto> mapFaktaArbeidsforhold(List<BeregningsgrunnlagPrStatusOgAndel> andeler) {
        return andeler.stream()
            .filter(a -> a.getAktivitetStatus().erArbeidstaker())
            .filter(a -> a.getArbeidsgiver().isPresent())
            .map(a -> {
                FaktaArbeidsforholdDto.Builder builder = new FaktaArbeidsforholdDto.Builder(IAYMapperTilKalkulus.mapArbeidsgiver(a.getArbeidsgiver().get()),
                    a.getArbeidsforholdRef().map(IAYMapperTilKalkulus::mapArbeidsforholdRef).orElse(InternArbeidsforholdRefDto.nullRef()));
                a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getErTidsbegrensetArbeidsforhold).ifPresent(builder::medErTidsbegrenset);
                a.mottarYtelse().ifPresent(builder::medHarMottattYtelse);
                a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden).ifPresent(builder::medHarLønnsendringIBeregningsperioden);
                return builder.manglerFakta() ? null : builder.build();
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static Optional<FaktaAktørDto> mapFaktaAktør(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
        FaktaAktørDto.Builder faktaAktørBuilder = FaktaAktørDto.builder();
        mapEtterlønnSluttpakke(faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapMottarFLYtelse(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyIArbeidslivetSN(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapSkalBesteberegnes(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        mapErNyoppstartetFL(andeler, faktaOmBeregningTilfeller, faktaAktørBuilder);
        return faktaAktørBuilder.erUgyldig() ? Optional.empty() : Optional.of(faktaAktørBuilder.build());
    }

    private static void mapErNyoppstartetFL(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                            List<no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller,
                                            FaktaAktørDto.Builder faktaAktørBuilder) {
        boolean harVurdertNyoppstartetFL = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL));
        if (harVurdertNyoppstartetFL) {
            andeler.stream().filter(a -> a.getAktivitetStatus().erFrilanser() && a.erNyoppstartet().isPresent())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::erNyoppstartet)
                .map(Optional::get)
                .ifPresent(faktaAktørBuilder::medErNyoppstartetFL);
        }
    }

    private static void mapSkalBesteberegnes(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        boolean harVurdertBesteberegning = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING));
        if (harVurdertBesteberegning) {
            boolean harFastsattBesteberegning = andeler.stream().anyMatch(a -> a.getBesteberegningPrÅr() != null);
            faktaAktørBuilder.medSkalBesteberegnes(harFastsattBesteberegning);
        }
    }

    private static void mapErNyIArbeidslivetSN(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        boolean harVurdertNyIArbeidslivetSN = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle.VURDER_SN_NY_I_ARBEIDSLIVET));
        if (harVurdertNyIArbeidslivetSN) {
            andeler.stream().filter(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::getNyIArbeidslivet)
                .ifPresent(faktaAktørBuilder::medErNyIArbeidslivetSN);
        }
    }

    private static void mapMottarFLYtelse(List<BeregningsgrunnlagPrStatusOgAndel> andeler, List<no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        boolean harVurdertMottarYtelse = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE));
        if (harVurdertMottarYtelse) {
            andeler.stream().filter(a -> a.getAktivitetStatus().erFrilanser() && a.mottarYtelse().isPresent())
                .findFirst()
                .map(BeregningsgrunnlagPrStatusOgAndel::mottarYtelse)
                .map(Optional::get)
                .ifPresent(faktaAktørBuilder::medHarFLMottattYtelse);
        }
    }

    private static void mapEtterlønnSluttpakke(List<no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller, FaktaAktørDto.Builder faktaAktørBuilder) {
        boolean harVurdertEtterlønnSluttpakke = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle.VURDER_ETTERLØNN_SLUTTPAKKE));
        boolean harEtterlønnSlutpakke = faktaOmBeregningTilfeller.stream().anyMatch(tilfelle -> tilfelle.equals(no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle.FASTSETT_ETTERLØNN_SLUTTPAKKE));
        if (harVurdertEtterlønnSluttpakke) {
            faktaAktørBuilder.medMottarEtterlønnSluttpakke(harEtterlønnSlutpakke);
        }
    }

}
