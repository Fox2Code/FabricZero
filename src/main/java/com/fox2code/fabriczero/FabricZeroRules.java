package com.fox2code.fabriczero;

import com.fox2code.fabriczero.api.FabricZeroAPI;

/**
 * This class combine all builtin rules of FabricZero for better editing
 * Note: FabricZero can hide itself by putting "fabriczero" as mod id
 */
class FabricZeroRules {
    static void builtIn() {
        FabricZeroAPI api = FabricZeroAPI.getInstance();
        api.hideMod("optifabric", "me.shedaniel.rei.impl.");
        api.addCurseProjectId("adorn", 320215);
        api.addCurseProjectId("appleskin", 248787);
        api.addCurseProjectId("aurora_keystrokes", 352659);
        api.addCurseProjectId("bedrockwaters", 396568);
        api.addCurseProjectId("betterdroppeditems", 350250);
        api.addCurseProjectId("betternether", 311377);
        api.addCurseProjectId("blockus", 312289);
        api.addCurseProjectId("campanion", 373138);
        api.addCurseProjectId("cavebiomes", 371307);
        api.addCurseProjectId("cinderscapes", 391429);
        api.addCurseProjectId("craftpresence", 297038);
        api.addCurseProjectId("dark-loading-screen", 365727);
        api.addCurseProjectId("diggusmaximus", 341888);
        api.addCurseProjectId("enchantedbookredesign", 398265);
        api.addCurseProjectId("euclid", 335863);
        api.addCurseProjectId("expandedstorage", 317856);
        api.addCurseProjectId("extraalchemy", 247357);
        api.addCurseProjectId("fabric", 306612);
        api.addCurseProjectId("fabric-language-kotlin", 308769);
        api.addCurseProjectId("fastbench", 364531);
        api.addCurseProjectId("fastfurnace", 364540);
        api.addCurseProjectId("horseinfo", 390027);
        api.addCurseProjectId("i-need-keybinds", 331734);
        api.addCurseProjectId("identity", 391390);
        api.addCurseProjectId("illuminations", 292908);
        api.addCurseProjectId("immersive_portals", 332273);
        api.addCurseProjectId("lightoverlay", 325492);
        api.addCurseProjectId("lithium", 360438);
        api.addCurseProjectId("mo_glass", 353426);
        api.addCurseProjectId("modmenu", 308702);
        api.addCurseProjectId("moreberries", 315749);
        api.addCurseProjectId("mostructures", 378266);
        api.addCurseProjectId("okzoomer", 354047);
        api.addCurseProjectId("optifabric", 322385);
        api.addCurseProjectId("overloadedarmorbar", 396300);
        api.addCurseProjectId("phosphor", 372124);
        api.addCurseProjectId("reborncore", 237903);
        api.addCurseProjectId("roughlyenoughitems", 310111);
        api.addCurseProjectId("shulkerboxtooltip", 315811);
        api.addCurseProjectId("simplexterrain", 352997);
        api.addCurseProjectId("slight-gui-modifications", 380393);
        api.addCurseProjectId("smb", 386293); // Better Mod Button
        api.addCurseProjectId("smoothscrollingeverywhere", 325861);
        api.addCurseProjectId("sodium", 394468);
        api.addCurseProjectId("soulshards", 291549);
        api.addCurseProjectId("storagecabinet", 304889);
        api.addCurseProjectId("techreborn", 233564);
        api.addCurseProjectId("terrestria", 323974);
        api.addCurseProjectId("traverse", 308777);
        api.addCurseProjectId("unsuspicious-stew", 399283);
        api.addCurseProjectId("voxelmap", 225179);
        api.addCurseProjectId("waila", 253449); // Hwyla
        api.addCurseProjectId("wolveswitharmor", 375969);
        api.addCurseProjectId("worldtooltips", 387568);
    }
}
