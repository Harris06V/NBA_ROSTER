package com.example.nba.factory;

import com.example.nba.domain.Position;

import java.util.Objects;
import java.util.UUID;

/** Builder for creating players with readable stepwise configuration. */
public final class PlayerBuilder {
    private String playerId = UUID.randomUUID().toString();
    private String name = "Unknown";
    private Position position = Position.SG;
    private int age = 19;
    private int offense = 70;
    private int defense = 70;
    private int yearsInLeague = 0;
    private int gLeagueDaysRemaining = 50;

    public PlayerBuilder playerId(String id) { this.playerId = Objects.requireNonNull(id); return this; }
    public PlayerBuilder name(String n) { this.name = Objects.requireNonNull(n); return this; }
    public PlayerBuilder position(Position p) { this.position = Objects.requireNonNull(p); return this; }
    public PlayerBuilder age(int a) { this.age = a; return this; }
    public PlayerBuilder offense(int o) { this.offense = o; return this; }
    public PlayerBuilder defense(int d) { this.defense = d; return this; }
    public PlayerBuilder yearsInLeague(int y) { this.yearsInLeague = y; return this; }
    public PlayerBuilder gLeagueDaysRemaining(int d) { this.gLeagueDaysRemaining = d; return this; }

    public String playerId() { return playerId; }
    public String name() { return name; }
    public Position position() { return position; }
    public int age() { return age; }
    public int offense() { return offense; }
    public int defense() { return defense; }
    public int yearsInLeague() { return yearsInLeague; }
    public int gLeagueDaysRemaining() { return gLeagueDaysRemaining; }
}
