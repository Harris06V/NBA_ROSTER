package com.example.nba;

import com.example.nba.audit.InMemoryAuditLogRepository;
import com.example.nba.domain.*;
import com.example.nba.factory.PlayerBuilder;
import com.example.nba.factory.PlayerFactory;
import com.example.nba.repo.InMemoryTeamRepository;
import com.example.nba.service.TeamManagementService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizationTest {

    @Test
    void onlyCoachCanSignPlayers_andAuditLogsRejectedAttempt() {
        var teams = new InMemoryTeamRepository();
        var audit = new InMemoryAuditLogRepository();
        var svc = new TeamManagementService(teams, audit);

        Role coach = new Coach("c1", "Coach");
        Role assistant = new AssistantCoach("a1", "Assistant");

        Team t = new Team("NYK", "Knicks", new SalaryCap(Money.of(140_000_000)));
        svc.registerTeam(coach, t);

        Player p = new PlayerFactory().create(ExperienceLevel.ROOKIE,
                new PlayerBuilder().playerId("p1").name("Rookie").position(Position.PG).age(19).offense(75).defense(70));

        assertThrows(SecurityException.class, () -> svc.signPlayer(assistant, "NYK", p,
                Contract.builder().totalValue(Money.of(5_000_000)).years(2).build(),
                new RookieScaleSalaryStrategy(Money.of(8_000_000))));

        assertTrue(audit.all().stream().anyMatch(e -> e.action().equals("SIGN_PLAYER_REJECTED")));
        assertTrue(audit.verifyIntegrity());
    }
}
