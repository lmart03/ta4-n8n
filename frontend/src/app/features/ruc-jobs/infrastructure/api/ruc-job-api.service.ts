import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from 'src/app/core/infrastructure/api/api.service';
import { environment } from 'src/environments/environment';
import { RobotStatus } from '../../domain/models/robot-status.model';
import { RucJobEvent } from '../../domain/models/ruc-job-event.model';
import { RucJobResults } from '../../domain/models/ruc-job-item.model';
import { RucJob } from '../../domain/models/ruc-job.model';

/** Acceso HTTP a los trabajos de consulta de RUC. Solo habla con la API: no tiene lógica de pantalla. */
@Injectable({ providedIn: 'root' })
export class RucJobApiService {
  private readonly api = inject(ApiService);

  /** `POST /v1/ruc/jobs` (multipart, campo `file`). Responde `201` con el trabajo creado. */
  upload(file: File): Observable<RucJob> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.api.post<RucJob>('v1/ruc/jobs', body);
  }

  /** `POST /v1/ruc/jobs/{id}/start`: pide arrancar el robot (lo recoge el lanzador local). */
  start(id: string): Observable<RucJob> {
    return this.api.post<RucJob>(`v1/ruc/jobs/${id}/start`, null);
  }

  get(id: string): Observable<RucJob> {
    return this.api.get<RucJob>(`v1/ruc/jobs/${id}`);
  }

  events(id: string): Observable<RucJobEvent[]> {
    return this.api.get<RucJobEvent[]>(`v1/ruc/jobs/${id}/events`);
  }

  results(id: string): Observable<RucJobResults> {
    return this.api.get<RucJobResults>(`v1/ruc/jobs/${id}/results`);
  }

  /** `POST /v1/ruc/jobs/{id}/result` (multipart, campo `file`): entrega a mano el ZIP que descargó el usuario de SUNAT. */
  uploadResult(id: string, file: File): Observable<RucJob> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.api.post<RucJob>(`v1/ruc/jobs/${id}/result`, body);
  }

  /** `POST /v1/ruc/jobs/{id}/lookup-api`: plan C, consulta los RUC en la API de consultaderuc.pe (sin robot). */
  lookupViaApi(id: string): Observable<RucJob> {
    return this.api.post<RucJob>(`v1/ruc/jobs/${id}/lookup-api`, null);
  }

  /** Dirección de descarga del TXT que se sube a la Consulta Múltiple (para hacer el paso a mano). */
  inputUrl(id: string): string {
    return `${environment.apiUrl}/v1/ruc/jobs/${id}/input`;
  }

  /** `GET /v1/ruc/robot/status`: el lanzador del robot consultó la cola hace pocos segundos. */
  robotStatus(): Observable<RobotStatus> {
    return this.api.get<RobotStatus>('v1/ruc/robot/status');
  }

  /** Dirección de descarga del Excel de resultados (se usa como `href`, el navegador hace la descarga). */
  exportUrl(id: string): string {
    return `${environment.apiUrl}/v1/ruc/jobs/${id}/export`;
  }
}
