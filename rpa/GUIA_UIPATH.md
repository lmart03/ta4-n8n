# Robot UiPath: transporte hacia la Consulta Múltiple de SUNAT

El robot **no interpreta datos de SUNAT**. Solo mueve archivos: baja el TXT del backend, lo sube a la **Consulta Múltiple de RUC** de SUNAT, descarga el ZIP que SUNAT devuelve y se lo entrega al backend, que lo procesa y arma el Excel. Es un robot atendido: si SUNAT pide una verificación, la resuelve una persona.

No se consulta RUC por RUC en la página individual: SUNAT la protege con Cloudflare y las consultas automáticas fallan. La Consulta Múltiple es la función oficial para consultar varios RUC con un archivo.

Estado: **probado de punta a punta** con los 20 RUC reales (14 segundos, trabajo `COMPLETADO`, 20 de 20 con datos de SUNAT).

## Antes de ejecutarlo (una sola vez por PC)

1. **Extensión de UiPath en Edge**, instalada desde Studio (Herramientas > Extensiones de UiPath).
2. En `edge://extensions` > Detalles de *UiPath Browser Automation*, activar **Permitir el acceso a las URL de archivos**. Sin esto el ámbito del selector de archivos falla con "requiere permitir que la extensión del navegador acceda a los archivos".
3. Backend en marcha (`docker compose -f infra/compose.yaml up -d`) y un trabajo creado con el Excel de RUC (la pantalla web o `POST /api/v1/ruc/jobs`). El `id` del trabajo es el único dato obligatorio del robot.

## Argumentos de `Main.xaml`

| Argumento | Obligatorio | Por defecto |
|---|---|---|
| `in_JobId` | Sí | id del trabajo |
| `in_ApiBase` | No | `http://localhost:8080/api/v1/ruc/jobs` |
| `in_WorkFolder` | No | `%LOCALAPPDATA%\TA4-RUC` (donde se guarda el TXT) |
| `in_DownloadsFolder` | No | `%USERPROFILE%\Downloads` (donde Edge deja el ZIP) |
| `in_FirstWaitSeconds` | No | 90 s esperando el ZIP antes de pedir ayuda |
| `in_InterventionWaitSeconds` | No | 600 s más, mientras una persona resuelve la verificación |

La ruta del TXT se arma con carpetas del sistema, así que no depende de la PC. El robot lo descarga él mismo y se lo entrega al selector de archivos de Edge: nadie navega por carpetas.

## Secuencia

| Paso | Qué hace |
|---|---|
| 1 | `GET /input`: descarga `consulta_ruc_<jobId>.txt` (un RUC por línea terminado en `\|`, UTF-8, CRLF) |
| 2 | `POST /status` con `EN_CONSULTA` |
| 3 | En la Consulta Múltiple: clic en la pestaña **Validar mediante archivo** |
| 4 | **Ámbito del selector de archivos** (`Browser File Picker Scope`) con la ruta del TXT; dentro, clic en **Seleccionar archivo** |
| 5 | Clic en **Enviar** |
| 6 | SUNAT muestra la página de resultados con el enlace `RM<fecha>.zip`; el robot hace clic en él (el selector usa `href` con comodín, no el nombre del ZIP) y Edge lo descarga |
| 7 | Espera un `RM*.zip` nuevo en Descargas (más reciente que el inicio de la ejecución) |
| 8 | Si pasan 90 s sin ZIP: `POST /status` con `REQUIERE_INTERVENCION`, y espera hasta 10 minutos más |
| 9 | `POST /result` con el ZIP (multipart, campo `file`), sin modificarlo |
| fallo | Cualquier error: `POST /status` con `FALLIDO` y el mensaje, y se relanza la excepción |

Al terminar cierra la ventana de Edge **solo si la abrió el propio robot** (`CloseMode=IfOpenedByAppBrowser`); no toca otras ventanas tuyas.

Respuestas de error del backend en `application/problem+json` (ver `docs/API.md`): `409` si el resultado ya se entregó, `422` si el ZIP no es válido (el trabajo queda igual y se puede reenviar).

## Cómo ejecutarlo

- **Desde la web (un clic):** deja abierto el lanzador en una terminal de PowerShell, desde la raíz del proyecto:
  ```
  ./scripts/lanzador-robot.ps1
  ```
  Compila el robot una vez al iniciar y luego revisa cada 2 segundos si la web pidió un arranque (`GET /api/v1/ruc/robot/pending-start`). Cuando pulsas **Empezar flujo**, toma el pedido (`POST .../claim`) y ejecuta el robot con el id del trabajo. No abre ningún puerto. Parámetros: `-ProjectDir` (por defecto `Documentos\UiPath\RobotSunat`, que sigue a OneDrive si está activo), `-ApiBase`, `-PollSeconds` y `-SkipBuild`. Ctrl+C lo detiene. Si la web espera más de 8 segundos al robot, avisa que el lanzador no está abierto.
- **Studio:** abrir el proyecto `RobotSunat`, Ejecutar, y escribir el `in_JobId`.
- **Línea de comandos:**
  ```
  uip rpa debug start --file-path "Main.xaml" --project-dir "<carpeta de RobotSunat>" --skip-build --input-arguments '{"in_JobId":"<id>"}'
  ```

## Cómo trabaja con tu pantalla

- Casi todo va con `DebuggerApi`: no mueve el mouse ni pide foco, y las acciones ocurren dentro de Edge.
- **Una excepción:** el clic en **Seleccionar archivo** usa el mouse real, porque el navegador solo abre el selector de archivos con un gesto de usuario genuino. Dura un par de segundos y trae Edge al frente.
- El robot no se salta ninguna verificación de SUNAT: si aparece, informa `REQUIERE_INTERVENCION` y espera a que la resuelvas tú en el navegador.

## Problemas conocidos

- **"requiere permitir que la extensión del navegador acceda a los archivos":** falta el paso 2 de "Antes de ejecutarlo".
- **Tiempo de espera del selector de archivos:** el clic de "Seleccionar archivo" no llegó a abrirlo (por ejemplo, Edge estaba minimizado o sin foco). Reintentar con Edge visible.
- **Varias ventanas de Edge con el mismo título:** pueden confundir al selector. Cierra las ventanas viejas de la Consulta Múltiple antes de ejecutar.
- **La página de SUNAT responde "Página de Error" o 525:** es un error de SUNAT o de Cloudflare, no del robot. Reintentar un momento después.
- **Se acumulan ZIP `RM*.zip` en Descargas:** el robot no los borra.
