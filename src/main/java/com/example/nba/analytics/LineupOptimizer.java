package com.example.nba.analytics;

import com.example.nba.domain.Player;
import com.example.nba.domain.Position;
import com.example.nba.domain.Team;

import java.util.*;

/**
 * Novel-ish algorithm: compute best starting 5 under positional constraints.
 * Constraint: must contain exactly one of each position (PG, SG, SF, PF, C).
 * Objective: maximize sum of visitor-based player value.
 *
 * This is a backtracking search with pruning (branch-and-bound).
 */
public final class LineupOptimizer {

    private final PlayerValueVisitor valueVisitor = new PlayerValueVisitor();

    public record Lineup(List<Player> starters, int score) { }

    public Lineup bestStartingFive(Team team) {
        Objects.requireNonNull(team);

        Map<Position, List<Player>> byPos = new EnumMap<>(Position.class);
        for (Position p : Position.values()) byPos.put(p, new ArrayList<>());

        for (Player pl : team) byPos.get(pl.position()).add(pl);

        for (Position p : Position.values()) {
            if (byPos.get(p).isEmpty()) {
                throw new IllegalStateException("Cannot build lineup: missing position " + p);
            }
        }

        List<Position> order = List.of(Position.PG, Position.SG, Position.SF, Position.PF, Position.C);

        // Sort candidates by descending value for pruning
        for (Position p : order) {
            byPos.get(p).sort(Comparator.comparingInt((Player pl) -> pl.accept(valueVisitor)).reversed());
        }

        List<Player> best = new ArrayList<>(5);
        int[] bestScore = { Integer.MIN_VALUE };

        backtrack(order, byPos, 0, new ArrayList<>(5), 0, bestScore, best);

        return new Lineup(List.copyOf(best), bestScore[0]);
    }

    private void backtrack(List<Position> order,
                           Map<Position, List<Player>> byPos,
                           int idx,
                           List<Player> chosen,
                           int scoreSoFar,
                           int[] bestScore,
                           List<Player> bestChosen) {

        if (idx == order.size()) {
            if (scoreSoFar > bestScore[0]) {
                bestScore[0] = scoreSoFar;
                bestChosen.clear();
                bestChosen.addAll(chosen);
            }
            return;
        }

        Position pos = order.get(idx);
        List<Player> candidates = byPos.get(pos);

        // Upper bound: score so far + best remaining per position
        int upperBound = scoreSoFar;
        for (int j = idx; j < order.size(); j++) {
            Position p = order.get(j);
            upperBound += byPos.get(p).get(0).accept(valueVisitor);
        }
        if (upperBound <= bestScore[0]) return; // prune

        for (Player pl : candidates) {
            chosen.add(pl);
            int nextScore = scoreSoFar + pl.accept(valueVisitor);
            backtrack(order, byPos, idx + 1, chosen, nextScore, bestScore, bestChosen);
            chosen.remove(chosen.size() - 1);
        }
    }
}
