package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;
import java.util.function.*;

import static easymark.webserver.WebServerUtils.checkCSRFToken;
import static easymark.webserver.WebServerUtils.makeCSRFToken;
import static io.javalin.core.security.SecurityUtil.roles;

public class GenericOrdListRoutes {
    public static <E extends Entity & Ordered> void configure(
            Javalin app,
            String thing,

            CreateFunction<E> createFunction,

            DeleteEntityNameFunction deleteEntityNameFunction,
            DeleteFunction deleteFunction,

            Function<Database, List<E>> tableGetter,
            Function<E, UUID> groupSelector
    ) {
        if (createFunction != null) {
            app.post("/" + thing, ctx -> {
                if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                    throw new ForbiddenResponse("Forbidden");

                UUID groupId;
                try {
                    groupId = UUID.fromString(ctx.formParam(FormKeys.GROUP_ID));
                } catch (Exception e) {
                    throw new BadRequestResponse();
                }

                try (DatabaseHandle db = DBMS.openWrite()) {
                    int maxOrdNum = tableGetter.apply(db.get())
                            .stream()
                            .filter(entity -> groupId.equals(groupSelector.apply(entity)))
                            .map(Ordered::getOrdNum)
                            .max(Integer::compareTo)
                            .orElse(-1);
                    E newEntity = createFunction.run(db.get(), ctx, groupId);
                    newEntity.setOrdNum(maxOrdNum + 1);
                    tableGetter.apply(db.get()).add(newEntity);
                    DBMS.store();
                }

                String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
                ctx.redirect(redirectUrl == null ? "/" : redirectUrl);
            }, roles(UserRole.ADMIN));
        }

        app.post("/" + thing + "/:id", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse("Forbidden");

            UUID entityId;
            try {
                entityId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String action = ctx.queryParam(QueryKeys.ACTION);
            if (action.equalsIgnoreCase("delete")) {
                try (DatabaseHandle db = DBMS.openWrite()) {
                    deleteFunction.run(db.get(), entityId, ctx);

                    // Re-number chapters
                    int i = 0;
                    for (E entity : tableGetter.apply(db.get()))
                        entity.setOrdNum(i++);

                    DBMS.store();
                }
            } else if (action.equalsIgnoreCase("move-up") || action.equalsIgnoreCase("move-down")) {
                boolean isUp = action.contains("up");
                try (DatabaseHandle db = DBMS.openWrite()) {
                    E current = tableGetter.apply(db.get())
                            .stream()
                            .filter(chapter -> chapter.getId().equals(entityId))
                            .findAny()
                            .orElseThrow(() -> new NotFoundResponse("Entity not found"));
                    Optional<E> other = tableGetter.apply(db.get())
                            .stream()
                            .filter(o -> (
                                    groupSelector.apply(o).equals(groupSelector.apply(current))
                                            && o.getOrdNum() == current.getOrdNum() + (isUp ? -1 : 1)
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


        app.get("/" + thing + "/:id/confirm-delete", ctx -> {
            UUID entityId;
            try {
                entityId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse();
            }

            String cancelUrl = ctx.queryParam(QueryKeys.CANCEL_URL);
            String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
            String deleteUrl = "/" + thing + "/" + entityId + "?action=delete";

            if (redirectUrl == null || cancelUrl == null)
                throw new BadRequestResponse();

            String entityName;
            try (DatabaseHandle db = DBMS.openRead()) {
                entityName = deleteEntityNameFunction.run(db.get(), entityId);
            }

            ctx.render("pages/confirm-delete.peb", Map.of(
                    ModelKeys.DELETE_URL, deleteUrl,
                    ModelKeys.DELETE_ENTITY_NAME, entityName,
                    ModelKeys.REDIRECT_URL, redirectUrl,
                    ModelKeys.CANCEL_URL, cancelUrl,
                    ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
            ));
        }, roles(UserRole.ADMIN));
    }

    @FunctionalInterface
    public interface CreateFunction<E> {
        E run(Database db, Context ctx, UUID groupId);
    }

    @FunctionalInterface
    public interface DeleteEntityNameFunction {
        String run(Database db, UUID entityId);
    }

    @FunctionalInterface
    public interface DeleteFunction {
        void run(Database db, UUID entityId, Context ctx);
    }
}
