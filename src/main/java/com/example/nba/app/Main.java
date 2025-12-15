package com.example.nba.app;

import com.example.nba.audit.InMemoryAuditLogRepository;
import com.example.nba.domain.Coach;
import com.example.nba.domain.Role;
import com.example.nba.integration.EspnClient;
import com.example.nba.integration.EspnRosterSeeder;
import com.example.nba.repo.InMemoryTeamRepository;
import com.example.nba.service.TeamManagementService;

public final class Main {
    public static void main(String[] args) {
        var teamsRepo = new InMemoryTeamRepository();
        var auditRepo = new InMemoryAuditLogRepository();
        var service = new TeamManagementService(teamsRepo, auditRepo);

        Role coach = new Coach("u1", "Coach Carter");

        var api = new EspnClient();
        new EspnRosterSeeder(api).seed(service, coach);

        new ConsoleMenu(service, coach).run();
    }
}
