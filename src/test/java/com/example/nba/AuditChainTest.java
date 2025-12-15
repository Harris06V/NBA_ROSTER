package com.example.nba;

import com.example.nba.audit.AuditEntry;
import com.example.nba.audit.InMemoryAuditLogRepository;
import com.example.nba.domain.Coach;
import com.example.nba.domain.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuditChainTest {

    @Test
    void auditChainVerifies() {
        var repo = new InMemoryAuditLogRepository();
        Role coach = new Coach("u1", "Coach");

        repo.append(new AuditEntry(coach, "A", "b0", "a0", repo.tailHash()));
        repo.append(new AuditEntry(coach, "B", "b1", "a1", repo.tailHash()));
        repo.append(new AuditEntry(coach, "C", "b2", "a2", repo.tailHash()));

        assertTrue(repo.verifyIntegrity());
        assertEquals(3, repo.all().size());
        assertNotEquals(repo.genesisHash(), repo.tailHash());
    }
}
