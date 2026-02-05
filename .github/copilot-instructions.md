# Skada - Copilot Instructions

## Context
Take as long as needed to understand the files, code and their purpose. Maximum code accuracy is essential. Anything you come up with will be used in hospitals so any mistakes could cost lives.

## Project Overview

**Skada** is a Minecraft Forge mod (1.20.1) that completely overhauls the combat system with realistic weapon physics, damage types, and material-based mechanics. The mod replaces vanilla damage calculations with a physics-driven system based on weapon geometry, material properties, and attack types.

### Core Concepts
- **Attack Types**: Slash, Thrust, Strike, Magic (each with unique lethality functions)
- **Elements**: Heat, Cold, Lightning, Ender, Wither, Aether, Physical (with resistances and affinities)
- **Weapon Profiles**: Geometric definitions of weapons including handle and weapon heads
- **Lethality System**: Physics-based damage calculations using angular momentum, moment of inertia, and material properties
- **Precision System**: Damage variance based on weapon accuracy characteristics

## Technology Stack

| Component | Technology |
|-----------|------------|
| Minecraft Version | 1.20.1 |
| Mod Loader | Forge (NeoForged Gradle 47.1.103) |
| Java Version | 17 |
| Mappings | Parchment (2023.09.03-1.20.1) |
| Build System | Gradle |
| Mixin Library | SpongePowered Mixin 0.8+ |
| Testing | JUnit 5 |

## Project Structure

```
src/main/java/com/cwjn/skada/
├── Skada.java                 # Main mod class (@Mod entry point)
├── SkadaRegistry.java         # Deferred registries for attack types, elements, attributes
├── SkadaCommand.java          # In-game commands for debugging and generation
├── CommonConfig.java          # Server-side configuration
├── ClientConfig.java          # Client-side configuration
├── client/                    # Client-side code (rendering, GUI, HUD)
│   ├── hud/                   # Custom reticle system
│   └── gui/                   # Screens and UI components
├── damage/                    # Damage handling system
│   ├── DamageHandler.java     # Main damage calculation logic
│   ├── SkadaDamageSource.java # Custom damage source with Skada info
│   └── SkadaDamageTypeTags.java
├── data/                      # Data structures and registries
│   ├── SkadaData.java         # Global constants and data holders
│   ├── damage/                # WeaponInfo, AttackTypeInfo, DamageInfo
│   ├── armour/                # Armour information classes
│   ├── mob/                   # Mob data definitions
│   ├── registry/              # Custom registry types (AttackType, Element)
│   └── gen/                   # Weapon/armour generation utilities
│       ├── weapon/            # Weapon profile and physics calculations
│       │   ├── parts/         # Weapon head geometries (Blade, AxeHead, etc.)
│       │   └── LethalityGenerationUtil.java
│       ├── attack/            # Attack type configurations
│       └── armour/            # Armour generation
├── event/                     # Forge event handlers
│   └── CommonEvent.java       # Registry creation, attribute modification
├── mixin/                     # Mixin classes
│   ├── attack_injectors/      # Inject Skada damage into attacks
│   ├── new_features/          # Add new features to vanilla classes
│   └── vanilla_rework/        # Remove/modify vanilla mechanics
├── network/                   # Network packets
│   ├── SkadaNetwork.java      # SimpleChannel setup
│   ├── client_to_server/      # C2S packets
│   └── server_to_client/      # S2C packets
└── util/                      # Utility classes
    ├── Util.java              # General utilities (1000+ lines)
    ├── PhysicsUtil.java       # Physics calculation helpers
    └── ColourLibrary.java     # Color constants for elements
```

## Key Classes

### Entry Points
- `Skada.java` - Main mod class with `@Mod(Skada.MODID)` annotation
- `SkadaRegistry.java` - Registers attack types, elements, and attributes
- `CommonEvent.java` - Creates custom registries on `NewRegistryEvent`

### Damage System
- `DamageHandler.java` - Subscribes to `LivingHurtEvent` at `LOWEST` priority
- `SkadaDamageSource.java` - Wraps vanilla `DamageSource` with `DamageInfo`
- `WeaponInfo.java` - Contains attack type map and element spread for weapons

### Physics System
- `WeaponProfile.java` - Describes weapon geometry (handle + weapon heads)
- `WeaponHead.java` - Abstract base for weapon head geometries
- `LethalityGenerationUtil.java` - Calculates lethality from physics properties
- `PhysicsUtil.java` - Angular velocity, moment of inertia calculations

### Registries
- `AttackType.java` - Record with name, lethality function, generator config, resist attribute
- `Element.java` - Elemental damage type with affinity/resistance attributes

## Coding Conventions

