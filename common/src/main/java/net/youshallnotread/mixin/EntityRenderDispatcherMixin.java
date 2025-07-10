package net.youshallnotread.mixin;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import net.youshallnotread.outline.Outliner;

// This mixin exists to get the interpolation amount to apply to our entity based outlines to ensure they have correct
// positions when rendering
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void youshallnotread$getFrameDelta(
            E entity,
            double d,
            double e,
            double f,
            float g,
            float h,
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int i,
            CallbackInfo ci) {
        Outliner.RENDER_DELTA = h;
    }
}
