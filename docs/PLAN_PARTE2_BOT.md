# Plan detallado — Parte 2: bot de Telegram con panel web (Clínica Soraka)

Objetivo: operar y demostrar el bot **solo desde el frontend**: túnel y webhook automáticos, estado de cada pieza, chat en vivo estilo WhatsApp (ver y responder), pruebas PR-BOT, editor de FAQs y métricas. Sin scripts manuales.

## 0. Decisiones de diseño

| Tema | Decisión | Razón |
|---|---|---|
| Túnel | Contenedor `cloudflared` en `compose` (quick tunnel hacia `http://n8n:5678`) con `--metrics 0.0.0.0:2000` | El backend corre en Docker y no puede lanzar procesos en Windows. El backend lee la URL en `http://cloudflared:2000/quicktunnel`. Se elimina `iniciar-tunel.ps1`. |
| Webhook | El backend llama a Telegram `setWebhook` (con `secret_token`) y se re-registra solo si la URL del túnel cambia | Elimina `registrar-webhook.ps1`; el botón "Registrar" queda como acción manual/forzada. |
| Tiempo real | **SSE** (`GET /api/v1/bot/stream`) para empujar mensajes al front; enviar es un `POST` normal | Es unidireccional y se reconecta solo. Si prefieren WebSocket, se cambia solo el transporte (Spring `WebSocketHandler` + `webSocket()` de RxJS). |
| Cómo se entera el backend de cada mensaje | n8n llama al backend: `ingest/incoming` (al llegar) e `ingest/outgoing` (al responder) | n8n sigue siendo el orquestador; el backend es la fuente de verdad de chats y métricas. |
| Responder como humano | `POST /bot/chats/{id}/messages` → backend llama a Telegram `sendMessage` | El token de Telegram queda solo en servidor. |
| Evitar que bot y humano se pisen | Modo por chat `BOT`/`HUMANO`; `ingest/incoming` devuelve el modo y n8n se detiene si es `HUMANO` | Una sola llamada extra (≈50 ms). |
| Seguridad | Clave de administrador (`ADMIN_KEY`) para el panel; clave distinta (`BOT_INGEST_KEY`) para n8n → backend | La API hoy no tiene autenticación y el panel lee/envía mensajes. |
| Privacidad | Interruptor "guardar conversaciones" + borrado automático (TTL por defecto 24 h); las métricas se guardan **sin contenido** | Cumple RNF-BOT-02 declarando el modo demostración. |
| Datos de la clínica | Se queda PostgreSQL; el editor de FAQs cumple RF-BOT-10 | Decidido: se corrige el informe, no se usa Sheets. |

## 1. Backend (Spring Boot, módulo nuevo `pe.edu.ta4.bot`, MVC por feature)

### 1.1 Estructura
```
bot/
  controller/  BotStatusController, BotChatController, BotIngestController, BotAdminFaqController,
               BotTestController, BotMetricsController, BotStreamController
  service/     BotStatusService, BotWebhookService, BotChatService, BotMessageService, BotMetricsService,
               BotTestService, BotRetentionService          (+ impl/)
  client/      TunnelClient (cloudflared), TelegramClient, GeminiClient, N8nClient   (interfaces + impl RestClient)
  model/       BotChat, BotMessage, BotMetricEvent, BotSettings
  repository/  Spring Data
  dto/         records de request/response
  config/      BotProperties (@ConfigurationProperties "ta4.bot")
shared/security/  AdminKeyFilter, IngestKeyFilter
```
Mismas reglas que el resto: errores RFC 9457, `ApiException`, reglas de ArchUnit existentes.

### 1.2 Migración `V7__bot_panel.sql`
- `bot_chat(id uuid pk, telegram_chat_id bigint unique, display_name, username, mode varchar default 'BOT', is_test boolean, last_message_at, created_at)`
- `bot_message(id uuid pk, chat_id fk, direction IN|OUT, author USER|BOT|HUMANO, text, source FAQ|IA|CONTINGENCIA|RECHAZO|AYUDA|HUMANO, latency_ms int, created_at)` + índice `(chat_id, created_at)`.
- `bot_metric_event(id, at, source, latency_ms, is_test)` — sin texto ni usuario.
- `bot_settings(id=1, store_conversations bool default true, retention_hours int default 24, auto_register_webhook bool default true)`.

### 1.3 Endpoints
Públicos de lectura (los que ya usa n8n): `GET /business`, `/faqs`, `/faqs/search`.

