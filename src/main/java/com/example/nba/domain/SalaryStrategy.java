package com.example.nba.domain;

public interface SalaryStrategy {
    Money annualSalary(Player p, Contract c);
}
