<div align="center">

<img src="https://raw.githubusercontent.com/Team-Arcadia/Arcadia-Admin-Pannel/main/.github/assets/arcadia_banner.png" alt="Arcadia Banner" width="100%">

<br><br>

# Arcadia LootBox

### Advanced Lootbox System for Minecraft Servers

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.1-brightgreen?style=for-the-badge&logo=curseforge&logoColor=white" alt="Minecraft">
  <img src="https://img.shields.io/badge/NeoForge-21.1.42-orange?style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAYAAAA" alt="NeoForge">
  <img src="https://img.shields.io/badge/Version-1.2.0-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=for-the-badge" alt="License">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
</p>

<p align="center">
  <a href="https://discord.gg/xjF8Rtzyd4">
    <img src="https://img.shields.io/discord/1457392657830772738?style=for-the-badge&color=5865F2&label=Discord&logo=discord&logoColor=white" alt="Discord">
  </a>
  &nbsp;
  <a href="https://arcadia-echoes-of-power.fr/">
    <img src="https://img.shields.io/badge/Website-Arcadia-blue?style=for-the-badge&logo=google-chrome" alt="Website">
  </a>
  &nbsp;
  <a href="https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00">
    <img src="https://img.shields.io/badge/Donate-Stripe-6772E5?style=for-the-badge&logo=stripe&logoColor=white" alt="Donate">
  </a>
</p>

</div>

---

<br>

<p align="center">
  <strong>Arcadia LootBox</strong> is a premium, feature-rich lootbox system designed for the <strong>Arcadia: Echoes of Power</strong> Minecraft server.<br>
  Powered by <strong>Arcadia Lib</strong>, it offers dual lootbox types, 50 integrated keys, Arcadia Hub integration, free timed lootboxes, and full configuration.
</p>

<br>

## Features

<table align="center">
<tr>
<td width="50%">

### Core System
- **Dual Lootbox Types** — `weighted` (% per item) & `guaranteed` (1 random + guaranteed drop)
- **50 Integrated Key Items** — Dungeon, Shop, Vote, Lootable, Event, Boss
- **6 Rarity Tiers** — Common, Uncommon, Rare, Epic, Legendary, Mythic
- **JSON Configuration** — Create unlimited lootboxes via simple config files
- **Physical Shulker Boxes** — 16 color variants as lootbox blocks

</td>
<td width="50%">

### Advanced Features
- **Free Timed Lootboxes** — Configurable cooldown (72h default, reducible with permissions)
- **Arcadia Hub Integration** — Steampunk-themed browser screen with shop link
- **Server Broadcasts** — Announce rare drops server-wide
- **Opening History** — Per-player tracking with admin commands
- **Anti-Autoclicker** — Rate-limiting protection

</td>
</tr>
<tr>
<td width="50%">

### Administration
- **16 Admin Commands** — Full management under `/arcadia_lootbox`
- **Tab Completion** — All commands with smart suggestions
- **Usage Limits** — Per-block maxUses with NBT persistence
- **Permission System** — LuckPerms integration (optional, works without)
- **Async Config Reload** — No main thread freeze

</td>
<td width="50%">

### Rewards & Effects
- **Command Rewards** — Execute server/player commands as loot
- **XP Rewards** — Grant experience on opening
- **Title Animations** — Configurable title/subtitle display
- **Particle System** — Batched, configurable, performance-friendly
- **Sound System** — Custom open/close sounds with volume/pitch

</td>
</tr>
</table>

<br>

## Key Categories

| Category | Tiers | Count | Description |
|:--------:|:-----:|:-----:|:------------|
| **Dungeon** | Common → Transcendent | 10 | Dropped by mobs in dungeons |
| **Shop** | Common → Transcendent | 10 | Purchased from the online store |
| **Vote** | Common → Transcendent | 10 | Earned by voting for the server |
| **Lootable** | Common → Transcendent | 10 | Found naturally in the world |
| **Event** | Bronze → Diamond | 5 | Special limited-time rewards |
| **Boss** | Minor → Overlord | 5 | Dropped by boss mobs |

<br>

## Commands

<details>
<summary><strong>Click to expand command list</strong></summary>

