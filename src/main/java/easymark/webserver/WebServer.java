package easymark.webserver;

import easymark.webserver.constants.*;
import easymark.webserver.routes.*;
import io.javalin.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;

import static easymark.webserver.WebServerUtils.*;

public class WebServer {
    public static final int PORT = 8080;

    public static Javalin create() {
        Javalin app = Javalin.create();

        app.config.accessManager((handler, ctx, permittedRoles) -> {
            if (isLoggedIn(ctx) && checkForExpiredSession(ctx))
                return;

            if (!permittedRoles.isEmpty()) {
                Set<UserRole> roles = ctx.sessionAttribute(SessionKeys.ROLES);
                if (roles == null || permittedRoles.stream().noneMatch(roles::contains))
                    throw new ForbiddenResponse("Not allowed");
            }

            handler.handle(ctx);
        });

        app.config.addStaticFiles("static");

        IndexRoutes.configure(app);
        CoursesRoutes.configure(app);
        ChaptersRoutes.configure(app);
        AssignmentsRoutes.configure(app);

        return app;
    }
}
