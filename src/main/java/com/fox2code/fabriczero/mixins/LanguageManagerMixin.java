package com.fox2code.fabriczero.mixins;

import com.google.common.collect.Lists;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings("ConstantConditions")
@Mixin(LanguageManager.class)
public class LanguageManagerMixin { // WIP
    /*@Shadow
    @Final
    private static LanguageDefinition field_25291;
    @Shadow
    private Map<String, LanguageDefinition> languageDefs;
    @Shadow
    private String currentLanguageCode;
    @Shadow
    private LanguageDefinition field_25292;

    @Shadow
    private static Map<String, LanguageDefinition> method_29393(Stream<ResourcePack> stream) {
        return null;
    }

    /**
     * @author Fox2Code
     * @reason Mixin is shit
     */
    /*@Overwrite
    public void apply(ResourceManager manager) {
        this.languageDefs = method_29393(manager.streamResourcePacks());
        LanguageDefinition languageDefinition = this.languageDefs.getOrDefault("en_us", field_25291);
        this.field_25292 = this.languageDefs.getOrDefault(this.currentLanguageCode, languageDefinition);
        List<LanguageDefinition> list = Lists.newArrayList(languageDefinition);
        if (this.field_25292 != languageDefinition) {
            LanguageDefinition extra = null;
            if (this.field_25292.getCode().startsWith("es_")) {
                extra = this.languageDefs.get("es_es");
            } else if (this.field_25292.getCode().startsWith("fr_")) {
                extra = this.languageDefs.get("fr_fr");
            } else if (this.field_25292.getCode().startsWith("de_")) {
                extra = this.languageDefs.get("de_de");
            } else if (this.field_25292.getCode().startsWith("nl_")) {
                extra = this.languageDefs.get("nl_nl");
            }
            if (extra != null && extra != this.field_25292) {
                list.add(extra);
            }
            list.add(this.field_25292);
        }
        TranslationStorage translationStorage = TranslationStorage.load(manager, list);
        try {
            I18nMixin.method_29391(translationStorage);
        } catch (Throwable t) {
            I18nMixin.field_25290(translationStorage);
        }
        Language.setInstance(translationStorage);
        System.gc(); // Memory Opt (This method is run just after resources reloading)
    }*/

    @Inject(method = "apply", at = @At("RETURN"))
    public void initHook(ResourceManager manager, CallbackInfo ci) {
        System.gc(); // Memory Opt (This method is run just after resources reloading)
    }
}
