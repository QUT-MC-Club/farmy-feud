package net.gegy1000.farmyfeud.game.active;

import net.gegy1000.plasmid.game.player.GameTeam;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class FfTeamState {
    public final GameTeam team;
    private final Set<UUID> participants = new HashSet<>();

    private int capturedSheep;

    public FfTeamState(GameTeam team) {
        this.team = team;
    }

    public void addParticipant(UUID id) {
        this.participants.add(id);
    }

    public int getParticipantCount() {
        return this.participants.size();
    }

    public Stream<UUID> participants() {
        return this.participants.stream();
    }

    public void incrementCapturedSheep() {
        this.capturedSheep++;
    }

    public void decrementCapturedSheep() {
        this.capturedSheep--;
    }

    public int getCapturedSheep() {
        return this.capturedSheep;
    }
}
