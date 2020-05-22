package no.nav.foreldrepenger.domene.medlem.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.domene.medlem.api.FinnMedlemRequest;
import no.nav.foreldrepenger.domene.medlem.api.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.medlem.api.MedlemskapsperiodeKoder;
import no.nav.foreldrepenger.domene.medlem.impl.rest.Medlemskapsunntak;
import no.nav.foreldrepenger.domene.medlem.impl.rest.MedlemsunntakRestKlient;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.kodeverk.Statuskode;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.medl.MedlemConsumer;

@ApplicationScoped
public class HentMedlemskapFraRegister {

    private static final Logger LOG = LoggerFactory.getLogger(HentMedlemskapFraRegister.class);

    private MedlemConsumer medlemConsumer;
    private MedlemsunntakRestKlient restKlient;
    private KodeverkRepository kodeverkRepository;

    HentMedlemskapFraRegister() {
        // CDI
    }

    @Inject
    public HentMedlemskapFraRegister(MedlemConsumer medlemConsumer,
                                     MedlemsunntakRestKlient restKlient,
                                     KodeverkRepository kodeverkRepository) {
        this.medlemConsumer = medlemConsumer;
        this.restKlient = restKlient;
        this.kodeverkRepository = kodeverkRepository;
    }

    public List<Medlemskapsperiode> finnMedlemskapPerioder(FinnMedlemRequest finnMedlemRequest, AktørId aktørId) {
        var perioder = finnMedlemskapPerioder(finnMedlemRequest);
        sammenlignLoggWSRS(aktørId, finnMedlemRequest, perioder);
        return perioder;
        //return finnPerioderRest(aktørId, finnMedlemRequest);
    }

    public List<Medlemskapsperiode> finnMedlemskapPerioder(FinnMedlemRequest finnMedlemRequest) {
        HentPeriodeListeRequest request = new HentPeriodeListeRequest();
        List<Medlemskapsperiode> medlemskapsperiodeList;
        request.setIdent(new Foedselsnummer().withValue(finnMedlemRequest.getFnr()));
        request.setInkluderPerioderFraOgMed(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(finnMedlemRequest.getFom()));
        request.setInkluderPerioderTilOgMed(DateUtil.convertToXMLGregorianCalendarRemoveTimezone(finnMedlemRequest.getTom()));
        request.withInkluderStatuskodeListe(
            new Statuskode().withValue(MedlemskapsperiodeKoder.PeriodeStatus.GYLD.toString()),
            new Statuskode().withValue(MedlemskapsperiodeKoder.PeriodeStatus.INNV.toString()),
            new Statuskode().withValue(MedlemskapsperiodeKoder.PeriodeStatus.UAVK.toString()));

        try {
            HentPeriodeListeResponse response = medlemConsumer.hentPeriodeListe(request);
            medlemskapsperiodeList = oversettFraPeriodeListeResponse(response);
        } catch (PersonIkkeFunnet ex) {
            throw MedlemFeil.FACTORY.feilVedKallTilMedlem(ex).toException();
        } catch (Sikkerhetsbegrensning ex) {
            throw MedlemFeil.FACTORY.fikkSikkerhetsavvikFraMedlem(ex).toException();
        }

        return medlemskapsperiodeList;
    }

    private List<Medlemskapsperiode> oversettFraPeriodeListeResponse(HentPeriodeListeResponse response) {
        return response.getPeriodeListe().stream().map(this::oversettFraPeriode).collect(Collectors.toList());
    }

    private Medlemskapsperiode oversettFraPeriode(Medlemsperiode medlemsperiode) {
        return new Medlemskapsperiode.Builder()
            .medFom(DateUtil.convertToLocalDate(medlemsperiode.getFraOgMed()))
            .medTom(DateUtil.convertToLocalDate(medlemsperiode.getTilOgMed()))
            .medDatoBesluttet(DateUtil.convertToLocalDate(medlemsperiode.getDatoBesluttet()))
            .medErMedlem(bestemErMedlem(medlemsperiode.getType().getValue()))
            .medKilde(mapTilKilde(medlemsperiode.getKilde().getValue()))
            .medDekning(mapTilDekning(medlemsperiode.getTrygdedekning().getValue()))
            .medLovvalg(mapTilLovvalg(medlemsperiode.getLovvalg().getValue()))
            .medLovvalgsland(finnLovvalgsland(medlemsperiode))
            .medStudieland(finnStudieland(medlemsperiode))
            .medMedlId(medlemsperiode.getId())
            .build();
    }

    private  List<Medlemskapsperiode> finnPerioderRest(AktørId aktørId, FinnMedlemRequest wsreq) {
        try {
            var mups = restKlient.finnMedlemsunntak(aktørId, wsreq.getFom(), wsreq.getTom()).stream()
                .map(this::mapFraMedlemsunntak)
                .collect(Collectors.toList());
            LOG.info("MEDL2 REST RS {}", mups);
            return mups;
        } catch (Exception e) {
            throw MedlemFeil.FACTORY.feilVedKallTilMedlem(e).toException();
        }
    }

