package com.example.nba.domain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.example.nba.collections.DoublyLinkedList;

public final class Team implements Iterable<Player> {
    private final String teamId;
    private final String name;
    private final SalaryCap salaryCap;
    private final DoublyLinkedList<Player> roster;

    private final Map<String, Money> annualSalaryByPlayerId = new HashMap<>();

    public Team(String teamId, String name, SalaryCap salaryCap) {
        this.teamId = Objects.requireNonNull(teamId);
        this.name = Objects.requireNonNull(name);
        this.salaryCap = Objects.requireNonNull(salaryCap);
        this.roster = new DoublyLinkedList<>();
    }

    public String teamId() { return teamId; }
    public String name() { return name; }
    public SalaryCap salaryCap() { return salaryCap; }
    public int rosterSize() { return roster.size(); }

    public void addPlayer(Player p, Money annualSalary) {
        Objects.requireNonNull(p);
        Objects.requireNonNull(annualSalary);
        if (rosterSize() >= 20) throw new IllegalStateException("roster full");
        roster.addLast(p);
        annualSalaryByPlayerId.put(p.playerId(), annualSalary);
    }

    public boolean removePlayer(Player p) {
        Objects.requireNonNull(p);
        boolean removed = roster.removeFirstOccurrence(p);
        if (removed) annualSalaryByPlayerId.remove(p.playerId());
        return removed;
    }

    public Optional<Player> findPlayerById(String playerId) {
        Objects.requireNonNull(playerId);
        for (Player p : roster) {
            if (playerId.equals(p.playerId())) return Optional.of(p);
        }
        return Optional.empty();
    }

    public Money annualSalaryFor(String playerId) {
        Money m = annualSalaryByPlayerId.get(playerId);
        if (m == null) throw new IllegalArgumentException("No salary tracked for playerId=" + playerId);
        return m;
    }

    @Override public Iterator<Player> iterator() { return roster.iterator(); }

    @Override
    public String toString() {
        return "Team[%s (%s) roster=%d capRemaining=%s]"
                .formatted(name, teamId, rosterSize(), salaryCap.remaining());
    }
}
