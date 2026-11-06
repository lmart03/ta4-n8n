# TA4 — Consulta de RUC con UiPath y bot de Telegram (Clínica Soraka)

Proyecto de la Tarea Académica 4 (UTP, Herramientas de Desarrollo Profesional TIC).

- **Parte 1 — Consulta de RUC:** se adjunta un Excel con 20 RUC; un robot de UiPath los consulta en la Consulta Múltiple de SUNAT y la web muestra razón social, dirección, condición y más datos, con exportación a Excel. Si SUNAT bloquea al robot, entra una API de respaldo (consultaderuc.pe) y la pantalla indica la fuente.
- **Parte 2 — Bot de Telegram:** responde preguntas frecuentes de la Clínica Soraka (ficticia) con n8n y Gemini. Un panel web permite ver las conversaciones en vivo, responder como recepción, probar el bot, editar las respuestas y ver métricas.

Backend Spring Boot (Java 25) + PostgreSQL, frontend Angular 21 (NgRx), n8n, túnel de Cloudflare y UiPath, con Docker Compose.

## Empezar

1. Instala Docker Desktop, Git, Node.js, UiPath Studio y el CLI (`npm install -g @uipath/cli @uipath/rpa-tool`).
2. `git clone https://github.com/lmart03/ta4-n8n.git`, copia `infra/.env.example` a `infra/.env` y completa las claves.
3. Doble clic en `iniciar.bat`: levanta todo y abre <http://localhost:4280>.

Guía completa, paso a paso (variables, bot, robot, pruebas y solución de problemas): **[docs/GUIA_EJECUCION.md](docs/GUIA_EJECUCION.md)**.

## Dónde está cada cosa

| Carpeta | Contenido |
|---|---|
| `backend/` | API Spring Boot: RUC (Excel, TXT, ZIP de SUNAT, exportación), bot (panel, chats, pruebas), FAQ. Migraciones Flyway. |
| `frontend/` | Web Angular: Consulta de RUC y panel del bot. |
| `n8n/` | Flujos de n8n del bot (`chatbot-telegram.json`) y de errores (`errores-bot.json`). |
| `rpa/RobotSunat/` | Proyecto de UiPath (robot). `rpa/GUIA_UIPATH.md` explica su secuencia. |
| `scripts/` | Lanzador local del robot (`lanzador-robot.ps1`) y arranque (`iniciar.ps1`). |
| `infra/` | `compose.yaml` y `.env.example`. |
| `docs/` | Guía de ejecución, contrato de la API, plan del bot y textos para el informe. |

API: <http://localhost:8080/scalar> (contrato en [docs/API.md](docs/API.md); errores RFC 9457).

## Seguridad

`infra/.env` y los Excel de `rpa/inbox/` no se versionan. Los secretos solo viven en `infra/.env`; no los pegues en capturas ni chats.
