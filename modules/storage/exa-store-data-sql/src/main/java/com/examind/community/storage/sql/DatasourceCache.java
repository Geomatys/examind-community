package com.examind.community.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * This should be a Spring component synchronized with application lifecycle. But for now, it would cause more
 * boilerplate.
 */
class DatasourceCache {

    private final Map<DbKey, DataSource> datasources = new ConcurrentHashMap<>();

    DataSource getOrCreate(HikariConfig dbConf) {
        final DbKey key = new DbKey(dbConf);
        return datasources.computeIfAbsent(key, k -> new HikariDataSource(k.config));
    }

    private static class DbKey {
        final String url;
        final String user;
        final HikariConfig config;

        public DbKey(HikariConfig config) {
            String url = config.getJdbcUrl();
            ensureNonNull("JDBC URL", url);
            this.config = config;
            /* Remove all uri parameters, because the identity of the datasource is bound to its protocol/address/db.
             * Use the first `?` because it is a character forbidden in uri protocol/path, but it could appear in uri
             * parameter values.
             */
            final int splitIdx = url.indexOf('?');
            if (splitIdx == 0) throw new IllegalArgumentException("Cannot accept an URI whose first character is `?`");
            this.url = splitIdx < 0 ? url : url.substring(0, splitIdx);
            this.user = config.getUsername() == null ? "" : config.getUsername();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DbKey dbKey = (DbKey) o;

            if (!url.equals(dbKey.url)) return false;
            return user.equals(dbKey.user);
        }

        @Override
        public int hashCode() {
            int result = url.hashCode();
            result = 31 * result + user.hashCode();
            return result;
        }
    }
}
