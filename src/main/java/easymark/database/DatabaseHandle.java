package easymark.database;

import easymark.database.models.*;

import java.util.concurrent.locks.*;

public class DatabaseHandle implements AutoCloseable {
    private final Lock lock;
    private final Database database;
    private final long takeTime;

    public DatabaseHandle(Lock lock, Database database, long takeTime) {
        this.lock = lock;
        this.database = database;
        this.takeTime = takeTime;
    }

    public Database get() {
        return database;
    }

    @Override
    public void close() throws RuntimeException {
        this.lock.unlock();

        long releaseTime = System.nanoTime();
        long holdTime = (releaseTime - this.takeTime) / 1_000_000;
        String lockType = this.lock.getClass().getSimpleName();
        System.out.println(Double.toString(Math.random()).substring(2, 4) + " " + lockType + " held for " + holdTime + "ms");
    }
}
