package easymark;

import easymark.database.*;
import io.javalin.*;
import io.javalin.plugin.rendering.*;
import io.javalin.plugin.rendering.template.*;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            DBMS.load();
        } catch (IOException e) {
            System.out.println("Failed to load database");
            e.printStackTrace();
            System.exit(1);
        }

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
