package pe.edu.ta4.ruc.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertThat(JobStatus.RECIBIDO.canMoveTo(JobStatus.VALIDANDO)).isTrue();
        assertThat(JobStatus.VALIDANDO.canMoveTo(JobStatus.LISTO_PARA_ROBOT)).isTrue();
        assertThat(JobStatus.LISTO_PARA_ROBOT.canMoveTo(JobStatus.EN_CONSULTA)).isTrue();
        assertThat(JobStatus.EN_CONSULTA.canMoveTo(JobStatus.PROCESANDO_RESULTADO)).isTrue();
        assertThat(JobStatus.PROCESANDO_RESULTADO.canMoveTo(JobStatus.COMPLETADO)).isTrue();
        assertThat(JobStatus.PROCESANDO_RESULTADO.canMoveTo(JobStatus.COMPLETADO_CON_OBSERVACIONES)).isTrue();
    }

    @Test
    void manualUploadCanSkipTheRobot() {
        assertThat(JobStatus.LISTO_PARA_ROBOT.canMoveTo(JobStatus.PROCESANDO_RESULTADO)).isTrue();
    }

    @Test
    void humanInterventionCanResume() {
        assertThat(JobStatus.EN_CONSULTA.canMoveTo(JobStatus.REQUIERE_INTERVENCION)).isTrue();
        assertThat(JobStatus.REQUIERE_INTERVENCION.canMoveTo(JobStatus.EN_CONSULTA)).isTrue();
    }

    @Test
    void failedJobsCanBeRetried() {
        assertThat(JobStatus.FALLIDO.canMoveTo(JobStatus.LISTO_PARA_ROBOT)).isTrue();
        assertThat(JobStatus.FALLIDO.canMoveTo(JobStatus.EN_CONSULTA)).isTrue();
    }

    @Test
    void finalStatesNeverChange() {
        for (JobStatus next : JobStatus.values()) {
            assertThat(JobStatus.COMPLETADO.canMoveTo(next)).isFalse();
            assertThat(JobStatus.COMPLETADO_CON_OBSERVACIONES.canMoveTo(next)).isFalse();
        }
        assertThat(JobStatus.COMPLETADO.isFinal()).isTrue();
        assertThat(JobStatus.COMPLETADO_CON_OBSERVACIONES.isFinal()).isTrue();
        assertThat(JobStatus.FALLIDO.isFinal()).isFalse();
    }

    @Test
    void cannotSkipSteps() {
        assertThat(JobStatus.RECIBIDO.canMoveTo(JobStatus.COMPLETADO)).isFalse();
        assertThat(JobStatus.RECIBIDO.canMoveTo(JobStatus.EN_CONSULTA)).isFalse();
        assertThat(JobStatus.VALIDANDO.canMoveTo(JobStatus.COMPLETADO)).isFalse();
    }

    @Test
    void resultIsOnlyAcceptedWhenTheJobIsReady() {
        assertThat(JobStatus.RECIBIDO.canReceiveResult()).isFalse();
        assertThat(JobStatus.VALIDANDO.canReceiveResult()).isFalse();
        assertThat(JobStatus.PROCESANDO_RESULTADO.canReceiveResult()).isFalse();
        assertThat(JobStatus.COMPLETADO.canReceiveResult()).isFalse();
        assertThat(JobStatus.LISTO_PARA_ROBOT.canReceiveResult()).isTrue();
        assertThat(JobStatus.EN_CONSULTA.canReceiveResult()).isTrue();
        assertThat(JobStatus.REQUIERE_INTERVENCION.canReceiveResult()).isTrue();
        assertThat(JobStatus.FALLIDO.canReceiveResult()).isTrue();
    }

    @Test
    void robotCanOnlyReportThreeStates() {
        assertThat(JobStatus.values()).filteredOn(JobStatus::isRobotReportable)
            .containsExactlyInAnyOrder(JobStatus.EN_CONSULTA, JobStatus.REQUIERE_INTERVENCION, JobStatus.FALLIDO);
    }

    @Test
    void entityRejectsIllegalTransitionsAndStampsCompletion() {
        OffsetDateTime now = OffsetDateTime.parse("2026-11-06T10:00:00Z");
        RucJob job = new RucJob(UUID.randomUUID(), 20, "ruc.xlsx", now);
        assertThatThrownBy(() -> job.moveTo(JobStatus.COMPLETADO, now)).isInstanceOf(IllegalStateException.class);

        job.moveTo(JobStatus.VALIDANDO, now);
        job.moveTo(JobStatus.LISTO_PARA_ROBOT, now);
        job.moveTo(JobStatus.PROCESANDO_RESULTADO, now.plusMinutes(1));
        assertThat(job.getCompletedAt()).isNull();
        job.moveTo(JobStatus.COMPLETADO, now.plusMinutes(2));
        assertThat(job.getCompletedAt()).isEqualTo(now.plusMinutes(2));
    }
}
