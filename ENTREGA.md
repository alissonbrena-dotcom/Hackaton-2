# Entrega — Tuckersoft Branch Engine

**Equipo:** G04 — Ariana Isla (202510309) y Alisson Breña (202410064).
Somos 2 integrantes; en `equipo.json` la tercera entrada lo indica explícitamente
("Sin tercer integrante"), porque el tablero exige 3 entradas.

## Resultado de los autotests

```
  ──────────────────────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
  ──────────────────────────────────────────────────────────────

   ★★★★★   5 / 5   Cinco estrellas.

   ✔  ★1  SEGURIDAD    65 comprobaciones
   ✔  ★2  NODOS        37 comprobaciones
   ✔  ★3  PARTIDAS     40 comprobaciones
   ✔  ★4  DECISIONES   101 comprobaciones
   ✔  ★5  ASINCRONIA   41 comprobaciones

   tablero: publicado como "G04"

   Las cinco estrellas. Bandersnatch sale para Navidad.
  ──────────────────────────────────────────────────────────────
```

Tests unitarios propios (`./mvnw test` en la raíz): **5/5** en
`DecisionServiceTest`, con Mockito, sin PostgreSQL ni red.

## Cómo correrlo

1. PostgreSQL con la base `bandersnatch` (usuario `tuckersoft`).
2. Copiar `.env.example` a `.env` y completar los valores.
3. `./mvnw spring-boot:run`
4. En otra terminal: `cd autotests && ./mvnw test`

## Estructura

Paquetes por funcionalidad dentro de `com.tuckersoft.branchengine`:

| Paquete | Contenido |
|:--|:--|
| `auth` | Registro y login (`AuthController`, `AuthService`) |
| `user` | Entidad `User`, `/users/me`, listado y cambio de rol |
| `security` | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, EntryPoint 401 y AccessDeniedHandler 403 |
| `node` | `StoryNode` y sus endpoints |
| `playthrough` | `Playthrough`, creación con control de capacidad, `/path` |
| `decision` | `Decision`, `BranchClassifier`, `DecisionService`, filtros y paginación, `DecisionCommittedEvent` |
| `notification` | `RealityLog` y `BranchNotificationListener` (correo asíncrono) |
| `config` | `AsyncConfig` (pool `branch-worker-`) y `DataInitializer` (admin) |
| `exception` | `ApiException` y `GlobalExceptionHandler` con el formato de error único |

## Flujo asíncrono

1. `POST /api/v1/decisions` entra a `DecisionService.create`, que es `@Transactional`.
   Valida la propiedad y el estado de la partida, clasifica el texto, aplica los stats,
   resuelve el nodo destino y el final, guarda `Playthrough` y `Decision` (`REGISTRADA`)
   y publica un `DecisionCommittedEvent` con `ApplicationEventPublisher`.
   El controller responde **201** de inmediato.
2. El evento lleva todo lo que el listener necesita (destinatario, displayName,
   jugador, rama, stats, texto original y la bandera de `MAIL_FAILURE`), porque en el
   otro hilo ya no hay usuario autenticado ni sesión de Hibernate.
3. `BranchNotificationListener` es un `@Component` separado con
   `@Async("branchExecutor")`, `@Transactional(propagation = REQUIRES_NEW)` y
   `@TransactionalEventListener(phase = AFTER_COMMIT)`. Solo se ejecuta **después del
   COMMIT** en PostgreSQL, así que la decisión ya existe cuando la busca. Con
   `@EventListener` podría ejecutarse antes del commit y no encontrarla.
4. En un hilo `branch-worker-X` (pool 2/4/50 de `AsyncConfig`) pasa la decisión a
   `PROCESANDO` y envía el Informe de Realidad con `JavaMailSender` al email del dueño
   de la partida.
   - Si el envío sale bien, la decisión queda en `ESTABILIZADA` y se guarda un
     `RealityLog` `SENT` con `sentAt`.
   - Si falla, la decisión queda en `ERROR`, se guarda un `RealityLog` `FAILED` con
     `errorMessage` y se registra un `log.error`.
5. Con `X-Bandersnatch-Simulate: MAIL_FAILURE` el listener lanza una
   `MailSendException` real, que atrapa el mismo `catch` que un fallo SMTP de verdad.
6. Al final se imprime `[BRANCH-LOG] ... | Thread: branch-worker-X | Status: ...`.

`DecisionService` nunca inyecta `JavaMailSender` ni referencia al listener: solo
publica el evento.

## Decisiones de diseño

- **El JWT lleva solo el email.** El filtro carga las authorities desde la BD en cada
  petición, así que un usuario promovido usa su mismo token con los permisos nuevos.
- **Las contraseñas se guardan con BCrypt.** Ningún response devuelve `password`,
  porque los controllers solo devuelven DTOs (records).
- **Hay un bloqueo pesimista al abrir una partida** (`findByNodeCodeForUpdate`), para
  que dos partidas simultáneas no superen `branchCapacity`.
- **Los filtros de `GET /decisions`** usan `Specification` en el repositorio, y un
  usuario normal solo ve decisiones de sus propias partidas.
- **Timeouts SMTP de 5 s**, para que un servidor de correo caído no deje colgado un
  hilo del pool.

## Pendiente

Nada de lo que evalúan los autotests quedó pendiente.
