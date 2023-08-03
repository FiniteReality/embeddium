<p align="center">
  <img src="logo.jpeg" width="250">
</p>

# Rubidium
[![](http://cf.way2muchnoise.eu/short_rubidium_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/rubidium)
[![](http://cf.way2muchnoise.eu/versions/Available%20for_rubidium_full.svg)](https://www.curseforge.com/minecraft/mc-mods/rubidium/files)

Rubidium is an Unofficial Fork of CaffeineMC's ["Sodium"](https://modrinth.com/mod/sodium), made to work with Forge Mod Loader.

## Disclaimer
Rubidium is not and never will be compatible with Optifine!


## Shaders
Want to play Minecraft with shaders with Rubidium? Try [Oculus!](https://www.curseforge.com/minecraft/mc-mods/oculus) made by Asek3!

## Features
* A modern OpenGL rendering pipeline for chunk rendering that takes advantage of multi-draw techniques, allowing for a significant reduction in CPU overhead (~90%) when rendering the world. This can make a huge difference to frame rates for most computers that are not bottle-necked by the GPU or other components. Even if your GPU can't keep up, you'll experience much more stable frame times thanks to the CPU being able to work on other rendering tasks while it waits.
* Vertex data for rendered chunks is made much more compact, allowing for video memory and bandwidth requirements to be cut by almost 40%.
* Nearby block updates now take advantage of multi-threading, greatly reducing lag spikes caused by chunks needing to be updated.
* Chunk faces which are not visible (or facing away from the camera) are culled very early in the rendering process, eliminating a ton of geometry that would have to be processed on the GPU only to be immediately discarded. For integrated GPUs, this can greatly reduce memory bandwidth requirements and provide a modest speedup even when GPU-bound.
* Plentiful optimizations for chunk loading and block rendering, making chunk loading significantly faster and less damaging to frame rates.
* Many optimizations for vertex building and matrix transformations, speeding up block entity, mob, and item rendering significantly for when you get carried away placing too many chests in one room.
* Many improvements to how the game manages memory and allocates objects, which in turn reduces memory consumption and lag spikes caused by garbage collector activity.
* Many graphical fixes for smooth lighting effects, making the game run better while still applying a healthy amount of optimization.
* Smooth lighting for fluids and other special blocks.
* Smooth biome blending for blocks and fluids, providing greatly improved graphical quality that is significantly less computationally intensive.
* Animated textures which are not visible in the world are not updated, speeding up texture updating on most hardware (especially AMD cards.)

## Discord
[![](https://dcbadge.vercel.app/api/server/UCsyn5RS4s)](https://discord.gg/UCsyn5RS4s)

## Contributors
<a href="https://github.com/Asek3/Rubidium/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Asek3/Rubidium" />
</a>

## License

[LGPL-3.0 license](https://github.com/Asek3/Rubidium/blob/1.18/dev/LICENSE)

## Consider supporting 
[![Support me on Patreon](https://img.shields.io/endpoint.svg?url=https%3A%2F%2Fshieldsio-patreon.vercel.app%2Fapi%3Fusername%3Dasek3%26type%3Dpatrons&style=for-the-badge)](https://patreon.com/asek3)