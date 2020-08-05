package easymark.subcommands;

import easymark.cli.*;
import easymark.webserver.*;
import io.javalin.*;

public class ServeCommand {
    public static void run(CommandLineArgs.Serve args) {
        System.out.println("Starting web server...");
        Javalin server = WebServer.create();
        server.start(WebServer.PORT);
    }
}
