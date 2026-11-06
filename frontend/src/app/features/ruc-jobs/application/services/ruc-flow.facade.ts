import { Injectable, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { GridSortKey, GridStatusFilter } from '../../domain/rules/results-grid.rule';
import { validateWorkbookFile } from '../../domain/rules/workbook-file.rule';
import { RucJobApiService } from '../../infrastructure/api/ruc-job-api.service';
import { RucFlowActions } from '../store/ruc-flow.actions';
import { rucFlowFeature } from '../store/ruc-flow.feature';

export { POLL_INTERVAL_MS, ROBOT_POLL_INTERVAL_MS } from '../store/ruc-flow.support';

/**
 * Puerta de entrada de la UI al estado del flujo (NgRx Store): expone el estado como Signals y traduce cada
 * gesto del usuario en una acción. La pantalla no conoce el Store, HTTP ni las reglas de las etapas.
 */
@Injectable({ providedIn: 'root' })
export class RucFlowFacade {
  private readonly store = inject(Store);
  private readonly api = inject(RucJobApiService);

  /** El `File` es un objeto del navegador, no serializable: se guarda aquí y el Store solo recibe sus datos. */
  private selectedFile: File | null = null;

  readonly fileName = this.store.selectSignal(rucFlowFeature.selectFileName);
  readonly fileSize = this.store.selectSignal(rucFlowFeature.selectFileSize);
  readonly clientError = this.store.selectSignal(rucFlowFeature.selectClientError);
  readonly job = this.store.selectSignal(rucFlowFeature.selectJob);
  readonly items = this.store.selectSignal(rucFlowFeature.selectItems);
  readonly problem = this.store.selectSignal(rucFlowFeature.selectProblem);
  readonly busy = this.store.selectSignal(rucFlowFeature.selectBusy);
  readonly offline = this.store.selectSignal(rucFlowFeature.selectOffline);
  readonly search = this.store.selectSignal(rucFlowFeature.selectSearch);
  readonly statusFilter = this.store.selectSignal(rucFlowFeature.selectStatusFilter);
  readonly sortKey = this.store.selectSignal(rucFlowFeature.selectSortKey);
  readonly sortDirection = this.store.selectSignal(rucFlowFeature.selectSortDirection);

  readonly flow = this.store.selectSignal(rucFlowFeature.selectFlow);
  readonly summary = this.store.selectSignal(rucFlowFeature.selectSummary);
  readonly visibleItems = this.store.selectSignal(rucFlowFeature.selectVisibleItems);
  readonly canBegin = this.store.selectSignal(rucFlowFeature.selectCanBegin);
  readonly canRetry = this.store.selectSignal(rucFlowFeature.selectCanRetry);
  readonly robotState = this.store.selectSignal(rucFlowFeature.selectRobotState);

  /** Dirección de descarga del Excel; solo existe cuando el trabajo terminó. */
  readonly exportUrl = this.store.selectSignal((state) => {
    const job = rucFlowFeature.selectJob(state);
    const done = job !== null && (job.status === 'COMPLETADO' || job.status === 'COMPLETADO_CON_OBSERVACIONES');
    return done ? this.api.exportUrl(job.id) : null;
  });

  /** Descarga del TXT para hacer a mano la consulta en SUNAT (plan B); existe mientras haya trabajo. */
  readonly inputUrl = this.store.selectSignal((state) => {
    const job = rucFlowFeature.selectJob(state);
    return job === null ? null : this.api.inputUrl(job.id);
  });

  /** Plan B: entrega el ZIP que el usuario descargó de SUNAT por su cuenta. */
  uploadManualResult(file: File): void {
    const job = this.job();
    if (job === null || this.busy()) return;
    this.store.dispatch(RucFlowActions.manualResultSelected({ jobId: job.id, file }));
  }

  /** Plan C: consulta los RUC en la API de consultaderuc.pe (el trabajo queda marcado con esa fuente). */
  lookupViaApi(): void {
    const job = this.job();
    if (job === null || this.busy()) return;
    this.store.dispatch(RucFlowActions.apiLookupRequested({ jobId: job.id }));
  }

  /** Empieza a vigilar si el robot está listo (la pantalla lo llama al abrirse). */
  watchRobot(): void {
    this.store.dispatch(RucFlowActions.robotWatchStarted());
  }

  /** Deja de vigilar al robot (la pantalla lo llama al cerrarse). */
  stopWatchingRobot(): void {
    this.store.dispatch(RucFlowActions.robotWatchStopped());
  }

  /** Elige el archivo: lo valida en el cliente y descarta cualquier error anterior. */
  select(file: File): void {
    this.selectedFile = file;
    this.store.dispatch(
      RucFlowActions.fileSelected({ name: file.name, size: file.size, error: validateWorkbookFile(file) }),
    );
  }

  /** Quita el archivo elegido (solo antes de empezar). */
  clear(): void {
    this.selectedFile = null;
    this.store.dispatch(RucFlowActions.fileCleared());
  }

  /** "Empezar flujo": sube el Excel (que lo valida) y pide el arranque del robot; desde ahí sigue el avance solo. */
  begin(): void {
    if (this.selectedFile === null || !this.canBegin()) return;
    this.store.dispatch(RucFlowActions.begin({ file: this.selectedFile }));
  }

  /** Vuelve a pedir el arranque del robot (después de un fallo, o si el primer pedido no llegó). */
  retry(): void {
    const job = this.job();
    if (!job || !this.canRetry()) return;
    this.store.dispatch(RucFlowActions.retry({ jobId: job.id }));
  }

  /** Vuelve a la pantalla inicial para un flujo nuevo. */
  reset(): void {
    this.selectedFile = null;
    this.store.dispatch(RucFlowActions.reset());
  }

  setSearch(text: string): void {
    this.store.dispatch(RucFlowActions.searchChanged({ text }));
  }

  clearFilters(): void {
    this.store.dispatch(RucFlowActions.searchChanged({ text: '' }));
    this.store.dispatch(RucFlowActions.statusFilterChanged({ filter: 'ALL' }));
  }

  setStatusFilter(filter: GridStatusFilter): void {
    this.store.dispatch(RucFlowActions.statusFilterChanged({ filter }));
  }

  /** Ordena por una columna; pulsar la misma columna invierte la dirección. */
  sortBy(key: GridSortKey): void {
    this.store.dispatch(RucFlowActions.sortRequested({ key }));
  }
}
