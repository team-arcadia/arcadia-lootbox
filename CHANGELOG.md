# Changelog

All notable changes to Arcadia LootBox are documented here.

---

## [1.2.6] - 2026-06-29

### Added

- **NBT / data-component loot items** — Lootbox rewards now accept the full vanilla `/give` item syntax, so any reward can carry data components (NBT). You can drop enchanted books, pre-enchanted gear, named items, custom potions, written books and more. Works for every `item` in a loot table and for the `guaranteedItem`. A bare id such as `minecraft:diamond` keeps working unchanged.
- **`ItemSpecResolver` utility** — A single resolver parses each spec with the vanilla `ItemParser`, applies its components onto a template stack, and degrades gracefully (base item without NBT) if registry access is missing or the spec is malformed.
- **`example_nbt.json`** — A new auto-generated example crate showcasing a Sharpness V book, a Looting III sword, strong healing potions and a multi-enchant pickaxe.

### Changed

- **Component-aware giving & preview** — The drop logic now clones a template stack (preserving every component) instead of rebuilding a bare item, and the preview GUI renders the real enchanted/named item so the tooltip matches the actual reward.

### Performance

- **Zero-overhead fast path** — Bare `namespace:id` specs skip the command parser entirely and resolve directly through the registry, so existing configs pay no parsing cost.

### Ajouts

- **Objets de butin avec NBT / data-components** — Les récompenses acceptent désormais la syntaxe d'objet complète de la commande `/give`, donc n'importe quelle récompense peut porter des data-components (NBT). On peut distribuer des livres enchantés, de l'équipement pré-enchanté, des objets nommés, des potions personnalisées, des livres écrits, etc. Fonctionne pour chaque `item` d'une table de butin et pour le `guaranteedItem`. Un id simple comme `minecraft:diamond` continue de fonctionner à l'identique.
- **Utilitaire `ItemSpecResolver`** — Un résolveur unique analyse chaque spec avec le `ItemParser` vanilla, applique ses composants sur un stack modèle, et se rabat proprement (objet de base sans NBT) si l'accès au registre manque ou si la spec est invalide.
- **`example_nbt.json`** — Un nouvel exemple de caisse auto-généré présentant un livre Tranchant V, une épée Butin III, des potions de soin puissant et une pioche multi-enchantée.

### Modifications

- **Distribution et aperçu sensibles aux composants** — La logique de drop clone désormais un stack modèle (préservant chaque composant) au lieu de reconstruire un objet nu, et le menu d'aperçu affiche le véritable objet enchanté/nommé afin que l'infobulle corresponde à la récompense réelle.

### Performance

- **Chemin rapide sans surcoût** — Les specs simples `namespace:id` contournent entièrement le parseur de commande et se résolvent directement via le registre, donc les configs existantes ne paient aucun coût d'analyse.

---

## [1.2.5] - 2026-06-08

### Changed

- **Arcadia Lib 1.2.14** — Updated the bundled `arcadia-lib` dependency from 1.2.0 to 1.2.14 and raised the required version range to `[1.2.14,)`.
- **NeoForge 21.1.219** — Bumped the NeoForge target from 21.1.42 to 21.1.219, aligning with the shared library and picking up the latest loader fixes.

### Security

- **Preview permission gate** — `LootHelper.openPreviewGui` now enforces the lootbox's own permission node before building the menu. Previously any player who could reach a preview (placed block, key item, or the `request_preview` packet) could read the full loot table — items, drop chances, quantities, guaranteed item and free-claim timer — of a restricted lootbox they were not allowed to open. The gate is centralized at the single shared chokepoint, closing all three entry paths at once.
- **Preview open throttle** — A 250 ms per-player cooldown on opening the preview GUI from any path, so raw `C2SRequestPreview` packets can no longer bypass the item-path anti-spam cooldown and flood server-side menu construction.
- **Safe-deny on LuckPerms error** — A transient LuckPerms backend failure during a permission check now denies the node instead of silently granting it to every OP.
- **Bulk-open autoclicker accounting** — Each lootbox in a bulk-open burst now counts toward the per-minute anti-autoclicker cap (a 10-box burst registers as 10 opens, not one), and the per-lootbox cooldown is now also enforced on the bulk path.

