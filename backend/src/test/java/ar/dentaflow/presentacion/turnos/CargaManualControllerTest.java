package ar.dentaflow.presentacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.auditoria.ServicioDeAuditoria;
import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Spec carga-manual-turnos — {@code POST /api/turnos} y {@code POST /api/turnos/sobreturnos} (C-12, design D1 a D4).
 * Datos ficticios. Sin {@code @Transactional}: {@code ServicioDeTurnos} exige su propia transacción
 * {@code READ COMMITTED} y el paciente se registra en otra, así que cada prueba hace commit y borra lo que creó. El
 * reloj es fijo: lunes 2026-11-30 12:00 en el consultorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Carga manual de turnos y sobreturnos (HTTP)")
class CargaManualControllerTest {

    private static final Instant AHORA = Instant.parse("2026-11-30T15:00:00Z");
    private static final LocalDate MARTES = LocalDate.of(2026, 12, 1);

    @TestConfiguration
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijoDePrueba() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    @Autowired
    private ServicioDeTurnos servicioDeTurnos;

    @Autowired
    private ServicioDeAuditoria auditoria;

    private EscenarioDeAgendaJdbc escenario;
    private final List<String> dnisCreados = new ArrayList<>();
    private final List<Long> usuariosCreados = new ArrayList<>();
    private long laura;
    private long martin;
    private long sillon1;
    private long sillon2;
    private long prestacion;
    private Sesion recepcion;
    private Sesion admin;
    private Sesion profesional;

