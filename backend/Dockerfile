# Imagen de la API SOLO para el build remoto de Render (DD-13, C-19 design D1).
# Render no tiene runtime nativo de Java. Este archivo NO se usa en desarrollo, tests ni CI:
# localmente se sigue con ./mvnw y MySQL nativo, sin Docker (DD-09).
#
# En Render: servicio web con Root Directory "backend", runtime Docker, health check /actuator/health,
# y las variables de .env.example (SPRING_PROFILES_ACTIVE=prod lo fija esta imagen).

FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /fuente
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Los tests ya corren en CI (H2 e it-mysql); acá solo se empaqueta.
RUN ./mvnw -B -q -DskipTests package && cp target/dentaflow-backend-*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 dentaflow
COPY --from=compilacion /fuente/app.jar app.jar
USER dentaflow
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
# El plan gratuito tiene poca memoria: el heap usa hasta el 75% de lo que da el contenedor.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
