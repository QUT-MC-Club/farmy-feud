package net.gegy1000.farmyfeud;

import net.fabricmc.api.ModInitializer;
import net.gegy1000.farmyfeud.game.FfConfig;
import net.gegy1000.farmyfeud.game.FfWaiting;
import net.gegy1000.plasmid.game.GameType;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FarmyFeud implements ModInitializer {
    public static final String ID = "farmy_feud";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<FfConfig> TYPE = GameType.register(
            new Identifier(FarmyFeud.ID, "farmy_feud"),
            FfWaiting::open,
            FfConfig.CODEC
    );

    @Override
    public void onInitialize() {
    }
}
