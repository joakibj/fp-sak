package no.nav.foreldrepenger.behandlingslager.behandling.nøkkeltallbehandling;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;


public record NøkkeltallBehandlingVentestatus(String behandlendeEnhet, BehandlingType behandlingType,
                                              BehandlingVenteStatus behandlingVenteStatus, LocalDate førsteUttakMåned,
                                              int antall) {

}