### Fixed

- **Gated lootboxes public without a permission backend** — On servers with no permission plugin, a lootbox carrying a permission node is now treated as public rather than collapsing to OP-only, matching the intended "soft" permission behaviour across both the open and preview paths.
- **Localized rare-drop broadcast** — The server-wide rare-drop announcement was hardcoded in French and sent identically to everyone; it is now built per recipient and shown in each player's own client language, with the rarity word localized too.
- **Localized command output** — `/arcadia_lootbox free`, `freetimer` and `stats` no longer emit hardcoded English fragments; they route through the existing translation keys. The lootbox item name prefix ("Lootbox:") is also localized now.
- **Rarity names beyond mythic** — Added EN/FR names for the `superior`, `divine`, `celestial` and `transcendent` tiers, and `getRarityName` now falls back to a capitalized label instead of leaking the raw translation key.
- **Free-timer contract** — `getRemainingFormatted` now distinguishes a free-disabled lootbox ("Unavailable") from a ready one ("Ready!") instead of reporting both as ready.

### Performance

- **Off-main-thread config reload** — `/arcadia_lootbox reload` now performs disk read and JSON parsing on a worker thread and marshals the atomic swap, client re-sync and feedback back onto the server thread, instead of touching the player list and network off-thread.
- **Off-main-thread autosave** — Periodic free-claim autosave now snapshots the data on the tick thread (cheap) and runs GSON serialization plus an atomic temp-file rename on a worker thread, so the server tick never blocks on disk IO. The shutdown save remains synchronous.
- **Per-player throttle eviction** — Anti-autoclicker timestamps and the new preview-open cooldown are dropped on logout instead of accumulating across sessions.

### Code Quality

- **Removed deprecated API** — Dropped the `EventBusSubscriber.Bus.MOD` argument flagged for removal in 1.21.1, clearing the build warning.
- **Per-instance pedestal pane** — Replaced a mutable static field shared across all open preview menus with a per-instance field.
- **Defensive history copy** — Opening history now stores a defensive copy of the received-items list.
- **Removed dead config keys** — Dropped the never-read `broadcast.format` / `broadcast.rareFormat` options now that broadcasts are localized in code.
- **Runtime version in logs** — Startup logs read the mod version from the container instead of a hardcoded string, and stale `v1.2.0` references were removed from comments.

### Modifications

- **Arcadia Lib 1.2.14** — Mise à jour de la dépendance `arcadia-lib` de 1.2.0 vers 1.2.14 et relèvement de la plage de version requise à `[1.2.14,)`.
- **NeoForge 21.1.219** — Passage de la cible NeoForge de 21.1.42 à 21.1.219, alignée sur la bibliothèque partagée et bénéficiant des derniers correctifs du loader.

### Sécurité

- **Contrôle de permission sur l'aperçu** — `LootHelper.openPreviewGui` vérifie désormais le nœud de permission de la lootbox avant de construire le menu. Auparavant, tout joueur pouvant atteindre un aperçu (bloc posé, objet clé ou paquet `request_preview`) pouvait lire la table de butin complète — objets, chances, quantités, objet garanti et timer gratuit — d'une lootbox restreinte qu'il n'était pas autorisé à ouvrir. Le contrôle est centralisé au point de passage unique partagé, fermant les trois chemins d'entrée d'un coup.
- **Limitation d'ouverture de l'aperçu** — Cooldown de 250 ms par joueur sur l'ouverture du menu d'aperçu depuis n'importe quel chemin, afin que les paquets `C2SRequestPreview` bruts ne puissent plus contourner le cooldown anti-spam de l'objet et inonder la construction de menus côté serveur.
- **Refus sécurisé en cas d'erreur LuckPerms** — Une défaillance transitoire du backend LuckPerms lors d'une vérification refuse désormais le nœud au lieu de l'accorder silencieusement à tous les OP.
- **Comptage anti-autoclicker en multi-ouverture** — Chaque lootbox d'une rafale de multi-ouverture compte désormais dans le plafond anti-autoclicker par minute (une rafale de 10 compte pour 10, pas 1), et le cooldown par lootbox est aussi appliqué sur le chemin multi-ouverture.

