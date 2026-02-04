# CHANGELOG / JOURNAL DES MODIFICATIONS

## [1.1.0] - 2026-01-14

### 🇺🇸 English
#### Added
- **Advanced Config System**: Auto-generates a `README.txt` guide (FR/EN) in the config folder.
- **Dynamic Localization**: Menu titles and item lores utilize the player's client language (FR/EN) without resource packs.
- **Opening Message**: Added `openMessage` field in JSON to customize chat feedback.
- **Security**: Added distance check (8 blocks) to auto-close GUI.
- **Anti-Spam**: Added cooldown to key item usage.

#### Changed
- **Improved Interaction**:
    - **Right Click**: Opens Preview GUI (See drops and chances).
    - **Left Click**: Opens the lootbox (if holding key).
- **Anti-Lag**: Dropped items are merged into stacks to prevent server lag.

#### Fixed
- **Crash Fix**: Resolved critical crash when placing Lootbox Shulkers (NBT Data issue).

---

### 🇫🇷 Français
#### Ajouté
- **Système de Configuration Avancé** : Le mod génère automatiquement un guide `README.txt` (FR/EN) dans le dossier config.
- **Localisation Dynamique** : Les titres des menus et les lores des items s'adaptent automatiquement à la langue du joueur (FR/EN) sans resource pack.
- **Message d'Ouverture** : Ajout d'une option `openMessage` dans le JSON pour personnaliser le message de chat.
- **Sécurité** : Ajout d'une vérification de distance (8 blocs) pour fermer le menu si le joueur s'éloigne.
- **Anti-Spam** : Ajout d'un cooldown sur l'item clé pour éviter le spam d'ouverture.

#### Changé
- **Interaction Améliorée** :
    - **Clic Droit** : Ouvre un GUI de prévisualisation (Voir les loots et les % de chance).
    - **Clic Gauche** : Ouvre la lootbox (si la clé est en main).
- **Anti-Lag** : Les items au sol sont regroupés pour éviter le lag serveur.

#### Corrigé
- **Crash Fix** : Correction d'un crash critique lors de la pose d'une Lootbox (Shulker) lié aux données NBT.

______________________________________________________________________

## [1.0.0] - initial Release

### 🇺🇸 English
#### Added
- **Lootbox System**: Added Lootbox block with 16 color variants.
- **JSON Configuration**: Dynamic loading of configs from `config/arcadia/arcadialootbox/`.
- **Rewards**: Weighted drop system (Items, Chance, Min/Max Quantity).
- **Keys**: Requires a specific item (Key) to open the Lootbox (Configurable).
- **Visuals**: Particle support upon opening.
- **Commands**: `/arcadialoot give` and `/arcadialoot reload`.

### 🇫🇷 Français
#### Ajouté
- **Système de Lootbox** : Ajout du bloc Lootbox avec 16 variantes de couleurs.
- **Configuration JSON** : Chargement dynamique des configurations depuis `config/arcadia/arcadialootbox/`.
- **Récompenses** : Système de drop pondéré (Items, Chance, Quantité Min/Max).
- **Clés** : Nécessite un item spécifique (Key) pour ouvrir la Lootbox (Configurable).
- **Visuels** : Support des particules lors de l'ouverture.
- **Commandes** : `/arcadialoot give` et `/arcadialoot reload`.
