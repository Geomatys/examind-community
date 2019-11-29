package org.constellation.provider.datastore;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

import org.apache.sis.storage.ConcurrentReadException;
import org.apache.sis.storage.ConcurrentWriteException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;

/**
 * Manage access to underlying datastore, providing concurrency safety. Its use is relatively easy. When modifying
 * storage state (reload, dispose, add, remove), create a new exclusive session using {@link #write()} method. If you
 * just want to explore data, create a non-blocking session through {@link #read()}. Once done, the session will provide
 * {@link Session#handle()} method to directly access underlying {@link DataStore}.
 *
 * IMPORTANT:
 * <ul>
 *     <li>Sessions are NOT re-entrant, please do not nest multiple sessions.</li>
 *     <li>You must close a session immediately after use to avoid dead locks on resource. Please use try-with-resource construct.</li>
 * </ul>
 * TODO: We could create a phantom reference system to manage sessions that have not been closed properly.
 */
class SafeAccess {

    /**
     * Maximum number of seconds to wait for a lock to be available. After that, throw error.
     */
    private static final long LOCK_TIMEOUT = 60;
    public static final TimeUnit TIMEOUT_UNIT = TimeUnit.SECONDS;

    /**
     * The synchronization management component. Beware that it's not reentrant. It's more complex than many other
     * synchronization solutions, but provide an efficient and safe way to upgrade a lock from read to write access.
     */
    private final StampedLock lock;

    private final DataStoreProvider provider;

    /**
     * SIS data-store access. Should NEVER be acccessed directly. Instead, please create a {@link Session} and call
     * {@link Session#handle()}. The only moment when we use it directly is when closing it, because there's no need to
     * release it if it is not yet initialized.
     */
    private DataStoreHandle handle;

    SafeAccess(final DataStoreProvider toSecure) {
        this.provider = toSecure;
        this.lock = new StampedLock();
    }

    /**
     * IMPORTANT:
     * <ul>
     *     <li>Sessions are NOT re-entrant, please do not nest multiple sessions.</li>
     *     <li>You must close a session immediately after use to avoid dead locks on resource. Please use try-with-resource construct.</li>
     * </ul>
     *
     * @return A new session, allowing multiple threads to read data concurrently (not blocking each other). To modify
     * underlying datastore state, please use {@link #write()} instead.
     */
    Session read() {
        return new ReadSession();
    }

    /**
     * IMPORTANT:
     * <ul>
     *     <li>Sessions are NOT re-entrant, please do not nest multiple sessions.</li>
     *     <li>You must close a session immediately after use to avoid dead locks on resource. Please use try-with-resource construct.</li>
     * </ul>
     *
     * @return A new exclusive session, allowing only current thread to access data.
     */
    Session write() {
        return new WriteSession();
    }

    /**
     * Force closing of underlying {@link DataStore} and all cached data.
     * @param session A session to perform closing operation into. Cannot be null.
     * @throws DataStoreException If data store closing fails.
     */
    void disposeDataStore(Session session) throws DataStoreException {
        session.write(() -> {
            if (handle != null) {
                try {
                    handle.close();
                } finally {
                    handle = null;
                }
            }
            return null;
        });
    }

    private long tryLock(final boolean write) {
        try {
            final long stamp = write ?
                    lock.tryWriteLock(LOCK_TIMEOUT, TIMEOUT_UNIT) : lock.tryReadLock(LOCK_TIMEOUT, TIMEOUT_UNIT);
            if (stamp == 0) throw write ? new ConcurrentReadException() : new ConcurrentWriteException();
            return stamp;

        } catch (InterruptedException | DataStoreException e) {
            final String[] args = write ? new String[]{"write", "read"} : new String[]{"read", "write"};
            throw new RuntimeException(String.format("Cannot %s data from provider because a %s operation takes a long time", args), e);
        }
    }

    /**
     * Defines a lock over source data storage. The locks lives until the session is closed. Session can be either
     * exclusive or allow for concurrent access. Read {@link SafeAccess} documentation to learn more about the way to
     * create and use sessions.
     */
    abstract class Session implements AutoCloseable {
        DataStoreHandle handle() throws DataStoreException {
            if (handle == null) {
                write(() -> {
                    /* Provider could have been updated while we were waiting for an exclusive lock. If it's the case,
                     * we do not need to reload it anymore, and we can downgrade the stamp directly.
                     */
                    if (handle == null) handle = new DataStoreHandle(provider.createBaseStore(), provider.getLogger());
                    return handle;
                });
            }
            return handle;
        }

        /**
         * Queries for a write access operation. If the session is a read-only one, this will try to temporarily
         * promote it as a write one, ensuring exclusive access all along provided operation. The session is reset to
         * its original state at the end of this call.
         * @param action User operation to execute with exclusive access to underlying data.
         * @throws ConcurrentReadException If we cannot acquire an exclusive lock because another read operation locks
         * the provider.
         * @throws DataStoreException If an error occurs while accessing inner resources, or user operation failed. It
         * includes interruption errors.
         */
        protected abstract <T> T write(Callable<T> action) throws DataStoreException;

        /**
         * Release the lock this session hands over source datastore.
         */
        @Override
        public abstract void close();
    }

    private class WriteSession extends Session {

        final long stamp;

        private WriteSession() {
            stamp = tryLock(true);
        }

        @Override
        protected <T> T write(Callable<T> action) throws DataStoreException {
            try {
                return action.call();
            } catch (RuntimeException | DataStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new DataStoreException("User operation failed", e);
            }
        }

        @Override
        public void close() {
            lock.unlockWrite(stamp);
        }
    }

    private class ReadSession extends Session {
        private long stamp;

        private ReadSession() {
            this.stamp = tryLock(false);
        }

        protected <T> T write(Callable<T> action) throws DataStoreException {
            try {
                // If we're a read session, we'll try to upgrade our privilege temporarily.
                long writeStamp = lock.tryConvertToReadLock(stamp);
                if (writeStamp == 0) {
                    writeStamp = lock.tryWriteLock(LOCK_TIMEOUT, TIMEOUT_UNIT);
                }
                if (writeStamp == 0) throw new ConcurrentReadException();
                try {
                    return action.call();
                } finally {
                    stamp = lock.tryConvertToReadLock(writeStamp);
                }
            } catch (RuntimeException | DataStoreException e) {
                throw e;
            } catch (Exception e) {
                throw new DataStoreException("User operation failed", e);
            }
        }

        @Override
        public void close() {
            lock.unlockRead(stamp);
        }
    }
}
