# Guía de ejecución — TA4 (RUC con UiPath + bot de Telegram)

Todo lo necesario para levantar el proyecto en una PC con Windows. Si lo clonaste de GitHub, sigue los pasos en orden; el primer arranque tarda unos minutos por las descargas.

## 1. Qué hace el proyecto

| Parte | Qué hace | Piezas |
|---|---|---|
| **1. Consulta de RUC** | Subes un Excel con 20 RUC; el robot de UiPath los consulta en la Consulta Múltiple de SUNAT y la web muestra los datos y exporta un Excel. Si SUNAT bloquea al robot, entra una API de respaldo (consultaderuc.pe) y el sistema lo dice. | Web (Angular), Backend (Spring Boot), PostgreSQL, UiPath, lanzador local |
| **2. Bot de Telegram** | Responde preguntas frecuentes de la **Clínica Soraka** (ficticia) con n8n y Gemini. Un panel web permite ver las conversaciones en vivo, responder como recepción, probar el bot y editar las respuestas. | n8n, Gemini, túnel de Cloudflare, backend, PostgreSQL |

```
Navegador ──► frontend (nginx :4280) ──► backend (:8080) ──► PostgreSQL
Telegram ──► Cloudflare (túnel) ──► n8n (:5678) ──► backend / Gemini
Web "Empezar flujo" ──► backend ──(cola)──► lanzador local ──► UiPath ──► Edge ──► SUNAT
```

Qué corre dónde: backend, frontend, PostgreSQL, n8n y el túnel van en **contenedores**. El robot de UiPath y el lanzador corren **directo en Windows** (necesitan tu Edge y tu mouse).

## 2. Requisitos

