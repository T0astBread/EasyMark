package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import static easymark.webserver.WebServerUtils.*;

public class IndexRoutes {
    public static void configure(Javalin app) {

        app.post("/login", ctx -> {
            final ForbiddenResponse FORBIDDEN = new ForbiddenResponse("Forbidden");

            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw FORBIDDEN;

            String providedAccessTokenStr = ctx.formParam("accessToken");
            logIn(ctx, providedAccessTokenStr);
            ctx.sessionAttribute(SessionKeys.LAST_SESSION_ACTION, LocalDateTime.now());
            ctx.redirect("/");
        });

        app.post("/logout", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");
            logOut(ctx);
            ctx.redirect("/");
        });

        app.get("/", ctx -> {
            Map<String, Object> model = new HashMap<>();
            model.put(ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx));

            Set<UserRole> roles = ctx.sessionAttribute(SessionKeys.ROLES);
            if (roles != null) {
                if (roles.contains(UserRole.ADMIN)) {
                    UUID adminId = ctx.sessionAttribute(SessionKeys.ENTITY_ID);
                    model.put(ModelKeys.ADMIN_ID, adminId);
                    try (DatabaseHandle db = DBMS.openRead()) {
                        List<Course> courses = db.get()
                                .getCourses()
                                .stream()
                                .filter(course -> adminId.equals(course.getAdminId()))
                                .sorted(Comparator.comparingInt(Course::getOrdNum))
                                .collect(Collectors.toUnmodifiableList());
                        model.put(ModelKeys.COURSES, courses);
                    }
                    ctx.render("pages/index.admin.peb", model);
                    return;
                } else if (roles.contains(UserRole.PARTICIPANT)) {
                    ctx.render("pages/index.participant.peb", model);
                    return;
                }
            }
            ctx.render("pages/index.peb", model);
        });
    }
}
