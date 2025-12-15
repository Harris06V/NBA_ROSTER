package com.example.nba.repo;

import com.example.nba.domain.Team;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface TeamRepository {
    void save(Team team);
    Optional<Team> findById(String teamId);
    List<Team> findAll();
    List<Team> search(Predicate<Team> predicate);
}
