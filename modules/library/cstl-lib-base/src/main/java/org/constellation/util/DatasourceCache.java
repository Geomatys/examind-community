package org.constellation.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.sql.DataSource;
import org.apache.sis.util.collection.BackingStoreException;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 * This should be a Spring component synchronized with application lifecycle. But for now, it would cause more
 * boilerplate.
 */
public class DatasourceCache {

    private Timer timer;
    private final Map<DbKey, DataSource> datasources = new ConcurrentHashMap<>();

    public DataSource getOrCreate(HikariConfig dbConf) {
        final DbKey key = new DbKey(dbConf);
        return datasources.computeIfAbsent(key, this::createDatasource);
    }

    private DataSource createDatasource(final DbKey key) {
        final HikariDataSource datasource = new HikariDataSource(key.config);
        if (key.config.isRegisterMbeans()) {
            listenJMX(datasource);
        }
        return datasource;
    }

    private void listenJMX(HikariDataSource datasource) {
        final Logger logger = Logger.getLogger("com.examind.community.storage.sql.metrics");
        final String poolName = datasource.getPoolName();
        final String jmxId = String.format("com.zaxxer.hikari:type=Pool (%s)", poolName);
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final HikariPoolMXBean poolJmx;
        try {
            poolJmx = JMX.newMXBeanProxy(mBeanServer, new ObjectName(jmxId), HikariPoolMXBean.class);
        } catch (MalformedObjectNameException e) {
            throw new BackingStoreException(e);
        }

        final Timer timer = getOrCreateTimer();
        timer.schedule(logTask(poolName, poolJmx, logger), 2000, 2000);
    }

    private TimerTask logTask(String poolName, HikariPoolMXBean poolJmx, Logger logger) {
        return new TimerTask() {
            @Override
            public void run() {
                logger.log(Level.FINE, () -> report(poolName, poolJmx));
            }
        };
    }

    private String report(String poolName, HikariPoolMXBean poolJmx) {
        return String.format(
                "%n-- SQL Connection Pool report (%s) --%n" +
                        "Idle           : %d%n" +
                        "Active         : %d%n" +
                        "Total          : %d%n" +
                        "Waiting threads: %d%n" +
                        "--%n",
                poolName,
                poolJmx.getIdleConnections(),
                poolJmx.getActiveConnections(),
                poolJmx.getTotalConnections(),
                poolJmx.getThreadsAwaitingConnection()
        );
    }

    private synchronized Timer getOrCreateTimer() {
        if (timer == null) {
            timer = new Timer("HikariCP Pool JMX", true);
        }
        return timer;

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
