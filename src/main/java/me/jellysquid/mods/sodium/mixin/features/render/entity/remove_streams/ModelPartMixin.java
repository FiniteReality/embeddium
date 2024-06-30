package me.jellysquid.mods.sodium.mixin.features.render.entity.remove_streams;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.model.geom.ModelPart;
import org.embeddedt.embeddium.render.entity.ModelPartExtended;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartExtended {
    @Shadow
    @Final
    private Map<String, ModelPart> children;
    private List<ModelPart> embeddium$allParts;

    private Optional<ModelPart> embeddium$optional;
    private Map<String, ModelPart> embeddium$descendantsByName;

    @Override
    public Optional<ModelPart> embeddium$asOptional() {
        if(embeddium$optional == null) {
            embeddium$optional = Optional.of((ModelPart)(Object)this);
        }
        return embeddium$optional;
    }

    public Map<String, ModelPart> embeddium$getDescendantsByName() {
        if(embeddium$descendantsByName == null) {
            Object2ObjectOpenHashMap<String, ModelPart> map = new Object2ObjectOpenHashMap<>();
            for(ModelPart part : embeddium$getPartsList()) {
                for(var entry : ((ModelPartMixin)(Object)part).children.entrySet()) {
                    map.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
            embeddium$descendantsByName = Map.copyOf(map);
        }
        return embeddium$descendantsByName;
    }

    @Override
    public List<ModelPart> embeddium$getPartsList() {
        if(embeddium$allParts == null) {
            ImmutableList.Builder<ModelPart> listBuilder = ImmutableList.builder();
            listBuilder.add((ModelPart)(Object)this);
            for(ModelPart part : this.children.values()) {
                listBuilder.addAll(part.getAllParts().toList());
            }
            embeddium$allParts = listBuilder.build();
        }
        return embeddium$allParts;
    }

    /**
     * @author embeddedt
     * @reason heavily reduce stream allocations
     */
    @Overwrite
    public Stream<ModelPart> getAllParts() {
        return embeddium$getPartsList().stream();
    }
}
