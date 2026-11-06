package pe.edu.ta4.ruc.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.model.ParsedResult;
import pe.edu.ta4.ruc.model.Reconciliation;
import pe.edu.ta4.ruc.model.SunatRecord;
import pe.edu.ta4.ruc.service.ReconciliationService;

@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    @Override
    public Reconciliation reconcile(List<String> requested, ParsedResult parsed) {
        Set<String> wanted = new LinkedHashSet<>(requested);
        Map<String, SunatRecord> byRuc = new LinkedHashMap<>();
        Set<String> unexpected = new LinkedHashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();

        for (SunatRecord record : parsed.records()) {
            if (!wanted.contains(record.ruc())) {
                unexpected.add(record.ruc());
            } else if (byRuc.putIfAbsent(record.ruc(), record) != null) {
                duplicates.add(record.ruc());
            }
        }

        List<String> missing = new ArrayList<>();
        for (String ruc : requested) {
            if (!byRuc.containsKey(ruc)) missing.add(ruc);
        }
        return new Reconciliation(byRuc, List.copyOf(missing), List.copyOf(unexpected), List.copyOf(duplicates));
    }
}
