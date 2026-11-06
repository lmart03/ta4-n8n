# API local

Base desde Windows: `http://localhost:8080`. Desde n8n: `http://backend:8080`. Desde la web (nginx): `http://localhost:4280/api`.
El puerto 8080 solo escucha en `127.0.0.1`. Documentación interactiva: `http://localhost:8080/scalar` (OpenAPI en `/v3/api-docs`).

Por ahora **no hay autenticación**: la API solo es alcanzable desde este equipo. La ranura para activarla (RNF-RUC-01) queda pendiente.

## Parte 1: consulta múltiple de RUC

Flujo: `Excel → TXT para SUNAT → ZIP de resultados de SUNAT → resultados + Excel`.

| Método | Ruta | Quién | Función |
|---|---|---|---|
| POST | `/api/v1/ruc/jobs` | Web | Cargar el Excel (multipart, campo `file`) y crear el trabajo |
| GET | `/api/v1/ruc/jobs?limit=20` | Web | Últimos trabajos, el más reciente primero (máx. 100) |
| GET | `/api/v1/ruc/jobs/{id}` | Web | Estado, totales y resumen |
| GET | `/api/v1/ruc/jobs/{id}/events` | Web | Historial de estados y rechazos |
| GET | `/api/v1/ruc/jobs/{id}/results` | Web | Resultados normalizados, una fila por RUC en el orden del Excel |
| GET | `/api/v1/ruc/jobs/{id}/export` | Web | Excel de resultados (solo si el trabajo terminó) |
| GET | `/api/v1/ruc/jobs/{id}/result-file` | Web | ZIP original de SUNAT, sin modificar |
| GET | `/api/v1/ruc/jobs/{id}/input` | Robot | TXT para subir a la Consulta Múltiple de SUNAT |
| POST | `/api/v1/ruc/jobs/{id}/status` | Robot | Reportar `EN_CONSULTA`, `REQUIERE_INTERVENCION` o `FALLIDO` (JSON) |
| POST | `/api/v1/ruc/jobs/{id}/result` | Robot (o a mano) | Entregar el ZIP de SUNAT (multipart, campo `file`) |
| POST | `/api/v1/ruc/jobs/{id}/start` | Web | **Empezar el flujo**: pedir que arranque el robot (`202`) |
| GET | `/api/v1/ruc/robot/pending-start` | Lanzador | Cola: trabajos con arranque pedido y aún no tomado |
| POST | `/api/v1/ruc/robot/jobs/{id}/claim` | Lanzador | Tomar el pedido antes de ejecutar el robot (`409` si ya lo tomaron) |

### Empezar el flujo desde la web

`POST /start` no ejecuta el robot: el backend corre en Docker y UiPath vive en Windows. Deja el pedido en una cola; el **lanzador local** (`scripts/lanzador-robot.ps1`) consulta `GET /robot/pending-start` cada 2 segundos, toma el pedido con `POST .../claim` y ejecuta el robot con el id del trabajo. No se abre ningún puerto en el equipo. Solo se puede pedir el arranque con el trabajo en `LISTO_PARA_ROBOT` o `FALLIDO` (reintento); con el robot en curso o el trabajo terminado responde `409`. Pedirlo dos veces seguidas no duplica nada.

El campo `startRequestedAt` del trabajo indica que hay un arranque pedido y aún no tomado; la pantalla lo usa para avisar si el lanzador no está abierto.

### Crear un trabajo

El Excel puede tener la hoja `RUC` (o, si no existe, usa la primera) y una columna `ruc` en la primera fila. Acepta RUC como texto o como número entero. Debe tener de 1 a 20 RUC (`RUC_MAX_ROWS`), cada uno con 11 dígitos, prefijo `10`, `15`, `16`, `17` o `20`, **dígito verificador correcto** y sin duplicados. Máximo 2 MB.

`201 Created` con cabecera `Location` y el trabajo:

```json
{ "id": "…", "status": "LISTO_PARA_ROBOT", "total": 20, "foundCount": 0, "missingCount": 0,
  "unexpectedCount": 0, "originalFilename": "ruc.xlsx", "summary": null,
  "createdAt": "2026-11-06T16:00:00Z", "updatedAt": "…", "completedAt": null, "startRequestedAt": null }
```

Si el Excel tiene problemas, responde `422` con **todos** a la vez y no crea nada (ver errores).

### Estados

`RECIBIDO → VALIDANDO → LISTO_PARA_ROBOT → EN_CONSULTA ⇄ REQUIERE_INTERVENCION → PROCESANDO_RESULTADO → COMPLETADO | COMPLETADO_CON_OBSERVACIONES`, y `FALLIDO` (reintentable). Si se entrega el ZIP a mano sin robot, se pasa de `LISTO_PARA_ROBOT` directo a `PROCESANDO_RESULTADO`.

### Entregar el resultado

`POST /result` acepta el ZIP una sola vez por trabajo (`409` en la segunda). Se lee todo en memoria: se rechazan nombres con rutas fuera del archivo, se limita el tamaño descomprimido y se exige un único `.txt`. Las demás entradas se ignoran. Un ZIP inválido da `422`, el trabajo queda como estaba y el rechazo aparece en `/events`.

Se cotejan los RUC pedidos con los recibidos:

