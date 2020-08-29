package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import easymark.webserver.sessions.*;
import io.javalin.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.roles;

public class TestRequestRoutes {
    public static void configure(Javalin app, SessionManager sessionManager) {

        new CommonRouteBuilder("test-requests")
                .withCreate(roles(UserRole.PARTICIPANT), ctx -> {
                    UUID chapterId;
                    try {
                        chapterId = UUID.fromString(ctx.formParam(FormKeys.CHAPTER_ID));
                    } catch (Exception e) {
                        throw new BadRequestResponse("Bad request");
                    }
                    Session session = getSession(sessionManager, ctx);
                    UUID particpantId = session.getUserId();

                    try (DatabaseHandle db = DBMS.openWrite()) {
                        boolean chapterExists = db.get().getChapters()
                                .stream()
                                .anyMatch(c -> c.getId().equals(chapterId));
                        if (!chapterExists)
                            throw new NotFoundResponse();

                        boolean participantExists = db.get().getParticipants()
                                .stream()
                                .anyMatch(p -> p.getId().equals(particpantId));
                        if (!participantExists)
                            throw new NotFoundResponse();

                        boolean testRequestExists = db.get().getTestRequests()
                                .stream()
                                .anyMatch(tr -> tr.getParticipantId().equals(particpantId) && tr.getChapterId().equals(chapterId));
                        if (testRequestExists)
                            throw new BadRequestResponse("A test request for this chapter already exists");

                        TestRequest newTestRequest = new TestRequest();
                        newTestRequest.setParticipantId(particpantId);
                        newTestRequest.setChapterId(chapterId);
                        newTestRequest.setTimestamp(LocalDateTime.now());
                        db.get().getTestRequests().add(newTestRequest);

                        DBMS.store();
                    }

                    ctx.redirect("/");
                })

                .withDelete(roles(UserRole.ADMIN), (ctx, testRequestId) -> {
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        TestRequest testRequest = db.get().getTestRequests()
                                .stream()
                                .filter(tr -> tr.getId().equals(testRequestId))
                                .findAny()
                                .orElseThrow(NotFoundResponse::new);
                        Chapter chapter = db.get().getChapters()
                                .stream()
                                .filter(ch -> ch.getId().equals(testRequest.getChapterId()))
                                .findAny()
                                .orElseThrow();
                        Course course = db.get().getCourses()
                                .stream()
                                .filter(c -> c.getId().equals(chapter.getCourseId()))
                                .findAny()
                                .orElseThrow();
                        db.get().getTestRequests().remove(testRequest);
                        logActivity(db.get(), getSession(sessionManager, ctx),
                                "Test request deleted for [b]" + course.getName() + " / " + chapter.getName() + "[/b] from [b]" + testRequest.getParticipantId() + "[/b]");
                        DBMS.store();
                    }

                    ctx.redirect("/");
                })

                .applyTo(app);
    }
}
