package ar.dentaflow.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec privacidad-datos-personales, "Sin datos personales en logs ni en errores": cuando la base rechaza
 * una fila por una clave única, Hibernate no debe escribir en el log los valores repetidos (que en
 * {@code paciente} serían el DNI o el email).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SinValoresDeLaBaseEnLogsTest {

    @Autowired
    private EntityManager entityManager;

    private final ListAppender<ILoggingEvent> capturados = new ListAppender<>();
    private final Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    @BeforeEach
    void capturar() {
        capturados.start();
        raiz.addAppender(capturados);
    }

    @AfterEach
    void soltar() {
        raiz.detachAppender(capturados);
    }

    @Test
    void violacionDeClaveUnicaNoDejaElValorEnElLog() {
        String valor = "ana." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        insertarSillon(valor);

        assertThatThrownBy(() -> insertarSillon(valor)).isNotNull();

        assertThat(capturados.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(mensaje -> mensaje.contains(valor));
    }

    private void insertarSillon(String nombre) {
        entityManager.createNativeQuery("INSERT INTO sillon (nombre, activo) VALUES (?1, TRUE)")
                .setParameter(1, nombre)
                .executeUpdate();
    }
}
