package net.gegy1000.farmyfeud.game;

import net.gegy1000.farmyfeud.game.active.FfActive;
import net.gegy1000.farmyfeud.game.map.FfMap;
import net.gegy1000.farmyfeud.game.map.FfMapBuilder;
import net.gegy1000.plasmid.game.GameWorld;
import net.gegy1000.plasmid.game.GameWorldState;
import net.gegy1000.plasmid.game.StartResult;
import net.gegy1000.plasmid.game.event.OfferPlayerListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.event.RequestStartListener;
import net.gegy1000.plasmid.game.player.JoinResult;
import net.gegy1000.plasmid.game.rule.GameRule;
import net.gegy1000.plasmid.game.rule.RuleResult;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;

import java.util.concurrent.CompletableFuture;

public final class FfWaiting {
    private final ServerWorld world;
    private final GameWorld gameWorld;
    private final FfMap map;
    private final FfConfig config;

    private final FfSpawnLogic spawnLogic;

    private FfWaiting(GameWorld gameWorld, FfMap map, FfConfig config) {
        this.world = gameWorld.getWorld();
        this.gameWorld = gameWorld;
        this.map = map;
        this.config = config;

        this.spawnLogic = new FfSpawnLogic(this.world, map);
    }

    public static CompletableFuture<Void> open(GameWorldState worldState, FfConfig config) {
        return new FfMapBuilder(config).create().thenAccept(map -> {
            GameWorld gameWorld = worldState.openWorld(map.createGenerator());

            FfWaiting waiting = new FfWaiting(gameWorld, map, config);

            gameWorld.newGame(game -> {
                game.setRule(GameRule.ALLOW_CRAFTING, RuleResult.DENY);
                game.setRule(GameRule.ALLOW_PORTALS, RuleResult.DENY);
                game.setRule(GameRule.ALLOW_PVP, RuleResult.DENY);
                game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
                game.setRule(GameRule.ENABLE_HUNGER, RuleResult.DENY);

                game.on(RequestStartListener.EVENT, waiting::requestStart);
                game.on(OfferPlayerListener.EVENT, waiting::offerPlayer);

                game.on(PlayerAddListener.EVENT, waiting::addPlayer);
                game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
            });
        });
    }

    private JoinResult offerPlayer(ServerPlayerEntity player) {
        if (this.gameWorld.getPlayerCount() >= this.config.players.getMaxPlayers()) {
            return JoinResult.gameFull();
        }

        return JoinResult.ok();
    }

    private StartResult requestStart() {
        if (this.gameWorld.getPlayerCount() < this.config.players.getMinPlayers()) {
            return StartResult.notEnoughPlayers();
        }

        FfActive.open(this.gameWorld, this.map, this.config);
        return StartResult.ok();
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private boolean onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.spawnPlayer(player);
        return true;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
        this.spawnLogic.spawnPlayerAtCenter(player);
    }
}
