package ar.dentaflow.aplicacion.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.usuarios.CatalogoDeRoles;
import ar.dentaflow.dominio.usuarios.CodificadorDeCredenciales;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.RolDisponible;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Spec gestion-usuarios — reglas de la gestión de usuarios (design D6). */
class ServicioDeUsuariosTest {

    private static final Long ADMIN_ID = 1L;

    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final ProfesionalRepositorio profesionales = mock(ProfesionalRepositorio.class);
    private final CodificadorDeCredenciales codificador = mock(CodificadorDeCredenciales.class);
    private final CatalogoDeRoles catalogo = mock(CatalogoDeRoles.class);
    private final ServicioDeUsuarios servicio =
            new ServicioDeUsuarios(usuarios, profesionales, codificador, catalogo);

    private Profesional laura;
    private Usuario recepcion;

    @BeforeEach
    void preparar() {
        when(codificador.codificar(anyString())).thenAnswer(i -> "hash(" + i.getArgument(0) + ")");
        when(usuarios.guardar(any())).thenAnswer(i -> i.getArgument(0));
        laura = AyudanteDeIds.conId(Profesional.nuevo("Laura", "Ejemplo", null, "laura@example.com"), 10L);
        when(profesionales.buscarPorId(10L)).thenReturn(Optional.of(laura));
        when(profesionales.listarTodos()).thenReturn(List.of(laura));
        recepcion = AyudanteDeIds.conId(Usuario.nuevo("recepcion@example.com", "hash", Rol.RECEPCION, null), 2L);
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(recepcion));
        when(usuarios.buscarPorEmail(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void crearCodificaLaContrasenaYDejaElUsuarioActivo() {
        UsuarioDetalle creado = servicio.crear("Nueva@Example.com", "contrasena-valida", Rol.RECEPCION, null);

        assertThat(creado.usuario().getEmail()).isEqualTo("nueva@example.com");
        assertThat(creado.usuario().getCredencialHash()).isEqualTo("hash(contrasena-valida)");
        assertThat(creado.usuario().isActivo()).isTrue();
        assertThat(creado.profesionalNombre()).isNull();
    }

    @Test
    void crearValidaElLargoDeLaContrasena() {
        assertThatThrownBy(() -> servicio.crear("a@example.com", "1234567", Rol.RECEPCION, null))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("al menos 8");
        assertThatThrownBy(() -> servicio.crear("a@example.com", "x".repeat(73), Rol.RECEPCION, null))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("72");
        assertThatThrownBy(() -> servicio.crear("a@example.com", "ñ".repeat(40), Rol.RECEPCION, null))
                .as("40 eñes son 80 bytes: BCrypt solo procesa 72").isInstanceOf(ValidacionDeDominioException.class);
        verify(usuarios, never()).guardar(any());
    }

    @Test
    void crearUnProfesionalVinculadoDevuelveElNombreDelProfesional() {
        UsuarioDetalle creado = servicio.crear("laura.u@example.com", "contrasena-valida", Rol.PROFESIONAL, 10L);

        assertThat(creado.usuario().getProfesionalId()).isEqualTo(10L);
        assertThat(creado.profesionalNombre()).isEqualTo("Laura Ejemplo");
    }

    @Test
    void crearConUnProfesionalInexistenteOInactivoFalla() {
        when(profesionales.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.crear("p@example.com", "contrasena-valida", Rol.PROFESIONAL, 99L))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("no existe o está inactivo");

        laura.desactivar();
        assertThatThrownBy(() -> servicio.crear("p@example.com", "contrasena-valida", Rol.PROFESIONAL, 10L))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("no existe o está inactivo");
    }

    @Test
    void crearConUnProfesionalQueYaTieneUsuarioDaConflicto() {
        when(usuarios.existeUsuarioDelProfesional(10L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear("p@example.com", "contrasena-valida", Rol.PROFESIONAL, 10L))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("ya tiene un usuario");
    }

    @Test
    void crearConUnEmailYaUsadoDaConflicto() {
        when(usuarios.buscarPorEmail("recepcion@example.com")).thenReturn(Optional.of(recepcion));

        assertThatThrownBy(() -> servicio.crear("Recepcion@Example.com", "contrasena-valida", Rol.RECEPCION, null))
                .isInstanceOf(ConflictoException.class).hasMessage("El email ya está en uso");
    }

    @Test
    void siLaBaseRechazaElEmailDuplicadoTambienEsConflicto() {
        when(usuarios.guardar(any())).thenThrow(new DataIntegrityViolationException(
                "could not execute statement [Duplicate entry for key 'usuario.UK_USUARIO_EMAIL']"));

        assertThatThrownBy(() -> servicio.crear("carrera@example.com", "contrasena-valida", Rol.RECEPCION, null))
                .isInstanceOf(ConflictoException.class).hasMessage("El email ya está en uso");
    }

    @Test
    void lasOperacionesSobreUnIdInexistenteDan404() {
        when(usuarios.buscarPorId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarRol(ADMIN_ID, 404L, Rol.RECEPCION, null))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.desactivar(ADMIN_ID, 404L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.activar(404L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.blanquearContrasena(ADMIN_ID, 404L, "contrasena-nueva"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void elAdministradorNoPuedeOperarSobreSiMismo() {
        Usuario admin = AyudanteDeIds.conId(Usuario.nuevo("admin@example.com", "hash", Rol.ADMINISTRADOR, null), ADMIN_ID);
        when(usuarios.buscarPorId(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> servicio.desactivar(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> servicio.cambiarRol(ADMIN_ID, ADMIN_ID, Rol.RECEPCION, null))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> servicio.blanquearContrasena(ADMIN_ID, ADMIN_ID, "contrasena-nueva"))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThat(admin.isActivo()).isTrue();
        assertThat(admin.getRol()).isEqualTo(Rol.ADMINISTRADOR);
        assertThat(admin.getCredencialHash()).isEqualTo("hash");
    }

    @Test
    void cambiarRolAProfesionalValidaElProfesionalYAplicaElCambio() {
        UsuarioDetalle cambiado = servicio.cambiarRol(ADMIN_ID, 2L, Rol.PROFESIONAL, 10L);

        assertThat(cambiado.usuario().getRol()).isEqualTo(Rol.PROFESIONAL);
        assertThat(cambiado.profesionalNombre()).isEqualTo("Laura Ejemplo");
    }

    @Test
    void cambiarRolConservandoElMismoProfesionalNoEsConflicto() {
        Usuario lauraUsuario = AyudanteDeIds.conId(Usuario.nuevo("l@example.com", "hash", Rol.PROFESIONAL, 10L), 3L);
        when(usuarios.buscarPorId(3L)).thenReturn(Optional.of(lauraUsuario));
        when(usuarios.existeUsuarioDelProfesional(10L)).thenReturn(true);

        assertThat(servicio.cambiarRol(ADMIN_ID, 3L, Rol.PROFESIONAL, 10L).usuario().getProfesionalId()).isEqualTo(10L);
    }

    @Test
    void desactivarYActivar() {
        assertThat(servicio.desactivar(ADMIN_ID, 2L).usuario().isActivo()).isFalse();
        assertThat(servicio.activar(2L).usuario().isActivo()).isTrue();
    }

    @Test
    void blanquearValidaElLargoYGuardaUnHashNuevo() {
        assertThatThrownBy(() -> servicio.blanquearContrasena(ADMIN_ID, 2L, "corta"))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(recepcion.getCredencialHash()).isEqualTo("hash");

        servicio.blanquearContrasena(ADMIN_ID, 2L, "contrasena-nueva");

        assertThat(recepcion.getCredencialHash()).isEqualTo("hash(contrasena-nueva)");
        verify(usuarios).guardar(recepcion);
    }

    @Test
    void listarDevuelveCadaUsuarioConElNombreDeSuProfesional() {
        Usuario lauraUsuario = AyudanteDeIds.conId(Usuario.nuevo("l@example.com", "hash", Rol.PROFESIONAL, 10L), 3L);
        when(usuarios.listarTodos()).thenReturn(List.of(recepcion, lauraUsuario));

        assertThat(servicio.listar()).extracting(UsuarioDetalle::profesionalNombre)
                .containsExactly(null, "Laura Ejemplo");
    }

    @Test
    void losProfesionalesDisponiblesSonLosActivosSinUsuario() {
        Profesional martin = AyudanteDeIds.conId(Profesional.nuevo("Martín", "Prueba", null, "m@example.com"), 11L);
        when(profesionales.listarActivos()).thenReturn(List.of(laura, martin));
        when(usuarios.idsDeProfesionalesConUsuario()).thenReturn(Set.of(10L));

        assertThat(servicio.listarProfesionalesDisponibles()).containsExactly(martin);
    }

    @Test
    void listarRolesDelegaEnElCatalogo() {
        List<RolDisponible> roles = List.of(new RolDisponible(Rol.ADMINISTRADOR, "Administrador"));
        when(catalogo.listar()).thenReturn(roles);

        assertThat(servicio.listarRoles()).isEqualTo(roles);
    }
}
