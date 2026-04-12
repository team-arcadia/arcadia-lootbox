package com.arcadia.lootbox.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Full EN/FR localization for all user-facing text.
 * Detects player language automatically via clientInformation().
 *
 * @author vyrriox
 */
public final class LanguageHelper {

    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> FR = new HashMap<>();

    private LanguageHelper() {}

    static {
        // ── General ─────────────────────────────────────────────────────
        put("lootbox.opened", "Lootbox opened!", "Lootbox ouverte !");
        put("lootbox.opened.title", "Lootbox Opened", "Lootbox Ouverte");
        put("lootbox.items.received", "{count} item(s) received!", "{count} objet(s) recu(s) !");
        put("lootbox.no.permission", "You don't have permission to open this lootbox.", "Vous n'avez pas la permission d'ouvrir cette lootbox.");
        put("lootbox.sneak.required", "Sneak + Right Click to open this lootbox.", "Accroupissez-vous + Clic Droit pour ouvrir cette lootbox.");
        put("lootbox.cooldown", "Cooldown: {time} remaining.", "Cooldown : {time} restant.");
        put("lootbox.autoclicker", "Too many openings! Please wait.", "Trop d'ouvertures ! Veuillez patienter.");
        put("lootbox.no.uses", "This lootbox has no remaining uses.", "Cette lootbox n'a plus d'utilisations restantes.");
        put("lootbox.no.key", "You need the correct key to open this lootbox.", "Vous avez besoin de la bonne cle pour ouvrir cette lootbox.");
        put("lootbox.ready", "Ready!", "Pret !");

        // ── Preview GUI ─────────────────────────────────────────────────
        put("preview.rarity", "Rarity", "Rarete");
        put("preview.key", "Key", "Cle");
        put("preview.type", "Type", "Type");
        put("preview.items", "Items", "Objets");
        put("preview.guaranteed", "Guaranteed", "Garanti");
        put("preview.chance", "Chance", "Chance");
        put("preview.quantity", "Quantity", "Quantite");
        put("preview.weight", "weight", "poids");
        put("preview.click.to.open", "Right-click with key to open", "Clic droit avec la cle pour ouvrir");
        put("preview.draw.button", "Draw!", "Tirer !");
        put("preview.draw.button.lore", "Click to open this lootbox", "Cliquez pour ouvrir cette lootbox");
        put("preview.type.weighted", "Weighted (% per item)", "Pondere (% par objet)");
        put("preview.type.guaranteed", "Guaranteed (1 item + guaranteed)", "Garanti (1 objet + garanti)");
        put("preview.free.available", "FREE claim available!", "Reclamation GRATUITE disponible !");
        put("preview.free.cooldown", "Free in: {time}", "Gratuit dans : {time}");

        // ── Commands ────────────────────────────────────────────────────
        put("cmd.reloading", "Reloading...", "Rechargement...");
        put("cmd.reloaded", "Reloaded {count} lootbox definitions.", "Recharge : {count} definitions de lootbox.");
        put("cmd.not.found", "Lootbox not found: {id}", "Lootbox introuvable : {id}");
        put("cmd.already.exists", "Lootbox already exists: {id}", "Lootbox deja existante : {id}");
        put("cmd.created", "Created '{id}'. Edit the JSON file to add loot.", "Cree '{id}'. Modifiez le fichier JSON pour ajouter du butin.");
        put("cmd.deleted", "Deleted: {id}", "Supprime : {id}");
        put("cmd.gave", "Gave {amount}x '{id}' to {player}.", "Donne {amount}x '{id}' a {player}.");
        put("cmd.gave.all", "Gave {amount}x '{id}' to {count} players.", "Donne {amount}x '{id}' a {count} joueurs.");
        put("cmd.gave.key", "Gave {amount}x {key} to {player}.", "Donne {amount}x {key} a {player}.");
        put("cmd.no.lootboxes", "No lootbox definitions found.", "Aucune definition de lootbox trouvee.");
        put("cmd.lootboxes", "Lootboxes ({count}):", "Lootbox ({count}) :");
        put("cmd.keys", "Registered Keys ({count}):", "Cles enregistrees ({count}) :");
        put("cmd.no.history", "{player} has no lootbox history.", "{player} n'a aucun historique de lootbox.");
        put("cmd.history", "History for {player} ({count} entries):", "Historique de {player} ({count} entrees) :");
        put("cmd.history.cleared", "Cleared history for {player}.", "Historique supprime pour {player}.");
        put("cmd.cooldown.reset", "Reset cooldowns for {player}.", "Cooldowns reinitialises pour {player}.");
        put("cmd.uses.set", "Set uses to {uses} at {pos}.", "Utilisations definies a {uses} en {pos}.");
        put("cmd.no.lootbox.at.pos", "No lootbox at that position.", "Aucune lootbox a cette position.");
        put("cmd.must.be.player", "This command must be run by a player.", "Cette commande doit etre executee par un joueur.");

        // ── Free lootbox ────────────────────────────────────────────────
        put("free.disabled", "Free claiming is not enabled for '{id}'.", "La reclamation gratuite n'est pas activee pour '{id}'.");
        put("free.not.ready", "{player} cannot claim '{id}' yet. Remaining: {time}", "{player} ne peut pas encore reclamer '{id}'. Restant : {time}");
        put("free.claimed", "Free lootbox '{id}' claimed by {player}!", "Lootbox gratuite '{id}' reclamee par {player} !");
        put("free.claimed.player", "You claimed a free {name}!", "Vous avez reclame un(e) {name} gratuit(e) !");
        put("free.ready", "Ready to claim!", "Pret a reclamer !");
        put("free.remaining", "Remaining: {time}", "Restant : {time}");
        put("free.timer.title", "Free lootbox '{id}' for {player}:", "Lootbox gratuite '{id}' pour {player} :");
        put("free.reset.one", "Reset free timer for '{id}' for {player}.", "Timer gratuit reinitialise pour '{id}' pour {player}.");
        put("free.reset.all", "Reset ALL free timers for {player}.", "TOUS les timers gratuits reinitialises pour {player}.");

        // ── Stats ───────────────────────────────────────────────────────
        put("stats.title", "Arcadia Lootbox Stats", "Statistiques Arcadia Lootbox");
        put("stats.definitions", "Definitions", "Definitions");
        put("stats.keys", "Keys", "Cles");
        put("stats.hub", "Hub", "Hub");
        put("stats.broadcast", "Broadcast", "Diffusion");
        put("stats.shop", "Shop URL", "URL Boutique");

        // ── Key tooltip ─────────────────────────────────────────────────
        put("key.tooltip", "Use on a Lootbox to open it", "Utilisez sur une Lootbox pour l'ouvrir");
        put("key.tooltip.mod", "Arcadia Lootbox Key", "Cle Arcadia Lootbox");

        // ── Rarity names ────────────────────────────────────────────────
        put("rarity.common", "Common", "Commune");
        put("rarity.uncommon", "Uncommon", "Peu commune");
        put("rarity.rare", "Rare", "Rare");
        put("rarity.epic", "Epic", "Epique");
        put("rarity.legendary", "Legendary", "Legendaire");
        put("rarity.mythic", "Mythic", "Mythique");

        // ── Hub screen ──────────────────────────────────────────────────
        put("hub.title", "Lootbox Hub", "Hub Lootbox");
        put("hub.subtitle", "Browse available lootboxes", "Parcourir les lootbox disponibles");
        put("hub.no.lootboxes", "No lootboxes available", "Aucune lootbox disponible");
        put("hub.buy.keys", "Buy Lootbox Keys", "Acheter des Cles de Lootbox");
        put("hub.close", "ESC to close", "ESC pour fermer");
        put("hub.drops", "{count} drops", "{count} drops");
        put("hub.type", "Type: {type}", "Type : {type}");

        // ── Lootbox info lore ───────────────────────────────────────────
        put("lore.rarity", "Rarity: {rarity}", "Rarete : {rarity}");
        put("lore.key", "Key: {key}", "Cle : {key}");
        put("lore.type", "Type: {type}", "Type : {type}");
        put("lore.max.uses", "Max Uses: {uses}", "Utilisations max : {uses}");
    }

