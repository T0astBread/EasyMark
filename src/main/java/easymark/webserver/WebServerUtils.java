package easymark.webserver;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.constants.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class WebServerUtils {

    public static String getUekFromContext(Context ctx) {
        String set = ctx.cookie(CookieKeys.SET);
        if (set == null)
            throw new BadRequestResponse("SET not set");
        String sek = ctx.sessionAttribute(SessionKeys.SEK);
        if (sek == null)
            throw new BadRequestResponse("SEK not set");
        String sekSalt = ctx.sessionAttribute(SessionKeys.SEK_SALT);
        if (sekSalt == null)
            throw new BadRequestResponse("SEK salt not set");
        return Cryptography.decryptUEK(sek, sekSalt, set);
    }

    public static <E extends Entity> E checkAccessTokenMatch(
            Context ctx,
            List<E> table,
            String providedIdentifier,
            String providedSecret,
            Function<E, AccessToken> getAccessToken,
            UserRole roleIfMatch
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
                ctx.sessionAttribute(SessionKeys.ROLES, Set.of(roleIfMatch));
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

    public static boolean checkForExpiredSession(Context ctx) {
        LocalDateTime lastSessionAction = ctx.sessionAttribute(SessionKeys.LAST_SESSION_ACTION);
        if (lastSessionAction == null || LocalDateTime.now().minusHours(5).isAfter(lastSessionAction)) {
            logOut(ctx);
            ctx.redirect("/?message=sessionExpired");
            return true;
        } else {
            ctx.sessionAttribute(SessionKeys.LAST_SESSION_ACTION, LocalDateTime.now());
            return false;
        }
    }

    public static boolean isLoggedIn(Context ctx) {
        Set<UserRole> roles = ctx.sessionAttribute(SessionKeys.ROLES);
        return roles != null && (roles.contains(UserRole.ADMIN) || roles.contains(UserRole.PARTICIPANT));
    }

    public static void logIn(Context ctx, String providedAccessTokenStr) {
        final ForbiddenResponse FORBIDDEN = new ForbiddenResponse("Forbidden");

        if (providedAccessTokenStr == null || providedAccessTokenStr.length() != Cryptography.ACCESS_TOKEN_LENGTH)
            throw FORBIDDEN;
        String providedIdentifier = providedAccessTokenStr.substring(0, Cryptography.ACCESS_TOKEN_IDENTIFIER_LENGTH);
        String providedSecret = providedAccessTokenStr.substring(Cryptography.ACCESS_TOKEN_IDENTIFIER_LENGTH);

        try (DatabaseHandle dbHandle = DBMS.openRead()) {
            Admin matchingAdmin = checkAccessTokenMatch(
                    ctx, dbHandle.get().getAdmins(),
                    providedIdentifier, providedSecret,
                    Admin::getAccessToken,
                    UserRole.ADMIN);
            Entity matchingEntity = matchingAdmin;

            if (matchingEntity == null) {
                matchingEntity = checkAccessTokenMatch(
                        ctx, dbHandle.get().getParticipants(),
                        providedIdentifier, providedSecret,
                        Participant::getCat,
                        UserRole.PARTICIPANT);
            } else {
                String iek = matchingAdmin.getIek();
                String iekSalt = matchingAdmin.getIekSalt();
                String uek = Cryptography.decryptUEK(iek, iekSalt, providedAccessTokenStr);
                String set = Cryptography.generateSET();
                String sekSalt = Cryptography.generateEncryptionSalt();
                String sek = Cryptography.encryptUEK(uek, sekSalt, set);
                ctx.cookie(CookieKeys.SET, set);
                ctx.sessionAttribute(SessionKeys.SEK, sek);
                ctx.sessionAttribute(SessionKeys.SEK_SALT, sekSalt);
                ctx.sessionAttribute(SessionKeys.NAME_DISPLAY, null);
                ctx.sessionAttribute(SessionKeys.AT_DISPLAY, null);
            }
            if (matchingEntity == null)
                throw FORBIDDEN;
            ctx.sessionAttribute(SessionKeys.ENTITY_ID, matchingEntity.getId());
        }
        ctx.req.changeSessionId();
    }

    public static void logOut(Context ctx) {
        ctx.sessionAttribute(SessionKeys.CSRF_TOKENS, null);
        ctx.sessionAttribute(SessionKeys.ROLES, null);
        ctx.sessionAttribute(SessionKeys.ENTITY_ID, null);
        ctx.sessionAttribute(SessionKeys.SEK, null);
        ctx.sessionAttribute(SessionKeys.SEK_SALT, null);
        ctx.sessionAttribute(SessionKeys.NAME_DISPLAY, null);
        ctx.sessionAttribute(SessionKeys.AT_DISPLAY, null);
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
}
