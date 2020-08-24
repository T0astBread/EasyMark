package easymark.database;

import com.google.gson.*;
import easymark.database.models.*;

import java.io.*;
import java.util.concurrent.locks.*;

public class DBMS {
    private static final File DATABASE_FILE = new File("database.json");
    private static final Gson GSON = new Gson();

    private static final ReadWriteLock dbLock = new ReentrantReadWriteLock();
    private static Database database = new Database();

    public static DatabaseHandle openRead() {
        Lock lock = dbLock.readLock();
        lock.lock();
        return new DatabaseHandle(lock, database);
    }

    public static DatabaseHandle openWrite() {
        Lock lock = dbLock.writeLock();
        lock.lock();
        return new DatabaseHandle(lock, database);
    }

    public static void load() throws IOException {
        if (!DATABASE_FILE.exists())
            return;

        try (FileReader reader = new FileReader(DATABASE_FILE)) {
            database = GSON.fromJson(reader, Database.class);
        }
    }

    public static void replace(Database database) {
        DBMS.database = database;
    }

    public static void store() throws IOException {
        try (FileWriter writer = new FileWriter(DATABASE_FILE)) {
            GSON.toJson(database, writer);
        }
    }
}