    // ── API ─────────────────────────────────────────────────────────────

    /**
     * Gets a translated string based on the player's client language.
     */
    public static String get(ServerPlayer player, String key) {
        boolean isFrench = isFrench(player);
        return isFrench ? FR.getOrDefault(key, EN.getOrDefault(key, key)) : EN.getOrDefault(key, key);
    }

    /**
     * Gets a translated string with placeholder replacement.
     */
    public static String get(ServerPlayer player, String key, Map<String, String> placeholders) {
        String result = get(player, key);
        for (var entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Gets a translated string with a single placeholder.
     */
    public static String get(ServerPlayer player, String key, String placeholder, String value) {
        return get(player, key).replace("{" + placeholder + "}", value);
    }

    /**
     * Gets the EN version of a key (for logs, non-player contexts).
     */
    public static String getEN(String key) {
        return EN.getOrDefault(key, key);
    }

    /**
     * Gets the FR version of a key.
     */
    public static String getFR(String key) {
        return FR.getOrDefault(key, EN.getOrDefault(key, key));
    }

    /**
     * Detects if a player's client is set to French.
     */
    public static boolean isFrench(ServerPlayer player) {
        if (player == null || player.clientInformation() == null) return false;
        String lang = player.clientInformation().language();
        return lang != null && lang.startsWith("fr");
    }

    /**
     * Gets the localized rarity display name.
     */
    public static String getRarityName(ServerPlayer player, String rarity) {
        if (rarity == null) return get(player, "rarity.common");
        return get(player, "rarity." + rarity.toLowerCase());
    }

    // ── Internal ────────────────────────────────────────────────────────

    private static void put(String key, String en, String fr) {
        EN.put(key, en);
        FR.put(key, fr);
    }
}
