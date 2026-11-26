package ar.dentaflow.aplicacion.agenda;

import java.util.List;

/**
 * Puerto de lectura de la agenda (C-11, design D7). Solo lee: la escritura de turnos sigue siendo exclusiva de
 * {@code ServicioDeTurnos} (C-09). Devuelve las filas ordenadas por inicio, apellido y nombre del profesional,
 * nombre del sillón e id del turno.
 */
public interface LecturaDeAgenda {

    List<FilaDeAgenda> buscar(CriterioDeAgenda criterio);
}
