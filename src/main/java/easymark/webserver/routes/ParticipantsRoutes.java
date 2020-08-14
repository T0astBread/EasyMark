package easymark.webserver.routes;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;

import static easymark.webserver.WebServerUtils.checkCSRFToken;
import static easymark.webserver.WebServerUtils.getUekFromContext;
import static io.javalin.core.security.SecurityUtil.roles;

public class ParticipantsRoutes {
    public static void configure(Javalin app) {

        app.post("/participants", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");

            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.formParam(FormKeys.COURSE_ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String uek = getUekFromContext(ctx);

            String rawName = ctx.formParam(FormKeys.NAME);
            String nameSalt = Cryptography.generateEncryptionSalt();
            String encName = Cryptography.encryptData(rawName, nameSalt, uek);
            String rawCat = Cryptography.generateAccessToken();

            Participant newParticipant;
            try (DatabaseHandle db = DBMS.openWrite()) {
                newParticipant = new Participant();
                newParticipant.setCourseId(courseId);
                newParticipant.setName(encName);
                newParticipant.setNameSalt(nameSalt);
                newParticipant.setCat(Cryptography.accessTokenFromString(rawCat));
                db.get().getParticipants().add(newParticipant);
                DBMS.store();
            }

            String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);

            ctx.sessionAttribute(SessionKeys.NAME_DISPLAY, rawName);
            ctx.sessionAttribute(SessionKeys.AT_DISPLAY, rawCat);
            ctx.redirect("/participants/cat-show?redirectUrl="+redirectUrl);
        }, roles(UserRole.ADMIN));

        app.get("/participants/cat-show", ctx -> {
            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            String name = ctx.sessionAttribute(SessionKeys.NAME_DISPLAY);
            String cat = ctx.sessionAttribute(SessionKeys.AT_DISPLAY);
            ctx.sessionAttribute(SessionKeys.NAME_DISPLAY, null);
            ctx.sessionAttribute(SessionKeys.AT_DISPLAY, null);
            if (name == null || cat == null)
                throw new BadRequestResponse();

            try {
                ctx.header("Cache-Control", "no-store");  // Needed so that back button doesn't break
                ctx.render("pages/participants_create.peb", Map.of(
                        ModelKeys.REDIRECT_URL, redirectUrl == null ? "/" : redirectUrl,
                        ModelKeys.NAME, name,
                        ModelKeys.NEW_AT, cat
                ));
            } catch (Exception e) {
                throw new BadRequestResponse("Parameters expired");
            }
        }, roles(UserRole.ADMIN));
    }
}
