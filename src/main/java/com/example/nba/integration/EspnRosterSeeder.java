package com.example.nba.integration;

import com.example.nba.domain.Contract;
import com.example.nba.domain.ExperienceLevel;
import com.example.nba.domain.Money;
import com.example.nba.domain.Player;
import com.example.nba.domain.Position;
import com.example.nba.domain.Role;
import com.example.nba.domain.SalaryCap;
import com.example.nba.domain.SalaryStrategy;
import com.example.nba.domain.StandardSalaryStrategy;
import com.example.nba.domain.Team;
import com.example.nba.factory.PlayerBuilder;
import com.example.nba.factory.PlayerFactory;
import com.example.nba.service.TeamManagementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class EspnRosterSeeder {

    private static final String CACHE_KEY = "espn";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EspnClient api;

    // Teams endpoint (your link)
    private static final String TEAMS_URL =
            "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams";

    public EspnRosterSeeder(EspnClient api) {
        this.api = api;
    }

    public void seed(TeamManagementService service, Role actor) {
        RosterCache cache = new RosterCache(CACHE_KEY);

        // 1) Try cache first
        if (cache.exists()) {
            System.out.println("Seeder(ESPN): cache found -> " + cache.path());
            try {
                seedFromCache(cache.load(), service, actor);
                System.out.println("Seeder(ESPN): loaded from cache ✅");
                return;
            } catch (Exception e) {
                System.out.println("Seeder(ESPN): cache load failed; will try live ESPN. Reason: " + e.getMessage());
            }
        } else {
            System.out.println("Seeder(ESPN): no cache found, using live ESPN...");
        }

        // 2) Build cache root
        ObjectNode cacheRoot = MAPPER.createObjectNode();
        cacheRoot.put("source", "ESPN");
        ArrayNode teamsArr = MAPPER.createArrayNode();
        cacheRoot.set("teams", teamsArr);

        // 3) Fetch teams
        JsonNode teamsJson = api.get(TEAMS_URL);

        // ESPN commonly: /sports/0/leagues/0/teams
        JsonNode teams = teamsJson.at("/sports/0/leagues/0/teams");
        if (teams == null || !teams.isArray()) {
            // fallback if ESPN changes shape
            teams = teamsJson.at("/leagues/0/teams");
        }
        if (teams == null || !teams.isArray()) {
            throw new RuntimeException("Unexpected ESPN teams JSON shape");
        }

        PlayerFactory pf = new PlayerFactory();
        SalaryStrategy salary = new StandardSalaryStrategy();

        int idx = 0;
        int successRosters = 0;

        for (JsonNode wrapper : teams) {
            JsonNode team = wrapper.get("team");
            if (team == null || team.isNull()) continue;

            idx++;

            String abbr = text(team, "abbreviation", "UNK");
            String name = text(team, "displayName", text(team, "name", abbr));
            String teamId = text(team, "id", abbr);

            // Register team (always)
            service.registerTeam(actor, new Team(abbr, name, new SalaryCap(Money.of(140_000_000))));

            System.out.printf("Loading roster %2d/30: %s (%s)%n", idx, name, abbr);

            // ✅ FIX: use direct JSON roster endpoint (no link-following)
            String rosterUrl =
                    "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/" + teamId + "/roster";

            try {
                JsonNode rosterJson = api.get(rosterUrl);
                ArrayNode cachedPlayers = MAPPER.createArrayNode();

                int added = extractAndSeedRoster(rosterJson, service, actor, abbr, pf, salary, cachedPlayers);

                ObjectNode teamCache = MAPPER.createObjectNode();
                teamCache.put("teamId", teamId);
                teamCache.put("abbr", abbr);
                teamCache.put("name", name);
                teamCache.set("players", cachedPlayers);
                teamsArr.add(teamCache);

                successRosters++;
                System.out.println("  ✅ Added " + added + " players");
                sleep(120);

            } catch (Exception e) {
                System.out.println("  ⚠ Failed roster for " + abbr + ": " + rootCause(e));
                System.out.println("  -> continuing...");
            }
        }

        // 4) Save cache if we got at least some rosters
        if (successRosters > 0) {
            try {
                cache.save(cacheRoot);
                System.out.println("Seeder(ESPN): saved cache -> " + cache.path());
            } catch (Exception e) {
                System.out.println("Seeder(ESPN): cache save failed (non-fatal): " + e.getMessage());
            }
        } else {
            System.out.println("Seeder(ESPN): live fetch failed for all rosters.");
        }

        System.out.println("Seeder(ESPN): seed complete ✅ (" + successRosters + "/30 rosters)");
    }

    private int extractAndSeedRoster(JsonNode rosterJson,
                                     TeamManagementService service,
                                     Role actor,
                                     String teamAbbr,
                                     PlayerFactory pf,
                                     SalaryStrategy salary,
                                     ArrayNode cachedPlayers) {

        // ESPN roster endpoint commonly has "athletes"
        JsonNode athletes = rosterJson.get("athletes");
        if (athletes == null || athletes.isNull()) {
            // fallback patterns
            athletes = rosterJson.at("/team/athletes");
        }

        int added = 0;

        if (athletes != null && athletes.isArray()) {
            for (JsonNode a : athletes) {
                // Sometimes grouped: athletes: [{ position:..., items:[...] }]
                if (a.has("items") && a.get("items").isArray()) {
                    for (JsonNode item : a.get("items")) {
                        if (seedOnePlayer(service, actor, teamAbbr, item, pf, salary, cachedPlayers)) added++;
                    }
                } else {
                    if (seedOnePlayer(service, actor, teamAbbr, a, pf, salary, cachedPlayers)) added++;
                }
            }
        }

        return added;
    }

    private boolean seedOnePlayer(TeamManagementService service,
                                  Role actor,
                                  String teamAbbr,
                                  JsonNode node,
                                  PlayerFactory pf,
                                  SalaryStrategy salary,
                                  ArrayNode cachedPlayers) {

        JsonNode athlete = node.has("athlete") ? node.get("athlete") : node;
        if (athlete == null || athlete.isNull()) return false;

        String pid = text(athlete, "id", null);
        String name = text(athlete, "displayName", null);
        if (pid == null || name == null) return false;

        String posRaw = athlete.at("/position/abbreviation").asText("");
        Position pos = mapPosition(posRaw);

        int off = deriveRating(name, 70, 95);
        int def = deriveRating(name + "#D", 60, 90);

        PlayerBuilder b = new PlayerBuilder()
                .playerId(pid)
                .name(name)
                .position(pos)
                .age(26)
                .offense(off)
                .defense(def);

        Player p = pf.create(ExperienceLevel.VETERAN, b);

        Contract c = Contract.builder()
                .totalValue(Money.of(5_000_000))
                .years(1)
                .build();

        try {
            service.signPlayer(actor, teamAbbr, p, c, salary);
        } catch (Exception ignored) {
            // cap/validation
        }

        ObjectNode pj = MAPPER.createObjectNode();
        pj.put("playerId", pid);
        pj.put("name", name);
        pj.put("pos", pos.name());
        pj.put("off", off);
        pj.put("def", def);
        cachedPlayers.add(pj);

        return true;
    }

    private void seedFromCache(JsonNode root, TeamManagementService service, Role actor) {
        JsonNode teams = root.get("teams");
        if (teams == null || !teams.isArray()) throw new IllegalArgumentException("Bad cache format");

        for (JsonNode t : teams) {
            String abbr = t.get("abbr").asText();
            String name = t.get("name").asText();
            service.registerTeam(actor, new Team(abbr, name, new SalaryCap(Money.of(140_000_000))));
        }

        PlayerFactory pf = new PlayerFactory();
        SalaryStrategy salary = new StandardSalaryStrategy();

        for (JsonNode t : teams) {
            String abbr = t.get("abbr").asText();
            for (JsonNode p : t.get("players")) {
                String playerId = p.get("playerId").asText();
                String name = p.get("name").asText();
                Position pos = Position.valueOf(p.get("pos").asText());
                int off = p.get("off").asInt();
                int def = p.get("def").asInt();

                PlayerBuilder b = new PlayerBuilder()
                        .playerId(playerId)
                        .name(name)
                        .position(pos)
                        .age(26)
                        .offense(off)
                        .defense(def);

                Player player = pf.create(ExperienceLevel.VETERAN, b);

                Contract c = Contract.builder()
                        .totalValue(Money.of(5_000_000))
                        .years(1)
                        .build();

                try { service.signPlayer(actor, abbr, player, c, salary); }
                catch (Exception ignored) {}
            }
        }
    }

    private static String text(JsonNode node, String field, String def) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? def : v.asText(def);
    }

    private static Position mapPosition(String raw) {
        raw = (raw == null) ? "" : raw.toUpperCase();
        if (raw.equals("PG")) return Position.PG;
        if (raw.equals("SG")) return Position.SG;
        if (raw.equals("SF")) return Position.SF;
        if (raw.equals("PF")) return Position.PF;
        if (raw.equals("C"))  return Position.C;
        if (raw.equals("G"))  return Position.PG;
        if (raw.equals("F"))  return Position.SF;
        return Position.SF;
    }

    private static int deriveRating(String key, int min, int max) {
        int h = Math.abs(key.hashCode());
        return min + (h % (max - min + 1));
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private static String rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getClass().getSimpleName() + ": " + cur.getMessage();
    }
}
