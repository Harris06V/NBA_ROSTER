package com.example.nba.analytics;

import com.example.nba.domain.*;

/**
 * Visitor that produces a "market value score" for different player types.
 * Used by the lineup optimizer and for reporting.
 */
public final class PlayerValueVisitor implements PlayerVisitor<Integer> {

    @Override
    public Integer visitRookie(RookiePlayer p) {
        // upside bonus
        return (int) Math.round(p.effectiveRating() + 5);
    }

    @Override
    public Integer visitVeteran(VeteranPlayer p) {
        // experience bonus but slight age/risk penalty if older
        int agePenalty = Math.max(0, p.age() - 32);
        return (int) Math.round(p.effectiveRating() + 3 - agePenalty * 0.5);
    }

    @Override
    public Integer visitTwoWay(TwoWayPlayer p) {
        // limited availability penalty
        int availabilityPenalty = p.gLeagueDaysRemaining() > 0 ? 2 : 0;
        return (int) Math.round(p.effectiveRating() - availabilityPenalty);
    }
}
