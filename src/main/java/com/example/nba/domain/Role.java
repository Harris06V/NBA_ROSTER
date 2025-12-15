package com.example.nba.domain;

/**
 * Sealed role model: only Coach and AssistantCoach are permitted roles.
 */
public sealed interface Role permits Coach, AssistantCoach {
    String id();
    String displayName();
}
