/*
 * Jeffrey
 * Copyright (C) 2025 Petr Bouda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pbouda.jeffrey.init;

import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Duration;

public abstract class DatabaseUtils {

    private static final Duration DEFAULT_BUSY_TIMEOUT = Duration.ofSeconds(10);

    public static DataSource notPooled(Path dbPath) {
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.OFF);
        config.setBusyTimeout((int) DEFAULT_BUSY_TIMEOUT.toMillis());

        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl(buildUrl(dbPath));
        return dataSource;
    }

    public static void migrate(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .validateOnMigrate(true)
                .validateMigrationNaming(true)
                .locations("classpath:db/migration")
                .sqlMigrationPrefix("V")
                .sqlMigrationSeparator("__")
                .load();

        flyway.migrate();
    }

    private static String buildUrl(Path dbPath) {
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }
}
