package easymark.webserver.routes;

import easymark.database.*;
import easymark.pebble.*;
import easymark.webserver.*;
import easymark.webserver.sessions.*;
import io.javalin.*;
import io.javalin.http.*;

import java.awt.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.roles;

public class SessionRoutes {
    public static void configure(Javalin app, SessionManager sessionManager) {
        new CommonRouteBuilder("sessions")
                .withDelete(roles(UserRole.ADMIN, UserRole.PARTICIPANT), (ctx, sessionId) -> {
                    Session ownSession = getSession(sessionManager, ctx);
                    Session toRevoke = sessionManager.get(sessionId);

                    if (!toRevoke.getUserId().equals(ownSession.getUserId()))
                        throw new UnauthorizedResponse("That session does not belong to you");

                    if (ownSession.getId().equals(sessionId))
                        logOut(sessionManager, ctx);
                    else
                        sessionManager.revoke(sessionId);

                    try (DatabaseHandle db = DBMS.openWrite()) {
                        Color c = toRevoke.getColor();
                        logActivity(db.get(), ownSession,
                                "Revoked session [session(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")]" + ShortUUIDFilter.apply(sessionId) + "[/session]");
                    }

                    redirectFromForm(ctx);
                })
                .applyTo(app);
    }
}
