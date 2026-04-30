# Changelog

All notable changes to Arcadia LootBox are documented here.

---

## [1.2.1] - 2026-04-30

### Added

- **Collapsible Hub categories** — The Lootbox Hub now opens with every category folded so the screen stays readable. Click a header to expand it; chevron icon flips between `▶` and `▼`. Per-category state is preserved across resizes and reopens.
- **Expand-all / Collapse-all buttons** — Top-right shortcuts on the Hub to deploy or fold every category at once.
- **Preview menu shows every reward at once with rarity filters** — The lootbox preview lists all drops directly, sorted by rarity (Mythic → Common). When more than one rarity is present, filter chips on the bottom row let you isolate a single rarity; an "All" chip resets the filter. Pagination kicks in past 28 items.
- **Right-click in air with a key opens its lootbox preview** — `LootboxKeyItem.use` resolves the key to its matching lootbox: 0 matches no-op, 1 match opens the preview directly, 2+ matches open the Hub so the player can pick which one.
- **Multi-draw on the Draw button** — Open multiple lootboxes in one click when you hold several keys: left-click = 1, right-click = up to all keys held (capped at 10), shift-click = up to 10. Per-lootbox cooldown is applied once after the bulk run; anti-autoclicker and usage caps are still enforced per opening.
- **Bulk-open helper** — `LootHelper.handleBulkLootboxAttempt` and `LootHelper.countKeysInInventory` for the new flow, with a hard `BULK_OPEN_LIMIT` of 10.
- **Keys-held indicator** — The info panel and the Draw button now display how many matching keys the player has on them.

### Fixed

- **Shift-click giving the lootbox shulker on Draw** — `PreviewMenu.clicked` now blocks every non-pickup click type (`QUICK_MOVE`, `SWAP`, `THROW`, `CLONE`, `PICKUP_ALL`, `QUICK_CRAFT`) and routes shift-click on the Draw slot to the bulk-open path instead of the vanilla quick-move that occasionally moved the GUI item back to the player.

### Performance

- **Single-pass sort & filter** — The Preview menu sorts its loot table once at construction (rarity desc, then chance asc) and applies filters as a cheap in-memory predicate.

---

### Ajouts

- **Catégories repliables dans le Hub** — Le Hub des Lootbox s'ouvre désormais avec toutes les catégories fermées pour rester lisible. Clic sur l'en-tête pour la déployer ; le chevron passe de `▶` à `▼`. L'état par catégorie est conservé entre les redimensionnements et les réouvertures.
- **Boutons Tout déployer / Replier** — Raccourcis en haut à droite du Hub pour ouvrir ou refermer toutes les catégories d'un coup.
- **Menu Preview affiche toutes les récompenses avec filtres de rareté** — Le menu liste tous les drops directement, triés par rareté (Mythique → Commune). Quand plusieurs raretés sont présentes, des puces de filtre en bas permettent d'isoler une rareté ; une puce "Toutes" réinitialise le filtre. Pagination automatique au-delà de 28 objets.
- **Clic-droit dans le vide avec une clé ouvre la lootbox correspondante** — `LootboxKeyItem.use` retrouve la lootbox associée à la clé : 0 correspondance → rien, 1 correspondance → ouvre l'aperçu directement, 2+ → ouvre le Hub pour laisser le joueur choisir.
- **Multi-tirage sur le bouton Draw** — Ouvrir plusieurs lootbox en un clic si vous avez plusieurs clés : clic gauche = 1, clic droit = toutes les clés (max 10), shift-clic = jusqu'à 10. Le cooldown par-lootbox n'est appliqué qu'une fois en fin de série ; l'anti-autoclicker et les limites d'utilisations restent appliqués à chaque ouverture.
- **Helper multi-ouverture** — `LootHelper.handleBulkLootboxAttempt` et `LootHelper.countKeysInInventory` pour le nouveau flux, avec un plafond `BULK_OPEN_LIMIT` de 10.
- **Indicateur de clés possédées** — Le panneau d'info et le bouton Draw affichent maintenant le nombre de clés correspondantes dans l'inventaire.

### Correctifs

- **Shift-clic sur Draw qui donnait le shulker lootbox** — `PreviewMenu.clicked` bloque maintenant tous les types de clic non-pickup (`QUICK_MOVE`, `SWAP`, `THROW`, `CLONE`, `PICKUP_ALL`, `QUICK_CRAFT`) et redirige le shift-clic sur le slot Draw vers le multi-tirage au lieu du quick-move vanilla qui faisait remonter l'objet GUI dans l'inventaire.

