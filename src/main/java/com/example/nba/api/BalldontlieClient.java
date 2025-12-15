package com.example.nba.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight client for the public balldontlie API. It intentionally avoids external
 * dependencies so it can run in constrained environments while still providing real
 * NBA data for teams and player rosters.
 */
public final class BalldontlieClient {
    private final HttpClient http;
    private final String baseUrl;

    public BalldontlieClient() {
        this("https://www.balldontlie.io/api/v1");
    }

    public BalldontlieClient(String baseUrl) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.baseUrl = Objects.requireNonNull(baseUrl);
    }

    public List<ApiTeam> fetchTeams() throws IOException, InterruptedException {
        String url = baseUrl + "/teams";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 300) {
            throw new IOException("Failed to fetch teams: HTTP " + resp.statusCode());
        }
        Object parsed = new SimpleJsonParser(resp.body()).parse();
        Map<?, ?> root = asMap(parsed, "teams root");
        List<?> data = asList(root.get("data"), "teams data");
        List<ApiTeam> teams = new ArrayList<>();
        for (Object o : data) {
            Map<?, ?> t = asMap(o, "team");
            int id = ((Number) t.getOrDefault("id", 0)).intValue();
            teams.add(new ApiTeam(
                    id,
                    stringOrEmpty(t.get("abbreviation")),
                    stringOrEmpty(t.get("city")),
                    stringOrEmpty(t.get("full_name")),
                    stringOrEmpty(t.get("conference")),
                    stringOrEmpty(t.get("division"))
            ));
        }
        return teams;
    }

    public List<ApiPlayer> fetchPlayersForTeam(int teamId) throws IOException, InterruptedException {
        List<ApiPlayer> players = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = baseUrl + "/players?per_page=100&page=" + page + "&team_ids[]=" + teamId;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                throw new IOException("Failed to fetch players for team=" + teamId + " HTTP " + resp.statusCode());
            }
            Object parsed = new SimpleJsonParser(resp.body()).parse();
            Map<?, ?> root = asMap(parsed, "players root");
            List<?> data = asList(root.get("data"), "players data");
            if (data.isEmpty()) break;
            for (Object o : data) {
                Map<?, ?> p = asMap(o, "player");
                Map<?, ?> team = asMap(p.get("team"), "player team");
                players.add(new ApiPlayer(
                        ((Number) p.getOrDefault("id", 0)).intValue(),
                        stringOrEmpty(p.get("first_name")),
                        stringOrEmpty(p.get("last_name")),
                        stringOrEmpty(p.get("position")),
                        ((Number) team.getOrDefault("id", 0)).intValue()
                ));
            }
            page++;
            Map<?, ?> meta = root.get("meta") instanceof Map<?, ?> m ? m : null;
            if (meta != null) {
                Number totalPages = meta.get("total_pages") instanceof Number n ? n : null;
                if (totalPages != null && page > totalPages.intValue()) break;
            }
        }
        return players;
    }

    private Map<?, ?> asMap(Object o, String where) {
        if (o instanceof Map<?, ?> m) return m;
        throw new IllegalArgumentException("Expected object for " + where);
    }

    private List<?> asList(Object o, String where) {
        if (o instanceof List<?> l) return l;
        throw new IllegalArgumentException("Expected array for " + where);
    }

    private String stringOrEmpty(Object o) {
        return o == null ? "" : o.toString();
    }

    public record ApiTeam(int id, String abbreviation, String city, String fullName, String conference, String division) {}
    public record ApiPlayer(int id, String firstName, String lastName, String position, int teamId) {}
}
