package com.cwjn.skada.client.gui.particles;

import com.cwjn.skada.util.Util;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class NumberParticle extends Particle {

    private static final Minecraft client = Minecraft.getInstance();
    private static final Style indicator = Style.EMPTY.withFont(Util.rl("indicator"));
    private final float number;
    private final float weight;
    private float initialGrav = 0.05f;
    private final int colour;

    protected NumberParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, float f, int i) {
        super(level, x, y, z, vx, vy, vz);

        gravity = 0.981f;
        weight = 0.25f; //kg
        hasPhysics = false;
        xd = vx;
        yd = vy;
        zd = vz;
        lifetime = 45;
        rCol = 1;
        gCol = 1;
        bCol = 1;
        alpha = 1;
        number = f;
        colour = i;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            if (this.age >= 20) {
                this.yd -= gravity*weight;
            }
            else {
                if (this.yd > 0) {
                    this.yd -= initialGrav;
                    initialGrav+=0.01f;
                    if (this.yd <= 0) {
                        yd = -0.03;
                    }
                }
            }
            this.move(this.xd, this.yd, this.zd);
            if (this.speedUpWhenYMotionIsBlocked && this.y == this.yo) {
                this.xd *= 1.1D;
                this.zd *= 1.1D;
            }
            this.xd *= this.friction-0.15;
            this.yd *= this.friction;
            this.zd *= this.friction-0.15;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void render(@NotNull VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
        PoseStack stack = new PoseStack();
        MutableComponent component = Component.literal(String.valueOf((int) number)).withStyle(indicator);
        Vec3 cam = pRenderInfo.getPosition();
        double displayX = Mth.lerp(pPartialTicks, xo, x) - cam.x;
        double displayY = Mth.lerp(pPartialTicks, yo, y) - cam.y;
        double displayZ = Mth.lerp(pPartialTicks, zo, z) - cam.z;
        stack.pushPose();
        stack.translate(displayX, displayY, displayZ);
        stack.mulPose(Axis.YP.rotationDegrees(-pRenderInfo.getYRot()));
        stack.mulPose(Axis.XP.rotationDegrees(pRenderInfo.getXRot()));
        stack.scale(-0.03f, -0.03f,  0.03f);
        client.font.drawInBatch(component, 0, 0, colour, false, stack.last().pose(), Minecraft.getInstance().renderBuffers().bufferSource(), Font.DisplayMode.NORMAL,0x555555, 15728880);
        stack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<NumberParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet spriteSet) {
            this.sprites = spriteSet;
        }

        public Particle createParticle(@NotNull NumberParticleType particleType, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new NumberParticle(level, x, y, z, dx, dy, dz, sprites, particleType.getDamageNumber(), particleType.getColour());
        }

    }

}