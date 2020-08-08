package net.gegy1000.farmyfeud.game.active;

import net.gegy1000.farmyfeud.entity.FarmSheepEntity;
import net.gegy1000.plasmid.game.player.GameTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import javax.annotation.Nullable;
import java.util.UUID;

public final class FfParticipant {
    private final ServerWorld world;
    public final UUID playerId;
    public final GameTeam team;

    public final EntityCarryStack<FarmSheepEntity> carryStack = new EntityCarryStack<>(3);

    private long respawnTime = -1;

    FfParticipant(ServerPlayerEntity player, GameTeam team) {
        this.world = player.getServerWorld();
        this.playerId = player.getUuid();
        this.team = team;
    }

    void startRespawn(long time) {
        this.respawnTime = time + FfActive.RESPAWN_TICKS;
    }

    boolean tryRespawn(long time) {
        if (this.respawnTime != -1 && time >= this.respawnTime) {
            this.respawnTime = -1;
            return true;
        }
        return false;
    }

    @Nullable
    public ServerPlayerEntity player() {
        return this.world.getServer().getPlayerManager().getPlayer(this.playerId);
    }
}
