package com.example.nba.league;

import com.example.nba.domain.Team;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton registry (thread-safe via classloader init).
 */
public final class LeagueRegistry {
    private final Map<String, Team> teams = new ConcurrentHashMap<>();

    private LeagueRegistry() {}

    private static final class Holder {
        private static final LeagueRegistry INSTANCE = new LeagueRegistry();
    }

    public static LeagueRegistry instance() { return Holder.INSTANCE; }

    public void registerTeam(Team team) {
        teams.put(team.teamId(), team);
    }

    public Optional<Team> getTeam(String teamId) {
        return Optional.ofNullable(teams.get(teamId));
    }
}
