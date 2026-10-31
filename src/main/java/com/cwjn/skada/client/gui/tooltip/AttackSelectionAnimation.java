package com.cwjn.skada.client.gui.tooltip;

import java.lang.ref.WeakReference;
import java.util.List;

/** Keeps the selection underline moving smoothly across per-frame tooltip reconstruction. */
final class AttackSelectionAnimation {
  private static final long DURATION_NANOS = 180_000_000L;
  private static final long HOVER_TIMEOUT_NANOS = 300_000_000L;
  private WeakReference<Object> owner = new WeakReference<>(null);
  private Object item;
  private List<String> order = List.of();
  private int selected;
  private double from;
  private double target;
  private long started;
  private long lastSeen;

  /** Tracks the selected icon's fixed position, resetting when the hover target, item or attack order changes. */
  double position(Object owner, Object item, List<String> order, int selected, long now) {
    if (this.owner.get() != owner || this.item != item || !this.order.equals(order)
        || now - lastSeen > HOVER_TIMEOUT_NANOS || order.size() < 2) {
      // A closed inventory must not be kept alive by this purely visual cache.
      this.owner = new WeakReference<>(owner);
      this.item = item;
      this.order = List.copyOf(order);
      this.selected = selected;
      from = target = selected;
      started = now;
    } else if (this.selected != selected) {
      // Continue from the currently visible position when another switch arrives before settling.
      from = interpolatedPosition(now);
      target = selected;
      this.selected = selected;
      started = now;
    }
    lastSeen = now;
    return interpolatedPosition(now);
  }

  /** Eases towards the selected position without frame-rate-dependent timing or overshoot. */
  private double interpolatedPosition(long now) {
    double progress = Math.max(0.0, Math.min(1.0, (double) (now - started) / DURATION_NANOS));
    double remaining = 1.0 - progress;
    return from + (target - from) * (1.0 - remaining * remaining * remaining);
  }
}
