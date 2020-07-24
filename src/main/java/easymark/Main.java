package easymark;

import io.javalin.*;

public class Main {
    public static void main(String[] args) {
        Javalin app = Javalin.create();

        app.get("/", ctx -> {
            ctx.result("hello world!");
        });

        app.start(8080);
    }
}
