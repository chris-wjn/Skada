package com.cwjn.skada.client.gui.screen;

import com.cwjn.skada.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class Animation {

    private final ResourceLocation[] frames;
    private int animSpeed;
    private int currentFrame = 1;
    private int vOffset, yOffset, height, delayFrames = 0;
    private boolean complete, playedSound = false;
    private SoundEvent soundEffect;
    private Animation nextAnim;

    public Animation(String path, int count, int speed, int vOffset, int yOffset, int height) {
        this.frames = createAnim(path, count);
        this.animSpeed = speed;
        this.vOffset = vOffset;
        this.yOffset = yOffset;
        this.height = height;
    }

    private static ResourceLocation[] createAnim(String path, int count) {
        ResourceLocation[] returnRL = new ResourceLocation[count];
        for (int i = 0; i < count; i++) {
            returnRL[i] = Util.rl(path + (i + 1) + ".png");
        }
        return returnRL;
    }

    public ResourceLocation getCurrentFrameThenIterate(int timer) {
        if (timer < delayFrames) {
            return frames[0];
        }
        int i = currentFrame;
        if (!playedSound && soundEffect != null) {
            playedSound = true;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(this.soundEffect, 1.0F));
        }
        if (currentFrame < frames.length) {
            if (timer % animSpeed == 0) {
                currentFrame++;
            }
        }
        else {
            complete = true;
        }
        return frames[i-1];
    }

    public void reset() {
        currentFrame = 1;
        playedSound = false;
        complete = false;
    }

    public boolean isComplete() {
        return complete;
    }

    public int getVOffset() {
        return vOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public int getHeight() {
        return height;
    }

    public void addDelay(int delayFrames) {
        this.delayFrames += delayFrames;
    }

    public void setSoundEffect(SoundEvent soundEffect) {
        this.soundEffect = soundEffect;
    }

    public void setNextAnim(Animation nextAnim) {
        this.nextAnim = nextAnim;
    }

    public Animation getNextAnim() {
        return nextAnim;
    }

}
