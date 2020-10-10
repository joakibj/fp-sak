package no.nav.foreldrepenger.behandling.revurdering;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
public class FagsakRevurderingTest {

    @Mock
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;
    private Behandling nyesteBehandling;
    private Behandling eldreBehandling;
    private Fagsak fagsak;
    private Saksnummer fagsakSaksnummer  = new Saksnummer("1");

    private Fagsak fagsakMedFlereBehandlinger;
    private Saksnummer fagsakMedFlereBehSaksnr  = new Saksnummer("2");

    @BeforeEach
    public void opprettBehandlinger() {
        fagsak = FagsakBuilder.nyEngangstønadForMor().medSaksnummer(fagsakSaksnummer).build();
        behandling = Behandling.forFørstegangssøknad(fagsak).build();

        fagsakMedFlereBehandlinger = FagsakBuilder.nyEngangstønadForMor().medSaksnummer(fagsakMedFlereBehSaksnr).build();
        nyesteBehandling = Behandling.forFørstegangssøknad(fagsakMedFlereBehandlinger)
            .medAvsluttetDato(LocalDateTime.now())
            .build();
        eldreBehandling = Behandling.forFørstegangssøknad(fagsakMedFlereBehandlinger)
            .medAvsluttetDato(LocalDateTime.now().minusDays(1))
            .build();
        lenient().when(behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.of(behandling));
    }

    @Test
    public void kanIkkeOppretteRevurderingNårÅpenBehandling() throws Exception {
        Behandlingsresultat.opprettFor(behandling);
        lenient().when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(any())).thenReturn(singletonList(behandling));

        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);
        assertThat(kanRevurderingOpprettes).isFalse();
    }

    @Test
    public void kanOppretteRevurderingNårÅpenKlage() {
        behandling.avsluttBehandling();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(behandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR,
            VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.VM_1005, null,
            Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O, false, false, null, null).buildFor(behandling);
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId())).thenReturn(Collections.emptyList());


        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);
        assertThat(kanRevurderingOpprettes).isTrue();
    }

    @Test
    public void kanOppretteRevurderingNårÅpentInnsyn() {
        behandling.avsluttBehandling();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(behandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR,
            VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.VM_1005, null,
            Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O, false, false, null, null).buildFor(behandling);
        when(behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId())).thenReturn(Collections.emptyList());

        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);
        assertThat(kanRevurderingOpprettes).isTrue();
    }

    @Test
    public void kanIkkeOppretteRevurderingNårBehandlingErHenlagt() {
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET).buildFor(behandling);
        lenient().when(behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.empty());
        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);

        assertThat(kanRevurderingOpprettes).isFalse();
    }

    @Test
    public void kanOppretteRevurderingDersomBehandlingErVedtatt() {
        behandling.avsluttBehandling();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(behandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.FØDSELSVILKÅRET_MOR,
            VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.VM_1005, null,
            Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O, false, false, null, null).buildFor(behandling);

        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);

        assertThat(kanRevurderingOpprettes).isTrue();
    }

    @Test
    public void kanIkkeOppretteRevurderingDersomAvlagPåSøkersOpplysningsplikt() {
        behandling.avsluttBehandling();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(behandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.SØKERSOPPLYSNINGSPLIKT,
            VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.VM_1005, null,
            Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O, false, false, null, null).buildFor(behandling);

        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);

        assertThat(kanRevurderingOpprettes).isFalse();
    }

    @Test
    public void kanOppretteRevurderingNårEnBehandlingErVedtattMenSisteBehandlingErHenlagt() {
        eldreBehandling.avsluttBehandling();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET).buildFor(nyesteBehandling);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(eldreBehandling);

        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.SØKERSOPPLYSNINGSPLIKT,
            VilkårUtfallType.OPPFYLT, null, null,
            null, false, false, null, null).buildFor(eldreBehandling);
        lenient().when(behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.of(eldreBehandling));
        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsakMedFlereBehandlinger);

        assertThat(kanRevurderingOpprettes).isTrue();
    }

    @Test
    public void kanOppretteRevurderingNårFlereBehandlingerErVedtattOgSisteKanRevurderes() {
        eldreBehandling.avsluttBehandling();
        nyesteBehandling.avsluttBehandling();
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(eldreBehandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.SØKERSOPPLYSNINGSPLIKT,
            VilkårUtfallType.IKKE_OPPFYLT, null, null,
            null, false, false, null, null).buildFor(eldreBehandling);

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(nyesteBehandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.SØKERSOPPLYSNINGSPLIKT,
            VilkårUtfallType.OPPFYLT, null, null,
            null, false, false, null, null).buildFor(nyesteBehandling);
        lenient().when(behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.of(nyesteBehandling));
        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsakMedFlereBehandlinger);

        assertThat(kanRevurderingOpprettes).isTrue();
    }

    @Test
    public void kanIkkeOppretteRevurderingNårFlereBehandlingerErVedtattOgSisteIkkeKanRevurderes() {
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(nyesteBehandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.SØKERSOPPLYSNINGSPLIKT,
            VilkårUtfallType.IKKE_OPPFYLT, null, null,
            null, false, false, null, null).buildFor(nyesteBehandling);

        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(eldreBehandling);
        VilkårResultat.builder().leggTilVilkårResultat(VilkårType.SØKERSOPPLYSNINGSPLIKT,
            VilkårUtfallType.OPPFYLT, null, null,
            null, false, false, null, null).buildFor(eldreBehandling);
        lenient().when(behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(any())).thenReturn(Optional.of(nyesteBehandling));
        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsakMedFlereBehandlinger);

        assertThat(kanRevurderingOpprettes).isFalse();
    }

    @Test
    public void behandlingerSkalSorteresSynkendePåAvsluttetDato(){
        Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        LocalDateTime now = LocalDateTime.now();
        Behandling nyBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(now).build();
        Behandling gammelBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(now.minusDays(1)).build();

        FagsakRevurdering.BehandlingAvsluttetDatoComparator behandlingAvsluttetDatoComparator = new FagsakRevurdering.BehandlingAvsluttetDatoComparator();

        List<Behandling> behandlinger = asList(nyBehandling, gammelBehandling);
        List<Behandling> sorterteBehandlinger = behandlinger.stream().sorted(behandlingAvsluttetDatoComparator).collect(Collectors.toList());

        assertThat(sorterteBehandlinger.get(0).getAvsluttetDato()).isEqualTo(now);
    }

    @Test
    public void behandlingerSkalSorteresSynkendePåOpprettetDatoNårAvsluttetDatoErNull(){
        Fagsak fagsak = FagsakBuilder.nyEngangstønadForMor().build();
        LocalDateTime now = LocalDateTime.now();
        Behandling nyBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(null).medOpprettetDato(now).build();
        Behandling gammelBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(now).medOpprettetDato(now.minusDays(1)).build();

        FagsakRevurdering.BehandlingAvsluttetDatoComparator behandlingAvsluttetDatoComparator = new FagsakRevurdering.BehandlingAvsluttetDatoComparator();

        List<Behandling> behandlinger = asList(nyBehandling, gammelBehandling);
        List<Behandling> sorterteBehandlinger = behandlinger.stream().sorted(behandlingAvsluttetDatoComparator).collect(Collectors.toList());

        assertThat(sorterteBehandlinger.get(0).getAvsluttetDato()).isNull();
        assertThat(sorterteBehandlinger.get(0).getOpprettetDato()).isEqualTo(now);
    }
}
