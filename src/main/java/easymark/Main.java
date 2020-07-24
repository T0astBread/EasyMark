package easymark;

import io.javalin.*;
import io.javalin.plugin.rendering.*;
import io.javalin.plugin.rendering.template.*;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        JavalinRenderer.register(JavalinPebble.INSTANCE, ".peb");

        Javalin app = Javalin.create();

        app.get("/", ctx -> {
            String name = ctx.sessionAttribute("name");
            Map<String, Object> model = new HashMap<>();
            model.put("name", name);
            model.put("stack", List.of("Java", "Jetty", "Javalin"));
            ctx.render("hellow.peb", model);
        });

        app.post("/", ctx -> {
            String name = ctx.formParam("name");
            ctx.sessionAttribute("name", name);
            ctx.redirect("/");
        });

        app.start(8080);
    }
}
