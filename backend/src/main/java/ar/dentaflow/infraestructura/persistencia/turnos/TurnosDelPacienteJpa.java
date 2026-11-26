package ar.dentaflow.infraestructura.persistencia.turnos;

import ar.dentaflow.dominio.pacientes.TurnosDelPaciente;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Implementación de {@link TurnosDelPaciente} sobre el repositorio de turnos; solo lee (C-15). */
@Component
public class TurnosDelPacienteJpa implements TurnosDelPaciente {

    private final TurnoRepositorio turnos;

    public TurnosDelPacienteJpa(TurnoRepositorio turnos) {
        this.turnos = turnos;
    }

    @Override
    public boolean hayActivosFuturos(long pacienteId, Instant ahora) {
        return turnos.hayTurnosActivosFuturosDelPaciente(pacienteId, ahora);
    }
}
