package easymark.webserver.routes;

import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.core.security.*;
import io.javalin.http.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static easymark.webserver.WebServerUtils.*;

public class CommonRouteBuilder {
    private final String resourcePluralName;
    private final List<Consumer<Javalin>> configuration;

    public CommonRouteBuilder(String resourcePluralName) {
        this.resourcePluralName = resourcePluralName;
        this.configuration = new LinkedList<>();
    }

    public CommonRouteBuilder withList(Set<Role> permittedRoles, Handler handler) {
        this.configuration.add(app -> {
            app.get("/" + this.resourcePluralName,
                    handler,
                    permittedRoles);
        });
        return this;
    }

    public CommonRouteBuilder withShow(Set<Role> permittedRoles, HandlerWithID handler) {
        this.configuration.add(app -> {
            app.get("/" + this.resourcePluralName + "/:id", ctx -> {
                handler.handle(ctx, getValidIDPathParam(ctx));
            }, permittedRoles);
        });
        return this;
    }

    public CommonRouteBuilder withNew(Set<Role> permittedRoles, Handler handler) {
        this.configuration.add(app -> {
            app.get("/" + this.resourcePluralName + "/new",
                    handler,
                    permittedRoles);
        });
        return this;
    }

    public CommonRouteBuilder withCreate(Set<Role> permittedRoles, Handler handler) {
        this.configuration.add(app -> {
            app.post("/" + this.resourcePluralName, ctx -> {
                checkCSRFFormSubmission(ctx);
                handler.handle(ctx);
            }, permittedRoles);
        });
        return this;
    }

    public CommonRouteBuilder withEdit(Set<Role> permittedRoles, HandlerWithID handler) {
        this.configuration.add(app -> {
            app.get("/" + this.resourcePluralName + "/:id/edit", ctx -> {
                handler.handle(ctx, getValidIDPathParam(ctx));
            }, permittedRoles);
        });
        return this;
    }

    public CommonRouteBuilder withUpdate(Set<Role> permittedRoles, HandlerWithID handler) {
        this.configuration.add(app -> {
            app.post("/" + this.resourcePluralName + "/:id", ctx -> {
                UUID id = getValidIDPathParam(ctx);
                checkCSRFFormSubmission(ctx);
                handler.handle(ctx, id);
            }, permittedRoles);
        });
        return this;
    }

    public CommonRouteBuilder withConfirmDelete(Set<Role> permittedRoles, HandlerWithID handler) {
        this.configuration.add(app -> {
            app.get("/" + this.resourcePluralName + "/:id/confirm-delete", ctx -> {
                handler.handle(ctx, getValidIDPathParam(ctx));
            }, permittedRoles);
        });
        return this;
    }

    public CommonRouteBuilder withDelete(Set<Role> permittedRoles, HandlerWithID handler) {
        this.configuration.add(app -> {
            app.post("/" + this.resourcePluralName + "/:id/delete", ctx -> {
                UUID id = getValidIDPathParam(ctx);
                checkCSRFFormSubmission(ctx);
                handler.handle(ctx, id);
            }, permittedRoles);
        });
        return this;
    }

    public <E extends Entity & Ordered> CommonRouteBuilder withCreateOrdered(
            Set<Role> permittedRoles,
            Function<Database, List<E>> tableSelector,
            Function<E, UUID> groupSelector,
            CreateFunction<E> createFunction
    ) {
        this.configuration.add(app -> {
            app.post("/" + this.resourcePluralName, ctx -> {
                checkCSRFFormSubmission(ctx);

                UUID groupId;
                try {
                    groupId = UUID.fromString(ctx.formParam(FormKeys.GROUP_ID));
                } catch (Exception e) {
                    throw new BadRequestResponse();
                }

                try (DatabaseHandle db = DBMS.openWrite()) {
                    List<E> table = tableSelector.apply(db.get());
                    int maxOrdNum = table
                            .stream()
                            .filter(entity -> groupId.equals(groupSelector.apply(entity)))
                            .map(Ordered::getOrdNum)
                            .max(Integer::compareTo)
                            .orElse(-1);
                    E newEntity = createFunction.run(db.get(), ctx, groupId);
                    newEntity.setOrdNum(maxOrdNum + 1);
                    table.add(newEntity);
                    DBMS.store();
                }

                String redirectUrl = ctx.formParam(FormKeys.REDIRECT_URL);
                ctx.redirect(redirectUrl == null ? "/" : redirectUrl);
            }, permittedRoles);
        });
        return this;
    }

    public <E extends Entity & Ordered> CommonRouteBuilder withUpdateOrder(
            Set<Role> permittedRoles,
            Function<Database, List<E>> tableSelector,
            Function<E, UUID> groupSelector
    ) {
        this.configuration.add(app -> {
            app.post("/" + this.resourcePluralName + "/:id/update-order", ctx -> {
                UUID id = getValidIDPathParam(ctx);
                checkCSRFFormSubmission(ctx);

                String direction = ctx.queryParam(QueryKeys.DIRECTION);
                boolean isUp = "up".equalsIgnoreCase(direction);
                try (DatabaseHandle db = DBMS.openWrite()) {
                    E current = tableSelector.apply(db.get())
                            .stream()
                            .filter(e -> e.getId().equals(id))
                            .findAny()
                            .orElseThrow(() -> new NotFoundResponse("Entity not found"));
                    UUID currentGroup = groupSelector.apply(current);
                    Optional<E> other = tableSelector.apply(db.get())
                            .stream()
                            .filter(o -> (
                                    groupSelector.apply(o).equals(currentGroup)
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
                redirectFromForm(ctx);
            }, permittedRoles);
        });
        return this;
    }

    public <E extends Entity & Ordered> CommonRouteBuilder withDeleteOrdered(
            Set<Role> permittedRoles,
            Function<Database, List<E>> tableSelector,
            Function<E, UUID> groupSelector,
            DeleteFunction deleteFunction
    ) {
        return withDelete(permittedRoles, (ctx, id) -> {
            try (DatabaseHandle db = DBMS.openWrite()) {
                UUID groupId = deleteFunction.run(db.get(), id, ctx);

                // Re-number entities in same group
                Iterable<E> entities = tableSelector.apply(db.get())
                        .stream()
                        .filter(e -> e.getOrdNum() > -2 && groupSelector.apply(e).equals(groupId))
                        .sorted(Comparator.comparingInt(Ordered::getOrdNum))
                        .collect(Collectors.toList());
                int i = 0;
                for (E entity : entities)
                    entity.setOrdNum(i++);

                DBMS.store();
            }

            redirectFromForm(ctx);
        });
    }

    public Javalin applyTo(Javalin app) {
        configuration.forEach(route -> route.accept(app));
        return app;
    }

    @FunctionalInterface
    public interface HandlerWithID {
        void handle(Context ctx, UUID id) throws Exception;
    }

    @FunctionalInterface
    public interface CreateFunction<E> {
        E run(Database db, Context ctx, UUID groupId);
    }

    @FunctionalInterface
    public interface DeleteFunction {
        UUID run(Database db, UUID entityId, Context ctx);
    }
}