    private record Sesion(long id, String bearer, Long profesionalId) {
    }

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        laura = escenario.profesional();
        martin = escenario.profesional();
        sillon1 = escenario.sillon();
        sillon2 = escenario.sillon();
        prestacion = escenario.prestacion(30);
        for (long profesional : new long[] {laura, martin}) {
            escenario.habilitar(profesional, prestacion);
            jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                    + "VALUES (?, 2, '09:00:00', '13:00:00')", profesional);
        }
        recepcion = usuario(Rol.RECEPCION, null);
        admin = usuario(Rol.ADMINISTRADOR, null);
        profesional = usuario(Rol.PROFESIONAL, escenario.profesional());
    }

    @AfterEach
    void limpiar() {
        // Los usuarios son creadores de turnos, historial y bloqueos: se borran esos primero, después los usuarios
        // y por último lo que creó el escenario (el profesional del usuario "profesional" incluido).
        long[] profesionales = {laura, martin, profesional.profesionalId()};
        for (long id : profesionales) {
            jdbc.update("DELETE FROM turno_historial WHERE turno_id IN (SELECT id FROM turno WHERE profesional_id = ?)", id);
            jdbc.update("DELETE FROM turno WHERE profesional_id = ?", id);
        }
        for (long id : new long[] {sillon1, sillon2}) {
            jdbc.update("DELETE FROM bloqueo WHERE sillon_id = ?", id);
        }
        usuariosCreados.forEach(id -> jdbc.update("DELETE FROM usuario WHERE id = ?", id));
        escenario.limpiar();
        dnisCreados.forEach(dni -> jdbc.update("DELETE FROM paciente WHERE dni = ?", dni));
    }

    private Sesion usuario(Rol rol, Long profesionalId) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        long id = usuarios.crear(email, "contrasena-valida", rol, profesionalId).usuario().getId();
        usuariosCreados.add(id);
        return new Sesion(id, "Bearer " + emisor.emitir(id).token(), profesionalId);
    }

    // --- Ayudas

    private static Instant hora(int hora, int minuto) {
        return ZonaHorariaConsultorio.aInstante(LocalDateTime.of(MARTES, LocalTime.of(hora, minuto)));
    }

    private static String iso(int hora, int minuto) {
        return String.format("2026-12-01T%02d:%02d:00-03:00", hora, minuto);
    }

    private String dniNuevo() {
        String dni = escenario.dniNuevo();
        dnisCreados.add(dni);
        return dni;
    }

    private Map<String, Object> paciente(String dni) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("dni", dni);
        datos.put("nombre", "Ana");
        datos.put("apellido", "Ejemplo");
        datos.put("telefono", "+541155550000");
        datos.put("email", "ana@example.com");
        return datos;
    }

    private Map<String, Object> cuerpo(String dni, long profesional, long sillon, String inicio) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("paciente", paciente(dni));
        datos.put("prestacionId", prestacion);
        datos.put("profesionalId", profesional);
        datos.put("sillonId", sillon);
        datos.put("inicio", inicio);
        return datos;
    }

    private ResultActions cargar(String ruta, Object cuerpo, Sesion sesion) throws Exception {
        var pedido = post(ruta).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
        return mockMvc.perform(sesion == null ? pedido : pedido.header(HttpHeaders.AUTHORIZATION, sesion.bearer()));
    }

    private ResultActions cargarComun(Object cuerpo, Sesion sesion) throws Exception {
        return cargar("/api/turnos", cuerpo, sesion);
    }

    private ResultActions cargarSobreturno(Object cuerpo, Sesion sesion) throws Exception {
        return cargar("/api/turnos/sobreturnos", cuerpo, sesion);
    }

    /** Un turno existente creado por el servicio (con su historial), para provocar conflictos. */
    private long turnoExistente(long profesional, long sillon, Instant inicio) {
        long pacienteId = escenario.paciente();
        return servicioDeTurnos.crear(new SolicitudDeTurno(pacienteId, profesional, sillon, prestacion, inicio,
                OrigenTurno.RECEPCION, false, Actor.usuario(recepcion.id()))).turno().getId();
    }

    private int cantidadDeTurnos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE profesional_id IN (?, ?)", Integer.class, laura,
                martin);
    }

    private int cantidadDeHistorial() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id IN "
                + "(SELECT id FROM turno WHERE profesional_id IN (?, ?))", Integer.class, laura, martin);
    }

    private int pacientesConDni(String dni) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni = ?", Integer.class, dni);
    }

    private long idDeRespuesta(ResultActions respuesta) throws Exception {
        return ((Number) json.readValue(respuesta.andReturn().getResponse().getContentAsString(), Map.class)
                .get("id")).longValue();
    }

    // --- 3.1 Carga común

    @Nested
    @DisplayName("POST /api/turnos")
    class CargaComun {

        @Test
        @DisplayName("recepción carga un turno en un horario libre: 201, hora del consultorio, UTC en la base y creador")
        void cargaEnHorarioLibre() throws Exception {
            String dni = dniNuevo();

            ResultActions respuesta = cargarComun(cuerpo(dni, laura, sillon1, iso(9, 0)), recepcion)
                    .andExpect(status().isCreated())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.inicio").value("2026-12-01T09:00:00-03:00"))
                    .andExpect(jsonPath("$.fin").value("2026-12-01T09:30:00-03:00"))
                    .andExpect(jsonPath("$.estado").value("pendiente"))
                    .andExpect(jsonPath("$.esSobreturno").value(false))
                    .andExpect(jsonPath("$.prestacionId").value(prestacion))
                    .andExpect(jsonPath("$.profesionalId").value(laura))
                    .andExpect(jsonPath("$.sillonId").value(sillon1));

            Turno guardado = servicioDeTurnos.buscarPorId(idDeRespuesta(respuesta)).orElseThrow();
            assertThat(guardado.getInicio()).isEqualTo(Instant.parse("2026-12-01T12:00:00Z"));
            assertThat(guardado.getOrigen()).isEqualTo(OrigenTurno.RECEPCION);
            assertThat(guardado.isEsSobreturno()).isFalse();
            assertThat(guardado.getCreadoPorUsuarioId()).isEqualTo(recepcion.id());
            List<CambioDeTurno> historial = auditoria.historialDe(guardado.getId());
            assertThat(historial).singleElement().satisfies(cambio -> {
                assertThat(cambio.accion()).isEqualTo(AccionDeTurno.CREACION);
                assertThat(cambio.actor().usuarioId()).isEqualTo(recepcion.id());
                assertThat(cambio.valorNuevo()).containsEntry("es_sobreturno", "false");
            });
        }

        @Test
        @DisplayName("el administrador también carga y queda como creador")
        void cargaDelAdministrador() throws Exception {
            ResultActions respuesta = cargarComun(cuerpo(dniNuevo(), laura, sillon1, iso(10, 0)), admin)
                    .andExpect(status().isCreated());

            Turno guardado = servicioDeTurnos.buscarPorId(idDeRespuesta(respuesta)).orElseThrow();
            assertThat(guardado.getCreadoPorUsuarioId()).isEqualTo(admin.id());
        }

        @Test
        @DisplayName("la respuesta no contiene token, DNI, teléfono ni email")
        void respuestaSinDatosSensibles() throws Exception {
            String dni = dniNuevo();

            String cuerpoDeRespuesta = cargarComun(cuerpo(dni, laura, sillon1, iso(9, 0)), recepcion)
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

            assertThat(cuerpoDeRespuesta.toLowerCase()).doesNotContain("token", dni, "1155550000", "ana@example.com",
                    "dni", "telefono", "email");
        }

        @Test
        @DisplayName("un esSobreturno verdadero en el cuerpo se ignora: en horario libre crea un turno común")
        void elFlagEnElCuerpoSeIgnora() throws Exception {
            Map<String, Object> pedido = cuerpo(dniNuevo(), laura, sillon1, iso(9, 0));
            pedido.put("esSobreturno", true);

            cargarComun(pedido, recepcion).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.esSobreturno").value(false));
        }

        @Test
        @DisplayName("un esSobreturno verdadero en el cuerpo no evita el 409 sobre un horario ocupado")
        void elFlagNoEvitaElConflicto() throws Exception {
            turnoExistente(laura, sillon1, hora(9, 0));
            Map<String, Object> pedido = cuerpo(dniNuevo(), laura, sillon2, iso(9, 0));
            pedido.put("esSobreturno", true);

            cargarComun(pedido, recepcion).andExpect(status().isConflict());

            assertThat(cantidadDeTurnos()).isEqualTo(1);
        }
    }

    // --- 3.2 Conflictos

    @Nested
    @DisplayName("conflictos y alternativas")
    class Conflictos {

        @Test
        @DisplayName("profesional ocupado: 409 que lo indica, sin turno ni historial nuevos y con alternativas")
        void profesionalOcupado() throws Exception {
            turnoExistente(laura, sillon1, hora(9, 0));
            int turnosAntes = cantidadDeTurnos();
            int historialAntes = cantidadDeHistorial();

            cargarComun(cuerpo(dniNuevo(), laura, sillon2, iso(9, 15)), recepcion)
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.detail").value(containsString("profesional")))
                    .andExpect(jsonPath("$.alternativas").isArray());

            assertThat(cantidadDeTurnos()).isEqualTo(turnosAntes);
            assertThat(cantidadDeHistorial()).isEqualTo(historialAntes);
        }

        @Test
        @DisplayName("sillón ocupado con el profesional libre: 409 que indica el sillón")
        void sillonOcupado() throws Exception {
            turnoExistente(laura, sillon1, hora(9, 0));

            cargarComun(cuerpo(dniNuevo(), martin, sillon1, iso(9, 0)), recepcion)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(containsString("sillón")));
        }

        @Test
        @DisplayName("fuera del horario de atención: 409 sin turno nuevo")
        void fueraDeHorario() throws Exception {
            cargarComun(cuerpo(dniNuevo(), laura, sillon1, iso(12, 45)), recepcion)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.alternativas").isArray());

            assertThat(cantidadDeTurnos()).isZero();
            assertThat(cantidadDeHistorial()).isZero();
        }

        @Test
        @DisplayName("bloqueo del sillón: 409")
        void bloqueoDelSillon() throws Exception {
            jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, modificado_por, "
                    + "modificado_en) VALUES ('SILLON', NULL, ?, ?, ?, 'OTRO', ?, ?)", sillon1,
                    Timestamp.from(hora(10, 0)), Timestamp.from(hora(12, 0)), recepcion.id(), Timestamp.from(AHORA));

            cargarComun(cuerpo(dniNuevo(), laura, sillon1, iso(10, 0)), recepcion).andExpect(status().isConflict());

            assertThat(cantidadDeTurnos()).isZero();
        }

        @Test
        @DisplayName("un turno cancelado no bloquea el horario")
        void turnoCanceladoNoBloquea() throws Exception {
            long existente = turnoExistente(laura, sillon1, hora(9, 0));
            servicioDeTurnos.cancelar(existente, Actor.usuario(recepcion.id()));

            cargarComun(cuerpo(dniNuevo(), laura, sillon1, iso(9, 0)), recepcion).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ofrece hasta 3 alternativas cercanas del mismo profesional, en hora del consultorio y con el sillón pedido")
        void alternativasCercanas() throws Exception {
            turnoExistente(laura, sillon1, hora(9, 0));
            turnoExistente(laura, sillon1, hora(9, 30));

            cargarComun(cuerpo(dniNuevo(), laura, sillon2, iso(9, 30)), recepcion)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.alternativas", hasSize(3)))
                    .andExpect(jsonPath("$.alternativas[0].inicio").value("2026-12-01T10:00:00-03:00"))
                    .andExpect(jsonPath("$.alternativas[0].fin").value("2026-12-01T10:30:00-03:00"))
                    .andExpect(jsonPath("$.alternativas[0].profesionalId").value(laura))
                    .andExpect(jsonPath("$.alternativas[0].sillonId").value(sillon2))
                    .andExpect(jsonPath("$.alternativas[1].inicio").value("2026-12-01T10:15:00-03:00"))
                    .andExpect(jsonPath("$.alternativas[2].inicio").value("2026-12-01T10:30:00-03:00"));
        }

        @Test
        @DisplayName("sin horarios libres en la ventana: 409 con la lista de alternativas vacía")
        void sinAlternativas() throws Exception {
            // Martes 09:00-13:00 de Laura completos y ningún otro día con horario: la semana siguiente no atiende.
            for (int minuto = 0; minuto < 240; minuto += 30) {
                turnoExistente(laura, sillon1, hora(9, 0).plusSeconds(minuto * 60L));
            }

            cargarComun(cuerpo(dniNuevo(), laura, sillon2, iso(9, 0)), recepcion)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.alternativas", hasSize(0)));
        }
    }

    // --- 3.3 Paciente por DNI

    @Nested
    @DisplayName("alta o actualización del paciente por DNI")
    class PacientePorDni {

        @Test
        @DisplayName("paciente nuevo: se crea con el DNI normalizado y el turno queda asociado")
        void pacienteNuevo() throws Exception {
            String dni = dniNuevo();
            String conPuntos = dni.substring(0, 2) + "." + dni.substring(2, 5) + "." + dni.substring(5);

            ResultActions respuesta = cargarComun(cuerpo(conPuntos, laura, sillon1, iso(9, 0)), recepcion)
                    .andExpect(status().isCreated());

            assertThat(pacientesConDni(dni)).isEqualTo(1);
            long pacienteId = jdbc.queryForObject("SELECT id FROM paciente WHERE dni = ?", Long.class, dni);
            Turno guardado = servicioDeTurnos.buscarPorId(idDeRespuesta(respuesta)).orElseThrow();
            assertThat(guardado.getPacienteId()).isEqualTo(pacienteId);
            assertThat(jdbc.queryForObject("SELECT consentimiento_privacidad_en FROM paciente WHERE dni = ?",
                    Timestamp.class, dni)).isNull();
        }

        @Test
        @DisplayName("paciente existente: se reutiliza y su contacto se reemplaza")
        void pacienteExistenteConTelefonoNuevo() throws Exception {
            long existente = escenario.paciente();
            String dni = jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, existente);
            Map<String, Object> pedido = cuerpo(dni, laura, sillon1, iso(9, 0));
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) pedido.get("paciente");
            datos.put("telefono", "+541166660000");

            ResultActions respuesta = cargarComun(pedido, recepcion).andExpect(status().isCreated());

            assertThat(pacientesConDni(dni)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT telefono FROM paciente WHERE id = ?", String.class, existente))
                    .isEqualTo("+541166660000");
            assertThat(servicioDeTurnos.buscarPorId(idDeRespuesta(respuesta)).orElseThrow().getPacienteId())
                    .isEqualTo(existente);
        }

        @Test
        @DisplayName("sin email: 400 con el error en paciente.email y sin crear paciente ni turno")
        void faltaElEmail() throws Exception {
            String dni = dniNuevo();
            Map<String, Object> pedido = cuerpo(dni, laura, sillon1, iso(9, 0));
            @SuppressWarnings("unchecked")
            Map<String, Object> datos = (Map<String, Object>) pedido.get("paciente");
            datos.remove("email");

            cargarComun(pedido, recepcion).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores['paciente.email']").exists())
                    .andExpect(jsonPath("$.alternativas").doesNotExist());

            assertThat(pacientesConDni(dni)).isZero();
            assertThat(cantidadDeTurnos()).isZero();
        }

        @Test
        @DisplayName("conflicto después de registrar a un paciente nuevo: 409 y el paciente queda registrado")
        void conflictoDespuesDeRegistrar() throws Exception {
            turnoExistente(laura, sillon1, hora(9, 0));
            String dni = dniNuevo();

            cargarComun(cuerpo(dni, laura, sillon2, iso(9, 0)), recepcion).andExpect(status().isConflict());

            assertThat(pacientesConDni(dni)).isEqualTo(1);
            assertThat(cantidadDeTurnos()).isEqualTo(1);
        }
    }

    // --- 3.4 Validaciones

    @Nested
    @DisplayName("validaciones de entrada")
    class Validaciones {

        @Test
        @DisplayName("inicio sin desplazamiento: 400 y ningún turno")
        void inicioSinDesplazamiento() throws Exception {
            cargarComun(cuerpo(dniNuevo(), laura, sillon1, "2026-12-01T09:00"), recepcion)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.alternativas").doesNotExist());

            assertThat(cantidadDeTurnos()).isZero();
        }

        @Test
        @DisplayName("inicio con segundos: 400 y ningún turno")
        void inicioConSegundos() throws Exception {
            cargarComun(cuerpo(dniNuevo(), laura, sillon1, "2026-12-01T09:00:30-03:00"), recepcion)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.alternativas").doesNotExist());

            assertThat(cantidadDeTurnos()).isZero();
        }

        @Test
        @DisplayName("inicio en el pasado según el reloj: 400 y ningún turno")
        void inicioEnElPasado() throws Exception {
            // El reloj fijo marca el lunes 12:00 del consultorio.
            cargarComun(cuerpo(dniNuevo(), laura, sillon1, "2026-11-30T10:00:00-03:00"), recepcion)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.alternativas").doesNotExist());

            assertThat(cantidadDeTurnos()).isZero();
        }

        @Test
        @DisplayName("cuerpo vacío: 400 con los campos obligatorios marcados")
        void cuerpoVacio() throws Exception {
            cargarComun(Map.of(), recepcion).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.paciente").exists())
                    .andExpect(jsonPath("$.errores.prestacionId").exists())
                    .andExpect(jsonPath("$.errores.profesionalId").exists())
                    .andExpect(jsonPath("$.errores.sillonId").exists())
                    .andExpect(jsonPath("$.errores.inicio").exists());
        }

        @Test
        @DisplayName("prestación, profesional o sillón inexistente: 404 sin alternativas")
        void recursoInexistente() throws Exception {
            Map<String, Object> sinPrestacion = cuerpo(dniNuevo(), laura, sillon1, iso(9, 0));
            sinPrestacion.put("prestacionId", 987_654_321L);
            cargarComun(sinPrestacion, recepcion).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.alternativas").doesNotExist());

            cargarComun(cuerpo(dniNuevo(), 987_654_321L, sillon1, iso(9, 0)), recepcion)
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.alternativas").doesNotExist());

            cargarComun(cuerpo(dniNuevo(), laura, 987_654_321L, iso(9, 0)), recepcion)
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.alternativas").doesNotExist());

            assertThat(cantidadDeTurnos()).isZero();
        }
    }

    // --- 3.5 Sobreturno

    @Nested
    @DisplayName("POST /api/turnos/sobreturnos")
    class Sobreturno {

        @Test
        @DisplayName("recepción y administrador cargan un sobreturno superpuesto: 201 con esSobreturno verdadero")
        void sobreturnoSuperpuesto() throws Exception {
            turnoExistente(laura, sillon1, hora(9, 0));

            cargarSobreturno(cuerpo(dniNuevo(), laura, sillon1, iso(9, 0)), recepcion)
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.esSobreturno").value(true))
                    .andExpect(jsonPath("$.estado").value("pendiente"));
            cargarSobreturno(cuerpo(dniNuevo(), laura, sillon1, iso(9, 0)), admin)
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.esSobreturno").value(true));

            assertThat(cantidadDeTurnos()).isEqualTo(3);
        }

        @Test
        @DisplayName("el historial registra creación con el usuario, el instante y es_sobreturno verdadero, sin datos personales")
        void historialDelSobreturno() throws Exception {
            String dni = dniNuevo();

            ResultActions respuesta = cargarSobreturno(cuerpo(dni, laura, sillon1, iso(9, 0)), recepcion)
                    .andExpect(status().isCreated());

            long id = idDeRespuesta(respuesta);
            List<CambioDeTurno> historial = auditoria.historialDe(id);
            assertThat(historial).singleElement().satisfies(cambio -> {
                assertThat(cambio.accion()).isEqualTo(AccionDeTurno.CREACION);
                assertThat(cambio.actor().usuarioId()).isEqualTo(recepcion.id());
                assertThat(cambio.ocurridoEn()).isEqualTo(AHORA);
                assertThat(cambio.valorNuevo()).containsEntry("es_sobreturno", "true");
            });
            String valorNuevo = jdbc.queryForObject("SELECT valor_nuevo FROM turno_historial WHERE turno_id = ?",
                    String.class, id);
            assertThat(valorNuevo).contains("\"es_sobreturno\":\"true\"")
                    .doesNotContain(dni, "@", "1155550000", "Ana", "Ejemplo");
        }

        @Test
        @DisplayName("fuera de horario: 409 sin alternativas y sin turno")
        void fueraDeHorario() throws Exception {
            cargarSobreturno(cuerpo(dniNuevo(), laura, sillon1, iso(13, 0)), recepcion)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.alternativas").doesNotExist());

            assertThat(cantidadDeTurnos()).isZero();
        }

        @Test
        @DisplayName("un turno común posterior en ese horario sigue pudiendo crearse")
        void unSobreturnoNoBloqueaTurnosComunes() throws Exception {
            cargarSobreturno(cuerpo(dniNuevo(), laura, sillon1, iso(9, 0)), recepcion).andExpect(status().isCreated());

            cargarComun(cuerpo(dniNuevo(), laura, sillon1, iso(9, 0)), recepcion).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.esSobreturno").value(false));
        }
    }

    // --- 3.6 RBAC

    @Nested
    @DisplayName("roles y sesión")
    class Roles {

        @Test
        @DisplayName("el profesional recibe 403 en ambos endpoints, sin crear paciente ni turno")
        void profesionalSinPermiso() throws Exception {
            String dni = dniNuevo();
            Map<String, Object> pedido = cuerpo(dni, laura, sillon1, iso(9, 0));

            cargarSobreturno(pedido, profesional).andExpect(status().isForbidden());
            cargarComun(pedido, profesional).andExpect(status().isForbidden());

            assertThat(pacientesConDni(dni)).isZero();
            assertThat(cantidadDeTurnos()).isZero();
        }

        @Test
        @DisplayName("sin sesión: 401 en ambos endpoints y ningún efecto")
        void sinSesion() throws Exception {
            String dni = dniNuevo();
            Map<String, Object> pedido = cuerpo(dni, laura, sillon1, iso(9, 0));

            cargarComun(pedido, null).andExpect(status().isUnauthorized());
            cargarSobreturno(pedido, null).andExpect(status().isUnauthorized());

            assertThat(pacientesConDni(dni)).isZero();
            assertThat(cantidadDeTurnos()).isZero();
        }
    }

    // --- 3.7 OpenAPI

    @Test
    @DisplayName("OpenAPI lista ambas rutas y ninguna ruta pública crea sobreturnos")
    void openApiListaLasRutas() throws Exception {
        String documento = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/turnos'].post.tags[0]").value("Turnos"))
                .andExpect(jsonPath("$.paths['/api/turnos'].post.responses.201").exists())
                .andExpect(jsonPath("$.paths['/api/turnos'].post.responses.409").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/sobreturnos'].post.tags[0]").value("Turnos"))
                .andExpect(jsonPath("$.paths['/api/turnos/sobreturnos'].post.responses.403").exists())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> paths = (Map<?, ?>) json.readValue(documento, Map.class).get("paths");
        assertThat(paths.keySet().stream().map(Object::toString).filter(ruta -> ruta.startsWith("/api/publico/")))
                .noneMatch(ruta -> ruta.contains("sobreturno"));
    }
}
