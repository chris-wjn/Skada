package com.cwjn.skada.data.gen.weapon.parts;

import com.cwjn.skada.data.SkadaData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

public abstract class WeaponHead {

  public double getVolume() {
    return 0;
  }

  public double getPointOfBalance() {
    return 0;
  }

  public abstract Codec<? extends WeaponHead> type();

  public abstract String typeKey();

  public static final Codec<WeaponHead> DISPATCH_CODEC = Codec.STRING.dispatch(
          "typeKey",
          WeaponHead::typeKey,
          SkadaData.WEAPON_HEAD_CODECS::get
  );

  public static final Codec<WeaponHead> CODEC = new Codec<>() {
    @Override
    public <T> DataResult<Pair<WeaponHead, T>> decode(DynamicOps<T> ops, T input) {
      if (ops != JsonOps.INSTANCE) {
        return DataResult.error(() -> "WeaponHead codec only supports JsonOps");
      }
      JsonElement json;
      try {
        json = (JsonElement) input;
      } catch (ClassCastException e) {
        return DataResult.error(() -> "Expected JsonElement for WeaponHead");
      }

      if (!json.isJsonObject()) return DataResult.error(() -> "WeaponHead must be a JSON object");
      JsonObject obj = json.getAsJsonObject();
      if (!obj.has("type")) return DataResult.error(() -> "WeaponHead missing 'type' field");
      String type = obj.get("type").getAsString();

      Codec<? extends WeaponHead> codec = switch (type) {
        case "blade" -> Blade.CODEC;
        case "axe" -> AxeHead.CODEC;
        case "mace" -> MaceHead.CODEC;
        case "pick" -> PickHead.CODEC;
        case "shovel" -> ShovelHead.CODEC;
        case "sickle" -> SickleHead.CODEC;
        default -> null;
      };

      if (codec == null) return DataResult.error(() -> "Unknown weapon head type: " + type);

      // Delegate decode; use raw types to avoid wildcard capture issues
      @SuppressWarnings({"rawtypes", "unchecked"})
      DataResult raw = codec.decode(JsonOps.INSTANCE, json);
      return raw.map(o -> {
        Pair<?, ?> p = (Pair<?, ?>) o;
        WeaponHead wh = (WeaponHead) p.getFirst();
        @SuppressWarnings("unchecked")
        T remainder = (T) p.getSecond();
        return Pair.of(wh, remainder);
      });
    }

    @Override
    public <T> DataResult<T> encode(WeaponHead input, DynamicOps<T> ops, T prefix) {
      System.out.println("Encoding WeaponHead of type: " + input.typeKey());
      if (ops != JsonOps.INSTANCE) {
        return DataResult.error(() -> "WeaponHead codec only supports JsonOps");
      }
      @SuppressWarnings("unchecked")
      Codec<WeaponHead> codec = (Codec<WeaponHead>) input.type();
      @SuppressWarnings({"rawtypes", "unchecked"})
      DataResult raw = codec.encodeStart(JsonOps.INSTANCE, input);
      System.out.println("Encoded raw result");
      return raw.flatMap(o -> {
        JsonElement json = (JsonElement) o;
        if (!json.isJsonObject()) return DataResult.error(() -> "Encoded WeaponHead must be a JSON object");
        JsonObject obj = json.getAsJsonObject();
        obj.addProperty("type", input.typeKey());
        @SuppressWarnings("unchecked")
        T t = (T) json;
        System.out.println("Added type field to WeaponHead JSON");
        return DataResult.success(t);
      });
    }
  };

}


