package com.cwjn.skada.client.gui.screen;

import com.cwjn.skada.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class Animation {
  private final ResourceLocation[] frames;
  private final int totalFrames;
  private final long durationMs; // Total animation duration in milliseconds
  private final int yOffset;
  private final int height;
  private final int vOffset;

  private long startTime = -1;
  private boolean complete = false;
  private Animation nextAnim;
  private double animProgess;
  private SoundEvent soundEffect;
  private long delay = 0;

  public Animation(String basePath, int frameCount, long durationMs, int vOffset, int yOffset, int height) {
    this.totalFrames = frameCount;
    this.durationMs = durationMs;
    this.yOffset = yOffset;
    this.height = height;
    this.vOffset = vOffset;

    this.frames = new ResourceLocation[frameCount];
    for (int i = 0; i < frameCount; i++) {
      frames[i] = Util.rl(basePath + (i + 1) + ".png");
    }
  }

  public ResourceLocation getCurrentFrame() {
    if (startTime == -1) {
      startTime = System.currentTimeMillis() + delay;
      if (soundEffect != null) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(this.soundEffect, 1.0F));
      }
    }

    long elapsed = System.currentTimeMillis() - startTime;
    if (elapsed < 0) {
      return frames[0]; // Still in delay period
    }

    if (elapsed >= durationMs) {
      complete = true;
      return frames[totalFrames - 1];
    }

    // Calculate current frame based on elapsed time
    int currentFrame = (int) ((elapsed * totalFrames) / durationMs);
    animProgess = (double) elapsed / durationMs; // Calculate animation progress as a percentage
    return frames[Math.min(currentFrame, totalFrames - 1)];
  }

  public void reset() {
    startTime = -1;
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

  public void addDelay(long delayMs) {
    this.delay = delayMs;
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

  public double getAnimProgess() {
    return animProgess;
  }
}
