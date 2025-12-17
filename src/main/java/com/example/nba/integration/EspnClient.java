package com.example.nba.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class EspnClient {
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public EspnClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public JsonNode get(String url) {
        int maxAttempts = 4;
        long backoffMs = 400;
        RuntimeException last = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .header("Accept", "application/json,text/plain,*/*")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        // These two help with ESPN sometimes returning HTML/blocked responses:
                        .header("Referer", "https://www.espn.com/")
                        .header("Origin", "https://www.espn.com")
                        .GET()
                        .build();

                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                String body = res.body();

                if (res.statusCode() != 200) {
                    last = new RuntimeException("ESPN status=" + res.statusCode() + " url=" + url + " body=" + abbreviate(body));
                } else if (looksLikeHtml(body)) {
                    last = new RuntimeException("ESPN returned HTML instead of JSON (blocked/redirected). url=" + url);
                } else {
                    return mapper.readTree(body);
                }

            } catch (Exception e) {
                last = new RuntimeException("ESPN call failed (attempt " + attempt + ") url=" + url, e);
            }

            sleep(backoffMs);
            backoffMs = Math.min(backoffMs * 2, 3000);
        }

        throw (last != null) ? last : new RuntimeException("ESPN call failed");
    }

    private static boolean looksLikeHtml(String body) {
        if (body == null) return false;
        String s = body.stripLeading();
        return s.startsWith("<!DOCTYPE html") || s.startsWith("<html") || s.startsWith("<");
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return (s.length() <= 180) ? s : s.substring(0, 180) + "...";
    }
}
