package ar.dentaflow.dominio.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.comun.ConflictoException;
import org.junit.jupiter.api.Test;

/** Spec turnos — "Sin solapamiento por profesional ni por sillón" (design D4, Q7). */
class TurnoSolapadoExceptionTest {

    @Test
    void esUnConflictoQueNombraElProfesional() {
        TurnoSolapadoException error = new TurnoSolapadoException(RecursoOcupado.PROFESIONAL);

        assertThat(error).isInstanceOf(ConflictoException.class);
        assertThat(error.recurso()).isEqualTo(RecursoOcupado.PROFESIONAL);
        assertThat(error.getMessage()).containsIgnoringCase("profesional");
    }

    @Test
    void esUnConflictoQueNombraElSillon() {
        TurnoSolapadoException error = new TurnoSolapadoException(RecursoOcupado.SILLON);

        assertThat(error.recurso()).isEqualTo(RecursoOcupado.SILLON);
        assertThat(error.getMessage()).containsIgnoringCase("sillón").doesNotContainIgnoringCase("profesional");
    }
}
