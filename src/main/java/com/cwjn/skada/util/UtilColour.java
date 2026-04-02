package com.cwjn.skada.util;

/**
 * Utility class for colour-related functions, such as generating colours based on percentages and defining common colour constants.
 * This class is not clientsided, as it is also used for console logging and server-side operations.
 */
public class UtilColour {

    public static final int BASIC = 0xBC9F71;
    public static final int HEAT = 0xFAA351;
    public static final int COLD = 0xB2FFFF;
    public static final int LIGHTNING = 0xFFE24F;
    public static final int ENDER = 0xBA53E6;
    public static final int WITHER = 0x632D75;
    public static final int AETHER = 0xFFFDE8;

    // Common UI colours
    public static final int WHITE = 0xFFFFFF;
    public static final int BLACK = 0x000000;
    public static final int GRAY = 0x808080;
    public static final int LIGHTER_GRAY = 0xC0C0C0;
    public static final int LIGHT_GRAY = 0xE0E0E0;
    public static final int DARK_GRAY = 0x404040;
    public static final int UI_TEXT_COLOUR = 0x503933;
    public static final int UI_BORDER_COLOUR = 0x91665B;
    public static final int UI_BACKGROUND_COLOUR = 0xCA8E61;

    // Status colours
    public static final int SUCCESS = 0x4CAF50;
    public static final int WARNING = 0xFF9800;
    public static final int ERROR = 0xF44336;
    public static final int INFO = 0x2196F3;

    // Minecraft-themed colours
    public static final int ENCHANTED_PURPLE = 0x8B00FF;
    public static final int EXPERIENCE_GREEN = 0x80FF20;
    public static final int HEALTH_RED = 0xFF0000;
    public static final int MANA_BLUE = 0x0080FF;
    public static final int GOLD = 0xFFD700;
    public static final int DIAMOND = 0x4FD0E7;
    public static final int EMERALD = 0x50C878;
    public static final int REDSTONE = 0xFF0000;

    // Nature/Element colours
    public static final int EARTH = 0x8B4513;
    public static final int WATER = 0x1E90FF;
    public static final int FIRE = 0xFF4500;
    public static final int AIR = 0xE6E6FA;
    public static final int NATURE = 0x228B22;
    public static final int SHADOW = 0x2F4F4F;
    public static final int LIGHT = 0xFFFACD;

    // Rarity colours (common in RPGs)
    public static final int COMMON = 0xFFFFFF;
    public static final int UNCOMMON = 0x1EFF00;
    public static final int RARE = 0x0070DD;
    public static final int EPIC = 0xA335EE;
    public static final int LEGENDARY = 0xFF8000;
    public static final int MYTHIC = 0xE6CC80;

    // UI accent colours
    public static final int HOVER = 0x40FFFFFF;
    public static final int SELECTED = 0x80FFFFFF;
    public static final int DISABLED = 0x808080;
    public static final int TRANSPARENT = 0x00000000;

    // Console colours (using ANSI escape codes)
    public static final String CONSOLE_RESET = "\033[0m";
    public static final String CONSOLE_BLACK = "\033[30m";
    public static final String CONSOLE_RED = "\033[31m";
    public static final String CONSOLE_GREEN = "\033[32m";
    public static final String CONSOLE_YELLOW = "\033[33m";
    public static final String CONSOLE_BLUE = "\033[34m";
    public static final String CONSOLE_PURPLE = "\033[35m";
    public static final String CONSOLE_CYAN = "\033[36m";
    public static final String CONSOLE_WHITE = "\033[37m";
    public static final String CONSOLE_BOLD = "\033[1m";
    public static final String CONSOLE_UNDERLINE = "\033[4m";

    /**
     * Gets a colour from red to green based the difference between the value and defaultValue,
     * where if the value is worse than the expected value, it is red, and if it is better, it is green.
     */
    public static int getColourByPercentage(double value, double defaultValue, boolean higherIsBetter) {
      double difference = value - defaultValue;
      int red = 128, green = 128, blue = 128; //start at #808080 so that when we add colour it'll be brighter
    
      float percentOfDefault;
      if (defaultValue == 0) percentOfDefault = (float) difference;
      else percentOfDefault = (float) (difference / defaultValue);
      int mainHex = Math.round(128 * UtilColour.mainToOtherHexRatio(percentOfDefault));
      int otherHex = 128 - mainHex;
      blue += otherHex;
    
      if (percentOfDefault < 0.05) {
        if (higherIsBetter) {
          red += mainHex;
          green += otherHex;
        } else {
          red += otherHex;
          green += mainHex;
        }
      } else {
        if (higherIsBetter) {
          red += otherHex;
          green += mainHex;
        } else {
          red += mainHex;
          green += otherHex;
        }
      }
      red = Math.min(255, red);
      green = Math.min(255, green);
      blue = Math.min(255, blue);
    
      return (red << 16) | (green << 8) | blue;
    }

    /*
        Get the ratio of main hex colour to other hex colours based on percentage.
        At 500% (5.0), the main hex colour is 100% and the other hex colours are 0%.
     */
    static float mainToOtherHexRatio(float percent) {
      float hexPercent = percent / 3.0f;
      return Math.max(0.5f, hexPercent);
    }

}
