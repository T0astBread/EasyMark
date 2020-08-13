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

            Map<String, Object> model = new HashMap<>();
            model.put(ModelKeys.NAME, rawName);
            model.put(ModelKeys.NEW_CAT, rawCat);
            String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
            if (redirectUrl != null)
                model.put(ModelKeys.REDIRECT_URL, redirectUrl);
            ctx.render("pages/participants_create.peb", model);
        }, roles(UserRole.ADMIN));
    }
}
