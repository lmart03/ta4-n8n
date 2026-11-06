import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { ProblemDetails } from 'src/app/core/infrastructure/api/models/problem-details.model';
import { RobotStatus } from '../../domain/models/robot-status.model';
import { RucJobEvent } from '../../domain/models/ruc-job-event.model';
import { RucJobItem } from '../../domain/models/ruc-job-item.model';
import { RucJob } from '../../domain/models/ruc-job.model';
import { GridSortKey, GridStatusFilter } from '../../domain/rules/results-grid.rule';

/** Todo lo que puede pasar en el flujo de consulta de RUC: el usuario, el backend y el sondeo. */
export const RucFlowActions = createActionGroup({
  source: 'Ruc Flow',
  events: {
    // Archivo
    'File Selected': props<{ name: string; size: number; error: string | null }>(),
    'File Cleared': emptyProps(),

    // Empezar: subir el Excel y pedir el arranque del robot
    Begin: props<{ file: File }>(),
    'Upload Succeeded': props<{ job: RucJob }>(),
    'Upload Failed': props<{ problem: ProblemDetails }>(),
    Retry: props<{ jobId: string }>(),
    'Start Succeeded': props<{ job: RucJob }>(),
    'Start Failed': props<{ jobId: string; problem: ProblemDetails }>(),

    // Plan B: el usuario consulta en SUNAT por su cuenta y entrega el ZIP a mano
    'Manual Result Selected': props<{ jobId: string; file: File }>(),
    'Api Lookup Requested': props<{ jobId: string }>(),
    'Manual Result Uploaded': props<{ job: RucJob }>(),
    'Manual Result Failed': props<{ problem: ProblemDetails }>(),

    // Seguimiento del trabajo (sondeo)
    'Poll Succeeded': props<{ job: RucJob; events: RucJobEvent[]; items: RucJobItem[]; now: number }>(),
    'Poll Failed': emptyProps(),
    Reset: emptyProps(),

    // Grilla
    'Search Changed': props<{ text: string }>(),
    'Status Filter Changed': props<{ filter: GridStatusFilter }>(),
    'Sort Requested': props<{ key: GridSortKey }>(),

    // Estado del robot (lanzador local)
    'Robot Watch Started': emptyProps(),
    'Robot Watch Stopped': emptyProps(),
    'Robot Checked': props<{ status: RobotStatus | null }>(),
  },
});