### Package Organization
- Use `com.cwjn.skada` as the root package
- Group related classes in subpackages (e.g., `data.damage`, `data.gen.weapon`)
- Client-only code goes in `client/` package

### Annotations
- Use `@Mod.EventBusSubscriber` for event subscription classes
- Use `@SubscribeEvent` for event handler methods
- Use `@OnlyIn(Dist.CLIENT)` for client-only methods
- Use `@Mixin` classes in the `mixin/` package

### Codecs
- Use Mojang's `Codec` API for serialization (not Gson directly)
- Define `public static final Codec<T> CODEC` in data classes
- Use `RecordCodecBuilder.create()` for complex codecs

### Configuration
- Use Forge's `ForgeConfigSpec` for configuration
- Server config in `CommonConfig.java`, client in `ClientConfig.java`

### Registries
- Use `DeferredRegister` for Forge registries
- Register on mod event bus in main class constructor

### Mixins
- Organize by purpose: `attack_injectors/`, `new_features/`, `vanilla_rework/`
- Use `@ModifyArg`, `@Inject`, `@Redirect` as appropriate
- Set appropriate `priority` for load order conflicts

## Physics System Guidelines

### Unit System
All physics calculations use:
- **Length**: centimeters (cm)
- **Mass**: grams (g)
- **Density**: g/cm³
- **Tolerance**: EPSILON = 1e-3

### Weapon Geometry
Refer to `.github/physics_and_geometry_instructions/` for detailed specs:
- `geometry.md` - Coordinate system, bevel math, cross-section handling
- `physics.md` - Volume, center of mass, moment of inertia calculations
- `heads/*.md` - Specific geometry for each weapon head type

### Key Formulas
- **Angular Velocity**: `ω = √(2τ / I)` where τ = torque, I = moment of inertia
- **Angular Momentum**: `L = I × ω`
- **Modified Superellipse Area**: `K = r / (r + 1)` for bevel curves

### Weapon Head Types
- `Blade` - Inline orientation, slashing/thrusting
- `AxeHead` - Perpendicular orientation, slashing/striking
- `PickHead` - Perpendicular orientation, thrusting
- `MaceHead` - Perpendicular orientation, striking
- `SickleHead` - Curved blade, slashing
- `ShovelHead` - Flat head, striking

## Testing

### Unit Tests
Located in `src/test/java/com/cwjn/skada/`
- Run with `./gradlew test`
- Use JUnit 5 assertions
- Focus on physics calculations and codec serialization

### In-Game Testing
Use `/skada` commands:
- `/skada get weaponInfo` - Show held weapon's Skada info
- `/skada get mobInfo <entity>` - Show mob's Skada stats
- `/skada generate weapons <namespace>` - Generate weapon configs

### Debug Mode
Set `SkadaData.DEBUG_ENABLED = true` for verbose damage logging in console.

## Build Commands

```bash
# Build the mod
./gradlew build

# Run client
./gradlew runClient

# Run server
./gradlew runServer

# Run data generation
./gradlew runData

# Run tests
./gradlew test
```

## Network Protocol

### Packet Naming Convention
- `S2C*` - Server to Client packets
- `C2S*` - Client to Server packets

### Standard Packet Structure
```java
public class MyPacket {
    public static void encode(MyPacket msg, FriendlyByteBuf buf) { ... }
    public static MyPacket decode(FriendlyByteBuf buf) { ... }
    public static void handle(MyPacket msg, Supplier<NetworkEvent.Context> ctx) { ... }
}
```

## Resource Loading

### JSON Configuration Files
Stored in `config/skada/`:
- `weapons/` - Weapon info JSON files
- `armour/` - Armour info JSON files
- `mobs/` - Mob stat JSON files

### Data-driven Reload
Weapon/armour/mob info reloads on:
- `ServerAboutToStartEvent`
- `OnDatapackSyncEvent` (datapack reload)

## Common Pitfalls

1. **Side Safety**: Always check `@OnlyIn(Dist.CLIENT)` for client code
2. **Registry Timing**: Register attributes before entities need them
3. **Mixin Priority**: Use higher priority (1100+) when overriding vanilla behavior
4. **Codec Optional Fields**: Use `.optionalFieldOf("name", defaultValue)`
5. **Physics Units**: Always use cm/g/g-cm³ - other docs may say mm incorrectly

## Dependencies

- **Spark** (optional) - Performance profiling
- **Simply Swords** (optional) - Additional weapon compatibility
- **Architectury API** - Cross-loader compatibility layer
- **Cloth Config** - Configuration UI

## Contributing

1. Follow existing code style and package organization
2. Add appropriate `@OnlyIn` annotations for client code
3. Use Mojang Codecs for any serializable data
4. Document physics calculations with comments explaining formulas
5. Write unit tests for new physics/calculation code
