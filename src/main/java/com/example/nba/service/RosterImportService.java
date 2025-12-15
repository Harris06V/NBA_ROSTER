package com.example.nba.service;

import com.example.nba.api.BalldontlieClient;
import com.example.nba.api.BalldontlieClient.ApiPlayer;
import com.example.nba.api.BalldontlieClient.ApiTeam;
import com.example.nba.audit.AuditEntry;
import com.example.nba.audit.AuditLogRepository;
import com.example.nba.domain.*;
import com.example.nba.repo.TeamRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Imports the full NBA league from the public balldontlie API and wires the data into the
 * existing domain model. Real player and team data are used, while salaries/ratings are
 * synthesized deterministically to keep the demo self contained.
 */
public final class RosterImportService {
    private final TeamRepository teams;
    private final AuditLogRepository audit;
    private final BalldontlieClient client;

    public RosterImportService(TeamRepository teams, AuditLogRepository audit, BalldontlieClient client) {
        this.teams = Objects.requireNonNull(teams);
        this.audit = Objects.requireNonNull(audit);
        this.client = Objects.requireNonNull(client);
    }

    /**
     * Pulls all teams and rosters from the API and registers them in the repository. Existing
     * teams will be overwritten in-place.
     */
    public void importLeague(Role actor) throws IOException, InterruptedException {
        List<ApiTeam> apiTeams = client.fetchTeams();
        for (ApiTeam t : apiTeams) {
            Team domainTeam = new Team(t.abbreviation(), t.fullName(), new SalaryCap(Money.of(155_000_000)));
            teams.save(domainTeam);
            audit.append(new AuditEntry(actor, "REGISTER_TEAM", "NONE", domainTeam.toString(), audit.tailHash()));
        }

        for (ApiTeam t : apiTeams) {
            List<ApiPlayer> players = client.fetchPlayersForTeam(t.id());
            for (ApiPlayer p : players) {
                Team team = teams.findById(t.abbreviation()).orElseThrow();
                if (team.rosterSize() >= 20) break;
                Player player = toPlayer(p);
                Contract contract = Contract.builder()
                        .totalValue(Money.of(determineSalary(player)))
                        .years(2)
                        .startDate(LocalDate.now())
                        .build();
                Money annual = new StandardSalaryStrategy().annualSalary(player, contract);
                if (!team.salaryCap().remaining().gte(annual)) continue; // skip if cap exceeded
                String before = team.toString();
                team.salaryCap().commit(annual);
                team.addPlayer(player, annual);
                String after = team.toString();
                audit.append(new AuditEntry(actor, "IMPORT_PLAYER", before, after, audit.tailHash()));
                teams.save(team);
            }
        }
    }

    private double determineSalary(Player p) {
        int base = p.overallRating();
        return Math.max(1_200_000, base * 350_000); // simple heuristic
    }

    private Player toPlayer(ApiPlayer api) {
        String id = "api-" + api.id();
        String name = (api.firstName() + " " + api.lastName()).trim();
        Position pos = switch (api.position()) {
            case "G" -> Position.PG;
            case "G-F", "F-G" -> Position.SG;
            case "F" -> Position.SF;
            case "F-C" -> Position.PF;
            case "C" -> Position.C;
            default -> Position.SG;
        };
        int offense = 60 + Math.abs(name.hashCode()) % 40;
        int defense = 55 + Math.abs((name + "d").hashCode()) % 40;
        int age = 20 + Math.abs(api.id()) % 15;
        int years = Math.max(1, (age - 18));
        return new VeteranPlayer(id, name, pos, age, offense, defense, years);
    }
}
