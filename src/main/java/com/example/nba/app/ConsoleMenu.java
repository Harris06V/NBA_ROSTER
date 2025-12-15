package com.example.nba.app;

import java.util.List;
import java.util.Scanner;

import com.example.nba.analytics.LineupOptimizer;
import com.example.nba.audit.AuditEntry;
import com.example.nba.domain.Contract;
import com.example.nba.domain.ExperienceLevel;
import com.example.nba.domain.Money;
import com.example.nba.domain.Player;
import com.example.nba.domain.Position;
import com.example.nba.domain.Role;
import com.example.nba.domain.RookieScaleSalaryStrategy;
import com.example.nba.domain.SalaryStrategy;
import com.example.nba.domain.StandardSalaryStrategy;
import com.example.nba.domain.Team;
import com.example.nba.factory.PlayerBuilder;
import com.example.nba.factory.PlayerFactory;
import com.example.nba.service.TeamManagementService;

public final class ConsoleMenu {

    private final TeamManagementService service;
    private final Role actor;
    private final Scanner in = new Scanner(System.in);

    public ConsoleMenu(TeamManagementService service, Role actor) {
        this.service = service;
        this.actor = actor;
    }

    public void run() {
        while (true) {
            System.out.println("\n=== NBA Roster Management ===");
            System.out.println("1) Sign player");
            System.out.println("2) Waive player");
            System.out.println("3) Trade player");
            System.out.println("4) Print roster");
            System.out.println("5) Optimize lineup");
            System.out.println("6) Show audit log / verify chain");
            System.out.println("0) Exit");
            System.out.print("> ");

            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> signPlayer();
                    case "2" -> waivePlayer();
                    case "3" -> tradePlayer();
                    case "4" -> printRoster();
                    case "5" -> optimizeLineup();
                    case "6" -> showAudit();
                    case "0" -> { System.out.println("Bye."); return; }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    private void signPlayer() {
        String teamId = pickTeam("Sign to which team?");
        System.out.println("Type: 1=ROOKIE 2=VETERAN 3=TWO_WAY");
        System.out.print("> ");
        ExperienceLevel type = switch (in.nextLine().trim()) {
            case "1" -> ExperienceLevel.ROOKIE;
            case "2" -> ExperienceLevel.VETERAN;
            case "3" -> ExperienceLevel.TWO_WAY;
            default -> throw new IllegalArgumentException("Bad type");
        };

        System.out.print("playerId: ");
        String pid = in.nextLine().trim();

        System.out.print("name: ");
        String name = in.nextLine().trim();

        System.out.print("position (PG/SG/SF/PF/C): ");
        Position pos = Position.valueOf(in.nextLine().trim().toUpperCase());

        int age = readInt("age: ");
        int off = readInt("offense (0-99): ");
        int def = readInt("defense (0-99): ");

        PlayerBuilder b = new PlayerBuilder()
                .playerId(pid)
                .name(name)
                .position(pos)
                .age(age)
                .offense(off)
                .defense(def);

        if (type == ExperienceLevel.VETERAN) b.yearsInLeague(readInt("yearsInLeague: "));
        if (type == ExperienceLevel.TWO_WAY) b.gLeagueDaysRemaining(readInt("gLeagueDaysRemaining: "));

        double total = readDouble("contract total value: ");
        int years = readInt("contract years: ");

        Contract contract = Contract.builder()
                .totalValue(Money.of(total))
                .years(years)
                .build();

        SalaryStrategy strategy = (type == ExperienceLevel.ROOKIE)
                ? new RookieScaleSalaryStrategy(Money.of(8_000_000))
                : new StandardSalaryStrategy();

        Player p = new PlayerFactory().create(type, b);
        service.signPlayer(actor, teamId, p, contract, strategy);

        System.out.println("Signed " + p.name() + " to " + teamId);
    }

    private void waivePlayer() {
        String teamId = pickTeam("Waive from which team?");
        Team t = service.getTeam(teamId);

        for (Player p : t) System.out.println(" - " + p.playerId() + " : " + p);

        System.out.print("playerId to waive: ");
        String pid = in.nextLine().trim();

        service.waivePlayer(actor, teamId, pid);
        System.out.println("Waived " + pid);
    }

    private void tradePlayer() {
        String from = pickTeam("Trade FROM which team?");
        String to = pickTeam("Trade TO which team?");
        if (from.equalsIgnoreCase(to)) throw new IllegalArgumentException("Teams must differ");

        Team t = service.getTeam(from);
        for (Player p : t) System.out.println(" - " + p.playerId() + " : " + p);

        System.out.print("playerId to trade: ");
        String pid = in.nextLine().trim();

        service.trade(actor, from, to, pid);
        System.out.println("Traded " + pid + " from " + from + " to " + to);
    }

    private void printRoster() {
        String teamId = pickTeam("Print roster for which team?");
        Team t = service.getTeam(teamId);

        System.out.println(t);
        for (Player p : t) System.out.println(" - " + p.playerId() + " : " + p);
    }

    private void optimizeLineup() {
        String teamId = pickTeam("Optimize lineup for which team?");
        Team t = service.getTeam(teamId);

        var opt = new LineupOptimizer();
        var lineup = opt.bestStartingFive(t);

        System.out.println("Best starting 5 (score=" + lineup.score() + "):");
        lineup.starters().forEach(p -> System.out.println(" - " + p.position() + " " + p.name() + " (" + p.playerId() + ")"));
    }

    private void showAudit() {
        List<AuditEntry> entries = service.audit().all();
        System.out.println("Audit entries: " + entries.size());
        for (AuditEntry e : entries) {
            System.out.println(" - " + e.timestamp() + " " + e.actorRole() + ":" + e.actorId()
                    + " " + e.action() + " hash=" + e.hash().substring(0, 12) + "...");
        }
        System.out.println("Audit chain OK? " + service.audit().verifyIntegrity());
    }

    private String pickTeam(String prompt) {
        System.out.println(prompt);
        for (Team t : service.listTeams()) {
            System.out.println(" - " + t.teamId() + " : " + t.name());
        }
        System.out.print("> ");
        return in.nextLine().trim().toUpperCase();
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        return Integer.parseInt(in.nextLine().trim());
    }

    private double readDouble(String prompt) {
        System.out.print(prompt);
        return Double.parseDouble(in.nextLine().trim());
    }
}
