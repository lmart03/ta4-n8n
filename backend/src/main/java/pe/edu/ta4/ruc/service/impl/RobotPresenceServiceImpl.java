package pe.edu.ta4.ruc.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.dto.RobotStatusResponse;
import pe.edu.ta4.ruc.service.RobotPresenceService;

@Service
public class RobotPresenceServiceImpl implements RobotPresenceService {
    /** El lanzador consulta cada 2 s; con 3 consultas perdidas seguidas se da por desconectado. */
    static final Duration ONLINE_WINDOW = Duration.ofSeconds(8);

    private final Clock clock;
    private final AtomicReference<Instant> lastSeen = new AtomicReference<>();

    public RobotPresenceServiceImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void heartbeat() {
        lastSeen.set(clock.instant());
    }

    @Override
    public RobotStatusResponse status() {
        Instant seen = lastSeen.get();
        if (seen == null) return new RobotStatusResponse(false, null);
        boolean online = Duration.between(seen, clock.instant()).compareTo(ONLINE_WINDOW) <= 0;
        return new RobotStatusResponse(online, OffsetDateTime.ofInstant(seen, clock.getZone()));
    }
}
