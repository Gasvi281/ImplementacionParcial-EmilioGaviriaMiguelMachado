# The Great EIA Camel vs. Dwarf Racing System

Proyecto académico de una liga de carreras camellos vs. enanos: API REST en Spring
Boot + frontend en React, con autenticación delegada a Keycloak.

## Stack

- **Backend:** Java 21, Spring Boot 3.x, Gradle, PostgreSQL, springdoc-openapi (Swagger UI).
- **Autenticación:** Keycloak como resource server (el backend nunca guarda passwords ni emite
  JWT propios).
- **Frontend:** React + Vite, sin librería de componentes ([frontend/](frontend/)).
- **Tests:** JUnit 5 + Mockito + H2.
- **Infraestructura:** Docker Compose (Postgres + Keycloak + backend + frontend).

## Arquitectura del backend

Paquete por dominio (no por capa), bajo `com.example.implementacionparcial`:

```
competitors/  teams/  races/  registrations/  results/  auditlog/
  ├── entity/       entidades JPA
  ├── dto/          XRequest, XUpdateRequest, XResponse, XSummaryResponse
  ├── mapper/       toEntity / toResponse / toSummary
  ├── repository/   IXxxRepository extends JpaRepository
  ├── service/      lógica de negocio (@Transactional)
  └── controller/   @RestController, sin lógica de negocio

common/
  ├── config/       SecurityConfig, OpenApiConfig
  ├── exceptions/   GlobalExceptionHandler y excepciones custom
  └── security/     CurrentUser, JwtRoleConverter
```

Roles de Keycloak: `ADMINISTRATOR`, `RACE_ORGANIZER`, `VIEWER`.

## Requisitos previos

- Docker y Docker Compose.
- Para desarrollo local sin Docker en el backend: JDK 21 (el `./gradlew` ya trae el wrapper).
- Para desarrollo local del frontend: Node.js 18+.

## Cómo correrlo (todo el stack con Docker)

1. Copiar el archivo de variables de entorno:

   ```bash
   cp .env.template .env
   ```

   Ajustar credenciales si se quiere, o dejar los valores por defecto para desarrollo local.

2. Levantar todo:

   ```bash
   docker compose up -d --build
   ```

   Esto levanta 4 contenedores:

   | Servicio | URL | Descripción |
   |---|---|---|
   | `postgres` | `localhost:5432` | Base de datos |
   | `keycloak` | `http://localhost:8180` | Auth (consola admin en `/admin`) |
   | `backend` | `http://localhost:8080` | API REST (Swagger en `/swagger-ui/index.html`) |
   | `frontend` | `http://localhost:5173` | Cliente web |

3. Entrar a `http://localhost:5173` y hacer login con alguno de los usuarios de prueba
   (precargados en el realm de Keycloak, ver [keycloak/realm-camel-dwarf-racing.json](keycloak/realm-camel-dwarf-racing.json)):

   | Usuario | Password | Rol |
   |---|---|---|
   | `admin.demo` | `admin1234` | `ADMINISTRATOR` |
   | `organizer.demo` | `organizer1234` | `RACE_ORGANIZER` |
   | `viewer.demo` | `viewer1234` | `VIEWER` |

   La consola de administración de Keycloak (`http://localhost:8180/admin`) usa las credenciales
   `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` del `.env` — son solo para administrar el realm,
   no para autenticarse contra la API.

## Cómo correrlo en desarrollo (sin reconstruir contenedores todo el tiempo)

Backend:

```bash
docker compose up -d postgres keycloak   # solo la infraestructura
./gradlew bootRun                         # perfil dev, se conecta a localhost:5432 y :8180
```

Frontend (ver detalle en [frontend/README.md](frontend/README.md)):

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

El proxy de Vite reenvía `/api/**` a `http://localhost:8080`, así que no hace falta configurar
CORS en el backend durante desarrollo.

## Tests

```bash
./gradlew test    # unitarios + integración (H2), con reportes en build/reports/tests
./gradlew build   # compila + corre tests
```

Por cada dominio: `XServiceTest`, `XMapperTest`, `XControllerIntegrationTest` (`@WebMvcTest`) y
`XRepositoryIntegrationTest` (`@DataJpaTest` + H2). Los flujos completos están en
`src/test/java/.../e2e/` con `@SpringBootTest` y un JWT falso (no depende de un Keycloak real
corriendo).

## API

Con el backend corriendo, la documentación interactiva (Swagger UI) queda en:

```
http://localhost:8080/swagger-ui.html
```

Módulos con API expuesta: `competitors`, `teams`, `races`, `registrations`, `results` y
`standings`. El audit log todavía no expone un endpoint de lectura (solo se registra
internamente); para consultarlo hoy hay que ir directo a la base de datos (tabla `audit_logs`).

## Variables de entorno

Definidas en `.env` (a partir de `.env.template`, nunca se commitea el `.env` real):

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` —
  conexión a PostgreSQL.
- `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD` — credenciales de la consola admin de Keycloak.
- `KEYCLOAK_PORT` — puerto público de Keycloak (default `8180`).

## Git

Ramas `feature/<dominio>` (ej. `feature/races`), con commits significativos de cada
integrante — no un solo commit final por rama.
