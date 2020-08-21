package xyz.nucleoid.farmyfeud.game;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.farmyfeud.game.active.FfActive;
import xyz.nucleoid.farmyfeud.game.map.FfMap;
import xyz.nucleoid.farmyfeud.game.map.FfMapBuilder;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.world.bubble.BubbleWorldConfig;

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

    public static CompletableFuture<GameWorld> open(GameOpenContext<FfConfig> context) {
        FfConfig config = context.getConfig();

        return new FfMapBuilder(config).create().thenCompose(map -> {
            BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                    .setGenerator(map.createGenerator(context.getServer()))
                    .setDefaultGameMode(GameMode.SPECTATOR);

            return context.openWorld(worldConfig).thenApply(gameWorld -> {
                FfWaiting waiting = new FfWaiting(gameWorld, map, config);

                return GameWaitingLobby.open(gameWorld, config.players, game -> {
                    game.on(RequestStartListener.EVENT, waiting::requestStart);

                    game.on(PlayerAddListener.EVENT, waiting::addPlayer);
                    game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
                });
            });
        });
    }

    private StartResult requestStart() {
        FfActive.open(this.gameWorld, this.map, this.config);
        return StartResult.OK;
    }

    private void addPlayer(ServerPlayerEntity player) {
        this.spawnPlayer(player);
    }

    private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
        this.spawnPlayer(player);
        return ActionResult.FAIL;
    }

    private void spawnPlayer(ServerPlayerEntity player) {
        this.spawnLogic.spawnPlayerAtCenter(player);
        this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);
    }
}
