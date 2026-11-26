package ar.dentaflow.aplicacion.reportes;

import ar.dentaflow.dominio.reportes.RegistroAsistenciaParaReporte;
import java.time.Instant;
import java.util.List;

/**
 * Puerto de lectura de turnos con asistencia registrada para reportes de ausentismo (C-23, design D2).
 * Solo lectura; nunca selecciona datos sensibles del paciente (DNI, teléfono, email ni token).
 */
public interface LecturaDeAusentismo {

    List<RegistroAsistenciaParaReporte> buscarEvaluados(Instant desde, Instant hasta, Long profesionalId);
}
