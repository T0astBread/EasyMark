package easymark.webserver.routes;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;

import static easymark.webserver.WebServerUtils.*;
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
            ctx.redirect("/participants/cat-show?redirectUrl=" + redirectUrl);
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


        app.get("/participants/:id/confirm-delete", ctx -> {
            UUID participantId;
            try {
                participantId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            Participant participant;
            Course course;
            try (DatabaseHandle db = DBMS.openRead()) {
                participant = db.get().getParticipants()
                        .stream()
                        .filter(p -> p.getId().equals(participantId))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new);
                course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(participant.getCourseId()))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new);
            }
            String uek = getUekFromContext(ctx);
            String name;
            try {
                name = Cryptography.decryptData(participant.getName(), participant.getNameSalt(), uek);
            } catch (Exception e) {
                name = "Decryption failure";
            }

            Map<String, Object> model = new HashMap<>();
            model.put(ModelKeys.DELETE_URL, "/participants/" + participantId + "/delete");
            model.put(ModelKeys.DELETE_ENTITY_NAME, name + " from course " + course.getName() + " including all associated test requests and assignment results");
            model.put(ModelKeys.CANCEL_URL, "/courses/" + course.getId() + "/grading");
            model.put(ModelKeys.REDIRECT_URL, "/courses/" + course.getId() + "/grading");
            model.put(ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx));
            ctx.render("pages/confirm-delete.peb", model);
        }, roles(UserRole.ADMIN));


        app.post("/participants/:id/delete", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");

            UUID participantId;
            try {
                participantId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            Participant participant;
            try (DatabaseHandle db = DBMS.openWrite()) {
                participant = db.get().getParticipants()
                        .stream()
                        .filter(p -> p.getId().equals(participantId))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new);
                db.get().getParticipants().remove(participant);
                db.get().getAssignmentResults()
                        .removeIf(ar -> ar.getParticipantId().equals(participantId));
                DBMS.store();
            }

            ctx.redirect("/courses/" + participant.getCourseId() + "/grading");
        }, roles(UserRole.ADMIN));


        app.get("/participants/:id/edit", ctx -> {
            UUID participantId;
            try {
                participantId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String uek = getUekFromContext(ctx);

            Participant participant;
            try (DatabaseHandle db = DBMS.openRead()) {
                participant = db.get().getParticipants()
                        .stream()
                        .filter(p -> p.getId().equals(participantId))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new);
            }

            String name;
            try {
                name = Cryptography.decryptData(participant.getName(), participant.getNameSalt(), uek);
            } catch (Exception e) {
                name = "Decryption failure";
            }
            Map<String, Object> model = new HashMap<>();
            model.put(ModelKeys.PARTICIPANT, participant);
            model.put(ModelKeys.NAME, name);
            model.put(ModelKeys.REDIRECT_URL, "/courses/"+participant.getCourseId()+"/grading");
            model.put(ModelKeys.BACK_URL, "/courses/"+participant.getCourseId()+"/grading");
            model.put(ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx));
            ctx.render("pages/participants_edit.peb", model);
        }, roles(UserRole.ADMIN));


        app.post("/participants/:id/update", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");

            UUID participantId;
            try {
                participantId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String name = ctx.formParam(FormKeys.NAME);
            String warning = ctx.formParam(FormKeys.WARNING);
            String group = ctx.formParam(FormKeys.GROUP);

            String uek = getUekFromContext(ctx);
            String nameSalt = Cryptography.generateEncryptionSalt();
            String nameEnc = Cryptography.encryptData(name, nameSalt, uek);

            Participant participant;
            try (DatabaseHandle db = DBMS.openWrite()) {
                participant = db.get().getParticipants()
                        .stream()
                        .filter(p -> p.getId().equals(participantId))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new);
                participant.setName(nameEnc);
                participant.setNameSalt(nameSalt);
                participant.setWarning(warning);
                participant.setGroup(group);
                DBMS.store();
            }

            String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
            if (redirectUrl != null)
                ctx.redirect(redirectUrl);
        }, roles(UserRole.ADMIN));
    }
}
