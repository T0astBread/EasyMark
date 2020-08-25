package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.roles;

public class AssignmentsRoutes {
    public static void configure(Javalin app) {
        new CommonRouteBuilder("assignments")
                .withCreateOrdered(roles(UserRole.ADMIN),
                        Database::getAssignments,
                        Assignment::getChapterId,
                        (db, ctx, groupId) -> {
                            Assignment newAssignment = new Assignment();
                            newAssignment.setChapterId(groupId);
                            try {
                                newAssignment.setMaxScore(Integer.parseInt(ctx.formParam(FormKeys.MAX_SCORE)));
                            } catch (Exception e) {
                                throw new BadRequestResponse("Max. score must be a number");
                            }
                            newAssignment.setName(ctx.formParam(FormKeys.NAME));
                            newAssignment.setLink(ctx.formParam(FormKeys.LINK));
                            return newAssignment;
                        })

                .withEdit(roles(UserRole.ADMIN), (ctx, assignmentId) -> {
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
                })

                .withUpdate(roles(UserRole.ADMIN), (ctx, assignmentId) -> {
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        Assignment assignment = db.get().getAssignments()
                                .stream()
                                .filter(c -> c.getId().equals(assignmentId))
                                .findAny()
                                .orElseThrow(() -> new NotFoundResponse("Assignment not found"));
                        try {
                            assignment.setMaxScore(Integer.parseInt(ctx.formParam(FormKeys.MAX_SCORE)));
                        } catch (Exception e) {
                            throw new BadRequestResponse("Max. score must be a number");
                        }
                        assignment.setName(ctx.formParam(FormKeys.NAME));
                        assignment.setLink(ctx.formParam(FormKeys.LINK));
                        DBMS.store();
                    }

                    String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
                    if (redirectUrl != null)
                        ctx.redirect(redirectUrl);
                })

                .withUpdateOrder(
                        roles(UserRole.ADMIN),
                        Database::getAssignments,
                        Assignment::getChapterId)

                .withConfirmDelete(roles(UserRole.ADMIN), (ctx, assignmentId) -> {
                    String cancelUrl = ctx.queryParam(QueryKeys.CANCEL_URL);
                    String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
                    String deleteUrl = "/assignments/" + assignmentId + "/delete";

                    if (redirectUrl == null || cancelUrl == null)
                        throw new BadRequestResponse();

                    String entityName;
                    try (DatabaseHandle db = DBMS.openRead()) {
                        entityName = "assignment \"" + db.get().getAssignments()
                                .stream()
                                .filter(course -> course.getId().equals(assignmentId))
                                .findAny()
                                .orElseThrow(NotFoundResponse::new)
                                .getName() + "\" including all associated results";
                    }

                    ctx.render("pages/confirm-delete.peb", Map.of(
                            ModelKeys.DELETE_URL, deleteUrl,
                            ModelKeys.DELETE_ENTITY_NAME, entityName,
                            ModelKeys.REDIRECT_URL, redirectUrl,
                            ModelKeys.CANCEL_URL, cancelUrl,
                            ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
                    ));
                })

                .withDeleteOrdered(
                        roles(UserRole.ADMIN),
                        Database::getAssignments,
                        Assignment::getChapterId,
                        (db, entityId, ctx) -> {
                            Assignment assignment = db.getAssignments()
                                    .stream()
                                    .filter(a -> a.getId().equals(entityId))
                                    .findAny()
                                    .orElseThrow(NotFoundResponse::new);
                            db.getAssignments().removeIf(a -> a.getId().equals(entityId));
                            db.getAssignmentResults().removeIf(ar ->
                                    ar.getAssignmentId().equals(entityId));
                            Chapter chapter = db.getChapters()
                                    .stream()
                                    .filter(c -> c.getId().equals(assignment.getChapterId()))
                                    .findAny()
                                    .orElseThrow(InternalServerErrorResponse::new);
                            if (chapter.getTestAssignmentId() != null && chapter.getTestAssignmentId().equals(assignment.getId()))
                                chapter.setTestAssignmentId(null);
                            return assignment.getChapterId();
                        })

                .applyTo(app);
    }
}
