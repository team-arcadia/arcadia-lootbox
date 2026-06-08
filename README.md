# Arcadia LootBox

[Consult the full CurseForge description](./CURSEFORGE_PAGE.md)

Arcadia LootBox is a NeoForge Minecraft mod that adds a complete, data-driven crate system for servers and singleplayer. Players collect themed keys, right-click matching shulker-box "lootboxes", and roll randomized rewards. Lootboxes are defined entirely in JSON — no Java, no KubeJS scripts, no datapacks — and admins manage everything through a single `/arcadia_lootbox` command tree. A polished Hub UI browses every lootbox on the server, and a Preview menu shows exactly what each one drops before you spend a key.

## Features

- **Data-driven JSON lootboxes** — Every crate is one JSON file in `config/arcadia/arcadialootbox/`. Hot-reload with `/arcadia_lootbox reload` (async, no tick freeze).
- **Two drop modes** — `weighted` (each item rolls independently with its own chance %) or `guaranteed` (one weighted pick from the pool + one always-given item).
- **50 themed key items** — Dungeon, Shop, Vote, Lootable, Event and Boss families with up to 10 tiers each, custom textures and bilingual tooltips. Defined in `KeyRegistry`; not modifiable by config.
- **Hub UI with collapsible categories** — Steampunk-styled client screen; categories fold by default, click a header to expand. Expand-all / collapse-all controls keep large key collections readable.
- **Preview menu with rarity filters** — Single clickable action row, 28 items per page, filter chips by rarity, multi-draw (left = 1, right = all your keys, shift = up to 10).
- **Right-click in air with a key** — Opens the matching lootbox preview directly (or the Hub if several lootboxes share the key).
- **Free timed lootboxes** — Per-lootbox free claims with configurable cooldown (72 h default, 48 h with `freeReducedPermission`). Persistent across restarts via `free_claims.json`, auto-saved every 5 min.
- **Soft LuckPerms integration** — `PermissionHelper` checks LuckPerms when present and silently falls back to vanilla OP checks when it isn't. Per-lootbox `permission` node, `freePermission`, `freeReducedPermission` all supported.
- **Server-safe** — `ClientEvents` and `LootboxHubScreen` are isolated under `Dist.CLIENT`; the dedicated server starts cleanly with no missing-class errors.
- **Hardened** — Anti-autoclicker (rate-limited rapid clicks), per-lootbox cooldowns, `maxUses` per placed block (NBT-persisted), `BULK_OPEN_LIMIT = 10` hard cap, strict slot-id bounds in the menu, 75 ms anti-spam on filter clicks, 250 ms anti-spam on key right-click-in-air, null-safe registry lookups, tooltip string sanitization.
- **Bilingual UI** — Every user-facing string flows through `LanguageHelper` (EN + FR), language detected per-player via `clientInformation().language()`.

## How a lootbox works

```
                   1. Player right-clicks the placed lootbox with the matching key
                   2. Server runs LootHelper.handleLootboxAttempt
                            |
                            v
       +---------------- permission check (per-lootbox + LuckPerms / OP) ----------------+
       |                                                                                  |
       |  passes                                                                          |
       v                                                                                  |
       sneak check  ->  key search (main hand, offhand, full inventory)                   |
       |                                                                                  |
       v                                                                                  |
       per-lootbox cooldown  ->  anti-autoclicker  ->  maxUses check (NBT on block)       |
       |                                                                                  |
       v                                                                                  |
       openLootboxLogic:                                                                  |
         - "weighted": each LootEntry rolls vs. its chance, all matches drop              |
         - "guaranteed": ONE weighted-random pick + the always-given guaranteedItem       |
         - command rewards roll independently                                             |
         - XP, particles, title animation, optional rare-drop broadcast                   |
         - consume key (skipped in creative)                                              |
         - increment block usage, destroy if destroyOnOpen or maxUses reached             |
         - record opening in history                                                      |
                                                                                          |
       fails at any step  ----->  localized chat error, no key consumed  ----------------+
```

Multi-draw routes the same way: `LootHelper.handleBulkLootboxAttempt` loops up to 10 times, re-checking key availability and `maxUses` each iteration, and sets the per-lootbox cooldown once at the end.

## Creating a lootbox

