package ar.dentaflow.infraestructura.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** Spec autenticacion-personal — "Administrador inicial" (design D11). */
@ExtendWith(OutputCaptureExtension.class)
class SeedAdministradorInicialTest {

    private static final String EMAIL = "admin-inicial@consultorio.example";
    private static final String CONTRASENA = "contrasena-del-admin-inicial";

    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final ServicioDeUsuarios servicio = mock(ServicioDeUsuarios.class);

    @Test
    void sinAdministradorActivoYConLasVariablesCreaUnAdministrador() throws Exception {
        when(usuarios.existeAdministradorActivo()).thenReturn(false);

        new SeedAdministradorInicial(usuarios, servicio, EMAIL, CONTRASENA).run(null);

        verify(servicio).crear(EMAIL, CONTRASENA, Rol.ADMINISTRADOR, null);
    }

    @Test
    void siYaHayUnAdministradorActivoNoHaceNada() throws Exception {
        when(usuarios.existeAdministradorActivo()).thenReturn(true);

        new SeedAdministradorInicial(usuarios, servicio, EMAIL, CONTRASENA).run(null);

        verify(servicio, never()).crear(anyString(), anyString(), any(), any());
    }

    @Test
    void sinLasVariablesAvisaSinDatosSensiblesYNoCreaNada(CapturedOutput salida) throws Exception {
        when(usuarios.existeAdministradorActivo()).thenReturn(false);

        new SeedAdministradorInicial(usuarios, servicio, EMAIL, "").run(null);
        new SeedAdministradorInicial(usuarios, servicio, " ", CONTRASENA).run(null);

        verify(servicio, never()).crear(anyString(), anyString(), any(), any());
        assertThat(salida.getOut()).contains("WARN").contains("APP_ADMIN_EMAIL").contains("APP_ADMIN_PASSWORD")
                .doesNotContain(EMAIL).doesNotContain(CONTRASENA);
    }

    @Test
    void conDatosInvalidosAvisaSinMostrarlosYArrancaIgual(CapturedOutput salida) throws Exception {
        when(usuarios.existeAdministradorActivo()).thenReturn(false);
        when(servicio.crear(EMAIL, "corta", Rol.ADMINISTRADOR, null))
                .thenThrow(new ValidacionDeDominioException("La contraseña debe tener al menos 8 caracteres"));

        new SeedAdministradorInicial(usuarios, servicio, EMAIL, "corta").run(null);

        assertThat(salida.getOut()).contains("al menos 8 caracteres").doesNotContain(EMAIL).doesNotContain("corta\"");
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(UsuarioRepositorio.class, () -> usuarios)
            .withBean(ServicioDeUsuarios.class, () -> servicio)
            .withUserConfiguration(SeedAdministradorInicial.class);

    @Test
    void soloExisteConAppSeedAdminEnTrue() {
        contexto.run(ctx -> assertThat(ctx).doesNotHaveBean(SeedAdministradorInicial.class));
        contexto.withPropertyValues("app.seed.admin=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SeedAdministradorInicial.class));
        contexto.withPropertyValues("app.seed.admin=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(SeedAdministradorInicial.class));
    }
}
