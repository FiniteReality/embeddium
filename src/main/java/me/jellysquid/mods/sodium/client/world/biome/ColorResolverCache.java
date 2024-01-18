package me.jellysquid.mods.sodium.client.world.biome;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import java.util.Map;

public class ColorResolverCache {
    private static final int BORDER = 1;

    private final Biome[][] biomes;
    private final Map<ColorResolver, int[][]> colors;
    private final WorldSlice biomeData;

    private final int sizeHorizontal;
    private final int sizeVertical;

    private final int blurHorizontal;

    private int baseX, baseY, baseZ;

    private final int[] tmpColorData;

    private final int radius;

    private boolean dirty = false;

    public ColorResolverCache(WorldSlice biomeData) {
        this.biomeData = biomeData;
        this.radius = Minecraft.getInstance().options.biomeBlendRadius;

        int borderXZ = radius + BORDER;
        int borderY = BORDER;

        this.sizeHorizontal = 16 + (borderXZ * 2);
        this.sizeVertical = 16 + (borderY * 2);

        this.blurHorizontal = radius;

        this.tmpColorData = new int[this.sizeHorizontal * this.sizeHorizontal];

        this.colors = new Reference2ReferenceOpenHashMap<>();
        this.biomes = new Biome[this.sizeVertical][];
    }

    public void update(ChunkRenderContext context) {
        SectionPos pos = context.getOrigin();

        int borderXZ = radius + BORDER;
        int borderY = BORDER;

        this.baseX = pos.minBlockX() - borderXZ;
        this.baseY = pos.minBlockY() - borderY;
        this.baseZ = pos.minBlockZ() - borderXZ;

        if(this.dirty) {
            this.colors.clear();
            for(int i = 0; i < this.sizeVertical; i++)
                this.biomes[i] = null;
            this.dirty = false;
        }
    }

    public int getColor(ColorResolver resolver, int posX, int posY, int posZ) {
        int x = Mth.clamp(posX - this.baseX, 0, this.sizeHorizontal);
        int y = Mth.clamp(posY - this.baseY, 0, this.sizeVertical);
        int z = Mth.clamp(posZ - this.baseZ, 0, this.sizeHorizontal);

        int[][] colors = this.colors.get(resolver);

        if (colors == null) {
            this.dirty = true;
            this.colors.put(resolver, colors = new int[this.sizeVertical][]);
        }

        int[] layer = colors[y];

        if (layer == null) {
            this.dirty = true;
            colors[y] = (layer = this.gatherColorsXZ(resolver, y));
        }

        return layer[this.indexXZ(x, z)];
    }

    private Biome[] gatherBiomes(int level) {
        WorldSlice biomeAccess = this.biomeData;
        Biome[] biomeData = new Biome[this.sizeHorizontal * this.sizeHorizontal];

        for (int x = 0; x < this.sizeHorizontal; x++) {
            for (int z = 0; z < this.sizeHorizontal; z++) {
                biomeData[this.indexXZ(x, z)] = biomeAccess.getBiome(x + this.baseX, level + this.baseY, z + this.baseZ);
            }
        }

        return biomeData;
    }

    private int[] gatherColorsXZ(ColorResolver resolver, int y) {
        Biome[] biomeData = this.getBiomeData(y);
        int[] colorData = new int[this.sizeHorizontal * this.sizeHorizontal];

        for (int x = 0; x < this.sizeHorizontal; x++) {
            for (int z = 0; z < this.sizeHorizontal; z++) {
                int index = this.indexXZ(x, z);
                colorData[index] = resolver.getColor(biomeData[index],
                        x + this.baseX, z + this.baseZ);
            }
        }

        BoxBlur.blur(colorData, this.tmpColorData, this.sizeHorizontal, this.sizeHorizontal, this.blurHorizontal);

        return colorData;
    }

    private Biome[] getBiomeData(int y) {
        Biome[] biomes = this.biomes[y];

        if (biomes == null) {
            this.biomes[y] = (biomes = this.gatherBiomes(y));
        }

        return biomes;
    }

    private int indexXZ(int x, int z) {
        return (x * this.sizeHorizontal) + z;
    }
}
