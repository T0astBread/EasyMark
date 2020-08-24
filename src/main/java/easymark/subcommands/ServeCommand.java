package easymark.subcommands;

import easymark.cli.*;
import easymark.webserver.*;
import io.javalin.*;

public class ServeCommand {
    public static void run(CommandLineArgs.Serve args) {
        System.out.println("Starting web server...");
        if (args.enableInsecureDebugMechanisms) {
            new Thread(() -> {
                try {
                    Thread.sleep(3500L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("=====================================");
                System.out.println("INSECURE DEBUG MECHANISMS ARE ENABLED");
                System.out.println("   Do not run this in production!");
                System.out.println("=====================================");
            }).start();
        }
        Javalin server = WebServer.create(args.enableInsecureDebugMechanisms);
        server.start(WebServer.PORT);
    }
}
