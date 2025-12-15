package com.example.nba.domain;

public final class VeteranPlayer extends Player {
    private final int yearsInLeague;

    public VeteranPlayer(String playerId, String name, Position position, int age, int offense, int defense, int yearsInLeague) {
        super(playerId, name, position, age, offense, defense);
        if (yearsInLeague < 0) throw new IllegalArgumentException("yearsInLeague");
        this.yearsInLeague = yearsInLeague;
    }

    public int yearsInLeague() { return yearsInLeague; }

    @Override public ExperienceLevel experienceLevel() { return ExperienceLevel.VETERAN; }

    @Override public <R> R accept(PlayerVisitor<R> v) { return v.visitVeteran(this); }
}
