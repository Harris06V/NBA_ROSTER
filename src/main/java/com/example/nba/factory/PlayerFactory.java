package com.example.nba.factory;

import com.example.nba.domain.*;

import java.util.Objects;

/** Factory Method style creation that returns a Player polymorphically. */
public final class PlayerFactory {

    public Player create(ExperienceLevel type, PlayerBuilder b) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(b);

        return switch (type) {
            case ROOKIE -> new RookiePlayer(b.playerId(), b.name(), b.position(), b.age(), b.offense(), b.defense());
            case VETERAN -> new VeteranPlayer(b.playerId(), b.name(), b.position(), b.age(), b.offense(), b.defense(), b.yearsInLeague());
            case TWO_WAY -> new TwoWayPlayer(b.playerId(), b.name(), b.position(), b.age(), b.offense(), b.defense(), b.gLeagueDaysRemaining());
        };
    }
}
