package com.fox2code.fabriczero.mixins;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(I18n.class)
public interface I18nMixin {
    @Accessor(value = "field_25290")
    static void method_29391(Language language) {}
}