Drop a new `.json` file into `config/arcadia/arcadialootbox/`. The file name becomes the lootbox ID (e.g. `treasure_chest.json` → `/arcadia_lootbox give @p treasure_chest`). Two starter examples (`example_weighted.json`, `example_guaranteed.json`) and a `README.txt` cheat sheet are generated automatically on first launch.

### Minimal weighted example

```json
{
  "displayName": "Treasure Chest",
  "color": "yellow",
  "keyItem": "arcadialootbox:shop_key_rare",
  "rarity": "rare",
  "type": "weighted",
  "broadcastRare": true,
  "lootTable": [
    { "item": "minecraft:diamond",   "minCount": 1, "maxCount": 3, "chance": 0.3,  "rarity": "rare",     "displayName": "Diamond",    "broadcast": true  },
    { "item": "minecraft:gold_ingot","minCount": 2, "maxCount": 5, "chance": 0.6,  "rarity": "uncommon", "displayName": "Gold Ingot", "broadcast": false },
    { "item": "minecraft:iron_ingot","minCount": 5, "maxCount": 10,"chance": 1.0,  "rarity": "common",   "displayName": "Iron Ingot", "broadcast": false }
  ],
  "particles":  ["minecraft:flame", "minecraft:happy_villager"],
  "openSound":  "minecraft:block.chest.open",
  "openMessage":"§aLootbox opened!"
}
```

Behavior: each entry of `lootTable` rolls **independently** against its `chance` (`0.0–1.0`). With the values above, on a single open the player could get 0 diamonds + 0 gold + 5 iron, or all three at once. `broadcast: true` on a single entry forces a server-wide message when it drops; `broadcastRare: true` at the top level triggers a broadcast whenever an entry's `rarity` meets the global broadcast threshold (`lootbox.toml > Broadcast`).

### Guaranteed example with free timer

```json
{
  "displayName": "Lucky Box",
  "color": "lime",
  "keyItem": "arcadialootbox:vote_key_common",
  "rarity": "uncommon",
  "type": "guaranteed",
  "guaranteedItem": "minecraft:bread",
  "guaranteedMinCount": 1,
  "guaranteedMaxCount": 3,
  "lootTable": [
    { "item": "minecraft:diamond",   "minCount": 1, "maxCount": 1, "chance": 0.05, "rarity": "legendary", "displayName": "Diamond" },
    { "item": "minecraft:emerald",   "minCount": 1, "maxCount": 3, "chance": 0.15, "rarity": "rare",      "displayName": "Emerald" },
    { "item": "minecraft:gold_ingot","minCount": 2, "maxCount": 5, "chance": 0.30, "rarity": "uncommon",  "displayName": "Gold Ingot" },
    { "item": "minecraft:iron_ingot","minCount": 3, "maxCount": 8, "chance": 0.50, "rarity": "common",    "displayName": "Iron Ingot" }
  ],
  "freeEnabled": true,
  "freeCooldownHours": 72,
  "freeReducedCooldownHours": 48,
  "freeReducedPermission": "arcadialootbox.free.reduced",
  "experienceReward": 5,
  "openMessage": "§aYou got something!"
}
```

Behavior: the player always receives 1–3 bread (`guaranteedItem`) **and** exactly one entry from `lootTable`, picked with `chance` used as a **weight** (higher = more likely). With these weights the player rolls iron ~50 % of the time, gold ~30 %, emerald ~15 %, diamond ~5 % — proportional to the sum of weights. The lootbox is also claimable for free every 72 h (48 h for players with the `arcadialootbox.free.reduced` LuckPerms node).

### Full field reference

