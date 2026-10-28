# Shelter Sleep

*Sleep only where you feel safe. A Fabric mod for Minecraft 1.21.11.*

In vanilla Minecraft you can place a bed anywhere and skip the night in two seconds. **Shelter Sleep** makes sleeping more immersive: the bed must be inside a proper shelter.

## Rules

The bed must be inside an enclosed area that has:

| Requirement | Message |
|---|---|
| A roof over the room | *I need a roof* |
| Enough free space (default 18 blocks, e.g. a 3×3 floor, 2 high) | *I need more space* |
| Walls closing the area (windows and small gaps are allowed) | *I need walls* |
| At least one door | *I need a door* |

Windows are fine: by default up to 25% of the walls can be open. Holes in the roof are not allowed unless you change the setting.

## Configuration

With [Mod Menu](https://modrinth.com/mod/modmenu): **Mods → Shelter Sleep → Configure**. Options: require a roof, require a door, minimum room size, allowed wall openings (%), allowed roof holes (%), ignore rules in Creative, and the four messages. Settings are stored in `config/shelter_sleep.json`.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.1 or newer
- Fabric API
- Mod Menu (optional)

Server-side mod: install it on the server, players do not need it. MidnightLib is bundled.

## Building from source

You need JDK 21.

```
./gradlew build          (Windows: gradlew.bat build)
```

The jar is created in `build/libs/` (use the one without `-sources`).

### Logic tests

The room-detection logic (`ShelterCheck.java`) has no Minecraft dependency and is tested with simulated rooms:

```
javac -d out src/main/java/com/sheltersleep/ShelterCheck.java tests/ShelterCheckTest.java
java -cp out ShelterCheckTest
```

## Project layout

- `ShelterCheck.java`: room detection logic (pure Java)
- `McBlocks.java`: connects the logic to Minecraft blocks
- `ShelterSleep.java`: hooks the sleeping event
- `ShelterConfig.java`, `ModMenuIntegration.java`: settings and Mod Menu screen

## Development

Written by [your name] with the assistance of AI (Claude, by Anthropic), which produced most of the code and the documentation text from my design and testing. [Edit this paragraph so it accurately describes your own contribution.]

## License

See the `LICENSE` file.

## Credits

Config screen powered by [MidnightLib](https://modrinth.com/mod/midnightlib) (MIT).
