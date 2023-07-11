package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.mottak.registrerer.DokumentRegistrererTjeneste;
import no.nav.foreldrepenger.mottak.registrerer.ManuellRegistreringAksjonspunktDto;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.xml.soeknad.v3.ObjectFactory;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellRegistreringDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellRegistreringOppdaterer implements AksjonspunktOppdaterer<ManuellRegistreringDto> {

    private FagsakRepository fagsakRepository;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private DokumentRegistrererTjeneste dokumentRegistrererTjeneste;

    private Instance<SøknadMapper> søknadMappere;

    ManuellRegistreringOppdaterer() {
        // CDI
    }

    @Inject
    public ManuellRegistreringOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                         HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                         DokumentRegistrererTjeneste dokumentRegistrererTjeneste,
                                         @Any Instance<SøknadMapper> søknadMappere) {
        this.søknadMappere = søknadMappere;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.dokumentRegistrererTjeneste = dokumentRegistrererTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(ManuellRegistreringDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingReferanse = param.getRef();
        var behandlingId = param.getBehandlingId();
        var resultatBuilder = OppdateringResultat.utenTransisjon();

        if (dto.getUfullstendigSoeknad()) {
            var adapter = new ManuellRegistreringAksjonspunktDto(!dto.getUfullstendigSoeknad());
            dokumentRegistrererTjeneste.aksjonspunktManuellRegistrering(behandlingReferanse, adapter)
                .ifPresent(ad -> resultatBuilder.medEkstraAksjonspunktResultat(ad, AksjonspunktStatus.OPPRETTET));
            lagHistorikkInnslag(behandlingId, HistorikkinnslagType.MANGELFULL_SØKNAD, null);
            return resultatBuilder
                .leggTilIkkeVurdertVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT).build();
        }

        ManuellRegistreringValidator.validerOpplysninger(dto);
        if (FagsakYtelseType.FORELDREPENGER.equals(behandlingReferanse.fagsakYtelseType())) {
            ManuellRegistreringValidator.validerAktivitetskrav(dto, behandlingReferanse.relasjonRolle());
        }

        var fagsak = fagsakRepository.finnEksaktFagsak(behandlingReferanse.fagsakId());
        var navBruker = fagsak.getNavBruker();
        var søknadXml = opprettSøknadsskjema(dto, behandlingReferanse, navBruker);
        var dokumentTypeId = finnDokumentType(dto, behandlingReferanse.behandlingType());

        var adapter = new ManuellRegistreringAksjonspunktDto(!dto.getUfullstendigSoeknad(), søknadXml,
            dokumentTypeId, dto.getMottattDato(), dto.isRegistrerVerge());
        dokumentRegistrererTjeneste.aksjonspunktManuellRegistrering(behandlingReferanse, adapter)
            .ifPresent(ad -> resultatBuilder.medEkstraAksjonspunktResultat(ad, AksjonspunktStatus.OPPRETTET));

        lagHistorikkInnslag(behandlingId, HistorikkinnslagType.REGISTRER_PAPIRSØK, dto.getKommentarEndring());
        return resultatBuilder.build();
    }

    private DokumentTypeId finnDokumentType(ManuellRegistreringDto dto, BehandlingType behandlingType) {
        var søknadsType = dto.getSoknadstype().getKode();

        if (FagsakYtelseType.ENGANGSTØNAD.getKode().equals(søknadsType)) {
            if (erFødsel(dto)) {
                return DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
            }
            if (erAdopsjon(dto)) {
                return DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON;
            }
        } else if (FagsakYtelseType.FORELDREPENGER.getKode().equals(søknadsType)) {
            if (erEndringssøknad(behandlingType)) {
                return DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;
            }
            if (erFødsel(dto)) {
                return DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
            }
            if (erAdopsjon(dto)) {
                return DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON;
            }
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.getKode().equals(søknadsType)) {
            return DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER;

        }
        return DokumentTypeId.UDEFINERT;
    }

    private boolean erEndringssøknad(BehandlingType behandlingType) {
        return behandlingType.equals(BehandlingType.REVURDERING);
    }

    private boolean erAdopsjon(ManuellRegistreringDto dto) {
        return FamilieHendelseType.ADOPSJON.getKode().equals(dto.getTema().getKode()) || FamilieHendelseType.OMSORG.getKode()
            .equals(dto.getTema().getKode());
    }

    private boolean erFødsel(ManuellRegistreringDto dto) {
        return FamilieHendelseType.FØDSEL.getKode().equals(dto.getTema().getKode());
    }

    private String opprettSøknadsskjema(ManuellRegistreringDto dto, BehandlingReferanse behandlingReferanse, NavBruker navBruker) {

        var ytelseType = behandlingReferanse.fagsakYtelseType();
        var behandlingType = behandlingReferanse.behandlingType();

        if (REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER.equals(dto.getAksjonspunktDefinisjon())) {
            // minihack for
            behandlingType = BehandlingType.REVURDERING;
        }

        var mapper = finnSøknadMapper(ytelseType, behandlingType);
        var søknad = mapper.mapSøknad(dto, navBruker);

        try {
            return JaxbHelper.marshalAndValidateJaxb(SøknadConstants.JAXB_CLASS,
                new ObjectFactory().createSoeknad(søknad),
                SøknadConstants.XSD_LOCATION,
                SøknadConstants.ADDITIONAL_XSD_LOCATION,
                SøknadConstants.ADDITIONAL_CLASSES);
        } catch (JAXBException | SAXException e) {
            throw new TekniskException("FP-453254", "Feil ved marshalling av søknadsskjema", e);
        }
    }

    private void lagHistorikkInnslag(Long behandlingId, HistorikkinnslagType innslagType, String kommentarEndring) {
        var innslag = new Historikkinnslag();
        var builder = new HistorikkInnslagTekstBuilder();

        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(innslagType);
        builder.medHendelse(innslagType);
        if (kommentarEndring != null) {
            builder.medBegrunnelse(kommentarEndring);
        }
        builder.build(innslag);
        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    public SøknadMapper finnSøknadMapper(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(SøknadMapper.class, søknadMappere, ytelseType, behandlingType).orElseThrow();
    }
}
