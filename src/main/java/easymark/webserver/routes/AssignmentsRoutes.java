package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;

import static easymark.webserver.WebServerUtils.*;

public class AssignmentsRoutes {

    public static void configure(Javalin app) {
        app.get("/assignments/:id/edit", ctx -> {
            UUID assignmentId;
            try {
                assignmentId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            Assignment assignment;
            try (DatabaseHandle db = DBMS.openRead()) {
                assignment = db.get().getAssignments()
                        .stream()
                        .filter(c -> c.getId().equals(assignmentId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Assignment not found"));
            }

            Map<String, Object> model = new HashMap<>();
            model.put(ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx));
            model.put(ModelKeys.ASSIGNMENT, assignment);
            model.put(ModelKeys.BACK_URL, ctx.queryParam(QueryKeys.BACK_URL));
            model.put(ModelKeys.REDIRECT_URL, ctx.queryParam(QueryKeys.RECIRECT_URL));
            ctx.render("pages/assignments_edit.peb", model);
        });

        app.post("/assignments/:id/save", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID assignmentId;
            try {
                assignmentId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            try (DatabaseHandle db = DBMS.openWrite()) {
                Assignment assignment = db.get().getAssignments()
                        .stream()
                        .filter(c -> c.getId().equals(assignmentId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Assignment not found"));
                assignment.setName(ctx.formParam(FormKeys.NAME));
                try {
                    assignment.setMaxScore(Integer.parseInt(ctx.formParam(FormKeys.MAX_SCORE)));
                } catch (Exception e) {
                    throw new BadRequestResponse("Max. score must be a number");
                }
                DBMS.store();
            }

            String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
            if (redirectUrl != null)
                ctx.redirect(redirectUrl);
        });

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