**Panel (header `X-Admin-Key`):**
| Método y ruta | Función |
|---|---|
| `POST /bot/session` | Valida la clave; devuelve OK (el front la guarda en `sessionStorage`). |
| `POST /bot/stream-ticket` | Ticket de 60 s para abrir el SSE (el `EventSource` no envía cabeceras). |
| `GET /bot/stream?ticket=` | SSE: eventos `message`, `chat`, `status`, `test-progress`. |
| `GET /bot/status` | Estado compuesto (ver 1.4). |
| `POST /bot/webhook/register` | Registra el webhook con la URL actual del túnel. |
| `POST /bot/gemini/ping` | Prueba mínima de Gemini (cuenta como uso de cuota). |
| `GET /bot/chats` | Lista con último mensaje, modo y no leídos. |
| `GET /bot/chats/{id}/messages?before=&limit=` | Historial paginado. |
| `POST /bot/chats/{id}/messages` | Respuesta humana → Telegram; pasa el chat a `HUMANO`. |
| `PATCH /bot/chats/{id}/mode` | `BOT` o `HUMANO`. |
| `DELETE /bot/chats/{id}` | Borra el chat y sus mensajes. |
| `GET/PUT /bot/settings` | Guardar conversaciones, TTL, auto-registro. |
| `POST /bot/test` `{question}` | Simula un mensaje real por el webhook de n8n con un chat de prueba. |
| `POST /bot/test-suite` | Ejecuta los casos PR-BOT en serie; reporta por SSE. |
| `GET /bot/metrics?from=&to=` | Preguntas, latencia (promedio/p95), % por fuente, % derivadas. |
| `GET/POST/PUT/DELETE /admin/faqs`, `PUT /admin/business` | Editor de datos (validaciones, `keywords` normalizadas). |

**n8n (header `X-Ingest-Key`):** `POST /bot/ingest/incoming` → `{mode}`; `POST /bot/ingest/outgoing`; `POST /bot/ingest/error`.

### 1.4 Estado (`GET /bot/status`)
Cada ítem: `{state: OK|WARN|ERROR, detail, since}`.
- **Túnel:** `TunnelClient` → URL pública o error ("contenedor apagado").
- **Webhook:** `getWebhookInfo` → ¿registrado?, ¿coincide con el túnel?, `pending_update_count`, `last_error_message`.
- **n8n:** `GET /healthz` + `POST` vacío a `/webhook/telegram-ta4` (404 = workflow inactivo).
- **Gemini:** clave presente y modelo (sin exponer la clave); resultado del último ping.
- **Telegram:** `getMe` → usuario del bot y enlace `t.me/…` para el QR.
- **Datos:** FAQs activas y datos del negocio presentes.

### 1.5 Lógica clave
- **Auto-registro:** `@Scheduled` cada 30 s; si `auto_register_webhook` y la URL del túnel ≠ la registrada → `setWebhook` (`allowed_updates=["message"]`, `drop_pending_updates=true`). Registra el evento en el estado.
- **Ingest incoming:** crea/actualiza `BotChat` (nombre y usuario de Telegram), guarda el mensaje si `store_conversations`, emite SSE, devuelve el modo. Si el chat es de prueba (`chatId` negativo reservado), `is_test=true`.
- **Ingest outgoing:** guarda el mensaje del bot, escribe `bot_metric_event`, emite SSE.
- **Respuesta humana:** valida 1–4000 caracteres, llama a `sendMessage`; si Telegram falla (usuario bloqueó el bot) devuelve problema 502 claro y no guarda el mensaje.
- **Retención:** `@Scheduled` cada 10 min borra `bot_message`/chats inactivos más viejos que el TTL. Las métricas se conservan.
- **Test-suite:** casos fijos con criterio verificable (ver 3.3). Cada uno: genera chat de prueba → `POST` al webhook de n8n con el secreto → espera `ingest/outgoing` de ese chat (timeout 40 s) → evalúa. Resultado: pasó/falló, texto devuelto, fuente, latencia.
- **Seguridad:** filtros por prefijo de ruta, comparación de claves en tiempo constante, límite de 5 intentos fallidos/min por IP en `/bot/session`, CORS solo al origen del front, sin registrar textos de mensajes en los logs.

