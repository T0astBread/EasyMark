package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.*;

public class ChaptersRoutes {
    public static void configure(Javalin app) {
        new CommonRouteBuilder("chapters")
                .withCreate(roles(UserRole.ADMIN), ctx -> {
                    UUID courseId;
                    try {
                        courseId = UUID.fromString(ctx.formParam(FormKeys.COURSE_ID));
                    } catch (Exception e) {
                        throw new BadRequestResponse();
                    }

                    LocalDate dueDate;
                    try {
                        dueDate = LocalDate.parse(ctx.formParam(FormKeys.DUE_DATE));
                    } catch (Exception e) {
                        throw new BadRequestResponse("Invalid due date");
                    }
                    boolean testRequired = "on".equalsIgnoreCase(ctx.formParam(FormKeys.TEST_REQUIRED));

                    try (DatabaseHandle db = DBMS.openWrite()) {
                        int maxOrdNum = db.get().getChapters()
                                .stream()
                                .filter(chapter -> courseId.equals(chapter.getCourseId()))
                                .map(Chapter::getOrdNum)
                                .max(Integer::compareTo)
                                .orElse(-1);

                        Chapter newChapter = new Chapter();
                        newChapter.setCourseId(courseId);
                        newChapter.setName(ctx.formParam(FormKeys.NAME));
                        newChapter.setDueDate(dueDate);
                        newChapter.setOrdNum(maxOrdNum + 1);
                        db.get().getChapters().add(newChapter);

                        if (testRequired) {
                            Assignment newTestAssignment = new Assignment();
                            newTestAssignment.setChapterId(newChapter.getId());
                            newTestAssignment.setName("Test");
                            newTestAssignment.setOrdNum(-2);
                            db.get().getAssignments().add(newTestAssignment);
                            newChapter.setTestAssignmentId(newTestAssignment.getId());
                        }

                        DBMS.store();
                    }
                    ctx.redirect("/courses/" + courseId);
                })

                .withEdit(roles(UserRole.ADMIN), (ctx, chapterId) -> {
                    Chapter chapter;
                    try (DatabaseHandle db = DBMS.openRead()) {
                        chapter = db.get().getChapters()
                                .stream()
                                .filter(c -> c.getId().equals(chapterId))
                                .findAny()
                                .orElseThrow(() -> new NotFoundResponse("Chapter not found"));
                    }

                    Map<String, Object> model = new HashMap<>();
                    model.put(ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx));
                    model.put(ModelKeys.CHAPTER, chapter);
                    model.put(ModelKeys.BACK_URL, ctx.queryParam(QueryKeys.BACK_URL));
                    model.put(ModelKeys.REDIRECT_URL, ctx.queryParam(QueryKeys.RECIRECT_URL));
                    ctx.render("pages/chapters_edit.peb", model);
                })

                .withUpdate(roles(UserRole.ADMIN), (ctx, chapterId) -> {
                    boolean testRequired = "on".equalsIgnoreCase(ctx.formParam(FormKeys.TEST_REQUIRED));
                    String name = ctx.formParam(FormKeys.NAME);
                    if (name == null)
                        throw new BadRequestResponse("Missing name");
                    LocalDate dueDate;
                    try {
                        dueDate = LocalDate.parse(ctx.formParam(FormKeys.DUE_DATE));
                    } catch (Exception e) {
                        throw new BadRequestResponse("Invalid due date");
                    }

                    try (DatabaseHandle db = DBMS.openWrite()) {
                        Chapter chapter = db.get().getChapters()
                                .stream()
                                .filter(c -> c.getId().equals(chapterId))
                                .findAny()
                                .orElseThrow(() -> new NotFoundResponse("Chapter not found"));

                        chapter.setName(name);
                        chapter.setDueDate(dueDate);

                        UUID testAssignmentId = chapter.getTestAssignmentId();
                        if (testRequired) {
                            if (testAssignmentId == null) {
                                Assignment newTestAssignment = new Assignment();
                                newTestAssignment.setChapterId(chapterId);
                                newTestAssignment.setName("Test");
                                newTestAssignment.setOrdNum(-2);
                                db.get().getAssignments().add(newTestAssignment);
                                chapter.setTestAssignmentId(newTestAssignment.getId());
                            }
                        } else {
                            if (testAssignmentId != null) {
                                db.get().getAssignments()
                                        .removeIf(a -> a.getId().equals(testAssignmentId));
                                chapter.setTestAssignmentId(null);
                            }
                        }

                        DBMS.store();
                    }

                    String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
                    if (redirectUrl != null)
                        ctx.redirect(redirectUrl);
                })

                .withUpdateOrder(
                        roles(UserRole.ADMIN),
                        Database::getChapters,
                        Chapter::getCourseId)

                .withConfirmDelete(roles(UserRole.ADMIN), (ctx, chapterId) -> {
                    String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
                    String cancelUrl = ctx.queryParam(QueryKeys.CANCEL_URL);
                    if (redirectUrl == null || cancelUrl == null)
                        throw new BadRequestResponse();

                    Chapter chapter;
                    try (DatabaseHandle db = DBMS.openRead()) {
                        chapter = db.get().getChapters()
                                .stream()
                                .filter(c -> c.getId().equals(chapterId))
                                .findAny()
                                .orElseThrow(() -> new NotFoundResponse("Chapter not found"));
                    }

                    ctx.render("pages/confirm-delete.peb", Map.of(
                            ModelKeys.DELETE_URL, "/chapters/" + chapterId + "/delete",
                            ModelKeys.DELETE_ENTITY_NAME, "chapter \"" + chapter.getName() + "\" including all associated test requests, assignments and assignment results",
                            ModelKeys.REDIRECT_URL, redirectUrl,
                            ModelKeys.CANCEL_URL, cancelUrl,
                            ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
                    ));
                })

                .withDeleteOrdered(
                        roles(UserRole.ADMIN),
                        Database::getChapters,
                        Chapter::getCourseId,
                        (db, chapterId, ctx) -> {
                            Chapter chapter = db.getChapters()
                                    .stream()
                                    .filter(ch -> ch.getId().equals(chapterId))
                                    .findAny()
                                    .orElseThrow(NotFoundResponse::new);
                            db.getAssignments()
                                    .removeIf(assignment -> chapterId.equals(assignment.getChapterId()));
                            db.getChapters().remove(chapter);
                            return chapter.getCourseId();
                        })

                .applyTo(app);
    }
}
