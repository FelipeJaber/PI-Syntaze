package com.instamvp.dto;

/** Critério de ordenação do leaderboard (GET /api/leaderboard?sort=...). */
public enum LeaderboardSort {
    /** Maior % de crescimento de seguidores no período (padrão). */
    GROWTH,
    /** Maior total de curtidas somadas nos posts do período. */
    LIKES,
    /** Maior taxa média de engajamento ((likes+comments)/followers) no período — melhor proxy de "retenção" que temos sem dados de churn de seguidores. */
    ENGAGEMENT,
    /** Maior número de posts publicados no período (cadência/atividade). */
    ACTIVITY;

    public static LeaderboardSort parse(String value) {
        if (value == null || value.isBlank()) return GROWTH;
        try {
            return LeaderboardSort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return GROWTH;
        }
    }
}
