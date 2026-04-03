package com.github.aliandr13.zenmo.migrate;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

public final class MigrateMain {

    private MigrateMain() {
    }

    static void main(String[] args) {
        String url = firstNonBlank(
                System.getenv("ZENMO_DATASOURCE_URL"),
                System.getProperty("zenmo.datasource.url"),
                "jdbc:postgresql://localhost:5432/zenmo");
        String user = firstNonBlank(
                System.getenv("ZENMO_DATASOURCE_USERNAME"),
                System.getProperty("zenmo.datasource.username"),
                "zenmo");
        String password = firstNonBlank(
                System.getenv("ZENMO_DATASOURCE_PASSWORD"),
                System.getProperty("zenmo.datasource.password"),
                "zenmo");

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();
        System.out.printf(
                "Flyway migrate finished: %d migration(s) applied, success=%s%n",
                result.migrationsExecuted,
                result.success);
        if (!result.success) {
            System.exit(1);
        }
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return "";
    }
}
