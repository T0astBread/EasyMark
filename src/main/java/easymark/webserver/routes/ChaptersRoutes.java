package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.*;

public class ChaptersRoutes {
    public static void configure(Javalin app) {

        app.post("/chapters", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");

            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.formParam(FormKeys.COURSE_ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
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
                newChapter.setOrdNum(maxOrdNum + 1);
                db.get().getChapters().add(newChapter);

                if (testRequired) {
                    Assignment newTestAssignment = new Assignment();
                    newTestAssignment.setChapterId(newChapter.getId());
                    newTestAssignment.setName("Test");
                    db.get().getAssignments().add(newTestAssignment);
                    newChapter.setTestAssignmentId(newTestAssignment.getId());
                }

                DBMS.store();
            }
            ctx.redirect("/courses/" + courseId);
        }, roles(UserRole.ADMIN));


        app.get("/chapters/:id/confirm-delete", ctx -> {
            UUID chapterId;
            try {
                chapterId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

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
                    ModelKeys.DELETE_URL, "/chapters/" + chapterId + "?action=delete",
                    ModelKeys.DELETE_ENTITY_NAME, "chapter \"" + chapter.getName() + "\" including all associated test requests, assignments and assignment results",
                    ModelKeys.REDIRECT_URL, redirectUrl,
                    ModelKeys.CANCEL_URL, cancelUrl,
                    ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
            ));
        }, roles(UserRole.ADMIN));


        app.post("/chapters/:id", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");

            UUID chapterId;
            try {
                chapterId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String action = ctx.queryParam(QueryKeys.ACTION);
            if (action.equalsIgnoreCase("delete")) {
                try (DatabaseHandle db = DBMS.openWrite()) {
                    db.get().getAssignments()
                            .removeIf(assignment -> chapterId.equals(assignment.getChapterId()));
                    db.get().getChapters()
                            .removeIf(chapter -> chapter.getId().equals(chapterId));

                    // Re-number chapters
                    int i = 0;
                    for (Chapter chapter : db.get().getChapters())
                        chapter.setOrdNum(i++);

                    DBMS.store();
                }
            } else if (action.equalsIgnoreCase("move-up") || action.equalsIgnoreCase("move-down")) {
                boolean isUp = action.contains("up");
                try (DatabaseHandle db = DBMS.openWrite()) {
                    Chapter current = db.get().getChapters()
                            .stream()
                            .filter(chapter -> chapter.getId().equals(chapterId))
                            .findAny()
                            .orElseThrow(() -> new NotFoundResponse("Chapter not found"));
                    Optional<Chapter> other = db.get().getChapters()
                            .stream()
                            .filter(chapter -> (
                                    chapter.getCourseId().equals(current.getCourseId())
                                            && chapter.getOrdNum() == current.getOrdNum() + (isUp ? -1 : 1)
                            ))
                            .findAny();
                    if (other.isPresent()) {
                        int currentOrdNum = current.getOrdNum();
                        current.setOrdNum(other.get().getOrdNum());
                        other.get().setOrdNum(currentOrdNum);
                        DBMS.store();
                    }
                }
            } else {
                throw new BadRequestResponse();
            }

            String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
            ctx.redirect(redirectUrl == null ? "/" : redirectUrl);
        }, roles(UserRole.ADMIN));

        app.get("/chapters/:id/edit", ctx -> {
            UUID chapterId;
            try {
                chapterId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

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
        });

        app.post("/chapters/:id/save", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID chapterId;
            try {
                chapterId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            boolean testRequired = "on".equalsIgnoreCase(ctx.formParam(FormKeys.TEST_REQUIRED));

            try (DatabaseHandle db = DBMS.openWrite()) {
                Chapter chapter = db.get().getChapters()
                        .stream()
                        .filter(c -> c.getId().equals(chapterId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Chapter not found"));

                chapter.setName(ctx.formParam(FormKeys.NAME));

                UUID testAssignmentId = chapter.getTestAssignmentId();
                if (testRequired) {
                    if (testAssignmentId == null) {
                        Assignment newTestAssignment = new Assignment();
                        newTestAssignment.setChapterId(chapterId);
                        newTestAssignment.setName("Test");
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
        });
    }
}
