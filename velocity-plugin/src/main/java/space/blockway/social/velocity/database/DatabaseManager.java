package space.blockway.social.velocity.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import space.blockway.social.velocity.config.VelocityConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.util.stream.Collectors;

/**
 * Owns the HikariCP DataSource and runs schema migrations on startup.
 * All other classes obtain {@link Connection} objects from this manager.
 *
 * @author Enzonic LLC — blockway.space
 */
public class DatabaseManager {

    private final VelocityConfig config;
    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(VelocityConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /** Initialise HikariCP and run schema migrations. */
    public void initialize() {
        HikariConfig hikari = new HikariConfig();

        boolean isSqlite = config.getDbType().equalsIgnoreCase("sqlite");

        if (isSqlite) {
            Path dbFile = config.getDataDirectory().resolve(config.getSqlitePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
            hikari.setMaximumPoolSize(1);          // SQLite is single-writer
            hikari.setConnectionTestQuery("SELECT 1");
            hikari.addDataSourceProperty("journal_mode", "WAL");
            hikari.addDataSourceProperty("foreign_keys", "true");
        } else {
            String url = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true",
                    config.getMysqlHost(), config.getMysqlPort(), config.getMysqlDatabase()
            );
            hikari.setJdbcUrl(url);
            hikari.setUsername(config.getMysqlUsername());
            hikari.setPassword(config.getMysqlPassword());
            hikari.setMaximumPoolSize(config.getHikariMaxPoolSize());
            hikari.setMinimumIdle(config.getHikariMinIdle());
            hikari.setConnectionTimeout(config.getHikariConnectionTimeout());
            hikari.setMaxLifetime(config.getHikariMaxLifetime());
        }

        hikari.setPoolName("BlockwaySocial");
        dataSource = new HikariDataSource(hikari);
        logger.info("Database pool created (type={})", config.getDbType());

        runMigrations(isSqlite);
    }

    private void runMigrations(boolean isSqlite) {
        String schemaResource = isSqlite ? "schema-sqlite.sql" : "schema-mysql.sql";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(schemaResource)) {
            if (in == null) {
                logger.error("Schema resource not found: {}", schemaResource);
                return;
            }
            String sql = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            try (Connection conn = getConnection()) {
                // Execute each statement individually (split on semicolons)
                for (String stmt : sql.split(";")) {
                    String trimmed = stmt.strip();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        try (Statement s = conn.createStatement()) {
                            s.execute(trimmed);
                        }
                    }
                }
            }
            logger.info("Database schema migrations applied.");
        } catch (IOException | SQLException e) {
            logger.error("Failed to run database migrations", e);
            throw new RuntimeException("Database migration failure", e);
        }
    }

    /** Obtain a connection from the pool. The caller must close it. */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Shut down the connection pool gracefully. */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database pool closed.");
        }
    }

    public boolean isSqlite() {
        return config.getDbType().equalsIgnoreCase("sqlite");
    }
}