    private void sammenlignLoggWSRS(AktørId aktørId, FinnMedlemRequest wsreq, List<Medlemskapsperiode> medlemskapsperioderWS) {
        try {
            var mups = restKlient.finnMedlemsunntak(aktørId, wsreq.getFom(), wsreq.getTom()).stream()
                .map(this::mapFraMedlemsunntak)
                .collect(Collectors.toList());
            if (mups.containsAll(medlemskapsperioderWS) && medlemskapsperioderWS.containsAll(mups)) {
                LOG.info("MEDL2 REST likt svar WS RS");
            } else {
                LOG.info("MEDL2 REST avvik WS {} RS {}", medlemskapsperioderWS, mups);
            }
        } catch (Exception e) {
            LOG.info("MEDL2 REST feil", e);
        }
    }

    private Medlemskapsperiode mapFraMedlemsunntak(Medlemskapsunntak medlemsperiode) {
        return new Medlemskapsperiode.Builder()
            .medFom(medlemsperiode.getFraOgMed())
            .medTom(medlemsperiode.getTilOgMed())
            .medDatoBesluttet(medlemsperiode.getSporingsinformasjon().getBesluttet())
            .medErMedlem(medlemsperiode.isMedlem())
            .medKilde(mapTilKilde(medlemsperiode.getSporingsinformasjon().getKilde()))
            .medDekning(mapTilDekning(medlemsperiode.getDekning()))
            .medLovvalg(mapTilLovvalg(medlemsperiode.getLovvalg()))
            .medLovvalgsland(finnLovvalgsland(medlemsperiode.getLovvalgsland()))
            .medStudieland(finnStudieland(medlemsperiode.getStudieinformasjon()))
            .medMedlId(medlemsperiode.getUnntakId())
            .build();
    }

    private Landkoder finnStudieland(Medlemsperiode medlemsperiode) {
        if (medlemsperiode.getStudieinformasjon() != null
            && medlemsperiode.getStudieinformasjon().getStudieland() != null) {
            return kodeverkRepository.finn(Landkoder.class, medlemsperiode.getStudieinformasjon().getStudieland().getValue());
        }
        return null;
    }

    private Landkoder finnLovvalgsland(Medlemsperiode medlemsperiode) {
        if (medlemsperiode.getLand() != null) {
            return kodeverkRepository.finn(Landkoder.class, medlemsperiode.getLand().getValue());
        }
        return null;
    }

    private Landkoder finnStudieland(Medlemskapsunntak.Studieinformasjon studieinformasjon) {
        if (studieinformasjon != null && studieinformasjon.getStudieland() != null) {
            return kodeverkRepository.finn(Landkoder.class, studieinformasjon.getStudieland());
        }
        return null;
    }

    private Landkoder finnLovvalgsland(String land) {
        if (land != null) {
            return kodeverkRepository.finn(Landkoder.class, land);
        }
        return null;
    }


    private MedlemskapDekningType mapTilDekning(String trygdeDekning) {
        MedlemskapDekningType dekningType = MedlemskapDekningType.UDEFINERT;
        if (trygdeDekning != null) {
            dekningType = MedlemskapsperiodeKoder.getDekningMap().get(trygdeDekning);
            if (dekningType == null) {
                dekningType = MedlemskapDekningType.UDEFINERT;
            }
        }
        return dekningType;
    }

    private MedlemskapType mapTilLovvalg(String lovvalg) {
        MedlemskapType medlemskapType = MedlemskapType.UDEFINERT;
        if (lovvalg != null) {
            if (MedlemskapsperiodeKoder.Lovvalg.ENDL.name().compareTo(lovvalg) == 0) {
                medlemskapType = MedlemskapType.ENDELIG;
            }
            if (MedlemskapsperiodeKoder.Lovvalg.UAVK.name().compareTo(lovvalg) == 0) {
                medlemskapType = MedlemskapType.UNDER_AVKLARING;
            }
            if (MedlemskapsperiodeKoder.Lovvalg.FORL.name().compareTo(lovvalg) == 0) {
                medlemskapType = MedlemskapType.FORELOPIG;
            }
        }
        return medlemskapType;
    }

    private MedlemskapKildeType mapTilKilde(String kilde) {
        MedlemskapKildeType kildeType = MedlemskapKildeType.UDEFINERT;
        if (kilde != null) {
            kildeType = MedlemskapKildeType.fraKode(kilde);
            if (kildeType == null) {
                kildeType = MedlemskapKildeType.ANNEN;
            }
            if (MedlemskapKildeType.SRVGOSYS.equals(kildeType)) {
                kildeType = MedlemskapKildeType.FS22;
            }
            if (MedlemskapKildeType.SRVMELOSYS.equals(kildeType)) {
                kildeType = MedlemskapKildeType.MEDL;
            }
        }
        return kildeType;
    }

    private boolean bestemErMedlem(String value) {
        boolean erMedlem = false;
        if (value != null) {
            if (MedlemskapsperiodeKoder.PeriodeType.PMMEDSKP.name().compareTo(value) == 0) {
                erMedlem = true;
            }
            if (MedlemskapsperiodeKoder.PeriodeType.PUMEDSKP.name().compareTo(value) == 0) {
                erMedlem = false;
            }
            if (MedlemskapsperiodeKoder.PeriodeType.E500INFO.name().compareTo(value) == 0) {
                erMedlem = false;
            }
        }
        return erMedlem;
    }
}
