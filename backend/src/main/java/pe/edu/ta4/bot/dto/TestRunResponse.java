package pe.edu.ta4.bot.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record TestRunResponse(boolean running, List<TestCaseResult> cases, OffsetDateTime startedAt) {
}
