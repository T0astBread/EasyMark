package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;

import static easymark.webserver.WebServerUtils.checkCSRFToken;
import static io.javalin.core.security.SecurityUtil.roles;

public class AssignmentsRoutes {
//    public static void configure(Javalin app) {
//
//        app.post("/assignments", ctx -> {
//            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
//                throw new ForbiddenResponse("Forbidden");
//
//            UUID courseId;
//            UUID chapterId;
//            try {
//                courseId = UUID.fromString(ctx.formParam(FormKeys.COURSE_ID));
//                chapterId = UUID.fromString(ctx.formParam(FormKeys.CHAPTER_ID));
//            } catch (Exception e) {
//                throw new BadRequestResponse();
//            }
//
//            try (DatabaseHandle db = DBMS.openWrite()) {
//                Assignment newAssignment = new Assignment();
//                newAssignment.setChapterId(chapterId);
//                newAssignment.setName(ctx.formParam(FormKeys.NAME));
//                try {
//                    newAssignment.setMaxScore(Integer.parseInt(ctx.formParam(FormKeys.MAX_SCORE)));
//                } catch (Exception e) {}
//                db.get().getAssignments().add(newAssignment);
//                DBMS.store();
//            }
//            ctx.redirect("/courses/" + courseId);
//        }, roles(UserRole.ADMIN));
//
//        app.post("/assignments/:id", ctx -> {
//            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
//                throw new ForbiddenResponse("Forbidden");
//
//            UUID assignmentId;
//            try {
//                assignmentId = UUID.fromString(ctx.pathParam(PathParams.ID));
//            } catch (Exception e) {
//                throw new BadRequestResponse();
//            }
//
//            String action = ctx.queryParam(QueryKeys.ACTION);
//            if (action.equalsIgnoreCase("delete")) {
//                try (DatabaseHandle db = DBMS.openWrite()) {
//                    db.get().getAssignments()
//                            .removeIf(assignment -> assignment.getId().equals(assignmentId));
//                    DBMS.store();
//                }
//            } else {
//                throw new BadRequestResponse();
//            }
//
//            String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
//            ctx.redirect(redirectUrl == null ? "/" : redirectUrl);
//        }, roles(UserRole.ADMIN));
//    }

    public static void configure(Javalin app) {

//        app.post("/assignments", ctx -> {
//            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
//                throw new ForbiddenResponse("Forbidden");
//
//            UUID courseId;
//            UUID chapterId;
//            try {
//                courseId = UUID.fromString(ctx.formParam(FormKeys.COURSE_ID));
//                chapterId = UUID.fromString(ctx.formParam(FormKeys.CHAPTER_ID));
//            } catch (Exception e) {
//                throw new BadRequestResponse();
//            }
//
//            try (DatabaseHandle db = DBMS.openWrite()) {
//                Assignment newAssignment = new Assignment();
//                newAssignment.setChapterId(chapterId);
//                newAssignment.setName(ctx.formParam(FormKeys.NAME));
//                try {
//                    newAssignment.setMaxScore(Integer.parseInt(ctx.formParam(FormKeys.MAX_SCORE)));
//                } catch (Exception e) {}
//                db.get().getAssignments().add(newAssignment);
//                DBMS.store();
//            }
//            ctx.redirect("/courses/" + courseId);
//        }, roles(UserRole.ADMIN));

        GenericOrdListRoutes.configure(
                app,
                "assignments",

                (db, ctx, groupId) -> {
                    Assignment newAssignment = new Assignment();
                    newAssignment.setChapterId(groupId);
                    newAssignment.setName(ctx.formParam(FormKeys.NAME));
                    try {
                        newAssignment.setMaxScore(Integer.parseInt(ctx.formParam(FormKeys.MAX_SCORE)));
                    } catch (Exception e) {}
                    return newAssignment;
                },

                (db, entityId, ctx) -> {
                    db.getAssignments().removeIf(assignment -> assignment.getId().equals(entityId));
                },

                Database::getAssignments,
                Assignment::getChapterId
        );
    }
}
