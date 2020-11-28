package com.fox2code.fabriczero.mixins;

import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(ResourcePackProfile.class)
public class ResourcePackProfileMixin {
    @Shadow
    @Final
    private ResourcePackSource source;

    @Inject(at = @At(value = "HEAD"),method = "getCompatibility", cancellable = true)
    public void getCompatibilityHook(CallbackInfoReturnable<ResourcePackCompatibility> cir) {
        if (this.source == ResourcePackSource.PACK_SOURCE_BUILTIN) {
            cir.setReturnValue(ResourcePackCompatibility.COMPATIBLE);
        }
    }
}
