package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;


class VilkårDtoMapper {

    private VilkårDtoMapper() {
        // SONAR - Utility classes should not have public constructors
    }

    static List<VilkårDto> lagVilkarDto(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (behandlingsresultat != null) {
            var vilkårResultat = behandlingsresultat.getVilkårResultat();
            if (vilkårResultat != null) {
                var list = vilkårResultat.getVilkårene().stream().map(vilkår -> {
                    var dto = new VilkårDto();
                    dto.setAvslagKode(vilkår.getAvslagsårsak() != null ? vilkår.getAvslagsårsak().getKode() : null);
                    dto.setVilkarType(vilkår.getVilkårType());
                    dto.setLovReferanse(vilkår.getVilkårType().getLovReferanse(behandling.getFagsakYtelseType()));
                    dto.setVilkarStatus(vilkår.getGjeldendeVilkårUtfall());
                    dto.setMerknadParametere(vilkår.getMerknadParametere());
                    dto.setOverstyrbar(erOverstyrbar(vilkår, behandling));

                    return dto;
                }).collect(Collectors.toList());
                return list;
            }
        }
        return Collections.emptyList();
    }

    // Angir om vilkåret kan overstyres (forutsetter at bruker har tilgang til å overstyre)
    private static boolean erOverstyrbar(Vilkår vilkår, Behandling behandling) {
        if (behandling.erÅpnetForEndring()) {
            // Manuelt åpnet for endring, må dermed også tillate overstyring
            return true;
        }
        if (!behandling.harSattStartpunkt() || BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            // Startpunkt for behandling N/A, må dermed også tillate overstyring
            return true;
        }
        var vilkårLøstFørStartpunkt = StartpunktType.finnVilkårHåndtertInnenStartpunkt(behandling.getStartpunkt())
            .contains(vilkår.getVilkårType());
        return !vilkårLøstFørStartpunkt;
    }
}