| Field | Type | Default | What it does |
|---|---|---|---|
| `displayName` | string | `"Lootbox"` | Human name shown in tooltips, titles, broadcasts. |
| `displayNameFR` | string | `""` | Optional French override. Falls back to `displayName`. |
| `color` | string | `"white"` | Dye color of the shulker-box. `"random"` picks a random color per stack. |
| `keyItem` | resource id | `"minecraft:tripwire_hook"` | Item the player must hold to open the lootbox. Usually one of the 50 registered keys. |
| `rarity` | string | `"common"` | One of `common, uncommon, rare, epic, legendary, mythic`. Drives color, broadcast threshold, sort order. |
| `type` | string | `"weighted"` | `"weighted"` or `"guaranteed"`. |
| `lootTable` | array | `[]` | Entries with `item, minCount, maxCount, chance (or weight in guaranteed mode), rarity, displayName, broadcast`. |
| `guaranteedItem` | resource id | `""` | (Guaranteed mode) Always-given item. |
| `guaranteedMinCount` / `guaranteedMaxCount` | int | `1 / 1` | (Guaranteed mode) Count range for the always-given item. |
| `permission` | string | `""` | LuckPerms node required to open. Empty = no check. |
| `cooldownTicks` | int | `20` | Per-player cooldown between two opens (20 = 1 s). |
| `maxUses` | int | `-1` | Hard cap of opens per placed block. `-1` = unlimited. |
| `destroyOnOpen` | bool | `false` | Destroy the placed block after the first open. |
| `requireSneakToOpen` | bool | `false` | Force shift + right-click to open. |
| `broadcastRare` | bool | `false` | Auto-broadcast drops whose `rarity` meets the global threshold. |
| `broadcastMessage` | string | `""` | Custom broadcast format. Empty = default `§6⚙ §d✦ <player> found <rarity> <count>x <item> in <lootbox>` message. |
| `openSound` / `closeSound` | resource id | chest open / `""` | Sounds played on opening / GUI close. |
| `openMessage` | string | `""` | Action-bar message on open. `openMessageFR` for the French variant. |
| `openTitle` / `openSubtitle` | string | auto | Title + subtitle shown on open. FR variants supported. |
| `particles` | array | `["minecraft:flame"]` | Particle types spawned on open. |
| `animation` | object | defaults | `{ type, durationTicks, particleCount, particleSpeed, particleRadius, particleType, playTitleAnimation }`. |
| `commandRewards` | array | `[]` | `{ command: "give {player} ...", chance: 0.0–1.0, asConsole: true }` — `{player}` is replaced safely. |
| `experienceReward` | number | `0` | XP awarded on open. |
| `freeEnabled` | bool | `false` | Allow the player to claim this lootbox for free on a timer. |
| `freeCooldownHours` | int | `72` | Free-claim cooldown in hours. |
| `freeReducedCooldownHours` | int | `48` | Reduced cooldown granted by `freeReducedPermission`. |
| `freePermission` / `freeReducedPermission` | string | `""` | LuckPerms nodes for the free-claim feature. |
| `requiredBiome` / `requiredLevel` | string / int | `""` / `0` | Optional gating for the open attempt. |
| `logOpening` | bool | `false` | Log every open of this lootbox to the server log. |
| `sortOrder` | int | `0` | Display order in the Hub. |
| `giveToInventoryOnly` | bool | `false` | Force rewards into inventory only (no ground drop). |

After editing any JSON, run `/arcadia_lootbox reload` — the reload is async, so the tick loop doesn't stall.

## Commands

All commands live under `/arcadia_lootbox` and require OP level 2 by default. Use tab completion to discover them.

| Command | What it does |
|---|---|
| `give <player> <id> [amount]` | Spawn the lootbox shulker-box in a player's inventory. |
| `giveall <id> [amount]` | Same, but for every online player. |
| `givekey <player> <key_id> [amount]` | Spawn a key item. |
| `reload` | Reload all JSON definitions (async). |
| `list` / `listkeys` | List loaded lootboxes / registered keys. |
| `info <id>` | Detailed info for a lootbox. |
| `create <id> <name>` / `delete <id>` | Create a starter template / remove a definition. |
| `preview <player> <id>` | Open the preview GUI for a player. |
| `history <player>` / `clearhistory <player>` | View / wipe the open history of a player. |
| `setuses <pos> <uses>` | Override the usage count on a placed block. |
| `resetcooldown <player>` | Clear all per-lootbox cooldowns for a player. |
| `free <player> <id>` / `freetimer <player> <id>` / `resetfree <player> [id]` | Manage the free-claim timer. |
| `stats` / `hub` | Show global stats / open the Hub UI. |

## Requirements

| Dependency | Version |
|------------|---------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219+ |
| Java | 21 |
| Arcadia Lib | 1.2.13+ (bundled in the jar) |
| LuckPerms | optional (soft integration) |

## Installation

