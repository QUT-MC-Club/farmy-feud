package xyz.nucleoid.farmyfeud.game;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.farmyfeud.game.active.FfActive;
import xyz.nucleoid.farmyfeud.game.map.FfMap;
import xyz.nucleoid.farmyfeud.game.map.FfMapBuilder;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;

public final class FfWaiting {
    private final ServerWorld world;
    private final GameSpace gameSpace;
    private final FfMap map;
    private final FfConfig config;

    private final FfSpawnLogic spawnLogic;

    private FfWaiting(GameSpace gameSpace, FfMap map, FfConfig config) {
        this.world = gameSpace.getWorld();
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;

        this.spawnLogic = new FfSpawnLogic(this.world, map);
    }

    public static GameOpenProcedure open(GameOpenContext<FfConfig> context) {
        FfConfig config = context.getConfig();

        FfMap map = new FfMapBuilder(config).create();

        BubbleWorldConfig worldConfig = new BubbleWorldConfig()
                .setGenerator(map.createGenerator(context.getServer()))
                .setDefaultGameMode(GameMode.SPECTATOR);

        return context.createOpenProcedure(worldConfig, game -> {
            FfWaiting waiting = new FfWaiting(game.getSpace(), map, config);

            GameWaitingLobby.applyTo(game, config.players);

            game.on(RequestStartListener.EVENT, waiting::requestStart);

            game.on(PlayerAddListener.EVENT, waiting::addPlayer);
            game.on(PlayerDeathListener.EVENT, waiting::onPlayerDeath);
        });
    }

    private StartResult requestStart() {
        FfActive.open(this.gameSpace, this.map, this.config);
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
