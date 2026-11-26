package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.dominio.comun.ConflictoException;
import java.util.List;

/**
 * La carga manual chocó (profesional o sillón ocupado, fuera de horario o bloqueo) y se sugieren horarios libres
 * cercanos del mismo profesional (C-12, design D3 y D4). Conserva el mensaje original; la lista puede venir vacía.
 */
public class ConflictoConAlternativasException extends ConflictoException {

    private final List<AlternativaDeTurno> alternativas;

    public ConflictoConAlternativasException(String mensaje, List<AlternativaDeTurno> alternativas) {
        super(mensaje);
        this.alternativas = List.copyOf(alternativas);
    }

    public List<AlternativaDeTurno> alternativas() {
        return alternativas;
    }
}
