package me.jellysquid.mods.sodium.client.world.biome;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.util.color.BoxBlur;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;

import java.util.Map;

public class ColorResolverCache {
    private static final int BORDER = 1;

    private final Biome[][] biomes;
    private final Map<ColorResolver, int[][]> colors;
    private final BiomeSlice biomeData;

    private final int sizeHorizontal;
    private final int sizeVertical;

    private final int blurHorizontal;

    private int baseX, baseY, baseZ;

    private final int[] tmpColorData;

    private final int radius;

    private boolean dirty = false;

    public ColorResolverCache(BiomeSlice biomeData, int radius) {
        this.biomeData = biomeData;
        this.radius = radius;

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
        ChunkSectionPos pos = context.getOrigin();

        int borderXZ = radius + BORDER;
        int borderY = BORDER;

        this.baseX = pos.getMinX() - borderXZ;
        this.baseY = pos.getMinY() - borderY;
        this.baseZ = pos.getMinZ() - borderXZ;

        if(this.dirty) {
            this.colors.clear();
            for(int i = 0; i < this.sizeVertical; i++)
                this.biomes[i] = null;
            this.dirty = false;
        }
    }

    public int getColor(ColorResolver resolver, int posX, int posY, int posZ) {
        var x = MathHelper.clamp(posX - this.baseX, 0, this.sizeHorizontal);
        var y = MathHelper.clamp(posY - this.baseY, 0, this.sizeVertical);
        var z = MathHelper.clamp(posZ - this.baseZ, 0, this.sizeHorizontal);

        int[][] colors = this.colors.get(resolver);

        if (colors == null) {
            this.dirty = true;
            this.colors.put(resolver, colors = new int[this.sizeVertical][]);
        }

        var layer = colors[y];

        if (layer == null) {
            this.dirty = true;
            colors[y] = (layer = this.gatherColorsXZ(resolver, y));
        }

        return layer[this.indexXZ(x, z)];
    }

    private Biome[] gatherBiomes(int level) {
        var biomeAccess = this.biomeData;
        var biomeData = new Biome[this.sizeHorizontal * this.sizeHorizontal];

        for (int x = 0; x < this.sizeHorizontal; x++) {
            for (int z = 0; z < this.sizeHorizontal; z++) {
                biomeData[this.indexXZ(x, z)] = biomeAccess.getBiome(x + this.baseX, level + this.baseY, z + this.baseZ).value();
            }
        }

        return biomeData;
    }

    private int[] gatherColorsXZ(ColorResolver resolver, int y) {
        Biome[] biomeData = this.getBiomeData(y);
        var colorData = new int[this.sizeHorizontal * this.sizeHorizontal];

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
