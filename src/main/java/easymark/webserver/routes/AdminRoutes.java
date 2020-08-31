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
import static io.javalin.core.security.SecurityUtil.roles;

public class AdminRoutes {
    public static void configure(Javalin app, SessionManager sessionManager) {

        new CommonRouteBuilder("admins")
                .withCreate(roles(UserRole.ADMIN), ctx -> {
                    Session session = getSession(sessionManager, ctx);

                    Admin newAdmin = new Admin();
                    Cryptography.AdminCreationSecrets adminSecrets = Cryptography.generateAdminSecrets();
                    newAdmin.setIek(adminSecrets.iek);
                    newAdmin.setIekSalt(adminSecrets.iekSalt);
                    newAdmin.setAccessToken(adminSecrets.accessToken);
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        db.get().getAdmins().add(newAdmin);
                        logActivity(db.get(), session, "Admin created: [b]" + newAdmin.getId() + "[/b]");
                        DBMS.store();
                    }

                    String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);

                    session.setAdminIdDisplay(newAdmin.getId());
                    session.setAtDisplay(adminSecrets.accessTokenStr);
                    ctx.redirect("/admins/show-access-token?redirectUrl=" + (redirectUrl == null ? "/" : redirectUrl));
                })

                .withConfirmDelete(roles(UserRole.ADMIN), (ctx, adminId) -> {
                    Session session = getSession(sessionManager, ctx);
                    boolean isSelf = adminId.equals(session.getUserId());
                    String entityName = (isSelf
                            ? "YOURSELF, which will revoke your server access and delete all of your courses with all associated"
                            : "admin " + adminId + " with all associated courses,")
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
                    Session session = getSession(sessionManager, ctx);
                    UUID ownAdminId = getSession(sessionManager, ctx).getUserId();

                    try (DatabaseHandle db = DBMS.openWrite()) {
                        boolean didExist = db.get().getAdmins()
                                .removeIf(tr -> tr.getId().equals(adminId));
                        if (!didExist)
                            throw new NotFoundResponse();

                        Utils.deleteResourcesOfAdmin(db.get(), adminId);

                        if (!adminId.equals(ownAdminId))
                            logActivity(db.get(), session, "Admin deleted: [b]" + adminId + "[/b]");

                        DBMS.store();
                    }

                    String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
                    if (adminId.equals(ownAdminId)) {
                        logOut(sessionManager, ctx);
                        redirectUrl = "/";
                    }

                    sessionManager.getAllOfUser(adminId)
                            .map(Session::getId)
                            .forEach(sessionManager::revoke);

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
            checkCSRFFormSubmission(ctx);
            Session session = getSession(sessionManager, ctx);

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
                logActivity(db.get(), session, "Reset access token of admin [b]" + adminId + "[/b]");
                DBMS.store();
            }

            // Revoke all but the current session of the admin
            sessionManager.getAllOfUser(adminId)
                    .map(Session::getId)
                    .filter(sessionId -> !sessionId.equals(session.getId()))
                    .forEach(sessionManager::revoke);

            session.setAdminIdDisplay(adminId);
            session.setAtDisplay(newSecrets.accessTokenStr);

            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            ctx.redirect("/admins/show-access-token?redirectUrl=" + (redirectUrl == null ? "/" : redirectUrl));
        }, roles(UserRole.ADMIN));


        app.get("/admins/show-access-token", ctx -> {
            Session session = getSession(sessionManager, ctx);
            String rawAccessToken = session.getAtDisplay();
            session.setAtDisplay(null);

            UUID adminId = session.getAdminIdDisplay();
            session.setAdminIdDisplay(null);
            UUID ownAdminId = session.getUserId();

            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            if (adminId.equals(ownAdminId)) {
                logOut(sessionManager, ctx);
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
