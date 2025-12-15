package com.example.nba.domain;

public final class TwoWayPlayer extends Player {
    private int gLeagueDaysRemaining;

    public TwoWayPlayer(String playerId, String name, Position position, int age, int offense, int defense, int gLeagueDaysRemaining) {
        super(playerId, name, position, age, offense, defense);
        if (gLeagueDaysRemaining < 0) throw new IllegalArgumentException("gLeagueDaysRemaining");
        this.gLeagueDaysRemaining = gLeagueDaysRemaining;
    }

    public int gLeagueDaysRemaining() { return gLeagueDaysRemaining; }

    public void assignToGLeague(int days) {
        if (days <= 0) throw new IllegalArgumentException("days");
        gLeagueDaysRemaining = Math.max(0, gLeagueDaysRemaining - days);
    }

    @Override public ExperienceLevel experienceLevel() { return ExperienceLevel.TWO_WAY; }

    @Override public <R> R accept(PlayerVisitor<R> v) { return v.visitTwoWay(this); }
}
