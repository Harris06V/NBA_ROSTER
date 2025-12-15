package com.example.nba.audit;

import com.example.nba.domain.Role;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Objects;

public final class AuditEntry {
    private final String actorId;
    private final String actorRole;
    private final String action;
    private final String beforeState;
    private final String afterState;
    private final Instant timestamp;
    private final String prevHash;
    private final String hash;

    public AuditEntry(Role actor, String action, String beforeState, String afterState, String prevHash) {
        Objects.requireNonNull(actor);
        this.actorId = actor.id();
        this.actorRole = actor.getClass().getSimpleName();
        this.action = Objects.requireNonNull(action);
        this.beforeState = Objects.requireNonNull(beforeState);
        this.afterState = Objects.requireNonNull(afterState);
        this.timestamp = Instant.now();
        this.prevHash = Objects.requireNonNull(prevHash);
        this.hash = sha256(prevHash + "|" + actorId + "|" + actorRole + "|" + action + "|" + beforeState + "|" + afterState + "|" + timestamp);
    }

    public String actorId() { return actorId; }
    public String actorRole() { return actorRole; }
    public String action() { return action; }
    public String beforeState() { return beforeState; }
    public String afterState() { return afterState; }
    public Instant timestamp() { return timestamp; }
    public String prevHash() { return prevHash; }
    public String hash() { return hash; }

    public boolean verifiesAgainst(String expectedPrevHash) {
        String recomputed = sha256(expectedPrevHash + "|" + actorId + "|" + actorRole + "|" + action + "|" + beforeState + "|" + afterState + "|" + timestamp);
        return recomputed.equals(hash);
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Override
    public String toString() {
        return "AuditEntry[%s %s action=%s hash=%s...]".formatted(actorRole, actorId, action, hash.substring(0, 10));
    }
}