### Correctifs

- **Lootbox restreintes publiques sans backend de permissions** — Sur les serveurs sans plugin de permissions, une lootbox portant un nœud de permission est désormais traitée comme publique au lieu de devenir OP-only, conformément au comportement « souple » prévu sur les chemins d'ouverture et d'aperçu.
- **Annonce de drop rare localisée** — L'annonce serveur d'un drop rare était codée en dur en français et envoyée à l'identique à tout le monde ; elle est désormais construite par destinataire et affichée dans la langue de chaque joueur, rareté incluse.
- **Sorties de commandes localisées** — `/arcadia_lootbox free`, `freetimer` et `stats` n'émettent plus de fragments en anglais codés en dur ; elles passent par les clés de traduction existantes. Le préfixe du nom d'objet lootbox (« Lootbox : ») est aussi localisé.
- **Noms de rareté au-delà de mythique** — Ajout des noms EN/FR pour les paliers `superior`, `divine`, `celestial` et `transcendent`, et `getRarityName` retombe désormais sur un libellé capitalisé au lieu d'exposer la clé de traduction brute.
- **Contrat du timer gratuit** — `getRemainingFormatted` distingue désormais une lootbox sans réclamation gratuite (« Indisponible ») d'une lootbox prête (« Prêt ! ») au lieu de signaler les deux comme prêtes.

### Performance

- **Rechargement de config hors thread principal** — `/arcadia_lootbox reload` effectue désormais la lecture disque et l'analyse JSON sur un thread worker et renvoie le swap atomique, la re-synchro des clients et le feedback sur le thread serveur, au lieu de toucher la liste des joueurs et le réseau hors thread.
- **Sauvegarde automatique hors thread principal** — La sauvegarde périodique des réclamations gratuites prend désormais un instantané sur le thread de tick (peu coûteux) et exécute la sérialisation GSON puis un renommage atomique de fichier temporaire sur un thread worker, afin que le tick serveur ne bloque jamais sur l'IO disque. La sauvegarde à l'arrêt reste synchrone.
- **Éviction des limiteurs par joueur** — Les horodatages anti-autoclicker et le nouveau cooldown d'ouverture d'aperçu sont supprimés à la déconnexion au lieu de s'accumuler entre les sessions.

### Qualité du code

- **Suppression d'API dépréciée** — Suppression de l'argument `EventBusSubscriber.Bus.MOD` marqué pour retrait en 1.21.1, éliminant l'avertissement de build.
- **Socle (pedestal) par instance** — Remplacement d'un champ statique mutable partagé entre tous les menus d'aperçu ouverts par un champ par instance.
- **Copie défensive de l'historique** — L'historique d'ouverture stocke désormais une copie défensive de la liste des objets reçus.
- **Clés de config mortes supprimées** — Suppression des options jamais lues `broadcast.format` / `broadcast.rareFormat` maintenant que les annonces sont localisées dans le code.
- **Version au runtime dans les logs** — Les logs de démarrage lisent la version du mod depuis le container au lieu d'une chaîne codée en dur, et les références obsolètes à `v1.2.0` ont été retirées des commentaires.

---

## [1.2.4] - 2026-05-01

### Changed

- **Single clickable action row** — Only row 6 of the Preview menu is interactive now. Layout: `[All][F1][F2][◀][DRAW][▶][F3][F4][F5]`. The filter chips were moved up from row 5 onto row 6 alongside the prev/Draw/next controls, reclaiming row 5 entirely for item display.
- **28 items per page (up from 21)** — Reclaiming row 5 for content gives 4 full rows × 7 columns of rewards. Fewer pages to flip through.
- **Up to 5 rarity chips on the same row** — The 5 filter slots split around the Draw button (2 on the left, 3 on the right). If a lootbox somehow defines more than 5 rarities, the top 5 (highest rarity first) are shown.

