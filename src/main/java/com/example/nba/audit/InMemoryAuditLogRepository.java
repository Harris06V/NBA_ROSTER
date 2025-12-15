package com.example.nba.audit;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditLogRepository implements AuditLogRepository {
    private static final String GENESIS = "GENESIS";
    private final List<AuditEntry> entries = new ArrayList<>();

    @Override public String genesisHash() { return GENESIS; }

    @Override
    public void append(AuditEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<AuditEntry> all() {
        return List.copyOf(entries);
    }

    @Override
    public String tailHash() {
        if (entries.isEmpty()) return GENESIS;
        return entries.get(entries.size() - 1).hash();
    }

    @Override
    public boolean verifyIntegrity() {
        String prev = GENESIS;
        for (AuditEntry e : entries) {
            if (!e.verifiesAgainst(prev)) return false;
            prev = e.hash();
        }
        return true;
    }
}
