package com.example.nba.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class RosterCache {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Path cacheFile;

    /**
     * cache/rosters_<key>.json
     * example: cache/rosters_espn.json
     */
    public RosterCache(String key) {
        Path dir = Paths.get("cache");
        this.cacheFile = dir.resolve("rosters_" + key + ".json");
    }

    public boolean exists() {
        return Files.exists(cacheFile);
    }

    public JsonNode load() {
        try {
            return MAPPER.readTree(Files.readString(cacheFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load cache: " + cacheFile, e);
        }
    }

    public void save(JsonNode root) {
        try {
            Files.createDirectories(cacheFile.getParent());
            Files.writeString(
                    cacheFile,
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root)
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to save cache: " + cacheFile, e);
        }
    }

    public Path path() {
        return cacheFile;
    }
}
