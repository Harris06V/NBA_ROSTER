package com.example.nba.integration;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Fetches per-player ESPN stats and converts them into "offense" and "defense" ratings.
 *
 * Uses endpoint documented in community lists:
 *   https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/athletes/{ATHLETE_ID}/stats?region=us&lang=en&contentorigin=espn
 *
 * If ESPN blocks/returns HTML/fields missing => returns failure with reason; caller can fallback.
 */
public final class StatBasedRatingModel {

    public record Rating(int offense, int defense) {}

    public record Result(boolean ok, Rating rating, String reason) {
        public static Result ok(Rating r) { return new Result(true, r, null); }
        public static Result fail(String reason) { return new Result(false, null, reason); }
    }

    private final EspnClient api;

    public StatBasedRatingModel(EspnClient api) {
        this.api = api;
    }

    public Result rate(String athleteId) {
        String url =
                "https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/athletes/"
                        + athleteId
                        + "/stats?region=us&lang=en&contentorigin=espn";

        try {
            JsonNode root = api.get(url);

            // Parse the new JSON structure with categories
            Double ppg = null, apg = null, rpg = null, spg = null, bpg = null, topg = null;

            JsonNode categories = root.get("categories");
            if (categories != null && categories.isArray()) {
                for (JsonNode category : categories) {
                    String name = category.get("name").asText();
                    if ("averages".equals(name)) {
                        JsonNode statistics = category.get("statistics");
                        if (statistics != null && statistics.isArray()) {
                            // Find the most recent season (2025-26, year 2026)
                            JsonNode latestStats = null;
                            int latestYear = 0;
                            for (JsonNode stat : statistics) {
                                int year = stat.at("/season/year").asInt(0);
                                if (year > latestYear) {
                                    latestYear = year;
                                    latestStats = stat;
                                }
                            }
                            if (latestStats != null) {
                                JsonNode statsArray = latestStats.get("stats");
                                if (statsArray != null && statsArray.isArray() && statsArray.size() >= 18) {
                                    // Labels: GP=0, GS=1, MIN=2, FG=3, FG%=4, 3PT=5, 3P%=6, FT=7, FT%=8, OR=9, DR=10, REB=11, AST=12, BLK=13, STL=14, PF=15, TO=16, PTS=17
                                    ppg = parseDoubleSafe(statsArray.get(17).asText());
                                    apg = parseDoubleSafe(statsArray.get(12).asText());
                                    rpg = parseDoubleSafe(statsArray.get(11).asText());
                                    spg = parseDoubleSafe(statsArray.get(14).asText());
                                    bpg = parseDoubleSafe(statsArray.get(13).asText());
                                    topg = parseDoubleSafe(statsArray.get(16).asText());
                                }
                            }
                        }
                        break;
                    }
                }
            }

            // If we can't get ANY meaningful stats, treat as fail (caller will fallback).
            if (ppg == null && apg == null && rpg == null && spg == null && bpg == null) {
                return Result.fail("no recognizable per-game stats in JSON");
            }

            // Simple but reasonable mapping:
            // offense: points + assists - turnovers
            // defense: rebounds + steals + blocks
            double oScore = 0.0;
            if (ppg != null) oScore += ppg * 2.2;
            if (apg != null) oScore += apg * 2.0;
            if (topg != null) oScore -= topg * 1.2;

            double dScore = 0.0;
            if (rpg != null) dScore += rpg * 1.4;
            if (spg != null) dScore += spg * 6.0;
            if (bpg != null) dScore += bpg * 5.0;

            int offense = clampToRating(oScore, 50, 99);
            int defense = clampToRating(dScore, 50, 99);

            return Result.ok(new Rating(offense, defense));
        } catch (Exception e) {
            // Important: surface WHY it failed (timeout/HTML/blocked/parse/etc.)
            return Result.fail(rootCause(e));
        }
    }

    /**
     * Searches ESPN JSON for a stat value by matching a key OR matching label/name fields in arrays.
     * Returns null if not found.
     */
    private static Double findStatValue(JsonNode root, String... keysOrLabels) {
        if (root == null || root.isNull()) return null;

        // 1) Direct key search by common JSON field names (rare but cheap).
        for (String k : keysOrLabels) {
            JsonNode direct = root.findValue(k);
            if (direct != null && direct.isNumber()) return direct.asDouble();
            if (direct != null && direct.isTextual()) {
                Double d = parseDoubleSafe(direct.asText());
                if (d != null) return d;
            }
        }

        // 2) Scan any arrays named "stats" / "categories" / "splits" / etc.
        // ESPN commonly stores stats as objects that include label/name and a "value"/"displayValue".
        for (String label : keysOrLabels) {
            Double d = scanForLabeledValue(root, label);
            if (d != null) return d;
        }

        return null;
    }

    private static Double scanForLabeledValue(JsonNode node, String label) {
        if (node == null || node.isNull()) return null;

        if (node.isObject()) {
            // If object looks like: { "name": "...", "value": 12.3 }
            String name = textAny(node, "name", "displayName", "shortDisplayName", "abbreviation", "label", "statName");
            if (name != null && equalsLoose(name, label)) {
                Double v = readValueAny(node, "value", "displayValue", "statValue");
                if (v != null) return v;
            }

            // recurse through all fields
            var it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                Double d = scanForLabeledValue(e.getValue(), label);
                if (d != null) return d;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                Double d = scanForLabeledValue(child, label);
                if (d != null) return d;
            }
        }

        return null;
    }

    private static Double readValueAny(JsonNode obj, String... fields) {
        for (String f : fields) {
            JsonNode v = obj.get(f);
            if (v == null || v.isNull()) continue;
            if (v.isNumber()) return v.asDouble();
            if (v.isTextual()) {
                Double d = parseDoubleSafe(v.asText());
                if (d != null) return d;
            }
        }
        return null;
    }

    private static String textAny(JsonNode obj, String... fields) {
        for (String f : fields) {
            JsonNode v = obj.get(f);
            if (v != null && !v.isNull() && v.isTextual()) return v.asText();
        }
        return null;
    }

    private static boolean equalsLoose(String a, String b) {
        if (a == null || b == null) return false;
        String x = a.trim().toLowerCase();
        String y = b.trim().toLowerCase();
        // loose match: exact OR contains
        return x.equals(y) || x.contains(y) || y.contains(x);
    }

    private static Double parseDoubleSafe(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        // remove percent sign etc.
        t = t.replace("%", "");
        try { return Double.parseDouble(t); }
        catch (Exception ignored) { return null; }
    }

    private static int clampToRating(double score, int minRating, int maxRating) {
        // Map an unbounded score into a rating-ish scale.
        // These constants just keep things in a sensible NBA-like range.
        double scaled = 50.0 + score; // base 50 + contribution
        int r = (int) Math.round(scaled);
        if (r < minRating) return minRating;
        if (r > maxRating) return maxRating;
        return r;
    }

    private static String rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        String msg = cur.getMessage();
        return cur.getClass().getSimpleName() + (msg == null ? "" : (": " + msg));
    }
}
