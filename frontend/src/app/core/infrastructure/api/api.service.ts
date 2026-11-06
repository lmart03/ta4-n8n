import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

/**
 * Cliente HTTP delgado: antepone la base de la API (`environment.apiUrl`) a cada endpoint.
 * Los servicios de cada feature (`*-api.service.ts`) dependen de este y no de `HttpClient`.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  post<T>(endpoint: string, body: unknown): Observable<T> {
    return this.http.post<T>(this.url(endpoint), body);
  }

  get<T>(endpoint: string): Observable<T> {
    return this.http.get<T>(this.url(endpoint));
  }

  put<T>(endpoint: string, body: unknown): Observable<T> {
    return this.http.put<T>(this.url(endpoint), body);
  }

  patch<T>(endpoint: string, body: unknown): Observable<T> {
    return this.http.patch<T>(this.url(endpoint), body);
  }

  delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(this.url(endpoint));
  }

  /** Dirección absoluta de un endpoint (para `EventSource`, descargas y enlaces). */
  urlOf(endpoint: string): string {
    return this.url(endpoint);
  }

  private url(endpoint: string): string {
    return `${environment.apiUrl}/${endpoint.replace(/^\/+/, '')}`;
  }
}
