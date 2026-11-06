package pe.edu.ta4.ruc.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Estados de un trabajo de consulta de RUC y las transiciones permitidas entre ellos. */
public enum JobStatus {
    RECIBIDO,
    VALIDANDO,
    LISTO_PARA_ROBOT,
    EN_CONSULTA,
    REQUIERE_INTERVENCION,
    PROCESANDO_RESULTADO,
    COMPLETADO,
    COMPLETADO_CON_OBSERVACIONES,
    FALLIDO;

    private static final Map<JobStatus, Set<JobStatus>> TRANSITIONS = Map.of(
        RECIBIDO, EnumSet.of(VALIDANDO, FALLIDO),
        VALIDANDO, EnumSet.of(LISTO_PARA_ROBOT, FALLIDO),
        LISTO_PARA_ROBOT, EnumSet.of(EN_CONSULTA, PROCESANDO_RESULTADO, FALLIDO),
        EN_CONSULTA, EnumSet.of(REQUIERE_INTERVENCION, PROCESANDO_RESULTADO, FALLIDO),
        REQUIERE_INTERVENCION, EnumSet.of(EN_CONSULTA, PROCESANDO_RESULTADO, FALLIDO),
        PROCESANDO_RESULTADO, EnumSet.of(COMPLETADO, COMPLETADO_CON_OBSERVACIONES, FALLIDO),
        COMPLETADO, EnumSet.noneOf(JobStatus.class),
        COMPLETADO_CON_OBSERVACIONES, EnumSet.noneOf(JobStatus.class),
        FALLIDO, EnumSet.of(LISTO_PARA_ROBOT, EN_CONSULTA, PROCESANDO_RESULTADO));

    private static final Set<JobStatus> RESULT_RECEIVABLE =
        EnumSet.of(LISTO_PARA_ROBOT, EN_CONSULTA, REQUIERE_INTERVENCION, FALLIDO);

    private static final Set<JobStatus> ROBOT_REPORTABLE = EnumSet.of(EN_CONSULTA, REQUIERE_INTERVENCION, FALLIDO);

    public boolean canMoveTo(JobStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    /** El resultado ya se entregó y procesó: el trabajo no cambia más. */
    public boolean isFinal() {
        return this == COMPLETADO || this == COMPLETADO_CON_OBSERVACIONES;
    }

    /** Solo en estos estados se acepta el ZIP de resultados. */
    public boolean canReceiveResult() {
        return RESULT_RECEIVABLE.contains(this);
    }

    /** Estados que el robot puede reportar por su cuenta (el resto los decide el backend). */
    public boolean isRobotReportable() {
        return ROBOT_REPORTABLE.contains(this);
    }
}
