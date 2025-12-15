package com.example.nba.domain;

public final class RookiePlayer extends Player {
    public RookiePlayer(String playerId, String name, Position position, int age, int offense, int defense) {
        super(playerId, name, position, age, offense, defense);
    }

    @Override public ExperienceLevel experienceLevel() { return ExperienceLevel.ROOKIE; }

    @Override public <R> R accept(PlayerVisitor<R> v) { return v.visitRookie(this); }
}