1. Place `ArcadiaLootbox-1.2.5.jar` in your `mods/` folder. Arcadia Lib is bundled inside.
2. (Optional) Install [LuckPerms](https://luckperms.net/) for permission-based features.
3. Start the server. On first launch, the mod creates `config/arcadia/arcadialootbox/` with two example lootboxes and a `README.txt` cheat sheet.
4. Edit the example JSON files or add your own. Run `/arcadia_lootbox reload` to apply changes live.

## Documentation

- [CHANGELOG.md](CHANGELOG.md) — Version history.
- [RULES.md](RULES.md) — Project conventions, architecture, AI assistant guidelines.
- [CURSEFORGE_PAGE.md](CURSEFORGE_PAGE.md) — Long-form CurseForge description.

## Credits

Author: vyrriox
Organization: Team Arcadia
License: LGPL-3.0-or-later — see [LICENSE](LICENSE). Forks and derivative works are welcome under the same license, provided you credit "vyrriox / Team Arcadia" and link back to the upstream repository.
Discord: [discord.gg/xjF8Rtzyd4](https://discord.gg/xjF8Rtzyd4)
Website: [arcadia-echoes-of-power.fr](https://arcadia-echoes-of-power.fr/)

---

# Arcadia LootBox (Version Française)

[Consulter la description CurseForge complète](./CURSEFORGE_PAGE.md)

Arcadia LootBox est un mod NeoForge pour Minecraft qui ajoute un système complet de crates data-driven pour serveurs et solo. Les joueurs collectent des clés thématiques, font clic-droit sur des « lootbox » (shulker-box posées) avec la clé correspondante, et tirent des récompenses aléatoires. Les lootbox sont entièrement définies en JSON — pas de Java, pas de scripts KubeJS, pas de datapacks — et les admins gèrent tout via un unique arbre de commandes `/arcadia_lootbox`. Une UI Hub soignée parcourt toutes les lootbox du serveur, et un menu Preview montre exactement ce que chacune drop avant de dépenser une clé.

## Caractéristiques

- **Lootbox JSON data-driven** — Chaque crate est un fichier JSON dans `config/arcadia/arcadialootbox/`. Rechargement à chaud via `/arcadia_lootbox reload` (asynchrone, sans freeze de tick).
- **Deux modes de drop** — `weighted` (chaque item tire indépendamment avec son propre %) ou `guaranteed` (un pick pondéré dans la pool + un item toujours donné).
- **50 clés thématiques** — Familles Donjon, Boutique, Vote, Trouvable, Événement, Boss avec jusqu'à 10 paliers chacune, textures custom et tooltips bilingues. Définies dans `KeyRegistry` ; pas modifiables par config.
- **UI Hub avec catégories repliables** — Écran client steampunk ; catégories repliées par défaut, clic sur l'en-tête pour déployer. Boutons Tout déployer / Tout replier pour garder lisibles les grosses collections de clés.
- **Menu Preview avec filtres de rareté** — Une seule rangée d'action cliquable, 28 objets par page, puces de filtre par rareté, multi-tirage (gauche = 1, droit = toutes vos clés, shift = jusqu'à 10).
- **Clic-droit dans le vide avec une clé** — Ouvre directement l'aperçu de la lootbox correspondante (ou le Hub si plusieurs lootbox partagent la clé).
- **Lootbox gratuites avec timer** — Réclamations gratuites par lootbox avec cooldown configurable (72 h par défaut, 48 h avec `freeReducedPermission`). Persistantes entre redémarrages via `free_claims.json`, sauvegarde auto toutes les 5 min.
- **Intégration LuckPerms souple** — `PermissionHelper` vérifie LuckPerms si présent, et retombe silencieusement sur les checks OP vanilla sinon. Node `permission` par lootbox, `freePermission`, `freeReducedPermission` tous supportés.
- **Sûr côté serveur** — `ClientEvents` et `LootboxHubScreen` sont isolés sous `Dist.CLIENT` ; le serveur dédié démarre proprement sans erreur de classe manquante.
- **Durci** — Anti-autoclicker, cooldowns par lootbox, `maxUses` par bloc posé (NBT-persisté), plafond dur `BULK_OPEN_LIMIT = 10`, bornes strictes sur les slot ids du menu, anti-spam 75 ms sur les clics de filtre, anti-spam 250 ms sur clic-droit en air, lookups de registry null-safe, sanitization des tooltips.
- **UI bilingue** — Chaque texte visible passe par `LanguageHelper` (EN + FR), langue détectée par joueur via `clientInformation().language()`.

## Comment fonctionne une lootbox

```
                   1. Le joueur clic-droit sur la lootbox posée avec la bonne clé
                   2. Le serveur exécute LootHelper.handleLootboxAttempt
                            |
                            v
       +-------------- check permission (par-lootbox + LuckPerms / OP) -----------------+
       |                                                                                  |
       |  passe                                                                           |
       v                                                                                  |
       check sneak  ->  recherche de clé (main, offhand, inventaire complet)              |
       |                                                                                  |
       v                                                                                  |
       cooldown par-lootbox  ->  anti-autoclicker  ->  check maxUses (NBT sur le bloc)    |
       |                                                                                  |
       v                                                                                  |
       openLootboxLogic :                                                                 |
         - "weighted" : chaque LootEntry tire selon son chance, tous les hits droppent    |
         - "guaranteed" : UN pick aléatoire pondéré + le guaranteedItem toujours donné    |
         - les command rewards tirent indépendamment                                      |
         - XP, particules, animation de titre, broadcast optionnel sur drop rare          |
         - consomme la clé (sauf en créatif)                                              |
         - incrémente l'usage du bloc, le détruit si destroyOnOpen ou maxUses atteint     |
         - enregistre l'ouverture dans l'historique                                       |
                                                                                          |
       échoue à toute étape  ----->  erreur localisée en chat, aucune clé consommée  ----+
```

Le multi-tirage suit le même chemin : `LootHelper.handleBulkLootboxAttempt` boucle jusqu'à 10 fois, re-vérifie à chaque tour la disponibilité de la clé et les `maxUses`, et applique le cooldown par-lootbox une seule fois en fin de série.

## Créer une lootbox

Déposez un nouveau fichier `.json` dans `config/arcadia/arcadialootbox/`. Le nom du fichier devient l'ID de la lootbox (ex. `treasure_chest.json` → `/arcadia_lootbox give @p treasure_chest`). Deux exemples (`example_weighted.json`, `example_guaranteed.json`) et une feuille de triche `README.txt` sont générés automatiquement au premier lancement.

### Exemple weighted minimal

```json
{
  "displayName": "Treasure Chest",
  "color": "yellow",
  "keyItem": "arcadialootbox:shop_key_rare",
  "rarity": "rare",
  "type": "weighted",
  "broadcastRare": true,
  "lootTable": [
    { "item": "minecraft:diamond",   "minCount": 1, "maxCount": 3, "chance": 0.3,  "rarity": "rare",     "displayName": "Diamant",  "broadcast": true  },
    { "item": "minecraft:gold_ingot","minCount": 2, "maxCount": 5, "chance": 0.6,  "rarity": "uncommon", "displayName": "Lingot d'or", "broadcast": false },
    { "item": "minecraft:iron_ingot","minCount": 5, "maxCount": 10,"chance": 1.0,  "rarity": "common",   "displayName": "Lingot de fer", "broadcast": false }
  ],
  "particles":  ["minecraft:flame", "minecraft:happy_villager"],
  "openSound":  "minecraft:block.chest.open",
  "openMessage":"§aLootbox ouverte !"
}
```

Comportement : chaque entrée de `lootTable` tire **indépendamment** contre son `chance` (`0.0–1.0`). Avec ces valeurs, sur une ouverture le joueur peut obtenir 0 diamant + 0 or + 5 fer, ou les trois d'un coup. `broadcast: true` sur une entrée force un message serveur global quand elle drop ; `broadcastRare: true` au top-level déclenche un broadcast dès qu'une entrée dont la `rarity` atteint le seuil global broadcast (`lootbox.toml > Broadcast`) drop.

### Exemple guaranteed avec timer gratuit

```json
{
  "displayName": "Lucky Box",
  "color": "lime",
  "keyItem": "arcadialootbox:vote_key_common",
  "rarity": "uncommon",
  "type": "guaranteed",
  "guaranteedItem": "minecraft:bread",
  "guaranteedMinCount": 1,
  "guaranteedMaxCount": 3,
  "lootTable": [
    { "item": "minecraft:diamond",   "minCount": 1, "maxCount": 1, "chance": 0.05, "rarity": "legendary", "displayName": "Diamant" },
    { "item": "minecraft:emerald",   "minCount": 1, "maxCount": 3, "chance": 0.15, "rarity": "rare",      "displayName": "Émeraude" },
    { "item": "minecraft:gold_ingot","minCount": 2, "maxCount": 5, "chance": 0.30, "rarity": "uncommon",  "displayName": "Lingot d'or" },
    { "item": "minecraft:iron_ingot","minCount": 3, "maxCount": 8, "chance": 0.50, "rarity": "common",    "displayName": "Lingot de fer" }
  ],
  "freeEnabled": true,
  "freeCooldownHours": 72,
  "freeReducedCooldownHours": 48,
  "freeReducedPermission": "arcadialootbox.free.reduced",
  "experienceReward": 5,
  "openMessage": "§aVous avez obtenu quelque chose !"
}
```

Comportement : le joueur reçoit toujours 1 à 3 pains (`guaranteedItem`) **et** exactement une entrée du `lootTable`, choisie avec `chance` utilisé comme **poids** (plus élevé = plus probable). Avec ces poids, le joueur tire du fer ~50 % du temps, de l'or ~30 %, des émeraudes ~15 %, du diamant ~5 % — proportionnel à la somme des poids. La lootbox est aussi réclamable gratuitement toutes les 72 h (48 h pour les joueurs avec le node LuckPerms `arcadialootbox.free.reduced`).

### Référence complète des champs

| Champ | Type | Défaut | Effet |
|---|---|---|---|
| `displayName` | string | `"Lootbox"` | Nom humain dans tooltips, titres, broadcasts. |
| `displayNameFR` | string | `""` | Override français optionnel. Sinon `displayName`. |
| `color` | string | `"white"` | Couleur de la shulker-box. `"random"` choisit aléatoirement par stack. |
| `keyItem` | id resource | `"minecraft:tripwire_hook"` | Item que le joueur doit tenir pour ouvrir. Généralement une des 50 clés intégrées. |
| `rarity` | string | `"common"` | Une de `common, uncommon, rare, epic, legendary, mythic`. Pilote couleur, seuil broadcast, ordre de tri. |
| `type` | string | `"weighted"` | `"weighted"` ou `"guaranteed"`. |
| `lootTable` | array | `[]` | Entrées avec `item, minCount, maxCount, chance (ou poids en guaranteed), rarity, displayName, broadcast`. |
| `guaranteedItem` | id resource | `""` | (Guaranteed) Item toujours donné. |
| `guaranteedMinCount` / `guaranteedMaxCount` | int | `1 / 1` | (Guaranteed) Plage de quantité de l'item garanti. |
| `permission` | string | `""` | Node LuckPerms requis. Vide = pas de check. |
| `cooldownTicks` | int | `20` | Cooldown par joueur entre deux ouvertures (20 = 1 s). |
| `maxUses` | int | `-1` | Plafond d'ouvertures par bloc posé. `-1` = illimité. |
| `destroyOnOpen` | bool | `false` | Détruit le bloc après la première ouverture. |
| `requireSneakToOpen` | bool | `false` | Force shift + clic-droit pour ouvrir. |
| `broadcastRare` | bool | `false` | Annonce auto les drops dont la `rarity` atteint le seuil global. |
| `broadcastMessage` | string | `""` | Format custom de broadcast. Vide = format par défaut. |
| `openSound` / `closeSound` | id resource | chest open / `""` | Sons à l'ouverture / à la fermeture du GUI. |
| `openMessage` | string | `""` | Message action-bar à l'ouverture. `openMessageFR` pour la VF. |
| `openTitle` / `openSubtitle` | string | auto | Titre + sous-titre à l'ouverture. Variantes FR supportées. |
| `particles` | array | `["minecraft:flame"]` | Types de particules au spawn. |
| `animation` | object | defaults | `{ type, durationTicks, particleCount, particleSpeed, particleRadius, particleType, playTitleAnimation }`. |
| `commandRewards` | array | `[]` | `{ command: "give {player} ...", chance: 0.0–1.0, asConsole: true }` — `{player}` remplacé de manière sûre. |
| `experienceReward` | number | `0` | XP donnée à l'ouverture. |
| `freeEnabled` | bool | `false` | Autorise la réclamation gratuite sur timer. |
| `freeCooldownHours` | int | `72` | Cooldown gratuit en heures. |
| `freeReducedCooldownHours` | int | `48` | Cooldown réduit accordé par `freeReducedPermission`. |
| `freePermission` / `freeReducedPermission` | string | `""` | Nodes LuckPerms pour la feature de claim gratuit. |
| `requiredBiome` / `requiredLevel` | string / int | `""` / `0` | Gating optionnel de l'ouverture. |
| `logOpening` | bool | `false` | Loggue chaque ouverture de cette lootbox dans les logs serveur. |
| `sortOrder` | int | `0` | Ordre d'affichage dans le Hub. |
| `giveToInventoryOnly` | bool | `false` | Force les rewards dans l'inventaire (jamais au sol). |

Après chaque édition de JSON, lancez `/arcadia_lootbox reload` — le rechargement est asynchrone, donc le tick loop ne bloque pas.

## Commandes

Toutes les commandes vivent sous `/arcadia_lootbox` et requièrent OP niveau 2 par défaut. Utilisez la tab-completion pour les découvrir.

| Commande | Effet |
|---|---|
| `give <joueur> <id> [quantité]` | Spawn la shulker-box lootbox dans l'inventaire d'un joueur. |
| `giveall <id> [quantité]` | Idem, pour tous les joueurs en ligne. |
| `givekey <joueur> <key_id> [quantité]` | Spawn une clé. |
| `reload` | Recharge toutes les définitions JSON (asynchrone). |
| `list` / `listkeys` | Liste les lootbox / les clés enregistrées. |
| `info <id>` | Infos détaillées d'une lootbox. |
| `create <id> <nom>` / `delete <id>` | Crée un template / supprime une définition. |
| `preview <joueur> <id>` | Ouvre le GUI preview pour un joueur. |
| `history <joueur>` / `clearhistory <joueur>` | Voir / vider l'historique d'ouverture d'un joueur. |
| `setuses <pos> <uses>` | Override le compteur d'usage d'un bloc posé. |
| `resetcooldown <joueur>` | Reset tous les cooldowns par-lootbox d'un joueur. |
| `free <joueur> <id>` / `freetimer <joueur> <id>` / `resetfree <joueur> [id]` | Gestion du timer de claim gratuit. |
| `stats` / `hub` | Stats globales / ouvre l'UI Hub. |

## Prérequis

| Dépendance | Version |
|------------|---------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.219+ |
| Java | 21 |
| Arcadia Lib | 1.2.13+ (incluse dans le jar) |
| LuckPerms | optionnel (intégration souple) |

## Installation

1. Placez `ArcadiaLootbox-1.2.5.jar` dans votre dossier `mods/`. Arcadia Lib est incluse dedans.
2. (Optionnel) Installez [LuckPerms](https://luckperms.net/) pour les fonctionnalités basées sur permissions.
3. Démarrez le serveur. Au premier lancement, le mod crée `config/arcadia/arcadialootbox/` avec deux lootbox d'exemple et une feuille de triche `README.txt`.
4. Éditez les JSON d'exemple ou ajoutez les vôtres. Lancez `/arcadia_lootbox reload` pour appliquer les changements en live.

## Documentation

- [CHANGELOG.md](CHANGELOG.md) — Historique des versions.
- [RULES.md](RULES.md) — Conventions du projet, architecture, règles pour les assistants IA.
- [CURSEFORGE_PAGE.md](CURSEFORGE_PAGE.md) — Description longue CurseForge.

## Credits

Auteur : vyrriox
Organisation : Team Arcadia
Licence : LGPL-3.0-or-later — voir [LICENSE](LICENSE). Les forks et travaux dérivés sont les bienvenus sous la même licence, à condition de créditer « vyrriox / Team Arcadia » et de pointer vers le dépôt d'origine.
Discord : [discord.gg/xjF8Rtzyd4](https://discord.gg/xjF8Rtzyd4)
Site web : [arcadia-echoes-of-power.fr](https://arcadia-echoes-of-power.fr/)
