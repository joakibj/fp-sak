package no.nav.foreldrepenger.domene.uttak.svp;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.vedtak.util.FPDateUtil;

class GrunnlagOppretter {

    private UttakRepositoryProvider repositoryProvider;

    GrunnlagOppretter(UttakRepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;

    }

    Behandling lagreBehandling() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        return scenario.lagre(repositoryProvider);
    }

    SvpGrunnlagEntitet lagTilrettelegging(Behandling behandling) {
        var jordmorsDato = LocalDate.of(2019, Month.APRIL, 1);
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsDato)
            .medIngenTilrettelegging(jordmorsDato)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medKopiertFraTidligereBehandling(false)
            .medMottattTidspunkt(FPDateUtil.nå())
            .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        return svpGrunnlag;
    }

    void lagreUttaksgrenser(Behandling behandling, LocalDate førsteLovligeUttaksdag, LocalDate mottaksdato) {
        var uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandling.getBehandlingsresultat())
            .medFørsteLovligeUttaksdag(førsteLovligeUttaksdag)
            .medMottattDato(mottaksdato)
            .build();
        repositoryProvider.getUttaksperiodegrenseRepository().lagre(behandling.getId(), uttaksperiodegrense);
    }
}
