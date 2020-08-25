package easymark.webserver.routes;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;
import java.util.stream.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.roles;

public class AdminRoutes {
    public static void configure(Javalin app) {

        new CommonRouteBuilder("admins")
                .withCreate(roles(UserRole.ADMIN), ctx -> {
                    Admin newAdmin = new Admin();
                    Cryptography.AdminCreationSecrets adminSecrets = Cryptography.generateAdminSecrets();
                    newAdmin.setIek(adminSecrets.iek);
                    newAdmin.setIekSalt(adminSecrets.iekSalt);
                    newAdmin.setAccessToken(adminSecrets.accessToken);
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        db.get().getAdmins().add(newAdmin);
                        DBMS.store();
                    }

                    String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);

                    ctx.sessionAttribute(SessionKeys.ADMIN_ID, newAdmin.getId());
                    ctx.sessionAttribute(SessionKeys.AT_DISPLAY, adminSecrets.accessTokenStr);
                    ctx.redirect("/admins/show-access-token?redirectUrl=" + (redirectUrl == null ? "/" : redirectUrl));
                })

                .withConfirmDelete(roles(UserRole.ADMIN), (ctx, adminId) -> {
                    boolean isSelf = adminId.equals(ctx.sessionAttribute(SessionKeys.ENTITY_ID));
                    String entityName = (isSelf
                            ? "YOURSELF, which will revoke your server access and delete all of your courses with all associated"
                            : "admin " + adminId + "with all associated courses,")
                            + " participants, chapters, test requests, assignments and assignment results";

                    ctx.render("pages/confirm-delete.peb", Map.of(
                            ModelKeys.DELETE_URL, "/admins/" + adminId + "/delete",
                            ModelKeys.DELETE_ENTITY_NAME, entityName,
                            ModelKeys.REDIRECT_URL, isSelf ? "/" : "/settings",
                            ModelKeys.CANCEL_URL, "/settings",
                            ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
                    ));
                })

                .withDelete(roles(UserRole.ADMIN), (ctx, adminId) -> {
                    UUID ownAdminId = ctx.sessionAttribute(SessionKeys.ENTITY_ID);

                    try (DatabaseHandle db = DBMS.openWrite()) {
                        boolean didExist = db.get().getAdmins()
                                .removeIf(tr -> tr.getId().equals(adminId));
                        if (!didExist)
                            throw new NotFoundResponse();

                        // Cascading delete admin
                        List<Course> courses = db.get().getCourses()
                                .stream()
                                .filter(course -> course.getAdminId().equals(adminId))
                                .peek(course -> {
                                    List<Chapter> chapters = db.get().getChapters()
                                            .stream()
                                            .filter(ch -> ch.getCourseId().equals(course.getId()))
                                            .peek(chapter -> {
                                                db.get().getAssignments()
                                                        .removeIf(assignment -> assignment.getChapterId().equals(chapter.getId()));
                                            })
                                            .collect(Collectors.toUnmodifiableList());
                                    db.get().getChapters().removeAll(chapters);

                                    List<Participant> participants = db.get().getParticipants()
                                            .stream()
                                            .filter(participant -> participant.getCourseId().equals(course.getId()))
                                            .peek(participant -> {
                                                db.get().getTestRequests()
                                                        .removeIf(testRequest -> testRequest.getParticipantId().equals(participant.getId()));
                                                db.get().getAssignmentResults()
                                                        .removeIf(assignmentResult -> assignmentResult.getParticipantId().equals(participant.getId()));
                                            })
                                            .collect(Collectors.toUnmodifiableList());
                                    db.get().getParticipants().removeAll(participants);
                                })
                                .collect(Collectors.toUnmodifiableList());
                        db.get().getCourses().removeAll(courses);

                        DBMS.store();
                    }

                    String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
                    if (adminId.equals(ownAdminId)) {
                        logOut(ctx);
                        redirectUrl = "/";
                    }
                    ctx.redirect(redirectUrl == null ? "/" : redirectUrl);
                })

                .applyTo(app);


        app.get("/admins/:id/reset-access-token", ctx -> {
            UUID adminId;
            try {
                adminId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            String cancelUrl = ctx.queryParam(QueryKeys.CANCEL_URL);

            ctx.render("pages/admins_reset-access-token.peb", Map.of(
                    ModelKeys.ADMIN_ID, adminId,
                    ModelKeys.REDIRECT_URL, redirectUrl == null ? "/" : redirectUrl,
                    ModelKeys.CANCEL_URL, cancelUrl == null ? "/" : cancelUrl,
                    ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
            ));
        }, roles(UserRole.ADMIN));


        app.post("/admins/:id/reset-access-token", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID adminId;
            try {
                adminId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String providedAccessToken = ctx.formParam(FormKeys.OLD_ACCESS_TOKEN);
            Cryptography.AdminCreationSecrets newSecrets = Cryptography.generateAdminSecrets();

            try (DatabaseHandle db = DBMS.openWrite()) {
                Admin admin = db.get().getAdmins()
                        .stream()
                        .filter(a -> a.getId().equals(adminId))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new);

                if (providedAccessToken != null && !providedAccessToken.isBlank()) {
                    String oldUEK;
                    try {
                        oldUEK = Cryptography.decryptUEK(admin.getIek(), admin.getIekSalt(), providedAccessToken);
                    } catch (Exception e) {
                        throw new BadRequestResponse("The provided access token was incorrect");
                    }
                    Set<UUID> coursesOfAdmin = db.get().getCourses()
                            .stream()
                            .filter(c -> c.getAdminId().equals(adminId))
                            .map(Entity::getId)
                            .collect(Collectors.toSet());

                    db.get().getParticipants()
                            .stream()
                            .filter(p -> coursesOfAdmin.contains(p.getCourseId()))
                            .forEach(p -> {
                                String rawName = Cryptography.decryptData(p.getName(), p.getNameSalt(), oldUEK);
                                String newSalt = Cryptography.generateEncryptionSalt();
                                String nameEncNew = Cryptography.encryptData(rawName, newSalt, newSecrets.uek);
                                p.setName(nameEncNew);
                                p.setNameSalt(newSalt);
                            });
                }

                admin.setIek(newSecrets.iek);
                admin.setIekSalt(newSecrets.iekSalt);
                admin.setAccessToken(newSecrets.accessToken);
                DBMS.store();
            }

            ctx.sessionAttribute(SessionKeys.ADMIN_ID, adminId);
            ctx.sessionAttribute(SessionKeys.AT_DISPLAY, newSecrets.accessTokenStr);

            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            ctx.redirect("/admins/show-access-token?redirectUrl=" + (redirectUrl == null ? "/" : redirectUrl));
        }, roles(UserRole.ADMIN));


        app.get("/admins/show-access-token", ctx -> {
            String rawAccessToken = ctx.sessionAttribute(SessionKeys.AT_DISPLAY);
            ctx.sessionAttribute(SessionKeys.AT_DISPLAY, null);

            UUID adminId = ctx.sessionAttribute(SessionKeys.ADMIN_ID);
            ctx.sessionAttribute(SessionKeys.ADMIN_ID, null);
            UUID ownAdminId = ctx.sessionAttribute(SessionKeys.ENTITY_ID);

            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            if (adminId.equals(ownAdminId)) {
                logOut(ctx);
                redirectUrl = "/";
            }

            ctx.header("Cache-Control", "no-store");
            ctx.render("pages/admins_show-access-token.peb", Map.of(
                    ModelKeys.ADMIN_ID, adminId,
                    ModelKeys.NEW_AT, rawAccessToken,
                    ModelKeys.REDIRECT_URL, redirectUrl == null ? "/" : redirectUrl
            ));
        }, roles(UserRole.ADMIN));
    }
}
