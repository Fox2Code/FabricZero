# Fabric Zero

Compatibility and performance enhancement mod  
*(Made for large mod-pack)*

## Features

- Fix crash with many mods.  
  Note: This mod does patches that can help any mod to be compatible but do not guarantee a 100%
  success I can't make a list of what this mod fix or not 
   *(You will need to find by yourself what this mod fix)*

- Fix [OptiFabric](https://www.curseforge.com/minecraft/mc-mods/optifabric)
  / [Optifine](https://www.optifine.net/home) compatibility with many mods  
  Ex: ([Lithium](https://www.curseforge.com/minecraft/mc-mods/lithium), 
  [Phosphor](https://www.curseforge.com/minecraft/mc-mods/phosphor),
  [REI](https://www.curseforge.com/minecraft/mc-mods/roughly-enough-items),
  and more)
 
- Allow [ModUpdater](https://www.curseforge.com/minecraft/mc-mods/modupdater) 
  to update all mods without having to download them one by one  
  \+  opt-in many mods to mod updater that haven't
  
- Optimize bytecode of classes to reduce CPU usage  
  \+ Minecraft specific bytecode optimisations  
  (These optimisations also apply to all mods classes  
   making this mod more efficient on large mod-packs)
   
## FAQ

- Q: Can I use FabricZero in my mod pack?  
  A: Yeah you can use it without needing to ask me the permission  
   as long you don't try to attempt to hide the fact that FabricZero is loaded

- Q: Does this mod has incompatibilities or less efficient with another mods?  
  A: This mod has no incompatibilities, and the optimisations of this mod do not 
   interfere with any optimisation from others mods.  
   I even recommend you try to use these mods with FabricZero:  
   [Sodium](https://www.curseforge.com/minecraft/mc-mods/sodium),
   [Lithium](https://www.curseforge.com/minecraft/mc-mods/lithium),
   [Phosphor](https://www.curseforge.com/minecraft/mc-mods/phosphor),
   [OptiFabric](https://www.curseforge.com/minecraft/mc-mods/optifabric)  
   Note: Sodium and OptiFabric **are still incompatible** since they modify the same part of the game code


- Q: How this mod optimizes bytecode?  
A: The mod has multiple bytecode optimizations for multiples this to optimize:
   - **Generic bytecode optimizations** (Optimize bytecode without being targeted at Minecraft)  
     These bytecode optimizations are not very effective in a near vanilla game since the
     java bytecode is already optimized with Proguard by mojang and because this mod
     does similar generic bytecode optimization to Proguard.
   - **Minecraft Targeted optimizations**  
     These bytecode optimizations target some minor game optimization, and some bad code design 
     like using complex lambda every OpenGL calls and replace them with a simple field get making the 
     game run faster even in a near-vanilla environment.
   - **Mod Targeted optimizations**  
     These bytecode optimizations target to developers unawareness and optimize the bytecode and
     making the game running faster on large mod-pack by for example
     redirecting `sin` and `cos` math calls to the minecraft optimized ones if the mod
     doesn't directly use them.
   
   Note: Most of these optimisations are also available for Bukkit/Spigot/Paper with
    [KibblePatcher](https://github.com/KibbleLands/KibblePatcher)