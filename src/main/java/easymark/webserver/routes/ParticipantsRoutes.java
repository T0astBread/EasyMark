package easymark.webserver.routes;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import easymark.webserver.sessions.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;
import java.util.stream.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.*;

public class ParticipantsRoutes {
    public static void configure(Javalin app, SessionManager sessionManager) {

        app.get("/participants/cat-show", ctx -> {
            Session session = getSession(sessionManager, ctx);
            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            String name = session.getNameDisplay();
            String cat = session.getAtDisplay();
            session.setNameDisplay(null);
            session.setAtDisplay(null);
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


        app.post("/participants/:id/reset-cat", ctx -> {
            Session session = getSession(sessionManager, ctx);
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");

            UUID participantId;
            try {
                participantId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String uek = getUek(ctx, session);

            Participant participant;
            String rawCat = Cryptography.generateAccessToken();
            AccessToken cat = Cryptography.accessTokenFromString(rawCat);
            try (DatabaseHandle db = DBMS.openWrite()) {
                participant = db.get().getParticipants()
                        .stream()
                        .filter(p -> p.getId().equals(participantId))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new);
                Course course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(participant.getCourseId()))
                        .findAny()
                        .orElseThrow();
                participant.setCat(cat);
                logActivity(db.get(), session,
                        "Reset CAT of participant in [b]" + course.getName() + ": " + participant.getId() + "[/b]");
                DBMS.store();
            }

            sessionManager.getAllOfUser(participantId)
                    .map(Session::getId)
                    .forEach(sessionManager::revoke);

            String rawName = Cryptography.decryptData(participant.getName(), participant.getNameSalt(), uek);
            String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
            session.setNameDisplay(rawName);
            session.setAtDisplay(rawCat);

            ctx.redirect("/participants/cat-show?redirectUrl=" + redirectUrl);
        }, roles(UserRole.ADMIN));


        app.get("/participants/csv-import", ctx -> {
            Session session = getSession(sessionManager, ctx);
            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.queryParam(QueryKeys.COURSE_ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Missing query param courseId");
            }

            Course course;
            try (DatabaseHandle db = DBMS.openRead()) {
                course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Course not found"));
            }

            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            String cancelUrl = ctx.queryParam(QueryKeys.CANCEL_URL);

            ctx.render("pages/participants_csv-import.peb", Map.of(
                    ModelKeys.COURSE, course,
                    ModelKeys.REDIRECT_URL, redirectUrl == null ? "/" : redirectUrl,
                    ModelKeys.CANCEL_URL, cancelUrl,
                    ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
            ));
        }, roles(UserRole.ADMIN));


        app.post("/participants/csv-import", ctx -> {
            Map<String, List<String>> formParams = ctx.formParamMap();

            Session session = getSession(sessionManager, ctx);
            if (!checkCSRFToken(ctx, getFormParam(formParams, FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID courseId;
            try {
                courseId = UUID.fromString(getFormParam(formParams, FormKeys.COURSE_ID));
            } catch (Exception e) {
                throw new BadRequestResponse("courseId was not specified");
            }
            UUID adminId = session.getUserId();
            String uek = getUek(ctx, session);

            String participantsCSV = getFormParam(formParams, FormKeys.DATA);
            if (participantsCSV == null)
                throw new BadRequestResponse("CSV data was not present");
            int firstNamePosition = Integer.parseInt(getFormParam(formParams, FormKeys.FIRST_NAME_POSITION));
            int lastNamePosition = Integer.parseInt(getFormParam(formParams, FormKeys.LAST_NAME_POSITION));
            int linesToSkip = Integer.parseInt(getFormParam(formParams, FormKeys.LINES_TO_SKIP));

            int minNumberOfFields = Math.max(firstNamePosition, lastNamePosition) + 1;

            Map<String, String> participantNameToCat = new HashMap<>();
            try (DatabaseHandle db = DBMS.openWrite()) {
                Optional<Course> course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny();
                if (course.isEmpty())
                    throw new NotFoundResponse("Course not found");
                if (!course.get().getAdminId().equals(adminId))
                    throw new ForbiddenResponse("You are not the admin of this course");

                List<Participant> participants = Arrays.stream(participantsCSV.split("(\r)?\n"))
                        .skip(linesToSkip)
                        .map(line -> line.split("\t"))
                        .filter(fields -> fields.length >= minNumberOfFields)
                        .map(fields -> {
                            String rawName = fields[firstNamePosition] + " " + fields[lastNamePosition];
                            String nameSalt = Cryptography.generateEncryptionSalt();
                            String nameEnc = Cryptography.encryptData(rawName, nameSalt, uek);

                            String rawCat = Cryptography.generateAccessToken();
                            AccessToken cat = Cryptography.accessTokenFromString(rawCat);

                            Participant participant = new Participant();
                            participant.setCourseId(courseId);
                            participant.setName(nameEnc);
                            participant.setNameSalt(nameSalt);
                            participant.setCat(cat);

                            participantNameToCat.put(rawName, rawCat);
                            return participant;
                        })
                        .collect(Collectors.toList());
                db.get().getParticipants().addAll(participants);
                logActivity(db.get(), session,
                        "Imported [b]" + participants.size() + "[/b] participants into [b]" + course.get().getName() + "[/b] from Moodle");
                DBMS.store();
            }

            String redirectUrl = getFormParam(formParams, FormKeys.REDIRECT_URL);
            if (redirectUrl == null)
                redirectUrl = "/";

            ctx.header("Cache-Control", "no-store");
            ctx.render("pages/participants_csv-import_show-cats.peb", Map.of(
                    ModelKeys.PARTICIPANTS, participantNameToCat,
                    ModelKeys.REDIRECT_URL, redirectUrl
            ));
        }, roles(UserRole.ADMIN));


        new CommonRouteBuilder("participants")
                .withCreate(roles(UserRole.ADMIN), ctx -> {
                    UUID courseId;
                    try {
                        courseId = UUID.fromString(ctx.formParam(FormKeys.COURSE_ID));
                    } catch (Exception e) {
                        throw new BadRequestResponse();
                    }

                    Session session = getSession(sessionManager, ctx);
                    String uek = getUek(ctx, session);

                    String rawName = ctx.formParam(FormKeys.NAME);
                    String nameSalt = Cryptography.generateEncryptionSalt();
                    String encName = Cryptography.encryptData(rawName, nameSalt, uek);
                    String rawCat = Cryptography.generateAccessToken();

                    Participant newParticipant;
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        Course course = db.get().getCourses()
                                .stream()
                                .filter(c -> c.getId().equals(courseId))
                                .findAny()
                                .orElseThrow(() -> new NotFoundResponse("Course not found"));
                        newParticipant = new Participant();
                        newParticipant.setCourseId(courseId);
                        newParticipant.setName(encName);
                        newParticipant.setNameSalt(nameSalt);
                        newParticipant.setCat(Cryptography.accessTokenFromString(rawCat));
                        db.get().getParticipants().add(newParticipant);
                        logActivity(db.get(), session,
                                "Participant created in [b]" + course.getName() + ": " + newParticipant.getId() + "[/b]");
                        DBMS.store();
                    }

                    String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);

                    session.setNameDisplay(rawName);
                    session.setAtDisplay(rawCat);
                    ctx.redirect("/participants/cat-show?redirectUrl=" + redirectUrl);
                })

                .withEdit(roles(UserRole.ADMIN), (ctx, participantId) -> {
                    Session session = getSession(sessionManager, ctx);
                    String uek = getUek(ctx, session);

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
                    model.put(ModelKeys.REDIRECT_URL, "/courses/" + participant.getCourseId() + "/grading");
                    model.put(ModelKeys.BACK_URL, "/courses/" + participant.getCourseId() + "/grading");
                    model.put(ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx));
                    ctx.render("pages/participants_edit.peb", model);
                })

                .withUpdate(roles(UserRole.ADMIN), (ctx, participantId) -> {
                    Session session = getSession(sessionManager, ctx);

                    String name = ctx.formParam(FormKeys.NAME);
                    String warning = ctx.formParam(FormKeys.WARNING);
                    String group = ctx.formParam(FormKeys.GROUP);
                    String notes = ctx.formParam(FormKeys.NOTES);

                    String uek = getUek(ctx, session);
                    String nameSalt = Cryptography.generateEncryptionSalt();
                    String nameEnc = Cryptography.encryptData(name, nameSalt, uek);

                    Participant participant;
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        participant = db.get().getParticipants()
                                .stream()
                                .filter(p -> p.getId().equals(participantId))
                                .findAny()
                                .orElseThrow(NotFoundResponse::new);
                        Course course = db.get().getCourses()
                                .stream()
                                .filter(c -> c.getId().equals(participant.getCourseId()))
                                .findAny()
                                .orElseThrow();
                        participant.setName(nameEnc);
                        participant.setNameSalt(nameSalt);
                        participant.setWarning(warning);
                        participant.setGroup(group);
                        participant.setNotes(notes);
                        logActivity(db.get(), session,
                                "Participant updated in [b]" + course.getName() + ": " + participant.getId() + "[/b]");
                        DBMS.store();
                    }

                    String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
                    if (redirectUrl != null)
                        ctx.redirect(redirectUrl);
                })

                .withConfirmDelete(roles(UserRole.ADMIN), (ctx, participantId) -> {
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
                    Session session = getSession(sessionManager, ctx);
                    String uek = getUek(ctx, session);
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
                })

                .withDelete(roles(UserRole.ADMIN), (ctx, participantId) -> {
                    Session session = getSession(sessionManager, ctx);
                    Participant participant;
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        participant = db.get().getParticipants()
                                .stream()
                                .filter(p -> p.getId().equals(participantId))
                                .findAny()
                                .orElseThrow(NotFoundResponse::new);
                        Course course = db.get().getCourses()
                                .stream()
                                .filter(c -> c.getId().equals(participant.getCourseId()))
                                .findAny()
                                .orElseThrow();
                        db.get().getParticipants().remove(participant);
                        db.get().getAssignmentResults()
                                .removeIf(ar -> ar.getParticipantId().equals(participantId));
                        logActivity(db.get(), session,
                                "Participant deleted from [b]" + course.getName() + ": " + participant.getId() + "[/b]");
                        DBMS.store();
                    }
                    sessionManager.getAllOfUser(participantId)
                            .map(Session::getId)
                            .forEach(sessionManager::revoke);
                    ctx.redirect("/courses/" + participant.getCourseId() + "/grading");
                })

                .applyTo(app);
    }
}
