package easymark.webserver.routes;

import easymark.webserver.*;
import easymark.webserver.sessions.*;
import io.javalin.*;
import io.javalin.http.*;

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
                    redirectFromForm(ctx);
                })
                .applyTo(app);
    }
}