| Command | Description |
|---------|-------------|
| `/arcadia_lootbox give <player> <id> [amount]` | Give a lootbox to a player |
| `/arcadia_lootbox giveall <id> [amount]` | Give a lootbox to all online players |
| `/arcadia_lootbox givekey <player> <key_id> [amount]` | Give a key item to a player |
| `/arcadia_lootbox free [player]` | Claim or check free lootbox status |
| `/arcadia_lootbox freetimer <player>` | Check remaining free lootbox cooldown |
| `/arcadia_lootbox reload` | Reload all JSON configurations |
| `/arcadia_lootbox list` | List all loaded lootbox definitions |
| `/arcadia_lootbox listkeys` | List all registered key items |
| `/arcadia_lootbox info <id>` | Show detailed lootbox information |
| `/arcadia_lootbox preview <player> <id>` | Open loot preview GUI for a player |
| `/arcadia_lootbox history <player>` | View player opening history |
| `/arcadia_lootbox clearhistory <player>` | Clear a player's history |
| `/arcadia_lootbox create <id> <name>` | Create a new lootbox template |
| `/arcadia_lootbox delete <id>` | Delete a lootbox definition |
| `/arcadia_lootbox setuses <pos> <uses>` | Set usage count on a placed block |
| `/arcadia_lootbox resetcooldown <player>` | Reset all cooldowns for a player |
| `/arcadia_lootbox stats` | Show global statistics |
| `/arcadia_lootbox hub` | Open the Arcadia Hub |

</details>

<br>

## Installation

```
1. Install NeoForge 21.1.42+ for Minecraft 1.21.1
2. Install Arcadia Lib (required dependency)
3. Drop ArcadiaLootbox-1.2.0.jar into your mods/ folder
4. (Optional) Install LuckPerms for permission-based features
5. Start the server — configs auto-generate in config/arcadia/arcadialootbox/
6. Edit JSON files, then /arcadia_lootbox reload
```

<br>

## Dependencies

| Mod | Required | Description |
|-----|:--------:|-------------|
| **Arcadia Lib** | Yes | Shared library (hub, messages, scheduler, theme) |
| **LuckPerms** | No | Optional — enables per-lootbox permissions & reduced free cooldowns |

<br>

## Configuration

The mod generates its config at `config/arcadia/lootbox.toml` with **30+ parameters** organized in sections:

- **General** — Cooldowns, usage limits, sneak requirement
- **Broadcast** — Server-wide announcements for rare drops
- **Hub** — Shop URL, display settings
- **Performance** — Particle limits, async reload, history size
- **Security** — Anti-autoclicker, interaction distance
- **Animation** — Title timings, particle effects
- **Sounds** — Volume, pitch, default sounds
- **Free Lootbox** — Default cooldown, permissions, global toggle

<br>

---

<br>

<div align="center">

## Version Française

</div>

<p align="center">
  <strong>Arcadia LootBox</strong> est un système de lootbox premium conçu pour le serveur <strong>Arcadia: Echoes of Power</strong>.<br>
  Propulsé par <strong>Arcadia Lib</strong>, il offre deux types de lootbox, 50 clés intégrées, l'intégration au Hub Arcadia, des lootbox gratuites avec timer, et une configuration complète.
</p>

<details>
<summary><strong>Caractéristiques principales</strong></summary>

- **Deux types de Lootbox** — `weighted` (% par item) et `guaranteed` (1 aléatoire + drop garanti)
- **50 Clés intégrées** — Donjon, Boutique, Vote, Trouvable, Événement, Boss
- **Lootbox gratuites** — Cooldown configurable (72h par défaut, réductible avec permissions)
- **Intégration Hub Arcadia** — Écran navigateur steampunk avec lien boutique
- **16 Commandes Admin** — Gestion complète sous `/arcadia_lootbox`
- **Système de permissions** — Intégration LuckPerms optionnelle
- **30+ Paramètres** — Configuration TOML complète
- **Bilingue** — Localisation complète EN/FR

</details>

<br>

---

<div align="center">

### Support the Project / Soutenir le Projet

<p>
  <strong>Your support helps us keep Arcadia alive and evolving!</strong><br>
  <em>Votre soutien nous aide à faire vivre et évoluer Arcadia !</em>
</p>

<a href="https://buy.stripe.com/3cI3co6X97Vy4IK50QfIs00">
  <img src="https://img.shields.io/badge/Donate_via_Stripe-Support_Us-6772E5?style=for-the-badge&logo=stripe&logoColor=white" alt="Support Us" height="40">
</a>

<br><br>

<a href="https://www.arcadia-echoes-of-power.fr/partenariat">
  <img src="https://img.shields.io/badge/Partners-Partenaires-orange?style=for-the-badge&logoColor=white" alt="Partners" height="30">
</a>

<br><br>

<sub>Made with care by <strong>vyrriox</strong> for the Arcadia community</sub>

</div>
