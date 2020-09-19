package easymark.webserver;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.constants.*;
import easymark.webserver.sessions.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class WebServerUtils {

    public static Session getSession(SessionManager sessionManager, Context ctx) {
        UUID sessionId = ctx.sessionAttribute(SessionKeys.SESSION_ID);
        try {
            return sessionManager.get(sessionId);
        } catch (SessionManager.NotRegisteredException | SessionManager.ExpiredException e) {
            logOut(sessionManager, ctx);
            throw new InternalServerErrorResponse("Lost session. Maybe it was revoked by someone else?\nPlease refresh the page.");
        }
    }

    public static String getUek(Context ctx, Session session) {
        String set = ctx.cookie(CookieKeys.SET);
        if (set == null)
            throw new BadRequestResponse("SET not set");
        String sek = session.getSek();
        if (sek == null)
            throw new BadRequestResponse("SEK not set");
        String sekSalt = session.getSekSalt();
        if (sekSalt == null)
            throw new BadRequestResponse("SEK salt not set");
        return Cryptography.decryptUEK(sek, sekSalt, set);
    }

    public static <E extends Entity> E checkAccessTokenMatch(
            List<E> table,
            String providedIdentifier,
            String providedSecret,
            Function<E, AccessToken> getAccessToken
    ) {
        Optional<E> matchingParticipant = table
                .stream()
                .filter(entity -> getAccessToken
                        .apply(entity)
                        .getIdentifier()
                        .equals(providedIdentifier))
                .findAny();
        if (matchingParticipant.isPresent()) {
            String participantSecret = getAccessToken
                    .apply(matchingParticipant.get())
                    .getSecret();
            if (Utils.PASSWORD_ENCODER.matches(providedSecret, participantSecret)) {
                return matchingParticipant.get();
            }
        }
        return null;
    }

    public static CSRFToken makeCSRFToken(Context ctx) {
        CSRFToken newToken = CSRFToken.random();

        List<CSRFToken> sessionCsrfTokens = ctx.sessionAttribute(SessionKeys.CSRF_TOKENS);
        if (sessionCsrfTokens == null) {
            sessionCsrfTokens = new ArrayList<>();
        } else {
            filterInvalidCSRFTokens(ctx);
        }
        sessionCsrfTokens.add(newToken);
        ctx.sessionAttribute(SessionKeys.CSRF_TOKENS, sessionCsrfTokens);
        ctx.header("Cache-Control", "no-store");  // Needed so that back button doesn't break

        return newToken;
    }

    public static boolean checkCSRFToken(Context ctx, String providedToken) {
        if (providedToken == null)
            return false;

        List<CSRFToken> sessionCsrfTokens = ctx.sessionAttribute(SessionKeys.CSRF_TOKENS);
        if (sessionCsrfTokens == null)
            return false;
        filterInvalidCSRFTokens(ctx);

        Optional<CSRFToken> match = sessionCsrfTokens
                .stream()
                .filter(csrfToken -> csrfToken.getValue().equals(providedToken))
                .findAny();

        match.ifPresent(sessionCsrfTokens::remove);
        ctx.sessionAttribute(SessionKeys.CSRF_TOKENS, sessionCsrfTokens);
        return match.isPresent();
    }

    public static void filterInvalidCSRFTokens(Context ctx) {
        List<CSRFToken> sessionCsrfTokens = ctx.sessionAttribute(SessionKeys.CSRF_TOKENS);
        if (sessionCsrfTokens != null) {
            sessionCsrfTokens = sessionCsrfTokens
                    .stream()
                    .filter(CSRFToken::isValid)
                    .collect(Collectors.toList());
            ctx.sessionAttribute(SessionKeys.CSRF_TOKENS, sessionCsrfTokens);
        }
    }

    public static void logIn(SessionManager sessionManager, Context ctx, String providedAccessTokenStr) {
        final ForbiddenResponse FORBIDDEN = new ForbiddenResponse("Forbidden");

        if (providedAccessTokenStr == null || providedAccessTokenStr.length() != Cryptography.ACCESS_TOKEN_LENGTH)
            throw FORBIDDEN;
        String providedIdentifier = providedAccessTokenStr.substring(0, Cryptography.ACCESS_TOKEN_IDENTIFIER_LENGTH);
        String providedSecret = providedAccessTokenStr.substring(Cryptography.ACCESS_TOKEN_IDENTIFIER_LENGTH);

        try (DatabaseHandle dbHandle = DBMS.openRead()) {
            Session newSession = null;
            String creationIPAddress = getRemoteIPAddress(ctx);

            Admin matchingAdmin = checkAccessTokenMatch(
                    dbHandle.get().getAdmins(),
                    providedIdentifier, providedSecret,
                    Admin::getAccessToken);
            if (matchingAdmin != null) {
                String iek = matchingAdmin.getIek();
                String iekSalt = matchingAdmin.getIekSalt();
                String uek = Cryptography.decryptUEK(iek, iekSalt, providedAccessTokenStr);
                String set = Cryptography.generateSET();
                String sekSalt = Cryptography.generateEncryptionSalt();
                String sek = Cryptography.encryptUEK(uek, sekSalt, set);
                newSession = Session.forAdmin(matchingAdmin, creationIPAddress, sek, sekSalt);
                ctx.cookie(CookieKeys.SET, set);
            } else {
                Participant matchingParticipant = checkAccessTokenMatch(
                        dbHandle.get().getParticipants(),
                        providedIdentifier, providedSecret,
                        Participant::getCat);
                if (matchingParticipant != null) {
                    newSession = Session.forParticipant(matchingParticipant, creationIPAddress);
                }
            }
            if (newSession == null)
                throw FORBIDDEN;
            ctx.sessionAttribute(SessionKeys.SESSION_ID, newSession.getId());
            sessionManager.register(newSession);
        }
        ctx.req.changeSessionId();
    }

    public static void logOut(SessionManager sessionManager, Context ctx) {
        UUID sessionId = ctx.sessionAttribute(SessionKeys.SESSION_ID);
        if (sessionId != null) {
            sessionManager.revoke(sessionId);
            ctx.sessionAttribute(SessionKeys.SESSION_ID, null);
        }
        ctx.removeCookie(CookieKeys.SET);
        ctx.req.changeSessionId();
    }

    public static void checkCSRFFormSubmission(Context ctx) {
        if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
            throw new ForbiddenResponse("Bad CSRF token");
    }

    public static UUID getValidIDPathParam(Context ctx) {
        try {
            return UUID.fromString(ctx.pathParam(PathParams.ID));
        } catch (Exception e) {
            throw new BadRequestResponse("Bad request");
        }
    }

    public static void redirectFromForm(Context ctx) {
        String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
        ctx.redirect(redirectUrl == null ? "/" : redirectUrl);
    }

    public static void logActivity(Database db, Session session, String text) {
        ActivityLogItem logItem = new ActivityLogItem();
        logItem.setAdminId(session.getUserId());
        logItem.setTimestamp(LocalDateTime.now());
        logItem.setOriginatingSessionId(session.getId());
        logItem.setOriginatingSessionColorR(session.getColor().getRed());
        logItem.setOriginatingSessionColorG(session.getColor().getGreen());
        logItem.setOriginatingSessionColorB(session.getColor().getBlue());
        logItem.setText(text);
        db.getActivityLogItems().add(logItem);
    }

    public static String getRemoteIPAddress(Context ctx) {
        String ipAddr = ctx.req.getHeader("X-Forwarded-For");
        if (ipAddr == null) ipAddr = ctx.req.getRemoteAddr();
        return ipAddr;
    }
}
