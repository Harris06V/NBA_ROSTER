package com.example.nba.repo;

import com.example.nba.domain.Team;

import java.util.*;
import java.util.function.Predicate;

public final class InMemoryTeamRepository implements TeamRepository {
    private final Map<String, Team> store = new HashMap<>();

    @Override
    public void save(Team team) {
        store.put(team.teamId(), team);
    }

    @Override
    public Optional<Team> findById(String teamId) {
        return Optional.ofNullable(store.get(teamId));
    }

    @Override
    public List<Team> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<Team> search(Predicate<Team> predicate) {
        Objects.requireNonNull(predicate);
        List<Team> out = new ArrayList<>();
        for (Team t : store.values()) if (predicate.test(t)) out.add(t);
        return out;
    }
}
