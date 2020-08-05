package com.fox2code.fabriczero.access.modupdater;

import com.thebrokenrail.modupdater.data.ModUpdate;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

public class UpdateAllButton extends ButtonWidget {
    // Just if the original mod author want to implement it's own update mechanism without causing conflict
    private static final boolean KILL_SWITCH = I18n.hasTranslation("gui.modupdater.update_all");
    public static final int BUTTON_WIDTH = 100;
    public static final int BUTTON_HEIGHT = 20;
    public static final int MARGIN_RIGHT = 5;
    public static final int MARGIN_TOP = 7;

    public UpdateAllButton(Screen screen) {
        super(screen.width - BUTTON_WIDTH - MARGIN_RIGHT, MARGIN_TOP, BUTTON_WIDTH, BUTTON_HEIGHT,
                new TranslatableText("fabriczero.updateAll"), button -> UpdateAll.updateAll());
        this.visible = !KILL_SWITCH;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            final ModUpdate[] modUpdates = UpdateAll.getRealUpdates();
            this.active = modUpdates != null && modUpdates.length != 0;
            super.render(matrices, mouseX, mouseY, delta);
        }
    }
}
