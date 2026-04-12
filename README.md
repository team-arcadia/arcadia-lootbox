# Arcadia LootBox

![License](https://img.shields.io/badge/license-All%20Rights%20Reserved-red) ![Version](https://img.shields.io/badge/version-1.2.0-blue)

A premium lootbox system for Minecraft 1.21.1 (NeoForge) with dual lootbox types, 50 integrated keys, Arcadia Hub integration, and full configuration. Powered by Arcadia Lib.

## Features

- **Dual Lootbox Types** — "Weighted" (each item rolls with its own %) and "Guaranteed" (one random item + guaranteed drop)
- **50 Key Items** — Dungeon, Shop, Vote, Lootable, Event, Boss keys across multiple tiers with custom textures
- **Arcadia Hub Integration** — Steampunk-themed client-side lootbox browser with shop link
- **6 Rarity Tiers** — Common, Uncommon, Rare, Epic, Legendary, Mythic with colored display
- **Server Broadcasts** — Announce rare drops server-wide
- **Opening History** — Per-player history tracking
- **Usage Limits** — maxUses per placed lootbox
- **Anti-Autoclicker** — Protection against rapid-fire abuse
- **14 Admin Commands** — Full management under `/arcadia_lootbox`
- **30+ Config Parameters** — Cooldowns, broadcasts, hub, performance, security, animations, sounds
- **Command & XP Rewards** — Execute commands and grant XP as lootbox rewards
- **Permission System** — Per-lootbox LuckPerms permission nodes
- **Bilingual** — Full English and French localization

## Installation

1. Install [NeoForge 21.1.42+](https://neoforged.net/) for Minecraft 1.21.1
2. Install [Arcadia Lib](https://github.com/vyrriox/arcadia-lib) (required dependency)
3. Drop `ArcadiaLootbox-1.2.0.jar` into your `mods/` folder
4. Start the server — config files are auto-generated in `config/arcadia/arcadialootbox/`
5. Edit JSON files to create your lootboxes, then `/arcadia_lootbox reload`

## Commands

| Command | Description |
|---------|-------------|
| `/arcadia_lootbox give <player> <id> [amount]` | Give a lootbox to a player |
| `/arcadia_lootbox giveall <id> [amount]` | Give a lootbox to all players |
| `/arcadia_lootbox givekey <player> <key_id> [amount]` | Give a key to a player |
| `/arcadia_lootbox reload` | Reload all configs |
| `/arcadia_lootbox list` | List all lootboxes |
| `/arcadia_lootbox listkeys` | List all registered keys |
| `/arcadia_lootbox info <id>` | Show lootbox details |
| `/arcadia_lootbox preview <player> <id>` | Open preview GUI for a player |
| `/arcadia_lootbox history <player>` | Show opening history |
| `/arcadia_lootbox clearhistory <player>` | Clear player history |
| `/arcadia_lootbox create <id> <name>` | Create a new lootbox template |
| `/arcadia_lootbox delete <id>` | Delete a lootbox definition |
| `/arcadia_lootbox setuses <pos> <uses>` | Set usage count on a placed lootbox |
| `/arcadia_lootbox resetcooldown <player>` | Reset all cooldowns for a player |
| `/arcadia_lootbox stats` | Show global statistics |
| `/arcadia_lootbox hub` | Open the Arcadia Hub |

## Credits

Author: vyrriox

---

# Arcadia LootBox (Version Française)

Un système de lootbox premium pour Minecraft 1.21.1 (NeoForge) avec deux types de lootbox, 50 clés intégrées, intégration au Hub Arcadia et configuration complète. Propulsé par Arcadia Lib.

## Caractéristiques

- **Deux types de Lootbox** — "Weighted" (chaque item a son % indépendant) et "Guaranteed" (un item aléatoire + drop garanti)
- **50 Items Clés** — Donjon, Boutique, Vote, Trouvable, Événement, Boss sur plusieurs tiers avec textures personnalisées
- **Intégration Hub Arcadia** — Navigateur client steampunk avec lien boutique
- **6 Niveaux de Rareté** — Common, Uncommon, Rare, Epic, Legendary, Mythic
- **Broadcasts Serveur** — Annonce des drops rares
- **Historique d'Ouvertures** — Suivi par joueur
- **Limites d'Utilisation** — maxUses par lootbox placée
- **Anti-Autoclicker** — Protection contre l'abus
- **14 Commandes Admin** — Gestion complète sous `/arcadia_lootbox`
- **30+ Paramètres** — Cooldowns, broadcasts, hub, performance, sécurité, animations, sons
- **Récompenses Commandes & XP** — Commandes et XP en récompenses
- **Système de Permissions** — Nodes LuckPerms par lootbox
- **Bilingue** — Localisation complète anglais et français

## Installation

1. Installer [NeoForge 21.1.42+](https://neoforged.net/) pour Minecraft 1.21.1
2. Installer [Arcadia Lib](https://github.com/vyrriox/arcadia-lib) (dépendance requise)
3. Placer `ArcadiaLootbox-1.2.0.jar` dans le dossier `mods/`
4. Démarrer le serveur — les fichiers de configuration sont auto-générés dans `config/arcadia/arcadialootbox/`
5. Modifier les fichiers JSON puis `/arcadia_lootbox reload`

## Credits

Author: vyrriox

### Links / Liens
- **Website**: [Arcadia: Echoes Of Power](https://arcadia-echoes-of-power.fr/)
- **Support**: [Discord](https://discord.gg/xjF8Rtzyd4)
- **Donation**: [Stripe](https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00)
