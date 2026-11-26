package ar.dentaflow.presentacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Spec "Permisos sobre el horario" y "Permisos sobre los bloqueos": el rol profesional solo ve lo
 * suyo (y los bloqueos del consultorio); administrador y recepción ven todo.
 */
class AccesoPorProfesionalTest {

    private static final Instant INICIO = Instant.parse("2026-12-24T11:00:00Z");
    private static final Instant FIN = Instant.parse("2026-12-24T16:00:00Z");
    private static final UsuarioAutenticado ADMIN = new UsuarioAutenticado(1L, "a@example.com", Rol.ADMINISTRADOR, null);
    private static final UsuarioAutenticado RECEPCION = new UsuarioAutenticado(2L, "r@example.com", Rol.RECEPCION, null);
    private static final UsuarioAutenticado PROFESIONAL_A = new UsuarioAutenticado(3L, "p@example.com", Rol.PROFESIONAL, 10L);
    private static final UsuarioAutenticado PROFESIONAL_SIN_VINCULO =
            new UsuarioAutenticado(4L, "s@example.com", Rol.PROFESIONAL, null);

    @Test
    void administradorYRecepcionAccedenACualquierProfesional() {
        assertThatCode(() -> AccesoPorProfesional.verificarProfesional(ADMIN, 99L)).doesNotThrowAnyException();
        assertThatCode(() -> AccesoPorProfesional.verificarProfesional(RECEPCION, 99L)).doesNotThrowAnyException();
    }

    @Test
    void elRolProfesionalSoloAccedeAlSuyo() {
        assertThatCode(() -> AccesoPorProfesional.verificarProfesional(PROFESIONAL_A, 10L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> AccesoPorProfesional.verificarProfesional(PROFESIONAL_A, 11L))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> AccesoPorProfesional.verificarProfesional(PROFESIONAL_SIN_VINCULO, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void profesionalPropioSoloParaElRolProfesional() {
        assertThat(AccesoPorProfesional.profesionalPropio(ADMIN)).isEmpty();
        assertThat(AccesoPorProfesional.profesionalPropio(PROFESIONAL_A)).contains(10L);
        assertThatThrownBy(() -> AccesoPorProfesional.profesionalPropio(PROFESIONAL_SIN_VINCULO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void puedeVerBloqueoPropioODelConsultorio() {
        Bloqueo deA = Bloqueo.deProfesional(10L, INICIO, FIN, MotivoBloqueo.AUSENCIA, null, 1L, INICIO);
        Bloqueo deB = Bloqueo.deProfesional(11L, INICIO, FIN, MotivoBloqueo.AUSENCIA, null, 1L, INICIO);
        Bloqueo deSillon = Bloqueo.deSillon(5L, INICIO, FIN, MotivoBloqueo.OTRO, null, 1L, INICIO);
        Bloqueo general = Bloqueo.delConsultorio(INICIO, FIN, MotivoBloqueo.FERIADO, null, 1L, INICIO);

        assertThat(AccesoPorProfesional.puedeVerBloqueo(PROFESIONAL_A, deA)).isTrue();
        assertThat(AccesoPorProfesional.puedeVerBloqueo(PROFESIONAL_A, general)).isTrue();
        assertThat(AccesoPorProfesional.puedeVerBloqueo(PROFESIONAL_A, deB)).isFalse();
        assertThat(AccesoPorProfesional.puedeVerBloqueo(PROFESIONAL_A, deSillon)).isFalse();
        assertThat(AccesoPorProfesional.puedeVerBloqueo(PROFESIONAL_SIN_VINCULO, general)).isFalse();
        assertThat(AccesoPorProfesional.puedeVerBloqueo(RECEPCION, deSillon)).isTrue();
        assertThat(AccesoPorProfesional.puedeVerBloqueo(ADMIN, deB)).isTrue();
    }
}