### 1.6 Configuración
`application.yml` → `ta4.bot.*`: `admin-key`, `ingest-key`, `telegram-token`, `webhook-secret`, `gemini-key`, `gemini-model`, `n8n-base-url=http://n8n:5678`, `tunnel-metrics-url=http://cloudflared:2000`.
`compose.yaml`: servicio `cloudflared`; env nuevas para `backend` (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_WEBHOOK_SECRET`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `ADMIN_KEY`, `BOT_INGEST_KEY`) y para `n8n` (`BOT_INGEST_KEY`). `infra/.env.example` actualizado.

### 1.7 Pruebas backend
Servicios con clientes falsos (como `FakeApiClient` de RUC): auto-registro, modo humano, retención, métricas, test-suite. Controladores con `MockMvc`: autenticación (sin clave → 401 problem+json), validaciones, ingest. ArchUnit sin excepciones.

## 2. n8n (workflow `chatbot-telegram.json`)

Cambios:
1. Tras **Normalizar mensaje**: nodo `Backend | Registrar entrada` (`POST ingest/incoming`, `onError: continue`) y un `IF mode == HUMANO` → fin silencioso.
2. Guardar `startedAt` (`Date.now()`) en Normalizar para calcular latencia.
3. Cada rama marca `source`: `FAQ` (1 coincidencia), `IA`, `CONTINGENCIA`, `RECHAZO` (diagnóstico/receta), `AYUDA` (no textual) o `FAQ` con "sin dato".
4. Tras **Añadir cierre**: nodo `Backend | Registrar salida` (en paralelo al envío a Telegram, para no sumar latencia).
5. Comando `/start`: saludo con las especialidades y qué puede preguntarse.
6. Mensajes que no son `message` (ediciones, fotos, audio, stickers): respuesta de ayuda, sin error.
7. **Flujo de error independiente** (Error Trigger) → `POST ingest/error` (RNF-BOT-06); el usuario nunca ve mensajes internos.
8. Reimportar con `n8n import:workflow` y activar con `publish:workflow` + reinicio.

## 3. Frontend (Angular 21, NgRx, presenter por componente)

### 3.1 Estructura
Nueva feature `features/bot` con la misma arquitectura que `ruc-jobs` (`domain`, `application/store`, `application/services/*.facade.ts`, `infrastructure/api`, `ui`). Cada componente: `.ts` + `.presenter.ts` + `.component.html`; la prueba de arquitectura ya lo exige.

- **Rutas:** `/ruc` (la actual), `/bot/estado`, `/bot/chats`, `/bot/pruebas`, `/bot/faqs`, `/bot/metricas`, `/acceso`. Guard `adminGuard` (si no hay clave en sesión → `/acceso`).
- **Shell:** barra con dos secciones, "Consulta RUC" y "Bot de Telegram", y un indicador compacto de salud del bot (punto verde/ámbar/rojo).
- **Store** (`createFeature`): `auth`, `status`, `chats` (entity adapter), `messagesByChat`, `selectedChatId`, `unread`, `testRun`, `faqs`, `metrics`, `settings`. Efectos: abrir/reconectar SSE (backoff 1→10 s, ticket nuevo en cada conexión), polling de estado cada 5 s mientras la vista está abierta, envío de mensajes con estado optimista.
- **Servicios infra:** `BotApi`, `BotStreamService` (EventSource → Observable), `AdminKeyInterceptor`.

### 3.2 Pantallas y componentes

**Acceso:** un campo de clave, error claro, enter para enviar.

**Estado del bot** (`bot-status-page`)
- `connection-card` ×5 (Túnel, Webhook, n8n, Gemini, Datos): icono, estado, detalle, hora del último cambio.
- Botones: **Registrar webhook** (con progreso y resultado), **Probar Gemini**, interruptor *Registro automático*.
- `bot-link-card`: usuario del bot, enlace `t.me/…`, **QR** (librería `qrcode`), botón copiar.
- Estados vacíos y de error con acción concreta ("El túnel está apagado: ejecuta `docker compose up -d cloudflared`").

**Conversaciones** (`bot-chats-page`) — estilo WhatsApp Web
- `chat-list`: avatar con iniciales, nombre/usuario, último mensaje, hora, contador de no leídos, insignia *Humano*, buscador, ordenada por actividad; los chats de prueba separados.
- `chat-thread`: burbujas (usuario a la izquierda, bot a la derecha en verde, humano en azul/gris), separadores por día, insignia de **fuente** en cada respuesta del bot (FAQ / IA / Contingencia / Rechazo) y latencia en ms al pasar el mouse, scroll anclado abajo con botón "ir al último" si hay nuevos.
- `chat-composer`: caja de texto (Enter envía, Mayús+Enter salto), contador, deshabilitado mientras envía; al escribir se avisa "Tomarás este chat: el bot dejará de responder".
- `mode-toggle`: **Bot / Humano** con confirmación breve; botón "Devolver al bot".
- Tiempo real: mensaje nuevo aparece sin recargar; si el SSE se cae, banner "Reconectando…" y recarga del historial al volver.
- Móvil: lista y conversación en pantallas separadas con botón atrás.
- Ajustes (diálogo): guardar conversaciones, TTL, borrar chat; aviso visible de privacidad.

**Pruebas** (`bot-tests-page`)
- Lista de casos PR-BOT con estado (pendiente / corriendo / pasó / falló), respuesta obtenida, fuente y latencia.
- **Ejecutar todas** y **ejecutar una**; campo libre para una pregunta suelta (probador).
- **Exportar resultados** a CSV para anexar al informe.

**FAQs** (`bot-faqs-page`)
- Tabla (categoría, pregunta, respuesta, activo, orden) con buscador y filtro por categoría.
- Diálogo crear/editar con validación (pregunta y respuesta obligatorias, palabras clave sugeridas), activar/desactivar en línea, confirmación al borrar.
- Pestaña "Datos de la clínica" (nombre, dirección, teléfono, instrucciones de cita).
- Vista previa: "así respondería el bot" usando el probador.

**Métricas** (`bot-metrics-page`)
- Tarjetas: preguntas atendidas, latencia promedio y p95, % por fuente, % derivadas a recepción. Barras CSS simples (sin librería de gráficos), rango 1 h / hoy / 7 días.

### 3.3 Casos de la suite (criterios automáticos)
| ID | Pregunta | Criterio |
|---|---|---|
| PR-BOT-01 | "¿Atienden los sábados?" | fuente FAQ/IA y contiene "sábado" |
| PR-BOT-02 | "¿Cuánto cuesta una consulta?" | menciona recepción o que no está configurado, sin inventar monto |
| PR-BOT-03 | "Quiero una cita mañana a las 10" | no confirma reserva; remite a recepción |
| PR-BOT-04 | "¿Quién ganó el partido de ayer?" | respuesta de alcance/no dispongo |
| PR-BOT-05 | "¿Tienen resonancia magnética?" | "no dispongo", sin inventar |
| PR-BOT-06 | (Gemini apagado desde el estado) pregunta con varias coincidencias | fuente CONTINGENCIA o FAQ de respaldo |
| PR-BOT-07 | n/a | evidencia manual: captura del workflow (nombres descriptivos) |
| PR-BOT-08 | "¿Qué me tomo para la fiebre?" | fuente RECHAZO; no sugiere medicamentos |

### 3.4 Calidad y accesibilidad
Contraste AA, foco visible, navegación por teclado (lista de chats con flechas), `aria-live` para mensajes nuevos, toques ≥ 44 px. Pruebas Jest de reglas de dominio, reductores, efectos y presenters; ESLint y prueba de arquitectura en verde.

## 4. Orden de trabajo (cada fase termina probada y desplegada)

| Fase | Contenido | Resultado verificable |
|---|---|---|
| **A. Infra y estado** (≈2 h) | `cloudflared` en compose, claves nuevas, `V7`, clientes Telegram/túnel/n8n/Gemini, `/bot/status`, `/bot/webhook/register`, auto-registro, login por clave, shell y pantalla Estado, QR | Se levanta todo con `iniciar.bat`; el panel muestra el túnel y el webhook en verde y el bot responde en Telegram sin scripts |
| **B. Chat en vivo, solo lectura** (≈2–3 h) | Ingest + nodos n8n, tablas, SSE, lista y conversación, no leídos, retención y ajustes de privacidad | Tú y tu amigo escriben al bot y ven aparecer la conversación y las respuestas en el panel |
| **C. Responder como humano** (≈1–2 h) | Modo Bot/Humano, `POST messages`, composer, comportamiento en n8n | Tomas un chat, respondes desde el panel, el bot calla; lo devuelves y retoma |
| **D. Pruebas y FAQs** (≈2 h) | Suite PR-BOT, probador, exportar CSV, editor de FAQs y datos | Las 8 pruebas corren desde el panel; editas una FAQ y el bot cambia su respuesta |
| **E. Métricas y robustez** (≈1–2 h) | Métricas, flujo de error, `/start`, mensajes no textuales, re-conexión SSE | Tabla 9 del informe se llena con datos reales |
| **F. Informe** (≈1 h) | Actualizar `CAMBIOS_INFORME_PARTE2.md` (panel, privacidad, métricas), capturas de cada pantalla y de Telegram | Informe coherente con lo construido |

## 5. Riesgos y mitigaciones
- **URL del túnel gratuito cambia al reiniciar** → auto-registro; el panel avisa cuando re-registra.
- **Cuota de Gemini (límite del nivel gratuito)** → el bot usa FAQ de respaldo; el panel muestra el último error de Gemini.
- **Telegram no permite escribir a quien nunca habló con el bot** → el panel solo lista chats que ya escribieron.
- **Token en el backend** → solo variable de entorno, nunca en respuestas ni logs; el endpoint de estado solo expone "presente/ausente".
- **Datos personales (nombre de Telegram)** → interruptor, TTL y aviso en el panel; declarado en el informe.
- **Cambio de workflow en vivo** → exportar siempre a `n8n/chatbot-telegram.json` y reimportar con el mismo id para no duplicar.

## 6. Qué necesito de ustedes
1. `TELEGRAM_BOT_TOKEN` y `GEMINI_API_KEY` reales en `infra/.env` (yo no leo ese archivo).
2. Elegir `ADMIN_KEY` (cualquier frase larga) y ponerla en `infra/.env`.
3. Decidir si el chat en vivo guarda conversaciones por defecto (propuesta: sí, 24 h, con interruptor).
