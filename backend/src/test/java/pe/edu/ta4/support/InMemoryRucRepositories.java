package pe.edu.ta4.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pe.edu.ta4.ruc.model.RucJob;
import pe.edu.ta4.ruc.model.RucJobEvent;
import pe.edu.ta4.ruc.model.RucJobItem;
import pe.edu.ta4.ruc.repository.RucJobEventRepository;
import pe.edu.ta4.ruc.repository.RucJobItemRepository;
import pe.edu.ta4.ruc.repository.RucJobRepository;

/** Repositorios simulados con memoria: permiten probar el servicio sin base de datos. */
public final class InMemoryRucRepositories {
    public final Map<UUID, RucJob> jobs = new LinkedHashMap<>();
    public final List<RucJobItem> items = new ArrayList<>();
    public final List<RucJobEvent> events = new ArrayList<>();

    public final RucJobRepository jobRepository = mock(RucJobRepository.class);
    public final RucJobItemRepository itemRepository = mock(RucJobItemRepository.class);
    public final RucJobEventRepository eventRepository = mock(RucJobEventRepository.class);

    @SuppressWarnings("unchecked")
    public InMemoryRucRepositories() {
        when(jobRepository.save(any(RucJob.class))).thenAnswer(call -> {
            RucJob job = call.getArgument(0);
            jobs.put(job.getId(), job);
            return job;
        });
        when(jobRepository.findById(any(UUID.class))).thenAnswer(call -> Optional.ofNullable(jobs.get(call.<UUID>getArgument(0))));
        when(jobRepository.findByIdForUpdate(any(UUID.class))).thenAnswer(call -> Optional.ofNullable(jobs.get(call.<UUID>getArgument(0))));
        when(jobRepository.findAllByOrderByCreatedAtDesc(any())).thenAnswer(call -> {
            int size = call.<org.springframework.data.domain.Pageable>getArgument(0).getPageSize();
            return jobs.values().stream().sorted(Comparator.comparing(RucJob::getCreatedAt).reversed()).limit(size).toList();
        });

        when(jobRepository.findByStartRequestedAtIsNotNullOrderByStartRequestedAtAsc()).thenAnswer(call ->
            jobs.values().stream().filter(job -> job.getStartRequestedAt() != null)
                .sorted(Comparator.comparing(RucJob::getStartRequestedAt)).toList());

        when(itemRepository.saveAll(any())).thenAnswer(call -> {
            Iterable<RucJobItem> saved = call.getArgument(0);
            for (RucJobItem item : saved) {
                if (!items.contains(item)) items.add(item);
            }
            return saved;
        });
        when(itemRepository.findByJobIdOrderByExcelRowAsc(any(UUID.class))).thenAnswer(call ->
            items.stream().filter(item -> item.getJobId().equals(call.<UUID>getArgument(0)))
                .sorted(Comparator.comparingInt(RucJobItem::getExcelRow)).toList());

        when(eventRepository.save(any(RucJobEvent.class))).thenAnswer(call -> {
            RucJobEvent event = call.getArgument(0);
            events.add(event);
            return event;
        });
        when(eventRepository.countByJobId(any(UUID.class))).thenAnswer(call ->
            (int) events.stream().filter(event -> event.getJobId().equals(call.<UUID>getArgument(0))).count());
        when(eventRepository.findByJobIdOrderBySequenceAsc(any(UUID.class))).thenAnswer(call ->
            events.stream().filter(event -> event.getJobId().equals(call.<UUID>getArgument(0)))
                .sorted(Comparator.comparingInt(RucJobEvent::getSequence)).toList());
    }
}