### Performance

- **Frame baked once** — The stained-glass frame is now pre-built in the constructor and cloned into the container on each rebuild, instead of being recomputed per click.
- **Per-rarity counts cached** — Filter chip tooltips read from a pre-computed map instead of iterating the full loot table on every click.
- **Rarity ordering computed once** — The ordered list of present rarities is built in the constructor and reused as an immutable `List.copyOf`.

### Security

- **Anti-spam click cooldown** — A 75 ms minimum gap between non-Draw clicks in the Preview menu, and a 250 ms cooldown on right-click-in-air with a key. Blocks autoclicker-based GUI floods without affecting normal play.
- **Lootbox revalidation before opening** — Every Draw click re-checks `LootboxManager.exists(id)` and the player's range before consuming a key. Prevents opening a lootbox that was just deleted/reloaded.
- **Strict slot bounds in `clicked`** — Out-of-range slot ids are now rejected immediately, preventing crafted packets from triggering vanilla shift-click logic on player inventory slots.
- **Null-safe key lookup** — `LootHelper.countKeysInInventory` and `LootboxKeyItem.use` both guard against `null` registry keys and `null` definitions, preventing NPEs on mods that strip item registrations between reloads.
- **String sanitization in tooltips** — Display names from JSON are now stripped of control characters and capped at 64 chars before being rendered, defeating log-injection / overflow tricks via crafted lootbox configs.

---

### Modifications

- **Une seule rangée cliquable** — Seule la rangée 6 du menu Preview est interactive maintenant. Disposition : `[All][F1][F2][◀][DRAW][▶][F3][F4][F5]`. Les puces de filtre sont montées de la rangée 5 vers la rangée 6, libérant la rangée 5 entièrement pour l'affichage des objets.
- **28 objets par page (contre 21)** — La récupération de la rangée 5 pour le contenu offre 4 rangées complètes × 7 colonnes de récompenses. Moins de pages à parcourir.
- **Jusqu'à 5 puces de rareté sur la même rangée** — Les 5 slots de filtre se répartissent autour du bouton Draw (2 à gauche, 3 à droite). Si une lootbox définit plus de 5 raretés, les 5 plus hautes (rareté décroissante) sont affichées.

### Performance

- **Cadre construit une seule fois** — Le cadre en vitres teintées est désormais pré-construit dans le constructeur et copié dans le conteneur à chaque reconstruction, au lieu d'être recalculé à chaque clic.
- **Comptes par rareté en cache** — Les tooltips des puces lisent depuis une map pré-calculée au lieu de parcourir toute la table de loot à chaque clic.
- **Ordre des raretés calculé une fois** — La liste ordonnée des raretés présentes est construite dans le constructeur et réutilisée comme `List.copyOf` immuable.

### Sécurité

- **Cooldown anti-spam sur les clics** — Écart minimum de 75 ms entre deux clics non-Draw dans le menu Preview, et cooldown de 250 ms sur le clic-droit dans le vide avec une clé. Bloque le flood GUI par autoclicker sans gêner le jeu normal.
- **Revalidation de la lootbox avant ouverture** — Chaque clic Draw re-vérifie `LootboxManager.exists(id)` et la distance du joueur avant de consommer une clé. Empêche d'ouvrir une lootbox qui vient d'être supprimée/rechargée.
- **Bornes strictes sur les slots dans `clicked`** — Les slot ids hors plage sont rejetés immédiatement, empêchant des paquets forgés de déclencher la logique vanilla de shift-clic sur les slots de l'inventaire joueur.
- **Lookup de clé null-safe** — `LootHelper.countKeysInInventory` et `LootboxKeyItem.use` se protègent désormais contre des clés de registry et des définitions `null`, empêchant les NPE sur des mods qui dé-enregistrent des items entre les rechargements.
- **Sanitization des chaînes dans les tooltips** — Les noms d'affichage venus du JSON sont nettoyés des caractères de contrôle et limités à 64 caractères avant rendu, neutralisant les tentatives d'injection de logs ou de débordement via des configs lootbox forgées.

---

## [1.2.3] - 2026-04-30