### Performance

- **Tri et filtre en une passe** — Le menu Preview trie sa table de loot une seule fois à la construction (rareté desc puis chance asc) et applique le filtre comme un prédicat en mémoire.

---

## [1.2.0] - 2026-04-12

### Added

- **Free Timed Lootboxes** — Configurable per-lootbox free claims with cooldown timers (72h default, 48h with permissions). Persistent across server restarts via JSON storage. Auto-save every 5 minutes.
- **Soft LuckPerms Integration** — Full LuckPerms support that works gracefully when LP is not installed. Falls back to vanilla OP checks. Per-lootbox permission nodes, reduced free cooldowns with permission.
- **PermissionHelper** — Centralized permission wrapper with cached LP detection, default fallback, and error handling.
- **FreeLootboxManager** — Persistent per-player claim tracker with formatted remaining time display.
- **3 New Commands** — `/arcadia_lootbox free`, `/arcadia_lootbox freetimer`, `/arcadia_lootbox resetfree` for free lootbox management.
- **Dual Lootbox Types** — Two distinct modes: "weighted" (each item rolls independently with its %) and "guaranteed" (picks ONE item from pool + always gives a guaranteed item).
- **50 Integrated Key Items** — Dungeon (10 tiers), Shop (10 tiers), Vote (10 tiers), Lootable (10 tiers), Event (5 tiers), Boss (5 tiers) with custom textures.
- **Arcadia Lib Integration** — Full integration with arcadia-lib: ArcadiaTheme, ArcadiaMessages, CooldownManager, SchedulerService, TextFormatter, ItemBuilder.
- **Arcadia Hub Module** — Lootbox card registered in the Arcadia Hub with steampunk-themed client-side browser screen.
- **Rarity System** — 6 rarity tiers (Common → Mythic) with colored display and broadcast support.
- **Server Broadcast** — Configurable server-wide announcements when rare items are found.
- **Opening History** — Per-player history tracking with admin commands to view and clear.
- **Usage Limits** — maxUses per placed lootbox with NBT persistence and admin override.
- **Anti-Autoclicker** — Detects rapid-fire openings and temporarily blocks abusers.
- **Title Animations** — Configurable title/subtitle on lootbox opening with fade timings.
- **Command Rewards** — Execute console/player commands as lootbox rewards with independent chances.
- **XP Rewards** — Grant experience points on lootbox opening.
- **Permission System** — Per-lootbox LuckPerms permission nodes via arcadia-lib.
- **Global TOML Config** — 30+ configurable parameters: cooldowns, broadcasts, hub, performance, security, animation, sounds.
- **Network Sync** — S2C packets sync lootbox data to clients for hub display.

### Changed

- **Package Rename** — Moved from `com.vyrriox.arcadialootbox` to `com.arcadia.lootbox`.
- **Commands** — All commands now under `/arcadia_lootbox` with 14 subcommands and full tab-completion.
- **Preview GUI** — Enhanced with copper-glass border, nether star info panel, rarity sorting, enchant glint on epic+ items.
- **Thread Safety** — ConcurrentHashMap for all managers, volatile fields, bounded history queues.
- **Random Optimization** — Uses `level.random` instead of `new Random()` per opening.
- **Particle Optimization** — Batched particle sending (single call with count > 1), configurable limit.
- **Config Validation** — Validates minCount <= maxCount, chance range, guaranteed type fields on load.
- **Async Reload** — Config reload runs off the main thread to prevent tick freezes.

### Fixed

- **ClassCastException** — Added `instanceof ServerPlayer` check in left-click handler.
- **minCount > maxCount crash** — Validated at config load time.
- **Unknown item silent failure** — Now logs a warning for missing items.
- **Random color support** — "random" color value now works as documented.
- **Block existence check** — Preview GUI validates block still exists.

### Ajouts

