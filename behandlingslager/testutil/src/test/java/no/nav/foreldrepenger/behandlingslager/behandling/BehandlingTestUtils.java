package no.nav.foreldrepenger.behandlingslager.behandling;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.AktørId;

class BehandlingTestUtils {

    static void leggTilAnnenPartPaSokand(Long behandlingId, AktørId aktørId, BehandlingRepositoryProvider repositoryProvider) {
        final OppgittAnnenPartBuilder oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder()
            .medAktørId(aktørId)
            .medType(SøknadAnnenPartType.FAR);

        final PersonopplysningRepository repository = repositoryProvider.getPersonopplysningRepository();
        repository.lagre(behandlingId, oppgittAnnenPartBuilder);
    }

    static Behandling byggForElektroniskSøknadOmFødsel(Fagsak fagsak, LocalDate fødselsdato, LocalDate mottattDato,
                                                       String ansvarligSaksbehandler, BehandlingRepositoryProvider repositoryProvider) {
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        behandling.setAnsvarligSaksbehandler(ansvarligSaksbehandler);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        final FamilieHendelseRepository fgRepository = repositoryProvider.getFamilieHendelseRepository();
        final FamilieHendelseBuilder hendelseBuilder = fgRepository.opprettBuilderFor(behandling);
        hendelseBuilder.medFødselsDato(fødselsdato).medAntallBarn(1);

        fgRepository.lagre(behandling, hendelseBuilder);
        repositoryProvider.getSøknadRepository().lagreOgFlush(behandling, new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .medMottattDato(mottattDato)
            .medElektroniskRegistrert(true)
            .build());

        return behandling;
    }
}
