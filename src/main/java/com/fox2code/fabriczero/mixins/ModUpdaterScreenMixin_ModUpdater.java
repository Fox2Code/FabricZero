package com.fox2code.fabriczero.mixins;

import com.fox2code.fabriczero.access.modupdater.UpdateAllButton;
import com.thebrokenrail.modupdater.client.gui.ModUpdateScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ModUpdateScreen.class)
public abstract class ModUpdaterScreenMixin_ModUpdater extends Screen {
    protected ModUpdaterScreenMixin_ModUpdater() {
        super(null);
    }

    @Inject(at = @At(value = "RETURN"),method = "init")
    public void initHook(CallbackInfo ci) {
        this.addButton(new UpdateAllButton(this));
    }
}
