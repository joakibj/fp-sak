package no.nav.foreldrepenger.dokumentbestiller.vedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ExtendWith(MockitoExtension.class)
public class VedtaksbrevUtlederTest {

    @Mock
    private Behandlingsresultat behandlingsresultatMock;
    @Mock
    private BehandlingVedtak behandlingVedtakMock;
    @Mock
    private KlageVurderingResultat klageVurderingResultat;
    @Mock
    private AnkeVurderingResultatEntitet ankeVurderingResultat;

    @Mock
    private Behandling behandling;
    @Mock
    private KlageRepository klageRepository;
    @Mock
    private AnkeRepository ankeRepository;
    @Mock
    private InnvilgelseFpLanseringTjeneste innvilgelseFpLanseringTjeneste;

    @BeforeEach
    public void setup() {
        lenient().doReturn(Vedtaksbrev.AUTOMATISK).when(behandlingsresultatMock).getVedtaksbrev();
        lenient().doReturn(false).when(behandlingVedtakMock).isBeslutningsvedtak();
        lenient().doReturn(VedtakResultatType.INNVILGET).when(behandlingVedtakMock).getVedtakResultatType();
        lenient().doReturn(FagsakYtelseType.ENGANGSTØNAD).when(behandling).getFagsakYtelseType();
        lenient().doReturn(DokumentMalType.INNVILGELSE_FORELDREPENGER_DOK).when(innvilgelseFpLanseringTjeneste).velgFpInnvilgelsesmal(behandling);
    }

    @Test
    public void skal_velge_positivt_ES() {
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository,
                ankeRepository, innvilgelseFpLanseringTjeneste)).isEqualTo(DokumentMalType.INNVILGELSE_ENGANGSSTØNAD);
    }

    @Test
    public void skal_velge_negativt_ES() {
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository,
                ankeRepository, innvilgelseFpLanseringTjeneste)).isEqualTo(DokumentMalType.AVSLAG_ENGANGSSTØNAD);
    }

    @Test
    public void skal_velge_positivt_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository,
                ankeRepository, innvilgelseFpLanseringTjeneste)).isEqualTo(DokumentMalType.INNVILGELSE_FORELDREPENGER_DOK);
    }

    @Test
    public void skal_velge_opphør_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(true).when(behandlingsresultatMock).isBehandlingsresultatOpphørt();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository,
                ankeRepository, innvilgelseFpLanseringTjeneste)).isEqualTo(DokumentMalType.OPPHØR_DOK);
    }

    @Test
    public void skal_velge_avslag_FP() {
        doReturn(FagsakYtelseType.FORELDREPENGER).when(behandling).getFagsakYtelseType();
        doReturn(VedtakResultatType.AVSLAG).when(behandlingVedtakMock).getVedtakResultatType();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository,
                ankeRepository, innvilgelseFpLanseringTjeneste)).isEqualTo(DokumentMalType.AVSLAG_FORELDREPENGER_DOK);
    }

    @Test
    public void skal_velge_uendret_utfall() {
        doReturn(true).when(behandlingVedtakMock).isBeslutningsvedtak();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository,
                ankeRepository, innvilgelseFpLanseringTjeneste)).isEqualTo(DokumentMalType.INGEN_ENDRING);
    }

    @Test
    public void skal_velge_fritekst() {
        doReturn(Vedtaksbrev.FRITEKST).when(behandlingsresultatMock).getVedtaksbrev();
        assertThat(VedtaksbrevUtleder.velgDokumentMalForVedtak(behandling, behandlingsresultatMock, behandlingVedtakMock, klageRepository,
                ankeRepository, innvilgelseFpLanseringTjeneste)).isEqualTo(DokumentMalType.FRITEKST_DOK);
    }

    @Test
    public void skal_velge_riktig_klagemal() {
        doReturn(Optional.of(klageVurderingResultat)).when(klageRepository).hentGjeldendeKlageVurderingResultat(behandling);

        doReturn(KlageVurdering.STADFESTE_YTELSESVEDTAK).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_STADFESTET);

        doReturn(KlageVurdering.AVVIS_KLAGE).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_AVVIST);

        doReturn(KlageVurdering.OPPHEVE_YTELSESVEDTAK).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_HJEMSENDT);

        doReturn(KlageVurdering.MEDHOLD_I_KLAGE).when(klageVurderingResultat).getKlageVurdering();
        assertThat(VedtaksbrevUtleder.velgKlagemal(behandling, klageRepository)).isEqualTo(DokumentMalType.KLAGE_OMGJØRING);
    }
    @Test
    public void skal_velge_riktig_ankemal() {
        doReturn(Optional.of(ankeVurderingResultat)).when(ankeRepository).hentAnkeVurderingResultat(any());

        doReturn(AnkeVurdering.ANKE_HJEMSEND_UTEN_OPPHEV).when(ankeVurderingResultat).getAnkeVurdering();
        assertThat(VedtaksbrevUtleder.velgAnkemal(behandling, ankeRepository)).isEqualTo(DokumentMalType.ANKEBREV_BESLUTNING_OM_OPPHEVING);

        doReturn(AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE).when(ankeVurderingResultat).getAnkeVurdering();
        assertThat(VedtaksbrevUtleder.velgAnkemal(behandling, ankeRepository)).isEqualTo(DokumentMalType.ANKEBREV_BESLUTNING_OM_OPPHEVING);

        doReturn(AnkeVurdering.ANKE_OMGJOER).when(ankeVurderingResultat).getAnkeVurdering();
        assertThat(VedtaksbrevUtleder.velgAnkemal(behandling, ankeRepository)).isEqualTo(DokumentMalType.VEDTAK_OMGJORING_ANKE_DOK);
    }
}
