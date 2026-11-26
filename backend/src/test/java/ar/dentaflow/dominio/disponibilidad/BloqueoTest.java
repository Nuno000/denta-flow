package ar.dentaflow.dominio.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Spec bloqueos-agenda — "Ámbito del bloqueo", "Franja absoluta", "Motivo del bloqueo",
 * "Registro de quién grabó el bloqueo" y "Edición y baja del bloqueo".
 */
class BloqueoTest {

    private static final Instant NUEVE = Instant.parse("2026-12-24T12:00:00Z");
    private static final Instant DIEZ = Instant.parse("2026-12-24T13:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-12-02T00:30:00Z");
    private static final Long ADMIN = 7L;

    private static Bloqueo deProfesional(Long profesionalId, Instant inicio, Instant fin) {
        return Bloqueo.deProfesional(profesionalId, inicio, fin, MotivoBloqueo.AUSENCIA, null, ADMIN, AHORA);
    }

    @Test
    void fabricasDejanAmbitoYRecursoCoherentesYRegistranAutor() {
        Bloqueo profesional = deProfesional(3L, NUEVE, DIEZ);
        Bloqueo sillon = Bloqueo.deSillon(5L, NUEVE, DIEZ, MotivoBloqueo.OTRO, "Mantenimiento", ADMIN, AHORA);
        Bloqueo consultorio = Bloqueo.delConsultorio(NUEVE, DIEZ, MotivoBloqueo.FERIADO, null, ADMIN, AHORA);

        assertThat(profesional.getAmbito()).isEqualTo(AmbitoBloqueo.PROFESIONAL);
        assertThat(profesional.getProfesionalId()).isEqualTo(3L);
        assertThat(profesional.getSillonId()).isNull();
        assertThat(sillon.getAmbito()).isEqualTo(AmbitoBloqueo.SILLON);
        assertThat(sillon.getSillonId()).isEqualTo(5L);
        assertThat(sillon.getProfesionalId()).isNull();
        assertThat(sillon.getDetalle()).isEqualTo("Mantenimiento");
        assertThat(consultorio.getAmbito()).isEqualTo(AmbitoBloqueo.CONSULTORIO);
        assertThat(consultorio.getProfesionalId()).isNull();
        assertThat(consultorio.getSillonId()).isNull();
        assertThat(consultorio.getInicio()).isEqualTo(NUEVE);
        assertThat(consultorio.getFin()).isEqualTo(DIEZ);
        assertThat(consultorio.getMotivo()).isEqualTo(MotivoBloqueo.FERIADO);
        assertThat(consultorio.getModificadoPor()).isEqualTo(ADMIN);
        assertThat(consultorio.getModificadoEn()).isEqualTo(AHORA);
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThatThrownBy(() -> deProfesional(null, NUEVE, DIEZ))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("profesional");
        assertThatThrownBy(() -> Bloqueo.deSillon(null, NUEVE, DIEZ, MotivoBloqueo.OTRO, null, ADMIN, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("sillón");
        assertThatThrownBy(() -> deProfesional(3L, DIEZ, DIEZ))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("anterior");
        assertThatThrownBy(() -> deProfesional(3L, DIEZ, NUEVE))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> deProfesional(3L, null, DIEZ))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("inicio");
        assertThatThrownBy(() -> Bloqueo.delConsultorio(NUEVE, DIEZ, null, null, ADMIN, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("motivo");
        assertThatThrownBy(() -> Bloqueo.delConsultorio(NUEVE, DIEZ, MotivoBloqueo.OTRO, "x".repeat(201), ADMIN, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("200");
        assertThatThrownBy(() -> Bloqueo.delConsultorio(NUEVE, DIEZ, MotivoBloqueo.OTRO, null, null, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void detalleDe200ConEspaciosSeRecorta() {
        String detalle = "  " + "d".repeat(200) + "  ";

        Bloqueo bloqueo = Bloqueo.delConsultorio(NUEVE, DIEZ, MotivoBloqueo.OTRO, detalle, ADMIN, AHORA);

        assertThat(bloqueo.getDetalle()).hasSize(200);
        assertThat(Bloqueo.delConsultorio(NUEVE, DIEZ, MotivoBloqueo.OTRO, "  ", ADMIN, AHORA).getDetalle()).isNull();
    }

    @Test
    void reprogramarCambiaFranjaMotivoDetalleYAutor() {
        Bloqueo bloqueo = deProfesional(3L, NUEVE, DIEZ);
        Instant once = Instant.parse("2026-12-24T14:00:00Z");
        Instant despues = AHORA.plusSeconds(60);

        bloqueo.reprogramar(NUEVE, once, MotivoBloqueo.VACACIONES, "Viaje", 9L, despues);

        assertThat(bloqueo.getFin()).isEqualTo(once);
        assertThat(bloqueo.getMotivo()).isEqualTo(MotivoBloqueo.VACACIONES);
        assertThat(bloqueo.getDetalle()).isEqualTo("Viaje");
        assertThat(bloqueo.getModificadoPor()).isEqualTo(9L);
        assertThat(bloqueo.getModificadoEn()).isEqualTo(despues);
        assertThat(bloqueo.getAmbito()).isEqualTo(AmbitoBloqueo.PROFESIONAL);
        assertThat(bloqueo.getProfesionalId()).isEqualTo(3L);
    }

    @Test
    void reprogramarInvalidoNoCambiaNada() {
        Bloqueo bloqueo = deProfesional(3L, NUEVE, DIEZ);

        assertThatThrownBy(() -> bloqueo.reprogramar(DIEZ, NUEVE, MotivoBloqueo.OTRO, "x", 9L, AHORA.plusSeconds(1)))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThat(bloqueo.getInicio()).isEqualTo(NUEVE);
        assertThat(bloqueo.getFin()).isEqualTo(DIEZ);
        assertThat(bloqueo.getMotivo()).isEqualTo(MotivoBloqueo.AUSENCIA);
        assertThat(bloqueo.getDetalle()).isNull();
        assertThat(bloqueo.getModificadoPor()).isEqualTo(ADMIN);
        assertThat(bloqueo.getModificadoEn()).isEqualTo(AHORA);
    }

    @Test
    void afectaASuRecursoYAlConsultorio() {
        Bloqueo deA = deProfesional(3L, NUEVE, DIEZ);
        Bloqueo deSillon = Bloqueo.deSillon(5L, NUEVE, DIEZ, MotivoBloqueo.OTRO, null, ADMIN, AHORA);
        Bloqueo general = Bloqueo.delConsultorio(NUEVE, DIEZ, MotivoBloqueo.FERIADO, null, ADMIN, AHORA);

        assertThat(deA.afectaAProfesional(3L)).isTrue();
        assertThat(deA.afectaAProfesional(4L)).isFalse();
        assertThat(deA.afectaASillon(3L)).isFalse();
        assertThat(deSillon.afectaASillon(5L)).isTrue();
        assertThat(deSillon.afectaASillon(6L)).isFalse();
        assertThat(deSillon.afectaAProfesional(5L)).isFalse();
        assertThat(general.afectaAProfesional(3L)).isTrue();
        assertThat(general.afectaASillon(5L)).isTrue();
    }

    @Test
    void superposicionConIntervalosSemiabiertos() {
        Bloqueo bloqueo = deProfesional(3L, NUEVE, DIEZ);

        assertThat(bloqueo.seSuperponeCon(DIEZ, DIEZ.plusSeconds(1800))).isFalse();
        assertThat(bloqueo.seSuperponeCon(NUEVE.minusSeconds(1800), NUEVE)).isFalse();
        assertThat(bloqueo.seSuperponeCon(DIEZ.minusSeconds(60), DIEZ.plusSeconds(1800))).isTrue();
        assertThat(bloqueo.seSuperponeCon(NUEVE.plusSeconds(600), NUEVE.plusSeconds(1200))).isTrue();
    }
}
