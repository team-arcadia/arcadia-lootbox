# Project Rules & AI/IDE Instructions

> This file is the single source of truth for any developer, AI assistant, or IDE working on this project.
> Read this ENTIRELY before making any change.

---

## 1. Project Identity

| Field | Value |
|---|---|
| **Project** | Arcadia LootBox |
| **Mod ID** | `arcadialootbox` |
| **Package** | `com.arcadia.lootbox` |
| **Mod Loader** | NeoForge 21.1+ |
| **Minecraft** | 1.21.1 |
| **Java** | 21 |
| **Author** | vyrriox |
| **License** | LGPL-3.0-or-later |
| **Dependency** | Arcadia Lib (`com.arcadia.lib`) in `libs/arcadia-lib-*.jar` |

---

## 2. Git Workflow

### Branch Strategy

```
main        <- Production. Tagged releases only. NEVER push directly.
staging     <- QA / testing. Merge develop here when ready to test.
develop     <- Daily work. All features and fixes merge here first.
hotfix      <- Emergency patches. Branch from main, merge back to main + develop.
feat/*      <- Feature branches. Branch from develop.
fix/*       <- Bug fix branches. Branch from develop.
```

### Rules

- **NEVER push directly to `main`.** Always go through `develop` -> `staging` -> `main`.
- **NEVER force push** (`--force`) on any shared branch.
- **Feature branches** must be named `feat/<short-description>` (e.g. `feat/daily-lootbox`).
- **Fix branches** must be named `fix/<short-description>` (e.g. `fix/npe-preview`).
- **Hotfix** is for critical production bugs only. Branch from `main`, fix, merge to `main` AND `develop`.
- **Delete feature/fix branches** after merge.

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add free lootbox timer system
fix: NPE when opening preview GUI
refactor: simplify LootboxManager reload logic
perf: batch particle sending
docs: update README commands section
chore: update CI workflow
```

- Present tense, lowercase, no period at end.
- First line max 72 characters.
- Reference issues: `fix: resolve crash on open (#12)`

### Releasing

```bash
# 1. Merge develop -> staging (test)
# 2. Merge staging -> main (release)
# 3. Tag and push:
git tag v1.2.1
git push origin v1.2.1
# GitHub Actions auto-builds and publishes the release.
```

---

## 3. Code Conventions

### Language Policy

| Context | Language |
|---|---|
| Code (variables, methods, classes) | **English** |
| Comments | **English**, minimal |
| UI text / chat messages | **English + French** via `LanguageHelper` |
| Documentation (README, CHANGELOG) | **English first, then French** |
| Git commits | **English** |

### Naming

| Element | Convention | Example |
|---|---|---|
| Classes | `PascalCase` | `LootboxManager` |
| Methods / fields | `camelCase` | `handleLootboxAttempt()`, `cooldownTicks` |
| Constants | `UPPER_SNAKE_CASE` | `COOLDOWN_PREFIX` |
| Packages | `lowercase` | `com.arcadia.lootbox.manager` |

### Architecture Rules

- **Use Arcadia Lib APIs** whenever possible:
  - `ItemBuilder` for all `ItemStack` creation (never manual `DataComponents`)
  - `ArcadiaMessages` for all chat messages (consistent prefix/styling)
  - `MessageHelper` for titles/action bar
  - `CooldownManager` for per-player cooldowns
  - `SchedulerService` for delayed/repeating tasks
  - `TextFormatter` for formatting numbers/durations
  - `PermissionHelper` (local) for soft LuckPerms integration

- **Thread Safety**:
  - Use `ConcurrentHashMap` for shared maps
  - Use `synchronized` blocks for `ArrayDeque` access
  - Atomic map swap on reload (volatile reference)
  - NEVER block the server thread with I/O

- **Lootbox Types**:
  - `weighted` — each item rolls independently with its own chance %
  - `guaranteed` — one item picked from pool (weighted random) + guaranteed item

- **Free Lootbox Timer**:
  - Per-lootbox configurable (`freeEnabled`, `freeCooldownHours`)
  - Reducible cooldown with permission (`freeReducedPermission`)
  - Persistent across restarts (`free_claims.json`)

- **Localization**:
  - ALL user-facing text MUST go through `LanguageHelper`
  - Every key MUST have both EN and FR translations
  - Language detected via `player.clientInformation().language()`

### What NOT To Do

- Do NOT modify Arcadia Lib source code without explicit permission
- Do NOT add new dependencies without discussion
- Do NOT use `new Random()` — use `level.random` or `RandomSource`
- Do NOT store sensitive data in code or config committed to git
- Do NOT increment version numbers unless explicitly asked
- Do NOT use KubeJS for keys — they are registered in `KeyRegistry`

---

## 4. Project Structure

```
src/main/java/com/arcadia/lootbox/
  |
  +-- ArcadiaLootbox.java            # Entry point (@Mod). Event registration.
  |
  +-- client/                         # CLIENT-SIDE ONLY (Dist.CLIENT)
  |   +-- ClientEvents.java           # Hub card registration
  |   +-- LootboxClientData.java      # Client-side data cache
  |   +-- LootboxHubScreen.java       # Steampunk hub browser (ArcadiaTheme)
  |
  +-- command/
  |   +-- LootboxCommands.java        # ALL commands under /arcadia_lootbox
  |                                    # Includes: give, giveall, givekey, reload,
  |                                    # list, listkeys, info, create, delete,
  |                                    # preview, history, clearhistory, setuses,
  |                                    # resetcooldown, free, freetimer, resetfree,
  |                                    # stats, hub
  |
  +-- config/
  |   +-- LootboxConfig.java          # Global TOML config (35+ params)
  |
  +-- data/
  |   +-- LootboxDefinition.java      # Per-lootbox JSON data model (record)
  |
  +-- item/
  |   +-- KeyRegistry.java            # 50 key item registrations (DeferredRegister)
  |   +-- LootboxKeyItem.java         # Key item base class
  |
  +-- manager/
  |   +-- LootboxManager.java         # Config loading, validation, caching
  |   +-- FreeLootboxManager.java     # Free claim timer persistence
  |   +-- HistoryManager.java         # Opening history tracking
  |   +-- UsageTracker.java           # Per-block usage counting (NBT)
  |
  +-- menu/
  |   +-- PreviewMenu.java            # Server-side preview GUI (ChestMenu 9x6)
  |
  +-- network/
  |   +-- LootboxNet.java             # Packet registration
  |   +-- S2COpenLootboxHub.java      # Open hub screen packet
  |   +-- S2CSyncLootboxList.java     # Sync lootbox list packet
  |
  +-- util/
      +-- LootHelper.java             # Core lootbox logic (open, give, broadcast)
      +-- PermissionHelper.java        # Soft LuckPerms wrapper
      +-- LanguageHelper.java          # EN/FR translation map

src/main/resources/
  +-- META-INF/neoforge.mods.toml    # Mod metadata + dependencies
  +-- pack.mcmeta                     # Resource pack metadata
  +-- assets/arcadialootbox/
      +-- lang/en_us.json             # Item/block translations (EN)
      +-- lang/fr_fr.json             # Item/block translations (FR)
      +-- models/item/*.json          # 50 key item models
      +-- textures/item/*.png         # 50 key textures (16x16)
```

---

## 5. Adding a New Feature (Step by Step)

```
1. git checkout develop
2. git pull origin develop
3. git checkout -b feat/my-feature
4. Write code following conventions above
5. Add EN + FR translations in LanguageHelper.java
6. Test locally: ./gradlew build && test in-game
7. git add . && git commit -m "feat: description"
8. git push origin feat/my-feature
9. Open PR: feat/my-feature -> develop
10. After review: merge, delete branch
```

---

## 6. Adding a New Command

1. Add the command in `LootboxCommands.register()` under the `arcadia_lootbox` literal
2. Use `PermissionHelper.hasPermission()` for permission checks
3. Use `source.hasPermission(2)` for op-level checks
4. Add lootbox ID suggestions with `SUGGEST_IDS` provider
5. Use `ArcadiaMessages.success/error/info()` for feedback
6. Add translations in `LanguageHelper` (both EN and FR maps)

---

## 7. Adding a New Key Category

1. Define tier names and rarities in `KeyRegistry`
2. Call `registerCategory()` in the static block
3. Generate textures (16x16 PNG) in `assets/arcadialootbox/textures/item/`
4. Generate item models in `assets/arcadialootbox/models/item/`
5. Add translations in `en_us.json` and `fr_fr.json`

---

## 8. Testing Checklist

Before submitting any PR:

- [ ] `./gradlew build` passes with zero errors
- [ ] Tested in singleplayer
- [ ] Tested on dedicated server (if applicable)
- [ ] All new text has EN + FR translations via LanguageHelper
- [ ] Thread-safe for shared state
- [ ] Commit messages follow conventions
- [ ] No hardcoded strings in user-facing messages

---

## 9. Environment Setup

```bash
# Clone
git clone https://github.com/Team-Arcadia/Arcadia-LootBox.git
cd Arcadia-LootBox

# The arcadia-lib JAR is in libs/ (committed to repo)

# Build
./gradlew build

# Run client (dev)
./gradlew runClient

# Run server (dev)
./gradlew runServer
```

### IDE Setup

- **IntelliJ IDEA**: Import as Gradle project. Run `./gradlew genIntellijRuns`.
- **Eclipse**: Run `./gradlew genEclipseRuns`.
- **VS Code**: Install Java Extension Pack. Open folder. Gradle tasks in terminal.

---

## 10. AI Assistant Instructions

If you are an AI (Claude, ChatGPT, Copilot, Cursor, etc.) working on this project:

1. **Read this file first.** Do not guess conventions.
2. **Never modify Arcadia Lib** unless explicitly told to.
3. **Never push to main directly.** Work on `develop` or feature branches.
4. **Never force push** on any shared branch.
5. **Never increment version** unless the user asks for it.
6. **Always add EN + FR translations** for any new user-facing text via `LanguageHelper`.
7. **Always use Arcadia Lib APIs** (ItemBuilder, ArcadiaMessages, CooldownManager, etc.).
8. **Always use `PermissionHelper`** for permission checks (soft LuckPerms).
9. **Ask before making destructive changes** (deleting files, force push, changing architecture).
10. **Communicate in French** with the user, code in English.
