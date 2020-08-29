package easymark.webserver;

import com.mitchellbosecke.pebble.*;
import easymark.*;
import easymark.cli.*;
import easymark.pebble.*;
import easymark.subcommands.*;
import easymark.webserver.constants.*;
import easymark.webserver.routes.*;
import easymark.webserver.sessions.*;
import io.javalin.*;
import io.javalin.http.*;
import io.javalin.plugin.rendering.template.*;

import java.util.*;

import static easymark.webserver.WebServerUtils.*;

public class WebServer {
    public static final int PORT = 8080;

    public static Javalin create(boolean enableInsecureDebugMechanisms) {
        JavalinPebble.configure(new PebbleEngine.Builder()
                .extension(new EasyMarkPebbleExtension())
                .build());
        Javalin app = Javalin.create();
        SessionManager sessionManager = new SessionManager();

        app.config.accessManager((handler, ctx, permittedRoles) -> {
            UUID sessionId = ctx.sessionAttribute(SessionKeys.SESSION_ID);
            Session session = null;
            try {
                session = sessionManager.get(sessionId);
            } catch (SessionManager.NotRegisteredException ignored) {
            } catch (SessionManager.ExpiredException e) {
                if (!enableInsecureDebugMechanisms) {
                    logOut(sessionManager, ctx);
                    ctx.redirect("/");
                    return;
                }
            }

            if (enableInsecureDebugMechanisms) {
                // Ensure the user is logged in as wanted role, if present
                String wantedRole = ctx.queryParam(QueryKeys.DEBUG_CHANGE_LOGIN);
                if (wantedRole != null) {
                    boolean wantsAdmin = wantedRole.equalsIgnoreCase(UserRole.ADMIN.toString());
                    Set<UserRole> roles = session == null ? null : session.getRoles();
                    if (roles == null) {
                        System.out.println("Logged in as " + (wantsAdmin ? "admin" : "participant"));
                        logIn(sessionManager, ctx, wantsAdmin ? Utils.DEBUG_ADMIN_AT : Utils.DEBUG_PARTICIPANT_AT);
                    } else {
                        boolean hasAdmin = roles.contains(UserRole.ADMIN);
                        if (hasAdmin != wantsAdmin) {
                            System.out.println("Logged out as " + (hasAdmin ? "admin" : "participant") + " and in as " + (wantsAdmin ? "admin" : "participant"));
                            logOut(sessionManager, ctx);
                            logIn(sessionManager, ctx, wantsAdmin ? Utils.DEBUG_ADMIN_AT : Utils.DEBUG_PARTICIPANT_AT);
                        }
                    }
                    try {
                        session = sessionManager.get(ctx.sessionAttribute(SessionKeys.SESSION_ID));
                    } catch (SessionManager.ExpiredException | SessionManager.NotRegisteredException e) {
                        throw new InternalServerErrorResponse("Failed to log in: " + e.getClass().getSimpleName());
                    }
                }
            }

            if (!permittedRoles.isEmpty()) {
                if (session == null || permittedRoles.stream().noneMatch(session.getRoles()::contains))
                    throw new ForbiddenResponse("Not allowed");
            }

            handler.handle(ctx);
        });

        app.config.addStaticFiles("static");

        IndexRoutes.configure(app, sessionManager);
        CoursesRoutes.configure(app, sessionManager);
        ChaptersRoutes.configure(app, sessionManager);
        AssignmentsRoutes.configure(app, sessionManager);
        ParticipantsRoutes.configure(app, sessionManager);
        TestRequestRoutes.configure(app, sessionManager);
        AdminRoutes.configure(app, sessionManager);
        SessionRoutes.configure(app, sessionManager);

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
