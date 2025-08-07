package xyz.nucleoid.farmyfeud.game.active;

import xyz.nucleoid.farmyfeud.entity.Carriable;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public final class FfTeamState {
    public final GameTeamKey team;
    private final Set<UUID> participants = new HashSet<>();

    private int capturedSheep;

    public FfTeamState(GameTeamKey team) {
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

    public void incrementCapturedSheep(Carriable sheep) {
        this.capturedSheep += sheep.getCarryWeight();
    }

    public void decrementCapturedSheep(Carriable sheep) {
        this.capturedSheep -= sheep.getCarryWeight();
    }

    public int getCapturedSheep() {
        return this.capturedSheep;
    }
}