- Todos presentes y sin avisos → `COMPLETADO`.
- Faltan RUC, llegan RUC no pedidos o duplicados, o hay filas problemáticas → `COMPLETADO_CON_OBSERVACIONES`, con el detalle en `summary` y por fila en `observation`. **No es un error HTTP.**

### Campos de los resultados

| Campo | Origen en el TXT de SUNAT |
|---|---|
| `razonSocial` | `Nombre ó RazonSocial` |
| `direccionFiscal` | `Direccion` + `Distrito` + `Provincia` + `Departamento` |
| `condicionContribuyente` | `Condicion del Contribuyente` (HABIDO / NO HABIDO) |
| `estadoContribuyente` | `Estado del Contribuyente` (ACTIVO…); **no es** la condición |
| `fechaInicioActividades` | `Fecha de Inicio de Actividades` (`dd/MM/yyyy`) |

Las columnas se leen por nombre, no por posición. El valor `-` de SUNAT se guarda vacío y el relleno `(.)` de la dirección se quita. En el Excel exportado, las fechas anteriores a 1900 (p. ej. el BCP, 1889) van como texto `dd/MM/yyyy`, porque Excel no admite fechas anteriores a 1900.

### Archivos y caché

Los originales se guardan en `rpa/inbox/<id>/` (`input.xlsx`, `input.txt`, `result.zip`, `result.txt`). Las respuestas con datos tributarios llevan `Cache-Control: no-store`, y las descargas `Content-Disposition: attachment`.

## Errores: RFC 9457

Todos los errores salen como `application/problem+json` (RFC 9457):

```json
{
  "type": "/problems/ruc-invalid-workbook",
  "title": "Excel de RUC inválido",
  "status": 422,
  "detail": "El Excel tiene 2 problemas.",
  "instance": "/api/v1/ruc/jobs",
  "traceId": "0f1c…",
  "errors": [
    { "pointer": "#/rows/4/ruc", "code": "RUC_INVALID_CHECK_DIGIT", "detail": "Fila 4: el dígito verificador del RUC 20100047219 no coincide." }
  ]
}
```

`traceId` también queda en el log del backend. En los `5xx`, `detail` es genérico y no filtra datos internos. `pointer` es un JSON Pointer (RFC 6901) y para el Excel apunta a la fila de Excel.

`type` es una URI que se puede abrir: `GET /problems` lista el catálogo y `GET /problems/{slug}` describe cada tipo.

| Tipo | Estado | Cuándo |
|---|---|---|
| `validation-error` | 400 | Falta un parámetro o campo, JSON mal formado, id que no es UUID |
| `not-found` | 404 | El trabajo o la ruta no existen |
| `method-not-allowed` | 405 | Método HTTP no admitido |
| `conflict` | 409 | Resultado ya entregado, transición no permitida, exportar antes de terminar |
| `payload-too-large` | 413 | Archivo de más de 2 MB |
| `unsupported-media-type` | 415 | Tipo de contenido no admitido |
| `ruc-invalid-workbook` | 422 | Excel con filas inválidas (códigos abajo) |
| `ruc-invalid-result-file` | 422 | ZIP de resultados inválido (códigos abajo) |
| `service-unavailable` | 503 | Base de datos o almacenamiento no disponibles |
| `internal-error` | 500 | Error inesperado |

Códigos de `errors[]`:

- **Excel:** `NOT_A_WORKBOOK`, `MISSING_SHEET`, `MISSING_RUC_COLUMN`, `NO_RUC`, `RUC_EMPTY`, `RUC_INVALID_FORMAT`, `RUC_INVALID_PREFIX`, `RUC_INVALID_CHECK_DIGIT`, `RUC_DUPLICATE`, `TOO_MANY_RUC`.
- **ZIP / TXT:** `NOT_A_ZIP`, `ZIP_UNSAFE_ENTRY`, `ZIP_TOO_LARGE`, `ZIP_TOO_MANY_ENTRIES`, `NO_TXT_IN_ZIP`, `MULTIPLE_TXT_IN_ZIP`, `EMPTY_RESULT`, `MISSING_COLUMN`.
- **Campos del JSON:** `INVALID_FIELD`, `INVALID_PARAMETER`.

## Parte 2: consultas del chatbot

Sin cambios de contrato respecto a la versión anterior (el flujo de n8n depende de ellos).

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/v1/business` | Datos ficticios de la clínica |
| GET | `/api/v1/faqs?active=true` | FAQ activas, ordenadas; `active=false` lista todas |
| GET | `/api/v1/faqs/search?q=sabado` | Hasta tres mejores coincidencias léxicas |

`GET /faqs/search?q=...` devuelve `{query, matches}`. Normaliza tildes, minúsculas y espacios, puntúa 3 por palabra clave y 1 por palabra de la pregunta, y conserva solo la puntuación más alta (máximo tres). Sin coincidencias devuelve `200` con `matches: []`. Una consulta vacía, de más de 500 caracteres o sin palabras útiles devuelve `400` (ahora como Problem Details, sin el arreglo `matches`; el flujo de n8n ya lo trata como contingencia). Es búsqueda léxica, no semántica.

## Otros

| Método | Ruta | Función |
|---|---|---|
| GET | `/actuator/health` | Estado de la API |
| GET | `/problems`, `/problems/{slug}` | Catálogo de tipos de error |
| GET | `/v3/api-docs`, `/scalar` | OpenAPI y Scalar |
