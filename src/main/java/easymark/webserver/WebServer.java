package easymark.webserver;

import easymark.*;
import easymark.cli.*;
import easymark.subcommands.*;
import easymark.webserver.constants.*;
import easymark.webserver.routes.*;
import io.javalin.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;

import static easymark.webserver.WebServerUtils.*;

public class WebServer {
    public static final int PORT = 8080;

    public static Javalin create(boolean enableInsecureDebugMechanisms) {
        Javalin app = Javalin.create();

        app.config.accessManager((handler, ctx, permittedRoles) -> {
            if (enableInsecureDebugMechanisms)
                ctx.sessionAttribute(SessionKeys.LAST_SESSION_ACTION, LocalDateTime.now());

            if (isLoggedIn(ctx) && checkForExpiredSession(ctx))
                return;

            if (enableInsecureDebugMechanisms) {
                String wantedRole = ctx.queryParam(QueryKeys.DEBUG_CHANGE_LOGIN);
                if (wantedRole != null) {
                    boolean wantsAdmin = wantedRole.equalsIgnoreCase(UserRole.ADMIN.toString());
                    Set<UserRole> roles = ctx.sessionAttribute(SessionKeys.ROLES);
                    if (roles == null) {
                        System.out.println("Logged in as " + (wantsAdmin ? "admin" : "participant"));
                        logIn(ctx, wantsAdmin ? Utils.DEBUG_ADMIN_AT : Utils.DEBUG_PARTICIPANT_AT);
                    } else {
                        boolean hasAdmin = roles.contains(UserRole.ADMIN);
                        if (hasAdmin != wantsAdmin) {
                            System.out.println("Logged out as " + (hasAdmin ? "admin" : "participant") + " and in as " + (wantsAdmin ? "admin" : "participant"));
                            logOut(ctx);
                            logIn(ctx, wantsAdmin ? Utils.DEBUG_ADMIN_AT : Utils.DEBUG_PARTICIPANT_AT);
                        }
                    }
                }
            }

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
        ParticipantsRoutes.configure(app);
        TestRequestRoutes.configure(app);
        AdminRoutes.configure(app);

        if (enableInsecureDebugMechanisms) {
            app.post("/reset-db", ctx -> {
                System.out.println("Resetting the database...");
                DebugSeedDatabaseCommand.run(new CommandLineArgs.DebugSeedDatabase());
                ctx.result("Reset the database");
            });
        }

        return app;
    }
}
