package easymark;

import easymark.cli.*;
import easymark.database.*;
import easymark.errors.*;
import easymark.subcommands.*;

import java.io.*;

public class Main {

    public static void main(String[] args) {
        try {
            final CommandLineArgs commandLineArgs = CLI.parse(args);

            try (DatabaseHandle db = DBMS.openWrite()) {
                DBMS.load();
            } catch (IOException e) {
                throw new UserFriendlyException("Failed to load database: " + e.getMessage(), ExitStatus.UNEXPECTED_ERROR);
            }

            if (commandLineArgs instanceof CommandLineArgs.CreateAdmin)
                CreateAdminCommand.run((CommandLineArgs.CreateAdmin) commandLineArgs);
            else if (commandLineArgs instanceof CommandLineArgs.ResetAdminToken)
                ResetAdminTokenCommand.run((CommandLineArgs.ResetAdminToken) commandLineArgs);
            else if (commandLineArgs instanceof CommandLineArgs.DeleteAdmin)
                DeleteAdminCommand.run((CommandLineArgs.DeleteAdmin) commandLineArgs);
            else if (commandLineArgs instanceof CommandLineArgs.Serve)
                ServeCommand.run((CommandLineArgs.Serve) commandLineArgs);
            else if (commandLineArgs instanceof CommandLineArgs.DebugSeedDatabase)
                DebugSeedDatabaseCommand.run((CommandLineArgs.DebugSeedDatabase) commandLineArgs);
            else {
                final String msg = "Subcommand not implemented: " + commandLineArgs.getClass().getSimpleName();
                throw new UserFriendlyException(msg, ExitStatus.UNEXPECTED_ERROR);
            }
        } catch (UserFriendlyException e) {
            handleUserFriendlyException(e);
        }
    }

    private static void handleUserFriendlyException(UserFriendlyException e) {
        e.printStackTrace();
        System.err.println(e.getMessage());
        System.exit(e.getProposedExitStatus() != null ? e.getProposedExitStatus() : ExitStatus.UNEXPECTED_ERROR);
    }
}
