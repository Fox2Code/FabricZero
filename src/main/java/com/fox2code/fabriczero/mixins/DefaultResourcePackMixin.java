package com.fox2code.fabriczero.mixins;

import com.fox2code.fabriczero.access.MCResources;
import net.minecraft.resource.DefaultResourcePack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Pseudo
@Mixin(DefaultResourcePack.class)
public class DefaultResourcePackMixin {

    @Inject(at = @At(value = "HEAD"),method = "getInputStream", cancellable = true)
    public void getInputStreamHook(String path, CallbackInfoReturnable<@Nullable InputStream> cir) {
        if (MCResources.successful) {
            if (path.equals("pack.mcmeta")) {
                cir.setReturnValue(new ByteArrayInputStream(MCResources.pack_mcmeta));
            } else if (path.equals("pack.png")) {
                cir.setReturnValue(new ByteArrayInputStream(MCResources.pack_png));
            }
        }
    }
}
