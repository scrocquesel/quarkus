package io.quarkus.hibernate.orm.deployment.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.cfg.AvailableSettings;

import io.quarkus.datasource.common.runtime.DatabaseKind;

public enum DatabaseOrmCompatibilityVersion {
    V5_6("5.6") {
        @Override
        public Map<String, String> settings(String dbKind) {
            Map<String, String> result = new HashMap<>(Map.of(
                    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#implicit-identifier-sequence-and-table-name
                    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#id-sequence-defaults
                    AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY, "legacy",
                    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#duration-mapping-changes
                    AvailableSettings.PREFERRED_DURATION_JDBC_TYPE, "BIGINT",
                    // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#instant-mapping-changes
                    AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE, "TIMESTAMP",
                    // https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#basic-arraycollection-mapping
                    // Not changing this for now as there's no setting and affected users should be rare, and they can fix their code rather easily.
                    // https://github.com/hibernate/hibernate-orm/blob/6.1/migration-guide.adoc#enum-mapping-changes
                    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#datatype-for-enums
                    // Not changing this because we cannot:
                    // there is no setting for this, so the schema will be incompatible.
                    // Runtime (queries, persisting) should continue to work, though.
                    // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#timezone-and-offset-storage
                    AvailableSettings.TIMEZONE_DEFAULT_STORAGE, "NORMALIZE"));

            if (!usedToSupportUuid(dbKind)) {
                // https://github.com/hibernate/hibernate-orm/blob/6.0/migration-guide.adoc#uuid-mapping-changes
                // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#uuid-mapping-changes-on-mariadb
                // https://github.com/hibernate/hibernate-orm/blob/6.2/migration-guide.adoc#uuid-mapping-changes-on-sql-server
                result.put(AvailableSettings.PREFERRED_UUID_JDBC_TYPE, "BINARY");
            }

            return result;
        }
    },
    LATEST("latest") {
        @Override
        public Map<String, String> settings(String dbKind) {
            // Nothing to add
            return Map.of();
        }
    };

    private static boolean usedToSupportUuid(String dbKind) {
        // As far as I can tell, only the PostgreSQL dialect used to support a native UUID type in ORM 5.x.
        return DatabaseKind.isPostgreSQL(dbKind);
    }

    private final String externalRepresentation;

    DatabaseOrmCompatibilityVersion(String externalRepresentation) {
        this.externalRepresentation = externalRepresentation;
    }

    public abstract Map<String, String> settings(String dbKind);

    public static class Converter
            implements org.eclipse.microprofile.config.spi.Converter<DatabaseOrmCompatibilityVersion> {
        @Override
        public DatabaseOrmCompatibilityVersion convert(String value) {
            final String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
            for (DatabaseOrmCompatibilityVersion candidate : values()) {
                if (candidate.externalRepresentation.equals(normalizedValue)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                    "Invalid ORM compatibility version: %1$s. Valid versions are: %2$s.",
                    value,
                    Arrays.stream(values())
                            .map(v -> v.externalRepresentation)
                            .collect(Collectors.toList())));
        }
    }
}
