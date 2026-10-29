package com.cwjn.skada.data.mob;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobDataWeightTest {

  @Test
  void shippedVanillaDataIncludesExplicitFiniteWeightsIncludingPlayer() throws IOException {
    JsonObject data = JsonParser.parseString(Files.readString(
        Path.of("src/main/resources/data/skada/mob_info/minecraft.json"))).getAsJsonObject();

    assertTrue(data.has("player"));
    assertEquals(70.0, data.getAsJsonObject("player").get("weight").getAsDouble());
    data.entrySet().forEach(entry -> {
      assertTrue(entry.getValue().getAsJsonObject().has("weight"), entry.getKey());
      double weight = entry.getValue().getAsJsonObject().get("weight").getAsDouble();
      assertTrue(Double.isFinite(weight) && weight >= 0.0, entry.getKey());
    });
  }
}
