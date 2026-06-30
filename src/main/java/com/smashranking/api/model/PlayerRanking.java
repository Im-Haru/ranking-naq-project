package com.smashranking.api.model;

public class PlayerRanking {

    private String playerName;
    private int totalPoints;
    private int tournamentsPlayed;
    private int bestPlacement;

    public PlayerRanking() {
    }

    public PlayerRanking(String playerName) {
        this.playerName = playerName;
        this.totalPoints = 0;
        this.tournamentsPlayed = 0;
        this.bestPlacement = Integer.MAX_VALUE;
    }

    public void addResult(int placement) {
        this.tournamentsPlayed++;
        this.totalPoints += pointsForPlacement(placement);
        if (placement < this.bestPlacement) {
            this.bestPlacement = placement;
        }
    }

    private int pointsForPlacement(int placement) {
        if (placement == 1) return 100;
        if (placement == 2) return 80;
        if (placement == 3) return 65;
        if (placement == 4) return 55;
        if (placement <= 6) return 40;
        if (placement <= 8) return 30;
        if (placement <= 12) return 20;
        if (placement <= 16) return 12;
        if (placement <= 24) return 6;
        if (placement <= 32) return 3;
        return 1;
    }

    // Getters / setters

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getTournamentsPlayed() {
        return tournamentsPlayed;
    }

    public void setTournamentsPlayed(int tournamentsPlayed) {
        this.tournamentsPlayed = tournamentsPlayed;
    }

    public int getBestPlacement() {
        return bestPlacement;
    }

    public void setBestPlacement(int bestPlacement) {
        this.bestPlacement = bestPlacement;
    }
}
