# Project Rules & AI/IDE Instructions

## 1. Project Identity

| Field | Value |
|-------|-------|
| Project | Arcadia LootBox |
| Mod ID | arcadialootbox |
| Package | com.arcadia.lootbox |
| Tech Stack | Java 21, NeoForge 21.1.42, Minecraft 1.21.1 |
| Author | vyrriox |
| License | All Rights Reserved |
| Dependencies | arcadia-lib 1.2.0+ |

## 2. Git Workflow

- **main**: production-ready code
- Commit format: `type: descriptive message`
- Types: feat, fix, refactor, docs, build, chore, perf
- Push after every meaningful change

## 3. Code Conventions

- **Language**: All code, variables, logs in English
- **Package**: `com.arcadia.lootbox.*`
- **Naming**: PascalCase (classes), camelCase (methods/fields), SCREAMING_SNAKE (constants)
- **Comments**: English only, minimalist
- **No wildcard imports**
- **Thread safety**: Use ConcurrentHashMap, volatile, SchedulerService for async

## 4. Project Structure

```
src/main/java/com/arcadia/lootbox/
├── ArcadiaLootbox.java        — Main mod class
├── command/
│   └── LootboxCommands.java   — All /arcadia_lootbox commands
├── config/
│   └── LootboxConfig.java     — Global TOML config
├── data/
│   └── LootboxDefinition.java — Per-lootbox JSON data model
├── item/
│   ├── KeyRegistry.java       — 50 key item registrations
│   └── LootboxKeyItem.java    — Key item base class
├── manager/
│   ├── LootboxManager.java    — Config loading & caching
│   ├── HistoryManager.java    — Opening history tracking
│   └── UsageTracker.java      — Per-block usage counting
├── menu/
│   └── PreviewMenu.java       — Server-side preview GUI
├── network/
│   ├── LootboxNet.java        — Packet registration
│   ├── S2COpenLootboxHub.java — Open hub packet
│   └── S2CSyncLootboxList.java— Sync lootbox list packet
├── client/
│   ├── ClientEvents.java      — Hub card registration
│   ├── LootboxClientData.java — Client-side data cache
│   └── LootboxHubScreen.java  — Hub screen (ArcadiaTheme)
└── util/
    └── LootHelper.java        — Core lootbox logic
```

## 5. Adding a New Feature

1. Create a feature branch from main
2. Implement in the appropriate package
3. Use arcadia-lib utilities (ArcadiaMessages, CooldownManager, etc.)
4. Update LootboxDefinition if new config fields needed
5. Add translations to en_us.json and fr_fr.json
6. Update CHANGELOG.md (English + French)
7. Test in dev environment
8. Commit and push

## 6. Testing Checklist

- [ ] Mod loads without errors
- [ ] All 50 keys register correctly
- [ ] Weighted lootbox: multiple items can drop
- [ ] Guaranteed lootbox: exactly 1 pool item + guaranteed item
- [ ] Preview GUI shows correct info
- [ ] Commands all work with tab-completion
- [ ] Hub screen opens and displays cards
- [ ] Config reload works (sync + async)
- [ ] Cooldowns prevent rapid opening
- [ ] History records openings correctly

## 7. Environment Setup

```bash
git clone <repo>
cd Arcadia-LootBox
# Ensure libs/arcadia-lib-1.2.0.jar exists
./gradlew build
./gradlew runServer
```

## 8. AI Assistant Instructions

1. Package is `com.arcadia.lootbox` — never use `com.vyrriox`
2. All commands start with `/arcadia_lootbox`
3. Use arcadia-lib APIs: ArcadiaMessages, CooldownManager, SchedulerService, ItemBuilder
4. Key items are registered in KeyRegistry — never use KubeJS
5. Two lootbox types must be maintained: "weighted" and "guaranteed"
6. Thread safety is critical — use ConcurrentHashMap and volatile
7. Never change version without explicit request
8. Maintain bilingual documentation (EN + FR)
