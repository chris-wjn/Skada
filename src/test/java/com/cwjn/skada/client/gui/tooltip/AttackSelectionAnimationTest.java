package com.cwjn.skada.client.gui.tooltip;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises animation timing independently of Minecraft rendering and wall-clock sleeps. */
class AttackSelectionAnimationTest {
  private static final List<String> TYPES = List.of("slash", "thrust", "strike");

  /** Last-to-first cycling returns the underline across the stationary strip without leaving its bounds. */
  @Test
  void lastToFirstStaysWithinTheIconStrip() {
    var animation = new AttackSelectionAnimation();
    Object owner = new Object();
    Object item = new Object();
    assertEquals(2.0, animation.position(owner, item, TYPES, 2, 0));
    assertEquals(2.0, animation.position(owner, item, TYPES, 0, 10_000_000L));
    double halfway = animation.position(owner, item, TYPES, 0, 100_000_000L);
    assertTrue(halfway > 0.0 && halfway < 1.0, "The underline should ease back towards the first fixed icon");
    assertEquals(0.0, animation.position(owner, item, TYPES, 0, 190_000_000L));
    assertEquals(0.0, animation.position(owner, item, TYPES, 0, 250_000_000L));
  }

  /** More frequent tooltip redraws cannot restart or speed up the underline. */
  @Test
  void redrawFrequencyDoesNotChangeTheVisiblePosition() {
    var sparse = new AttackSelectionAnimation();
    var frequent = new AttackSelectionAnimation();
    Object owner = new Object();
    Object item = new Object();
    for (var animation : List.of(sparse, frequent)) {
      animation.position(owner, item, TYPES, 0, 0);
      animation.position(owner, item, TYPES, 1, 10_000_000L);
    }
    for (long now = 20_000_000L; now < 100_000_000L; now += 10_000_000L) {
      frequent.position(owner, item, TYPES, 1, now);
    }
    assertEquals(sparse.position(owner, item, TYPES, 1, 100_000_000L),
        frequent.position(owner, item, TYPES, 1, 100_000_000L));
  }

  /** Switching again mid-transition starts from the visible position and reaches the newest choice. */
  @Test
  void rapidSwitchesStayContinuous() {
    var animation = new AttackSelectionAnimation();
    Object owner = new Object();
    Object item = new Object();
    animation.position(owner, item, TYPES, 0, 0);
    animation.position(owner, item, TYPES, 1, 10_000_000L);
    double visible = animation.position(owner, item, TYPES, 1, 70_000_000L);
    assertEquals(visible, animation.position(owner, item, TYPES, 2, 70_000_000L));
    double later = animation.position(owner, item, TYPES, 2, 130_000_000L);
    assertTrue(later > visible && later < 2.0);
    assertEquals(2.0, animation.position(owner, item, TYPES, 2, 250_000_000L));
  }

  /** Hovering another slot, replacing the item or changing its supported types cannot inherit old motion. */
  @Test
  void unrelatedHoverTargetsResetImmediately() {
    var animation = new AttackSelectionAnimation();
    Object owner = new Object();
    Object item = new Object();
    animation.position(owner, item, TYPES, 0, 0);
    animation.position(owner, item, TYPES, 1, 10_000_000L);
    Object otherOwner = new Object();
    assertEquals(2.0, animation.position(otherOwner, item, TYPES, 2, 50_000_000L));
    Object otherItem = new Object();
    assertEquals(0.0, animation.position(otherOwner, otherItem, TYPES, 0, 60_000_000L));
    assertEquals(1.0, animation.position(otherOwner, otherItem, List.of("slash", "strike"), 1, 70_000_000L));
  }

  /** Reopening a tooltip after its hover expires starts at the current choice; a single type stays still. */
  @Test
  void staleHoversAndSingleAttackItemsRemainStill() {
    var animation = new AttackSelectionAnimation();
    Object owner = new Object();
    Object item = new Object();
    animation.position(owner, item, TYPES, 2, 0);
    animation.position(owner, item, TYPES, 0, 10_000_000L);
    assertEquals(0.0, animation.position(owner, item, TYPES, 0, 500_000_000L));
    assertEquals(0.0, animation.position(owner, item, List.of("slash"), 0, 510_000_000L));
    assertEquals(0.0, animation.position(owner, item, List.of("slash"), 0, 520_000_000L));
  }
}
