package com.example.nba;

import com.example.nba.analytics.LineupOptimizer;
import com.example.nba.audit.InMemoryAuditLogRepository;
import com.example.nba.domain.*;
import com.example.nba.factory.PlayerBuilder;
import com.example.nba.factory.PlayerFactory;
import com.example.nba.repo.InMemoryTeamRepository;
import com.example.nba.service.TeamManagementService;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class LineupOptimizerTest {

    @Test
    void bestStartingFiveUsesAllPositions() {
        var teams = new InMemoryTeamRepository();
        var audit = new InMemoryAuditLogRepository();
        var svc = new TeamManagementService(teams, audit);
        Role coach = new Coach("c1", "Coach");

        Team t = new Team("BOS", "Celtics", new SalaryCap(Money.of(140_000_000)));
        svc.registerTeam(coach, t);

        PlayerFactory pf = new PlayerFactory();
        // Add at least 1 per position (and some extras)
        add(svc, coach, t.teamId(), pf.create(ExperienceLevel.VETERAN, new PlayerBuilder().playerId("pg1").name("PG1").position(Position.PG).age(28).offense(85).defense(80).yearsInLeague(6)), 10_000_000);
        add(svc, coach, t.teamId(), pf.create(ExperienceLevel.VETERAN, new PlayerBuilder().playerId("sg1").name("SG1").position(Position.SG).age(27).offense(86).defense(78).yearsInLeague(5)), 10_000_000);
        add(svc, coach, t.teamId(), pf.create(ExperienceLevel.ROOKIE, new PlayerBuilder().playerId("sf1").name("SF1").position(Position.SF).age(20).offense(84).defense(79)), 6_000_000);
        add(svc, coach, t.teamId(), pf.create(ExperienceLevel.VETERAN, new PlayerBuilder().playerId("pf1").name("PF1").position(Position.PF).age(30).offense(81).defense(84).yearsInLeague(8)), 9_000_000);
        add(svc, coach, t.teamId(), pf.create(ExperienceLevel.TWO_WAY, new PlayerBuilder().playerId("c1").name("C1").position(Position.C).age(22).offense(77).defense(88).gLeagueDaysRemaining(30)), 1_000_000);

        // extras
        add(svc, coach, t.teamId(), pf.create(ExperienceLevel.VETERAN, new PlayerBuilder().playerId("pg2").name("PG2").position(Position.PG).age(26).offense(70).defense(72).yearsInLeague(4)), 5_000_000);
        add(svc, coach, t.teamId(), pf.create(ExperienceLevel.VETERAN, new PlayerBuilder().playerId("sg2").name("SG2").position(Position.SG).age(29).offense(75).defense(75).yearsInLeague(7)), 4_000_000);

        var opt = new LineupOptimizer();
        var lineup = opt.bestStartingFive(t);

        assertEquals(5, lineup.starters().size());
        var positions = EnumSet.noneOf(Position.class);
        lineup.starters().forEach(p -> positions.add(p.position()));
        assertEquals(EnumSet.allOf(Position.class), positions);
        assertTrue(lineup.score() > 0);
    }

    private void add(TeamManagementService svc, Role coach, String teamId, Player p, double annual) {
        svc.signPlayer(coach, teamId, p,
                Contract.builder().totalValue(Money.of(annual)).years(1).build(),
                new StandardSalaryStrategy());
    }
}
