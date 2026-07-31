package com.godlights.edplugin.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * EdPlugin's own currency store. Doesn't depend on Vault or any external
 * economy plugin - balances live in economy.yml. {@link VaultEconomyBridge}
 * optionally exposes this same store through Vault's Economy API so other
 * Vault-aware plugins can see it too.
 */
public final class EconomyHook {

    /** Reserved account id for the server treasury - never a real player's UUID (Mojang UUIDs are never nil). */
    public static final UUID TREASURY_ID = new UUID(0L, 0L);

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final double startingBalance;
    private final String currencySingular;
    private final String currencyPlural;

    public EconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "economy.yml");
        this.startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);
        this.currencySingular = plugin.getConfig().getString("economy.currency-singular", "코인");
        this.currencyPlural = plugin.getConfig().getString("economy.currency-plural", "코인");
        load();
    }

    public boolean isAvailable() {
        return true;
    }

    public double getBalance(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, startingBalance);
            save();
        }
        return balances.get(uuid);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return false;
        }
        double balance = getBalance(player);
        if (balance < amount) {
            return false;
        }
        balances.put(player.getUniqueId(), balance - amount);
        save();
        return true;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return false;
        }
        balances.put(player.getUniqueId(), getBalance(player) + amount);
        save();
        return true;
    }

    /** Server treasury: funded by waystone fees and shop tax, spent on bounty payouts. Can go negative. */
    public double getTreasuryBalance() {
        return balances.getOrDefault(TREASURY_ID, 0.0);
    }

    public void depositTreasury(double amount) {
        if (amount <= 0) {
            return;
        }
        balances.merge(TREASURY_ID, amount, Double::sum);
        save();
    }

    public void withdrawTreasury(double amount) {
        if (amount <= 0) {
            return;
        }
        balances.merge(TREASURY_ID, -amount, Double::sum);
        save();
    }

    public String format(double amount) {
        String currency = amount == 1.0 ? currencySingular : currencyPlural;
        return String.format(Locale.KOREA, "%,.2f %s", amount, currency);
    }

    public String currencyPlural() {
        return currencyPlural;
    }

    public String currencySingular() {
        return currencySingular;
    }

    public void load() {
        balances.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            try {
                balances.put(UUID.fromString(key), yaml.getDouble(key));
            } catch (IllegalArgumentException ignored) {
                // malformed entry, skip
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            yaml.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save economy.yml", e);
        }
    }
}
