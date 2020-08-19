package xyz.nucleoid.farmyfeud.game.active;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.widget.BossBarWidget;

public final class FfTimerBar implements AutoCloseable {
    private final BossBarWidget bar;

    FfTimerBar(GameWorld gameWorld) {
        LiteralText title = new LiteralText("Game ends in...");
        this.bar = BossBarWidget.open(gameWorld.getPlayerSet(), title, BossBar.Color.RED, BossBar.Style.NOTCHED_10);
    }

    public void update(long timeLeft, long totalTime) {
        if (timeLeft % 20 == 0) {
            this.bar.setTitle(this.getText(timeLeft));
            this.bar.setProgress((float) timeLeft / totalTime);
        }
    }

    private Text getText(long tickUntilEnd) {
        long secondsUntilEnd = tickUntilEnd / 20;

        long minutes = secondsUntilEnd / 60;
        long seconds = secondsUntilEnd % 60;
        String time = String.format("%02d:%02d", minutes, seconds);

        return new LiteralText("Game ends in: " + time + "...");
    }

    @Override
    public void close() {
        this.bar.close();
    }
}
