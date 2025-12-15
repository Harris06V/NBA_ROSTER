package com.example.nba.domain;

import java.util.Objects;

public abstract class Player {
    private final String playerId;
    private final String name;
    private final Position position;
    private final int age;
    private final int offense;
    private final int defense;
    private int fatigue; // 0..100, higher = more tired

    protected Player(String playerId, String name, Position position, int age, int offense, int defense) {
        this.playerId = Objects.requireNonNull(playerId);
        this.name = Objects.requireNonNull(name);
        this.position = Objects.requireNonNull(position);
        if (age < 16) throw new IllegalArgumentException("age too low");
        this.age = age;
        this.offense = clamp0to99(offense);
        this.defense = clamp0to99(defense);
        this.fatigue = 0;
    }

    public String playerId() { return playerId; }
    public String name() { return name; }
    public Position position() { return position; }
    public int age() { return age; }
    public int offense() { return offense; }
    public int defense() { return defense; }

    /** Overall rating in [0..99]. */
    public int overallRating() {
        return (offense + defense) / 2;
    }

    /** Fatigue [0..100]. */
    public int fatigue() { return fatigue; }

    public void applyMinutes(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("minutes");
        // Simple fatigue model: +1 fatigue per 2 minutes, max 100
        fatigue = Math.min(100, fatigue + (minutes / 2));
    }

    public void rest() {
        fatigue = Math.max(0, fatigue - 20);
    }

    /** Used by lineup optimizer as a penalty. */
    public double effectiveRating() {
        // reduce effectiveness as fatigue rises
        double penalty = fatigue * 0.35;
        return Math.max(0, overallRating() - penalty);
    }

    protected int clamp0to99(int x) {
        return Math.max(0, Math.min(99, x));
    }

    public abstract ExperienceLevel experienceLevel();

    public abstract <R> R accept(PlayerVisitor<R> v);

    @Override
    public String toString() {
        return "%s(%s, %s, O:%d D:%d Fat:%d)".formatted(name, playerId, position, offense, defense, fatigue);
    }
}
