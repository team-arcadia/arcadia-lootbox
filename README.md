<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=for-the-badge&logo=mojangstudios" alt="Minecraft 1.21.1"/>
  <img src="https://img.shields.io/badge/NeoForge-21.1+-orange?style=for-the-badge" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Java-21-red?style=for-the-badge&logo=openjdk" alt="Java 21"/>
  <img src="https://img.shields.io/github/v/release/Team-Arcadia/Arcadia-LootBox?style=for-the-badge&label=Version&color=blue" alt="Version"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Team-Arcadia/Arcadia-LootBox/build.yml?style=for-the-badge&label=Build" alt="Build"/>
  <img src="https://img.shields.io/github/license/Team-Arcadia/Arcadia-LootBox?style=for-the-badge" alt="License"/>
</p>

<h1 align="center">Arcadia LootBox</h1>

<p align="center">
  <b>Premium lootbox system for Minecraft servers</b><br/>
  <i>Powered by <a href="https://github.com/Team-Arcadia">Arcadia Lib</a> | Built for NeoForge 1.21.1</i>
</p>

<p align="center">
  <a href="#features">Features</a> |
  <a href="#commands">Commands</a> |
  <a href="#installation">Installation</a> |
  <a href="#configuration">Configuration</a> |
  <a href="#key-categories">Keys</a> |
  <a href="#contributing">Contributing</a> |
  <a href="#version-fran%C3%A7aise">Francais</a>
</p>

---

## Overview

Arcadia LootBox is a feature-rich, optimized lootbox system designed for Minecraft modded servers. It provides dual lootbox types (weighted & guaranteed), 50 integrated key items, free timed lootboxes with configurable cooldowns, Arcadia Hub integration with steampunk theming, and a complete permission system with optional LuckPerms support.

## Features

| Feature | Description |
|---|---|
| **Dual Lootbox Types** | "Weighted" (each item rolls with its own %) and "Guaranteed" (one random item + guaranteed drop) |
| **50 Key Items** | Dungeon, Shop, Vote, Lootable, Event, Boss keys across multiple tiers with custom textures |
| **Free Timed Lootboxes** | Per-lootbox configurable cooldowns (72h default), reducible to 48h with permissions. Persistent across restarts |
| **Steampunk Hub** | ArcadiaTheme client-side lootbox browser with configurable shop link |
| **6 Rarity Tiers** | Common, Uncommon, Rare, Epic, Legendary, Mythic with colored display |
| **Server Broadcasts** | Announce rare drops server-wide with configurable rarity threshold |
| **Soft LuckPerms** | Full LuckPerms integration that works gracefully without LP installed (vanilla OP fallback) |
| **Opening History** | Per-player history tracking with admin commands to view and clear |
| **Usage Limits** | Per-block maxUses with NBT persistence and admin override |
| **Anti-Autoclicker** | Rate-limiting protection against rapid-fire abuse |
| **Command Rewards** | Execute server/player commands as lootbox rewards with independent chances |
| **XP Rewards** | Grant experience points on opening |
| **Bilingual** | Automatic language detection (English/French) based on client settings |
| **30+ Config Params** | Cooldowns, broadcasts, hub, performance, security, animation, sounds, free timers |
| **Optimized** | Thread-safe managers, atomic map swap, async reload, batched particles, tick-friendly |

## Commands

All commands use the prefix `/arcadia_lootbox`.

### Lootbox Management
| Command | Permission | Description |
|---|---|---|
| `/arcadia_lootbox give <player> <id> [amount]` | Op Level 2 | Give a lootbox to a player |
| `/arcadia_lootbox giveall <id> [amount]` | Op Level 2 | Give a lootbox to all online players |
| `/arcadia_lootbox givekey <player> <key_id> [amount]` | Op Level 2 | Give a key item to a player |
| `/arcadia_lootbox reload` | Op Level 2 | Reload all JSON configurations |
| `/arcadia_lootbox list` | Op Level 2 | List all loaded lootbox definitions |
| `/arcadia_lootbox listkeys` | Op Level 2 | List all registered key items |
| `/arcadia_lootbox info <id>` | Op Level 2 | Show detailed lootbox information |
| `/arcadia_lootbox create <id> <name>` | Op Level 2 | Create a new lootbox template |
| `/arcadia_lootbox delete <id>` | Op Level 2 | Delete a lootbox definition |

### Player Interaction
| Command | Permission | Description |
|---|---|---|
| `/arcadia_lootbox preview <player> <id>` | Op Level 2 | Open loot preview GUI for a player |
| `/arcadia_lootbox history <player>` | Op Level 2 | View player opening history |
| `/arcadia_lootbox clearhistory <player>` | Op Level 2 | Clear a player's history |
| `/arcadia_lootbox setuses <pos> <uses>` | Op Level 2 | Set usage count on a placed block |
| `/arcadia_lootbox resetcooldown <player>` | Op Level 2 | Reset all cooldowns for a player |