### Changed

- **Filter chips now use stained-glass panes** — Previously rarity filters were dyes / lapis / amethyst / gold ingot / nether star, which blended visually with actual loot items and made the filter row look like more rewards. Each rarity now gets a stained-glass pane in its own color (lime/blue/purple/yellow/magenta/white). Glass panes read clearly as "tabs/buttons", not as drops.
- **Yellow pedestal around the Draw button** — Slots 48 and 50 (flanking the Draw) now show yellow stained-glass panes, framing the Draw button as a centerpiece instead of letting it float in a dark row.
- **Uniform orange frame** — The whole border now uses one warm tone instead of mixed orange/blue/gray rows. Only the Draw pedestal breaks the pattern in yellow, keeping the visual focus where it belongs.
- **Tighter item tooltip** — The redundant rarity line was merged into the item name (`Item Name • Rarity`). Each tooltip now uses three lines instead of five, with `»` arrows replacing colons for a more polished look.

---

### Modifications

- **Puces de filtre en vitres teintées** — Avant, les filtres étaient des teintures / lapis / améthyste / lingot d'or / nether star, qui se confondaient visuellement avec les vrais objets et donnaient l'impression que la rangée de filtres contenait encore plus de drops. Chaque rareté a maintenant une vitre teintée à sa couleur (lime/bleu/violet/jaune/magenta/blanc). Une vitre se lit clairement comme un "onglet/bouton", pas comme un drop.
- **Piédestal jaune autour du bouton Draw** — Les slots 48 et 50 (autour du Draw) affichent désormais des vitres jaunes, encadrant le bouton Draw comme la pièce maîtresse au lieu de le laisser perdu dans une rangée sombre.
- **Cadre orange uniforme** — Toute la bordure utilise désormais une seule teinte chaude au lieu de mélanger orange/bleu/gris. Seul le piédestal du Draw rompt le motif en jaune, gardant l'attention visuelle là où il faut.
- **Tooltip d'objet plus serré** — La ligne de rareté redondante est fusionnée dans le nom (`Nom • Rareté`). Chaque tooltip passe de cinq à trois lignes, avec des flèches `»` à la place des deux-points pour un rendu plus propre.

---

## [1.2.2] - 2026-04-30

### Changed

- **Preview menu cleaned up** — The bottom of the lootbox preview was crowded: filter chips, pagination arrows and the Draw button all shared the same row. The layout now has three distinct zones: items grid (rows 1–3, 21 slots), rarity filter row (row 4), action bar (row 5) with the Draw button centered and prev/next arrows on its sides.
- **Three-band frame** — Orange glass for the content frame (top + sides), yellow accents on the filter row, black glass anchoring the action bar. Visual cue tells the player at a glance which row does what.
- **Item tooltip refresh** — Cleaner separators (`§8:` / `§8-`) and a single trailing line removed; reads better at a glance.
- **Info pane refresh** — Heavy `═` separators replaced by lighter `▬`; sections are visually grouped (header → guaranteed → filter → keys → page).

---

### Modifications

- **Menu Preview nettoyé** — Le bas du menu était surchargé : les puces de filtre, les flèches de pagination et le bouton Draw partageaient la même rangée. Le layout est maintenant en trois zones distinctes : grille d'objets (rangées 1–3, 21 slots), rangée de filtres de rareté (rangée 4), barre d'action (rangée 5) avec le bouton Draw centré et les flèches précédent/suivant sur ses côtés.
- **Cadre en trois bandes** — Verre orange pour le cadre du contenu (haut + côtés), accents jaune sur la rangée des filtres, verre noir ancrant la barre d'action. L'œil identifie immédiatement à quoi sert chaque rangée.
- **Tooltip d'objet rafraîchi** — Séparateurs plus propres (`§8:` / `§8-`) et une ligne vide superflue retirée ; plus lisible d'un coup d'œil.
- **Panneau Info rafraîchi** — Séparateurs lourds `═` remplacés par des `▬` plus légers ; les sections sont visuellement regroupées (en-tête → garanti → filtre → clés → page).

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
