package space.blockway.social.velocity.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.util.Map;

/**
 * Loads and exposes all configuration values from config.yml in the plugin data directory.
 * Supports {@link #reload()} to re-read from disk without restarting the proxy.
 *
 * @author Enzonic LLC — blockway.space
 */
public class VelocityConfig {

    private final Path dataDirectory;
    private final Logger logger;

    // Database
    private String dbType = "sqlite";
    private String sqlitePath = "blockwaysocial.db";
    private String mysqlHost = "localhost";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "blockwaysocial";
    private String mysqlUsername = "root";
    private String mysqlPassword = "";
    private int hikariMaxPoolSize = 10;
    private int hikariMinIdle = 2;
    private long hikariConnectionTimeout = 30000L;
    private long hikariMaxLifetime = 1800000L;

    // API
    private boolean apiEnabled = true;
    private int apiPort = 25580;
    private String apiBind = "0.0.0.0";
    private String masterKey = "CHANGE_ME_NOW_master_key";

    // Social
    private int linkCodeExpiryMinutes = 10;
    private int maxFriends = 200;
    private int maxPartySize = 10;
    private int chatHistoryLimit = 100;

    public VelocityConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    /** Copy the bundled config.yml to the data directory if it does not already exist. */
    public void saveDefault() {
        try {
            Files.createDirectories(dataDirectory);
            Path target = dataDirectory.resolve("config.yml");
            if (!Files.exists(target)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.copy(in, target);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to save default config.yml", e);
        }
    }

    /** Load (or reload) configuration from disk. */
    @SuppressWarnings("unchecked")
    public void load() {
        saveDefault();
        Path configFile = dataDirectory.resolve("config.yml");
        try (InputStream in = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            if (root == null) return;

            // Database
            Map<String, Object> db = (Map<String, Object>) root.getOrDefault("database", Map.of());
            dbType = getString(db, "type", "sqlite");
            sqlitePath = getString(db, "sqlite-path", "blockwaysocial.db");
            Map<String, Object> mysql = (Map<String, Object>) db.getOrDefault("mysql", Map.of());
            mysqlHost = getString(mysql, "host", "localhost");
            mysqlPort = getInt(mysql, "port", 3306);
            mysqlDatabase = getString(mysql, "database", "blockwaysocial");
            mysqlUsername = getString(mysql, "username", "root");
            mysqlPassword = getString(mysql, "password", "");
            hikariMaxPoolSize = getInt(mysql, "maximum-pool-size", 10);
            hikariMinIdle = getInt(mysql, "minimum-idle", 2);
            hikariConnectionTimeout = getLong(mysql, "connection-timeout", 30000L);
            hikariMaxLifetime = getLong(mysql, "max-lifetime", 1800000L);

            // API
            Map<String, Object> api = (Map<String, Object>) root.getOrDefault("api", Map.of());
            apiEnabled = getBool(api, "enabled", true);
            apiPort = getInt(api, "port", 25580);
            apiBind = getString(api, "bind", "0.0.0.0");
            masterKey = getString(api, "master-key", "CHANGE_ME_NOW_master_key");

            // Social
            Map<String, Object> social = (Map<String, Object>) root.getOrDefault("social", Map.of());
            linkCodeExpiryMinutes = getInt(social, "link-code-expiry-minutes", 10);
            maxFriends = getInt(social, "max-friends", 200);
            maxPartySize = getInt(social, "max-party-size", 10);
            chatHistoryLimit = getInt(social, "chat-history-limit", 100);

            logger.info("Configuration loaded from {}", configFile);
        } catch (IOException e) {
            logger.error("Failed to load config.yml, using defaults", e);
        }
    }

    public void reload() {
        load();
        logger.info("Configuration reloaded.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String getString(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return v instanceof String s ? s : def;
    }

    private int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }

    private long getLong(Map<String, Object> map, String key, long def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.longValue();
        return def;
    }

    private boolean getBool(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        return def;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getDbType() { return dbType; }
    public String getSqlitePath() { return sqlitePath; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public int getHikariMaxPoolSize() { return hikariMaxPoolSize; }
    public int getHikariMinIdle() { return hikariMinIdle; }
    public long getHikariConnectionTimeout() { return hikariConnectionTimeout; }
    public long getHikariMaxLifetime() { return hikariMaxLifetime; }
    public boolean isApiEnabled() { return apiEnabled; }
    public int getApiPort() { return apiPort; }
    public String getApiBind() { return apiBind; }
    public String getMasterKey() { return masterKey; }
    public int getLinkCodeExpiryMinutes() { return linkCodeExpiryMinutes; }
    public int getMaxFriends() { return maxFriends; }
    public int getMaxPartySize() { return maxPartySize; }
    public int getChatHistoryLimit() { return chatHistoryLimit; }
    public Path getDataDirectory() { return dataDirectory; }
}
