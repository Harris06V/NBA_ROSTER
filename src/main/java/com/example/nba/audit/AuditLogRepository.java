package com.example.nba.audit;

import java.util.List;

public interface AuditLogRepository {
    String genesisHash();
    void append(AuditEntry entry);
    List<AuditEntry> all();
    String tailHash();
    boolean verifyIntegrity();
}
