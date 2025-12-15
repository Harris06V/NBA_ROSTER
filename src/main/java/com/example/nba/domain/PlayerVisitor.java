package com.example.nba.domain;

public interface PlayerVisitor<R> {
    R visitRookie(RookiePlayer p);
    R visitVeteran(VeteranPlayer p);
    R visitTwoWay(TwoWayPlayer p);
}
