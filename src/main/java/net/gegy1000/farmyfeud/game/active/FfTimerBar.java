package net.gegy1000.farmyfeud.game.active;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

// TODO: duplication with spleef!
public final class FfTimerBar implements AutoCloseable {
    private final ServerBossBar bar;

    public FfTimerBar() {
        LiteralText title = new LiteralText("Game ends in...");

        this.bar = new ServerBossBar(title, BossBar.Color.RED, BossBar.Style.NOTCHED_10);
        this.bar.setDarkenSky(false);
        this.bar.setDragonMusic(false);
        this.bar.setThickenFog(false);
    }

    public void update(long timeLeft, long totalTime) {
        if (timeLeft % 20 == 0) {
            this.bar.setName(this.getText(timeLeft));
            this.bar.setPercent((float) timeLeft / totalTime);
        }
    }

    public void addPlayer(ServerPlayerEntity player) {
        this.bar.addPlayer(player);
    }

    private Text getText(long ticksUntilDrop) {
        long secondsUntilDrop = ticksUntilDrop / 20;

        long minutes = secondsUntilDrop / 60;
        long seconds = secondsUntilDrop % 60;
        String time = String.format("%02d:%02d", minutes, seconds);

        return new LiteralText("Game ends in: " + time + "...");
    }

    @Override
    public void close() {
        this.bar.clearPlayers();
        this.bar.setVisible(false);
    }
}