| Qué | Para qué | Cómo |
|---|---|---|
| Windows 10/11 64 bits | Todo | — |
| [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/) (contenedores Linux, WSL 2) | Backend, web, BD, n8n, túnel | Instalar, abrir y esperar a que diga "running". |
| [Git](https://git-scm.com/download/win) | Clonar el repo | — |
| [Node.js 22 o 24](https://nodejs.org/) | Instalar el CLI de UiPath (y correr pruebas del front, opcional) | `node --version` |
| **UiPath Studio** (Community sirve) y la extensión **UiPath para Edge** | El robot de RUC | Studio → Herramientas → Extensiones de UiPath → Edge. |
| **CLI de UiPath** (`uip`) | El lanzador ejecuta el robot con este comando | `npm install -g @uipath/cli @uipath/rpa-tool` y luego `uip --version` |
| Microsoft Edge | Navegador que usa el robot | Viene con Windows. |
| Cuenta de Telegram | Crear el bot con BotFather | — |
| Clave de **Gemini** ([Google AI Studio](https://aistudio.google.com/apikey)) | Respuestas redactadas del bot (opcional: sin ella responde solo con las FAQ) | — |
| Token de [consultaderuc.pe](https://consultaderuc.pe) | API de respaldo de la Parte 1 (opcional pero recomendado: SUNAT suele bloquear al robot) | Plan con acceso a la API. |

No hace falta instalar Java, Maven, PostgreSQL ni n8n: vienen en los contenedores.

Una sola vez, en Edge: `edge://extensions` → detalles de *UiPath Browser Automation* → activar **Permitir el acceso a las URL de archivos**. Sin esto el robot no puede subir el TXT.

## 3. Clonar y configurar `infra/.env`

```powershell
git clone https://github.com/lmart03/ta4-n8n.git
cd ta4-n8n
Copy-Item infra\.env.example infra\.env
notepad infra\.env
```

`infra/.env` **no se sube a Git** (tiene secretos). Variables:

| Variable | Obligatoria | Qué poner |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER` | Sí | `ta4` sirve. |
| `POSTGRES_PASSWORD` | Sí | Secreto aleatorio nuevo. |
| `N8N_ENCRYPTION_KEY` | Sí | Secreto aleatorio nuevo (32+ caracteres). No la cambies después: n8n no podría leer lo guardado. |
| `RUC_UPLOAD_KEY` | Sí | Secreto aleatorio nuevo. |
| `TELEGRAM_WEBHOOK_SECRET` | Sí | Secreto alfanumérico nuevo. |
| `ADMIN_KEY` | Sí, para el panel del bot | La clave que escribes en la web (`/acceso`). Frase larga. Sin ella el panel queda cerrado. |
| `BOT_INGEST_KEY` | Sí, para el panel del bot | Otra clave distinta; la usa n8n para avisar al backend. |
| `TELEGRAM_BOT_TOKEN` | Para el bot | Token de BotFather. |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | Opcional | Clave y modelo (por defecto `gemini-3.5-flash-lite`). |
| `CONSULTA_RUC_API_TOKEN` | Opcional | Token de consultaderuc.pe. Vacío = sin respaldo por API. |
| `CONSULTA_RUC_API_AUTO_FALLBACK` | No | `false` para que el respaldo sea manual (botón) y no automático. |
| `N8N_WEBHOOK_URL` | No | Déjala como está; el túnel se maneja solo. |

Generar secretos (copia cada línea a su variable; no los compartas):

```powershell
function New-Secret {
    $b = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($b) } finally { $rng.Dispose() }
    ([BitConverter]::ToString($b)).Replace('-', '')
}
'POSTGRES_PASSWORD='        + (New-Secret)
'N8N_ENCRYPTION_KEY='       + (New-Secret)
'RUC_UPLOAD_KEY='           + (New-Secret)
'TELEGRAM_WEBHOOK_SECRET='  + (New-Secret)
'ADMIN_KEY='                + (New-Secret)
'BOT_INGEST_KEY='           + (New-Secret)
```

## 4. Arrancar todo

Doble clic en **`iniciar.bat`** (o `.\iniciar.bat` en PowerShell). Hace tres cosas: levanta los contenedores, espera al backend y abre `http://localhost:4280`. También inicia el lanzador del robot en segundo plano.

Opcional, **una sola vez**: `.\iniciar.bat /instalar` hace que el lanzador arranque solo al iniciar sesión y registra el enlace `ta4robot://` que usa el botón "Encender robot" de la web.

Sin el `.bat`: `docker compose -f infra/compose.yaml up -d --build`.

| Dirección | Qué es |
|---|---|
| <http://localhost:4280> | La web (Consulta RUC y Bot de Telegram) |
| <http://localhost:8080/scalar> | Documentación y pruebas de la API |
| <http://localhost:5678> | n8n (crea la cuenta propietaria la primera vez) |

Revisar: `docker compose -f infra/compose.yaml ps` (todos en *Up*). El primer build del backend corre las pruebas de Maven y tarda.

## 5. Bot de Telegram (Parte 2)

1. **Crear el bot**: en Telegram habla con `@BotFather` → `/newbot` → copia el token a `TELEGRAM_BOT_TOKEN`. Pon la clave de Gemini en `GEMINI_API_KEY`.
2. **Recrear los contenedores** para que lean las variables: `docker compose -f infra/compose.yaml up -d`.
3. **Importar los flujos en n8n** (primera vez; los JSON ya traen sus ids, así que reimportar actualiza en vez de duplicar):

```powershell
docker cp n8n\errores-bot.json infra-n8n-1:/tmp/err.json
docker cp n8n\chatbot-telegram.json infra-n8n-1:/tmp/wf.json
docker exec infra-n8n-1 n8n import:workflow --input=/tmp/err.json
docker exec infra-n8n-1 n8n import:workflow --input=/tmp/wf.json
docker exec infra-n8n-1 n8n publish:workflow --id=ta4ErrorFlow01
docker exec infra-n8n-1 n8n publish:workflow --id=qq62axyGmA9Trked
docker restart infra-n8n-1
```

   (También sirve importarlos desde el editor de n8n: *Import from file*, y publicarlos.)
4. **Abrir el panel**: <http://localhost:4280/bot> → te pide la clave: es `ADMIN_KEY`.
5. En **Estado** deben salir en verde: túnel, webhook, n8n, Gemini y datos. El túnel de Cloudflare es un contenedor (`cloudflared`) y el backend **registra el webhook de Telegram solo** cuando cambia la dirección; el botón "Registrar webhook" es para forzarlo.
6. Escríbele al bot desde Telegram (hay un QR en Estado). En **Conversaciones** aparece al instante; puedes "Tomar el chat" y responder tú.
7. **Pruebas** → "Ejecutar todas" corre la matriz PR-BOT del informe y exporta un CSV.

Notas: el túnel gratuito cambia de dirección si reinicias `cloudflared`. Las conversaciones se guardan 24 h por defecto (ajustable en Conversaciones → Privacidad). Las preguntas frecuentes se editan en la pestaña **Preguntas frecuentes** sin reprogramar nada.

## 6. Consulta de RUC con el robot (Parte 1)

1. Abre <http://localhost:4280/ruc>. El indicador debe decir **Robot disponible** (lo da el lanzador). Si dice "no disponible": `iniciar.bat` o el botón "Encender robot".
2. Adjunta un Excel `.xlsx` (hasta 2 MB) con una columna llamada `ruc` y **exactamente 20** RUC únicos de 11 dígitos con dígito verificador válido. Una nota bajo la tabla cuenta como fila y produce error; el mensaje indica fila y motivo.
3. **Iniciar consulta**. El backend valida y genera el TXT; el lanzador toma el pedido y ejecuta el robot de `rpa/RobotSunat` (abre Edge, sube el TXT a la Consulta Múltiple, descarga el ZIP y se lo entrega al backend).
4. Cuando termina, la grilla muestra los datos y hay un botón para descargar el Excel.

**Si el robot falla** (SUNAT bloquea con «The requested URL was rejected», Edge no abre o el clic falla): el lanzador reintenta una vez y, si sigue fallando, el backend consulta por la **API de respaldo** (`CONSULTA_RUC_API_TOKEN`) y la pantalla y el historial indican que la fuente fue la API, no SUNAT. Sin token, el trabajo queda en *Falló* y puedes reintentar o subir a mano el ZIP que descargues de SUNAT.

Detalles del robot (argumentos, secuencia, selectores): [`rpa/GUIA_UIPATH.md`](../rpa/GUIA_UIPATH.md). El proyecto de UiPath está en **`rpa/RobotSunat`**: ábrelo en Studio desde esa carpeta (si lo editas, edita ahí). La carpeta `.local` de UiPath no se versiona; se regenera sola al compilar.

El lanzador compila el robot **una vez al iniciar**: si cambias `Main.xaml`, reinicia el lanzador.

## 7. Pruebas y calidad

```powershell
# Backend (Java 25 en un contenedor, no hace falta instalar nada)
docker run --rm -v "${PWD}/backend:/app" -v m2cache:/root/.m2 -w /app maven:3.9.11-eclipse-temurin-25 mvn -B -ntp test

# Frontend
cd frontend
npm ci
npm test          # Jest
npm run lint      # ESLint + reglas de arquitectura
npm run build
```

## 8. Operación diaria

```powershell
docker compose -f infra/compose.yaml ps                       # estado
docker compose -f infra/compose.yaml logs --tail 80 backend n8n
docker compose -f infra/compose.yaml up -d --build backend    # tras cambiar backend
docker compose -f infra/compose.yaml up -d --build frontend   # tras cambiar la web
docker compose -f infra/compose.yaml down                     # apagar (conserva los datos)
```

**No uses `down -v`**: borra la base de datos y la configuración de n8n. Log del lanzador: `%LOCALAPPDATA%\TA4-RUC\lanzador.log`.

## 9. Problemas frecuentes

| Síntoma | Qué revisar |
|---|---|
| `docker compose` dice que falta una variable | Falta algo obligatorio en `infra/.env`; `docker compose -f infra/compose.yaml config --quiet` dice cuál. |
| El panel dice "ADMIN_KEY no está configurada" | Pon `ADMIN_KEY` en `infra/.env` y `docker compose -f infra/compose.yaml up -d backend`. |
| La web pide la clave y no la acepta | Es la de `ADMIN_KEY`, no la de n8n ni la de Telegram. Tras 10 intentos fallidos hay 1 minuto de espera. |
| Estado: **Webhook** con aviso | Pulsa "Registrar webhook" (o espera 30 s, se registra solo). Necesita el túnel en verde. |
| Estado: **n8n** "workflow inactivo" | Importa y publica los dos flujos (sección 5, paso 3) y reinicia n8n. |
| Estado: **Túnel** en rojo | `docker compose -f infra/compose.yaml up -d cloudflared` y revisa que haya internet. |
| El bot no responde en Telegram | Revisa Estado; luego Pruebas → "Probar una pregunta". Si Pruebas funciona y Telegram no, el webhook apunta a otra dirección. |
| "Robot no disponible" | El lanzador no corre: `iniciar.bat`. Revisa `lanzador.log`. |
| El robot no abre Edge o se queda en "arrancando" | El lanzador lo corta a los 50 s y reintenta una vez; luego pasa a la API. Cierra ventanas de Edge de SUNAT antes de reintentar. |
| «The requested URL was rejected» | Firewall de SUNAT bloqueó la IP/sesión; suele pasar tras varias corridas seguidas. Espera, cambia de red o usa el respaldo por API. |
| `uip` no se reconoce | `npm install -g @uipath/cli @uipath/rpa-tool` y abre una terminal nueva. |
| 422 al subir el Excel | Debe tener exactamente 20 RUC válidos y únicos; el mensaje indica fila y motivo. |
| Puerto 8080, 4280 o 5678 ocupado | Cierra lo que lo use o cambia el puerto en `infra/compose.yaml`. |

## 10. Seguridad

- `infra/.env` y los `.xlsx` de `rpa/inbox/` están en `.gitignore`: **nunca los subas**.
- No pegues tokens ni claves en chats, capturas ni en los JSON de n8n (los flujos leen todo de variables de entorno).
- El token de Telegram y las claves de Gemini y de la API de RUC solo viven en `infra/.env`; la web nunca los recibe.
- El panel (`/bot`) y las rutas `/api/v1/admin` exigen `ADMIN_KEY`; n8n usa `BOT_INGEST_KEY`. Sin claves configuradas quedan cerradas.
