package easymark.database;

import easymark.database.models.*;

import java.util.concurrent.locks.*;

public class DatabaseHandle implements AutoCloseable {
    private final Lock lock;
    private final Database database;

    public DatabaseHandle(Lock lock, Database database) {
        this.lock = lock;
        this.database = database;
    }

    public Database get() {
        return database;
    }

    @Override
    public void close() throws RuntimeException {
        this.lock.unlock();
    }
}
