package easymark;

import easymark.cli.*;
import easymark.database.*;
import io.javalin.*;
import io.javalin.plugin.rendering.*;
import io.javalin.plugin.rendering.template.*;

import java.io.*;
import java.util.*;

public class Main {
    private static final int UNEXPECTED_ERROR = 1;
    private static final int USER_ERROR = 2;


    public static void main(String[] args) {
        final CommandLineArgs commandLineArgs;
        try {
            commandLineArgs = CLI.parse(args);
        } catch (UserFriendlyException e) {
            handleUserFriendlyException(e);
            return;  // unreachable
        }

        try {
            DBMS.load();
        } catch (IOException e) {
            System.out.println("Failed to load database");
            e.printStackTrace();
            System.exit(UNEXPECTED_ERROR);
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

    private static void handleUserFriendlyException(UserFriendlyException e) {
        e.printStackTrace();
        System.err.println(e.getMessage());
        System.exit(USER_ERROR);
    }
}
