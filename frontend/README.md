# Frontend — EIA Camel vs. Dwarf Racing

React + Vite, sin librería de componentes. Habla con el backend (Spring Boot, `:8080`) y con
Keycloak (`:8180`) — nunca guarda ni valida credenciales por su cuenta.

## Correr en desarrollo

```bash
cp .env.example .env.local   # ajustar si Keycloak no corre en localhost:8180
npm install
npm run dev
```

Requiere que `db` + `keycloak` (y el backend) estén corriendo — ver `compose.yml` en la raíz del
repo. El proxy de Vite reenvía `/api/**` a `http://localhost:8080`, así que no hace falta CORS en
el backend durante desarrollo.

Usuarios de prueba (del realm importado): `admin.demo` / `admin1234` (ADMINISTRATOR),
`organizer.demo` / `organizer1234` (RACE_ORGANIZER), `viewer.demo` / `viewer1234` (VIEWER).

## Correr con Docker

```bash
docker compose up -d --build
```

Levanta todo el stack (Postgres, Keycloak, backend y este frontend) y lo deja en
`http://localhost:5173`. El `Dockerfile` compila el sitio estático (`npm run build`) y lo sirve
con nginx, que además hace de proxy de `/api/**` hacia `backend:8080` dentro de la red de Docker
— mismo truco que el proxy de Vite en desarrollo, para no depender de CORS.

Las variables `VITE_KEYCLOAK_*` quedan fijadas en tiempo de build (Vite las incrusta en el
bundle, no se pueden inyectar en runtime) — se pasan como build args en `compose.yml` y ya
apuntan a `http://localhost:${KEYCLOAK_PORT}`, que es lo que ve el navegador.

## Alcance

Cubre los 5 módulos con API lista: standings, competitors, teams, races, registrations y
results. No incluye audit log (el backend aún no expone su lectura) ni edición de resultados ya
registrados (solo creación) — se dejó fuera a propósito para mantener el frontend simple; el
endpoint `PATCH /api/results/{id}` ya existe si se quiere agregar después.
