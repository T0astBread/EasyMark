package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.time.*;
import java.util.*;

import static easymark.webserver.WebServerUtils.checkCSRFToken;
import static io.javalin.core.security.SecurityUtil.roles;

public class TestRequestRoutes {
    public static void configure(Javalin app) {

        app.post("/test-requests", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID chapterId;
            try {
                chapterId = UUID.fromString(ctx.formParam(FormKeys.CHAPTER_ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }
            UUID particpantId = ctx.sessionAttribute(SessionKeys.ENTITY_ID);

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
        }, roles(UserRole.PARTICIPANT));


        app.post("/test-requests/:id/delete", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID testRequestId;
            try {
                testRequestId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            try (DatabaseHandle db = DBMS.openWrite()) {
                boolean didExist = db.get().getTestRequests()
                        .removeIf(tr -> tr.getId().equals(testRequestId));
                if (!didExist)
                    throw new NotFoundResponse();
                DBMS.store();
            }

            ctx.redirect("/");
        }, roles(UserRole.ADMIN));
    }
}
