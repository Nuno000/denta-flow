package ar.dentaflow.aplicacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Spec bloqueos-agenda — ámbito, recurso activo, registro del autor, edición sin cambiar el ámbito,
 * consulta por rango y aviso de turnos afectados sin tocarlos (design D7).
 */
@ExtendWith(MockitoExtension.class)
class ServicioDeBloqueosTest {

    private static final Instant AHORA = Instant.parse("2026-12-02T00:30:00Z");
    private static final Instant INICIO = Instant.parse("2026-12-24T11:00:00Z");
    private static final Instant FIN = Instant.parse("2026-12-24T16:00:00Z");
    private static final Long ADMIN = 7L;
    private static final Long PROFESIONAL = 3L;
    private static final Long SILLON = 5L;

    @Mock
    private BloqueoRepositorio bloqueos;

    @Mock
    private ProfesionalRepositorio profesionales;

    @Mock
    private SillonRepositorio sillones;

    @Mock
    private TurnosAfectadosPorBloqueo turnosAfectados;

    private ServicioDeBloqueos servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioDeBloqueos(bloqueos, profesionales, sillones, turnosAfectados,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Profesional profesional(boolean activo) {
        Profesional profesional = AyudanteDeIds.conId(
                Profesional.nuevo("Laura", "Ejemplo", null, "laura@example.com"), PROFESIONAL);
        if (!activo) {
            profesional.desactivar();
        }
        return profesional;
    }

    private static Sillon sillon(boolean activo) {
        Sillon sillon = AyudanteDeIds.conId(Sillon.nuevo("Sillón 1"), SILLON);
        if (!activo) {
            sillon.desactivar();
        }
        return sillon;
    }

    private void guardarDevuelveElArgumento() {
        when(bloqueos.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private ResultadoBloqueo crear(AmbitoBloqueo ambito, Long profesionalId, Long sillonId) {
        return servicio.crear(ambito, profesionalId, sillonId, INICIO, FIN, MotivoBloqueo.AUSENCIA, null, ADMIN);
    }

    @Test
    void crearDeProfesionalLoBloqueaAntesDeGuardarYRegistraAutor() {
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        guardarDevuelveElArgumento();

        ResultadoBloqueo resultado = crear(AmbitoBloqueo.PROFESIONAL, PROFESIONAL, null);

        Bloqueo bloqueo = resultado.bloqueo();
        assertThat(bloqueo.getAmbito()).isEqualTo(AmbitoBloqueo.PROFESIONAL);
        assertThat(bloqueo.getModificadoPor()).isEqualTo(ADMIN);
        assertThat(bloqueo.getModificadoEn()).isEqualTo(AHORA);
        InOrder orden = inOrder(profesionales, bloqueos, turnosAfectados);
        orden.verify(profesionales).bloquearPorId(PROFESIONAL);
        orden.verify(bloqueos).guardar(any());
        orden.verify(turnosAfectados).listar(bloqueo);
        verifyNoInteractions(sillones);
    }

    @Test
    void crearDeSillonBloqueaElSillon() {
        when(sillones.bloquearPorId(SILLON)).thenReturn(Optional.of(sillon(true)));
        guardarDevuelveElArgumento();

        ResultadoBloqueo resultado = crear(AmbitoBloqueo.SILLON, null, SILLON);

        assertThat(resultado.bloqueo().getSillonId()).isEqualTo(SILLON);
        verify(sillones).bloquearPorId(SILLON);
        verifyNoInteractions(profesionales);
    }

    @Test
    void crearDelConsultorioNoBloqueaRecursos() {
        guardarDevuelveElArgumento();

        ResultadoBloqueo resultado = crear(AmbitoBloqueo.CONSULTORIO, null, null);

        assertThat(resultado.bloqueo().getAmbito()).isEqualTo(AmbitoBloqueo.CONSULTORIO);
        verifyNoInteractions(profesionales, sillones);
    }

    @Test
    void recursoInexistenteEs404YInactivoNoSePermite() {
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.empty());
        when(sillones.bloquearPorId(SILLON)).thenReturn(Optional.of(sillon(false)));

        assertThatThrownBy(() -> crear(AmbitoBloqueo.PROFESIONAL, PROFESIONAL, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> crear(AmbitoBloqueo.SILLON, null, SILLON))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verify(bloqueos, never()).guardar(any());
    }

    @Test
    void profesionalInactivoNoSePermite() {
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(false)));

        assertThatThrownBy(() -> crear(AmbitoBloqueo.PROFESIONAL, PROFESIONAL, null))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verify(bloqueos, never()).guardar(any());
    }

    @Test
    void ambitoYRecursoIncoherentesSonDatoInvalido() {
        assertThatThrownBy(() -> crear(null, null, null)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> crear(AmbitoBloqueo.PROFESIONAL, null, null))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> crear(AmbitoBloqueo.PROFESIONAL, PROFESIONAL, SILLON))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> crear(AmbitoBloqueo.SILLON, PROFESIONAL, null))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> crear(AmbitoBloqueo.CONSULTORIO, null, SILLON))
                .isInstanceOf(ValidacionDeDominioException.class);
        verifyNoInteractions(profesionales, sillones, bloqueos);
    }

    @Test
    void devuelveLosTurnosAfectadosSinPedirNadaMasAlPuerto() {
        TurnoAfectado turno = new TurnoAfectado(41L, INICIO, INICIO.plus(Duration.ofMinutes(30)), PROFESIONAL, SILLON);
        guardarDevuelveElArgumento();
        when(turnosAfectados.listar(any())).thenReturn(List.of(turno));

        ResultadoBloqueo resultado = crear(AmbitoBloqueo.CONSULTORIO, null, null);

        assertThat(resultado.turnosAfectados()).containsExactly(turno);
        verify(turnosAfectados).listar(resultado.bloqueo());
        verifyNoMoreInteractions(turnosAfectados);
    }

    @Test
    void laImplementacionProvisoriaNoInformaTurnos() {
        Bloqueo bloqueo = Bloqueo.delConsultorio(INICIO, FIN, MotivoBloqueo.FERIADO, null, ADMIN, AHORA);

        assertThat(new SinTurnosAfectados().listar(bloqueo)).isEmpty();
    }

    private Bloqueo guardadoDeProfesional() {
        return AyudanteDeIds.conId(
                Bloqueo.deProfesional(PROFESIONAL, INICIO, FIN, MotivoBloqueo.AUSENCIA, null, ADMIN, AHORA), 20L);
    }

    @Test
    void editarConOtroAmbitoORecursoEsDatoInvalidoYNoCambia() {
        Bloqueo bloqueo = guardadoDeProfesional();
        when(bloqueos.buscarPorId(20L)).thenReturn(Optional.of(bloqueo));
        Instant nuevoFin = FIN.plusSeconds(3600);

        assertThatThrownBy(() -> servicio.editar(20L, INICIO, nuevoFin, MotivoBloqueo.OTRO, null,
                AmbitoBloqueo.CONSULTORIO, null, null, 9L)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.editar(20L, INICIO, nuevoFin, MotivoBloqueo.OTRO, null,
                null, 4L, null, 9L)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.editar(20L, INICIO, nuevoFin, MotivoBloqueo.OTRO, null,
                null, null, SILLON, 9L)).isInstanceOf(ValidacionDeDominioException.class);

        assertThat(bloqueo.getFin()).isEqualTo(FIN);
        verify(bloqueos, never()).guardar(any());
    }

    @Test
    void editarConAmbitoIgualUOmitidoRegistraElNuevoAutor() {
        Bloqueo bloqueo = guardadoDeProfesional();
        when(bloqueos.buscarPorId(20L)).thenReturn(Optional.of(bloqueo));
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        guardarDevuelveElArgumento();
        Instant nuevoFin = FIN.plusSeconds(3600);

        ResultadoBloqueo resultado = servicio.editar(20L, INICIO, nuevoFin, MotivoBloqueo.VACACIONES, "Viaje",
                AmbitoBloqueo.PROFESIONAL, PROFESIONAL, null, 9L);

        assertThat(resultado.bloqueo().getFin()).isEqualTo(nuevoFin);
        assertThat(resultado.bloqueo().getModificadoPor()).isEqualTo(9L);
        assertThat(resultado.turnosAfectados()).isEmpty();
        servicio.editar(20L, INICIO, FIN, MotivoBloqueo.AUSENCIA, null, null, null, null, 9L);
        assertThat(bloqueo.getFin()).isEqualTo(FIN);
    }

    @Test
    void inexistenteEs404() {
        when(bloqueos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.editar(99L, INICIO, FIN, MotivoBloqueo.OTRO, null, null, null, null, 9L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(bloqueos, never()).eliminar(any());
    }

    @Test
    void eliminarBorraElBloqueo() {
        Bloqueo bloqueo = guardadoDeProfesional();
        when(bloqueos.buscarPorId(20L)).thenReturn(Optional.of(bloqueo));

        servicio.eliminar(20L);

        verify(bloqueos).eliminar(bloqueo);
    }

    @Test
    void listarExigeUnRangoValidoDeHasta366Dias() {
        assertThatThrownBy(() -> servicio.listar(null, FIN, null, null)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.listar(INICIO, null, null, null)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.listar(FIN, INICIO, null, null)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.listar(INICIO, INICIO.plus(Duration.ofDays(367)), null, null))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("366");
        verifyNoInteractions(bloqueos);
    }

    @Test
    void listarDelegaConLosFiltros() {
        Instant hasta = INICIO.plus(Duration.ofDays(366));
        List<Bloqueo> encontrados = List.of(guardadoDeProfesional());
        when(bloqueos.listarQueSeSuperponen(INICIO, hasta, PROFESIONAL, null)).thenReturn(encontrados);
        when(bloqueos.listarQueAfectanAProfesional(PROFESIONAL, INICIO, hasta)).thenReturn(encontrados);

        assertThat(servicio.listar(INICIO, hasta, PROFESIONAL, null)).isEqualTo(encontrados);
        assertThat(servicio.listarQueAfectanAProfesional(PROFESIONAL, INICIO, hasta)).isEqualTo(encontrados);
    }
}