- **Lootbox gratuites avec timer** — Claims gratuits par lootbox avec cooldown configurable (72h par défaut, 48h avec permissions). Persistant au redémarrage. Auto-save toutes les 5 minutes.
- **Intégration LuckPerms souple** — Support LuckPerms complet qui fonctionne sans LP installé. Fallback sur OP vanilla. Permissions par lootbox, cooldowns réduits avec permission.
- **3 Nouvelles commandes** — `/arcadia_lootbox free`, `/arcadia_lootbox freetimer`, `/arcadia_lootbox resetfree`.
- **Deux types de Lootbox** — Mode "weighted" (% indépendant par item) et "guaranteed" (1 item tiré + item garanti).
- **50 Clés intégrées** — Donjon (10), Boutique (10), Vote (10), Trouvable (10), Événement (5), Boss (5) avec textures.
- **Intégration Arcadia Lib** — ArcadiaTheme, ArcadiaMessages, CooldownManager, SchedulerService, TextFormatter, ItemBuilder.
- **Module Hub Arcadia** — Carte lootbox dans le Hub avec écran client steampunk.
- **Système de rareté** — 6 niveaux (Common → Mythic) avec couleurs et broadcast.
- **Broadcast serveur** — Annonces configurables pour les drops rares.
- **Historique d'ouvertures** — Suivi par joueur avec commandes admin.
- **Limites d'utilisation** — maxUses par lootbox placée avec persistance NBT.
- **Anti-Autoclicker** — Détection des ouvertures rapides.
- **Animations titre** — Titre/sous-titre configurables à l'ouverture.
- **Récompenses commandes** — Exécution de commandes comme récompenses.
- **Récompenses XP** — Points d'expérience à l'ouverture.
- **Système de permissions** — Nodes LuckPerms par lootbox.
- **Config TOML globale** — 30+ paramètres configurables.

### Modifications

- **Renommage package** — `com.vyrriox.arcadialootbox` → `com.arcadia.lootbox`.
- **Commandes** — Toutes sous `/arcadia_lootbox` avec 14 sous-commandes et auto-complétion.
- **GUI Preview** — Bordure cuivre, étoile info, tri par rareté, glint enchantement.
- **Thread Safety** — ConcurrentHashMap partout, champs volatile.
- **Optimisation particules** — Envoi groupé, limite configurable.

### Correctifs

- **ClassCastException** — Vérification `instanceof ServerPlayer`.
- **Crash minCount > maxCount** — Validation au chargement.
- **Item inconnu silencieux** — Warning dans les logs.

---

## [1.1.0] - 2026-01-14

### Added

- **Advanced Config System** — Auto-generates a README.txt guide (FR/EN) in the config folder.
- **Dynamic Localization** — Menu titles and item lores utilize the player's client language (FR/EN).
- **Opening Message** — Added openMessage field in JSON to customize chat feedback.
- **Security** — Added distance check (8 blocks) to auto-close GUI.
- **Anti-Spam** — Added cooldown to key item usage.

### Changed

- **Improved Interaction** — Right Click: Preview GUI. Left Click: Open lootbox.
- **Anti-Lag** — Dropped items merged into stacks.

### Fixed

- **Crash Fix** — Resolved critical crash when placing Lootbox Shulkers (NBT Data issue).

### Ajouts

- **Système de Configuration Avancé** — Guide README.txt auto-généré (FR/EN).
- **Localisation Dynamique** — Titres et lores adaptés à la langue du joueur.
- **Message d'Ouverture** — Option openMessage dans le JSON.
- **Sécurité** — Vérification de distance (8 blocs).
- **Anti-Spam** — Cooldown sur les clés.

### Modifications

- **Interaction Améliorée** — Clic Droit: Preview. Clic Gauche: Ouverture.
- **Anti-Lag** — Items au sol regroupés.

### Correctifs

- **Crash Fix** — Correction crash NBT lors de la pose des Shulkers.

---

## [1.0.0] - Initial Release

### Added

- **Lootbox System** — Lootbox block with 16 color variants.
- **JSON Configuration** — Dynamic loading from config/arcadia/arcadialootbox/.
- **Rewards** — Weighted drop system (Items, Chance, Min/Max Quantity).
- **Keys** — Requires a specific item to open (Configurable).
- **Visuals** — Particle support upon opening.
- **Commands** — /arcadialoot give and /arcadialoot reload.

### Ajouts

- **Système de Lootbox** — Bloc Lootbox avec 16 variantes de couleurs.
- **Configuration JSON** — Chargement dynamique depuis config/arcadia/arcadialootbox/.
- **Récompenses** — Système de drop pondéré.
- **Clés** — Item spécifique requis pour ouvrir.
- **Visuels** — Support des particules.
- **Commandes** — /arcadialoot give et /arcadialoot reload.
