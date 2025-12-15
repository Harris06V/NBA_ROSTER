package com.example.nba.app;

import com.example.nba.api.BalldontlieClient;
import com.example.nba.audit.InMemoryAuditLogRepository;
import com.example.nba.domain.Coach;
import com.example.nba.domain.Money;
import com.example.nba.domain.Role;
import com.example.nba.domain.SalaryCap;
import com.example.nba.domain.Team;
import com.example.nba.repo.InMemoryTeamRepository;
import com.example.nba.service.RosterImportService;
import com.example.nba.service.TeamManagementService;

public final class Main {
    public static void main(String[] args) {
        var teamsRepo = new InMemoryTeamRepository();
        var auditRepo = new InMemoryAuditLogRepository();
        var service = new TeamManagementService(teamsRepo, auditRepo);

        Role coach = new Coach("u1", "Coach Carter");

        try {
            new RosterImportService(teamsRepo, auditRepo, new BalldontlieClient()).importLeague(coach);
            System.out.println("Loaded real NBA teams and rosters from balldontlie.io");
        } catch (Exception e) {
            System.err.println("Failed to import from API, falling back to sample teams: " + e.getMessage());
            service.registerTeam(coach, new Team("LAL", "Lakers", new SalaryCap(Money.of(140_000_000))));
            service.registerTeam(coach, new Team("GSW", "Warriors", new SalaryCap(Money.of(140_000_000))));
            service.registerTeam(coach, new Team("BOS", "Celtics", new SalaryCap(Money.of(140_000_000))));
        }

        // Start interactive menu
        new ConsoleMenu(service, coach).run();
    }
}
