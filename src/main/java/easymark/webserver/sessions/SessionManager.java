package easymark.webserver.sessions;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class SessionManager {
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public void register(Session session) {
        this.sessions.put(session.getId(), session);
    }

    public Session get(UUID sessionId) throws NotRegisteredException, ExpiredException {
        if (sessionId == null || !this.sessions.containsKey(sessionId))
            throw new NotRegisteredException();
        Session session = this.sessions.get(sessionId);
        LocalDateTime lastSessionAction = session.getLastSessionAction();
        if (lastSessionAction == null || LocalDateTime.now().minusHours(5).isAfter(lastSessionAction)) {
            this.sessions.remove(session.getId());
            throw new ExpiredException();
        }
        return session;
    }

    public void revoke(UUID sessionId) {
        this.sessions.remove(sessionId);
    }

    public Stream<Session> getAllOfUser(UUID userId) {
        return this.sessions.values()
                .stream()
                .filter(session -> session.getUserId().equals(userId));
    }

    public void revokeAllSessionsOfUser(UUID userId) {
        getAllOfUser(userId)
                .map(Session::getId)
                .forEach(this::revoke);
    }

    public static class NotRegisteredException extends Exception {
    }

    public static class ExpiredException extends Exception {
    }
}
