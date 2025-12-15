package com.example.nba.service;

import java.util.List;
import java.util.Objects;

import com.example.nba.audit.AuditEntry;
import com.example.nba.audit.AuditLogRepository;
import com.example.nba.domain.Coach;
import com.example.nba.domain.Contract;
import com.example.nba.domain.Money;
import com.example.nba.domain.Player;
import com.example.nba.domain.Role;
import com.example.nba.domain.SalaryStrategy;
import com.example.nba.domain.Team;
import com.example.nba.repo.TeamRepository;

public final class TeamManagementService {
    private final TeamRepository teams;
    private final AuditLogRepository audit;

    public TeamManagementService(TeamRepository teams, AuditLogRepository audit) {
        this.teams = Objects.requireNonNull(teams);
        this.audit = Objects.requireNonNull(audit);
    }

    // For CLI
    public List<Team> listTeams() { return teams.findAll(); }
    public Team getTeam(String teamId) { return teams.findById(teamId).orElseThrow(); }
    public AuditLogRepository audit() { return audit; }

    public void registerTeam(Role actor, Team team) {
        String before = "NONE";
        teams.save(team);
        String after = team.toString();
        audit.append(new AuditEntry(actor, "REGISTER_TEAM", before, after, audit.tailHash()));
    }

    public void signPlayer(Role actor, String teamId, Player player, Contract contract, SalaryStrategy strategy) {
        Team team = teams.findById(teamId).orElseThrow();
        String before = team.toString();

        if (!(actor instanceof Coach)) {
            audit.append(new AuditEntry(actor, "SIGN_PLAYER_REJECTED", before, before, audit.tailHash()));
            throw new SecurityException("Only Coach may sign players");
        }

        Money annual = Objects.requireNonNull(strategy).annualSalary(player, contract);
        team.salaryCap().commit(annual);
        team.addPlayer(player, annual);

        String after = team.toString();
        audit.append(new AuditEntry(actor, "SIGN_PLAYER", before, after, audit.tailHash()));
        teams.save(team);
    }

    public void waivePlayer(Role actor, String teamId, String playerId) {
        Team team = teams.findById(teamId).orElseThrow();
        String before = team.toString();

        if (!(actor instanceof Coach)) {
            audit.append(new AuditEntry(actor, "WAIVE_PLAYER_REJECTED", before, before, audit.tailHash()));
            throw new SecurityException("Only Coach may waive players");
        }

        Player p = team.findPlayerById(playerId).orElse(null);
        if (p == null) {
            audit.append(new AuditEntry(actor, "WAIVE_PLAYER_NOT_FOUND", before, before, audit.tailHash()));
            throw new IllegalArgumentException("Player not on roster: " + playerId);
        }

        Money annual = team.annualSalaryFor(playerId);

        team.removePlayer(p);
        team.salaryCap().uncommit(annual);

        String after = team.toString();
        audit.append(new AuditEntry(actor, "WAIVE_PLAYER", before, after, audit.tailHash()));
        teams.save(team);
    }

    public void trade(Role actor, String fromTeamId, String toTeamId, String playerId) {
        Team from = teams.findById(fromTeamId).orElseThrow();
        Team to = teams.findById(toTeamId).orElseThrow();

        String beforeFrom = from.toString();
        String beforeTo = to.toString();

        if (!(actor instanceof Coach)) {
            audit.append(new AuditEntry(actor, "TRADE_REJECTED",
                    beforeFrom + " | " + beforeTo,
                    beforeFrom + " | " + beforeTo,
                    audit.tailHash()));
            throw new SecurityException("Only Coach may execute trades");
        }

        Player p = from.findPlayerById(playerId).orElse(null);
        if (p == null) throw new IllegalArgumentException("Player not on from-team: " + playerId);

        Money annual = from.annualSalaryFor(playerId);

        from.removePlayer(p);
        from.salaryCap().uncommit(annual);

        to.salaryCap().commit(annual);
        to.addPlayer(p, annual);

        String afterFrom = from.toString();
        String afterTo = to.toString();

        audit.append(new AuditEntry(actor, "TRADE_PLAYER",
                beforeFrom + " | " + beforeTo,
                afterFrom + " | " + afterTo,
                audit.tailHash()));

        teams.save(from);
        teams.save(to);
    }
}