### Free Lootbox Timer
| Command | Permission | Description |
|---|---|---|
| `/arcadia_lootbox free <player> <id>` | Op Level 2 | Claim a free lootbox for a player |
| `/arcadia_lootbox freetimer <player> <id>` | Op Level 2 | Check remaining free cooldown |
| `/arcadia_lootbox resetfree <player> [id]` | Op Level 2 | Reset free timer(s) for a player |

### Other
| Command | Permission | Description |
|---|---|---|
| `/arcadia_lootbox stats` | Op Level 2 | Show global statistics |
| `/arcadia_lootbox hub` | Op Level 2 | Open the Arcadia Hub |

## Installation

### Requirements
- Minecraft **1.21.1**
- NeoForge **21.1+**
- [Arcadia Lib](https://github.com/Team-Arcadia) **>= 1.2.0**

### Steps
1. Download the latest release from the [Releases](https://github.com/Team-Arcadia/Arcadia-LootBox/releases) page
2. Place `arcadia-lib-1.2.0.jar` in your `mods/` folder
3. Place `ArcadiaLootbox-1.2.0.jar` in your `mods/` folder
4. (Optional) Install [LuckPerms](https://luckperms.net/) for permission-based features
5. Start the server — configs auto-generate in `config/arcadia/arcadialootbox/`

### Client Installation (Optional)
Installing on the client enables the steampunk ArcadiaTheme rendering for the lootbox hub screen. The mod works without client installation (vanilla chest preview).

## Configuration

### Lootbox Definitions
Each lootbox is a JSON file in `config/arcadia/arcadialootbox/`. Two types available:
- **weighted** — Each item rolls independently with its own chance percentage
- **guaranteed** — One item is picked from the pool (weighted random) + a guaranteed item always drops

### Free Timed Lootboxes
Each lootbox can enable free timed claims with these JSON fields:
```json
{
  "freeEnabled": true,
  "freeCooldownHours": 72,
  "freePermission": "",
  "freeReducedCooldownHours": 48,
  "freeReducedPermission": "arcadialootbox.free.reduced"
}
```
Set `freeEnabled: false` to disable free claims for specific lootboxes.

### Global Config
The TOML config at `config/arcadia/lootbox.toml` has 35+ parameters in sections: General, Broadcast, Hub, Performance, Security, Animation, Sounds, Free Lootbox.

## Key Categories

| Category | Tiers | Count | Source |
|:--------:|:-----:|:-----:|:-------|
| **Dungeon** | Common - Transcendent | 10 | Mob drops in dungeons |
| **Shop** | Common - Transcendent | 10 | Online store purchase |
| **Vote** | Common - Transcendent | 10 | Server vote rewards |
| **Lootable** | Common - Transcendent | 10 | World loot (chests, fishing) |
| **Event** | Bronze - Diamond | 5 | Limited-time events |
| **Boss** | Minor - Overlord | 5 | Boss mob drops |

## Architecture

```
com.arcadia.lootbox
  +-- ArcadiaLootbox.java           Entry point, event registration
  +-- client/
  |   +-- ClientEvents.java          Hub card registration
  |   +-- LootboxClientData.java     Client-side data cache
  |   +-- LootboxHubScreen.java      Steampunk hub browser
  +-- command/
  |   +-- LootboxCommands.java       All /arcadia_lootbox commands
  +-- config/
  |   +-- LootboxConfig.java         Global TOML config (35+ params)
  +-- data/
  |   +-- LootboxDefinition.java     Per-lootbox JSON data model
  +-- item/
  |   +-- KeyRegistry.java           50 key item registrations
  |   +-- LootboxKeyItem.java        Key item base class
  +-- manager/
  |   +-- LootboxManager.java        Config loading & caching
  |   +-- FreeLootboxManager.java    Free claim timer persistence
  |   +-- HistoryManager.java        Opening history tracking
  |   +-- UsageTracker.java          Per-block usage counting
  +-- menu/
  |   +-- PreviewMenu.java           Server-side preview GUI
  +-- network/
  |   +-- LootboxNet.java            Packet registration
  |   +-- S2COpenLootboxHub.java     Open hub packet
  |   +-- S2CSyncLootboxList.java    Sync lootbox list packet
  +-- util/
      +-- LootHelper.java            Core lootbox logic
      +-- PermissionHelper.java      Soft LuckPerms wrapper
```

## Building from Source

```bash
git clone https://github.com/Team-Arcadia/Arcadia-LootBox.git
cd Arcadia-LootBox
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## Contributing

We welcome contributions! Please read our [Contributing Guide](.github/CONTRIBUTING.md) before submitting a pull request.

## Links

- [Arcadia: Echoes of Power](https://arcadia-echoes-of-power.fr/)
- [Discord](https://discord.gg/xjF8Rtzyd4)
- [Donate](https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00)

## License

All Rights Reserved. See [LICENSE](LICENSE) for details.

## Credits

**Author:** vyrriox
**Organization:** [Team Arcadia](https://github.com/Team-Arcadia)

---

<h1 align="center">Arcadia LootBox (Version Francaise)</h1>

<p align="center">
  <b>Systeme de lootbox premium pour serveurs Minecraft</b><br/>
  <i>Propulse par <a href="https://github.com/Team-Arcadia">Arcadia Lib</a> | Construit pour NeoForge 1.21.1</i>
</p>

## Apercu

Arcadia LootBox est un systeme de lootbox complet et optimise concu pour les serveurs Minecraft modes. Il propose deux types de lootbox (pondere et garanti), 50 cles integrees, des lootbox gratuites avec timer configurable, l'integration au Hub Arcadia avec theme steampunk, et un systeme de permissions complet avec support optionnel de LuckPerms.

## Caracteristiques

| Fonctionnalite | Description |
|---|---|
| **Deux types de Lootbox** | "Weighted" (% par item) et "Guaranteed" (1 aleatoire + drop garanti) |
| **50 Items Cles** | Donjon, Boutique, Vote, Trouvable, Evenement, Boss sur plusieurs tiers |
| **Lootbox gratuites** | Cooldown configurable par lootbox (72h defaut), reductible a 48h avec permissions. Persistant |
| **Hub Steampunk** | Ecran client ArcadiaTheme avec lien boutique configurable |
| **6 Niveaux de Rarete** | Common, Uncommon, Rare, Epic, Legendary, Mythic avec affichage colore |
| **Broadcasts Serveur** | Annonces serveur pour les drops rares |
| **LuckPerms Souple** | Integration complete, fonctionne sans LP (fallback OP vanilla) |
| **Historique** | Suivi par joueur avec commandes admin |
| **Limites d'Utilisation** | maxUses par lootbox placee avec persistance NBT |
| **Anti-Autoclicker** | Protection contre l'abus |
| **Bilingue** | Detection automatique de la langue (Anglais/Francais) |
| **35+ Parametres** | Configuration TOML complete |
| **Optimise** | Thread-safe, swap atomique, reload async, particules groupees |

## Commandes

Toutes les commandes utilisent le prefixe `/arcadia_lootbox`.

### Gestion des Lootbox
| Commande | Permission | Description |
|---|---|---|
| `/arcadia_lootbox give <joueur> <id> [quantite]` | Op Niveau 2 | Donner une lootbox |
| `/arcadia_lootbox giveall <id> [quantite]` | Op Niveau 2 | Donner a tous les joueurs |
| `/arcadia_lootbox givekey <joueur> <cle_id> [quantite]` | Op Niveau 2 | Donner une cle |
| `/arcadia_lootbox reload` | Op Niveau 2 | Recharger les configurations |
| `/arcadia_lootbox list` | Op Niveau 2 | Lister les lootbox |
| `/arcadia_lootbox listkeys` | Op Niveau 2 | Lister les cles |
| `/arcadia_lootbox info <id>` | Op Niveau 2 | Details d'une lootbox |
| `/arcadia_lootbox create <id> <nom>` | Op Niveau 2 | Creer un template |
| `/arcadia_lootbox delete <id>` | Op Niveau 2 | Supprimer une definition |

### Timer Lootbox Gratuites
| Commande | Permission | Description |
|---|---|---|
| `/arcadia_lootbox free <joueur> <id>` | Op Niveau 2 | Reclamer une lootbox gratuite |
| `/arcadia_lootbox freetimer <joueur> <id>` | Op Niveau 2 | Verifier le cooldown restant |
| `/arcadia_lootbox resetfree <joueur> [id]` | Op Niveau 2 | Reinitialiser le timer |

## Installation

### Prerequis
- Minecraft **1.21.1**
- NeoForge **21.1+**
- [Arcadia Lib](https://github.com/Team-Arcadia) **>= 1.2.0**

### Etapes
1. Telecharger la derniere release depuis [Releases](https://github.com/Team-Arcadia/Arcadia-LootBox/releases)
2. Placer `arcadia-lib-1.2.0.jar` dans le dossier `mods/`
3. Placer `ArcadiaLootbox-1.2.0.jar` dans le dossier `mods/`
4. (Optionnel) Installer [LuckPerms](https://luckperms.net/) pour les permissions avancees
5. Demarrer le serveur — configs auto-generees dans `config/arcadia/arcadialootbox/`

### Installation Client (Optionnel)
Installer sur le client active le rendu steampunk ArcadiaTheme pour le hub lootbox.

## Compiler depuis les Sources

```bash
git clone https://github.com/Team-Arcadia/Arcadia-LootBox.git
cd Arcadia-LootBox
./gradlew build
```

## Contribuer

Les contributions sont les bienvenues ! Lisez notre [Guide de Contribution](.github/CONTRIBUTING.md) avant de soumettre une pull request.

## Liens

- [Arcadia: Echoes of Power](https://arcadia-echoes-of-power.fr/)
- [Discord](https://discord.gg/xjF8Rtzyd4)
- [Donation](https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00)

## Credits

**Auteur :** vyrriox
**Organisation :** [Team Arcadia](https://github.com/Team-Arcadia)
