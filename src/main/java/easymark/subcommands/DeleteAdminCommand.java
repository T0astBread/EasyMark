package easymark.subcommands;

import easymark.*;
import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.errors.*;

import java.io.*;

public class DeleteAdminCommand {
    public static void run(CommandLineArgs.DeleteAdmin args) throws UserFriendlyException {
        try (DatabaseHandle db = DBMS.openWrite()) {
            final Admin admin = Utils.findAdminBySelector(db, args.adminSelector);

            if (admin != null) {
                db.get().getAdmins().remove(admin);

                Utils.deleteResourcesOfAdmin(db.get(), admin.getId());

                DBMS.store();
                System.out.println("Removed one matching admin with all associated courses and participants");
            }

        } catch (IOException e) {
            throw new UserFriendlyException("Failed to write to database: " + e.getMessage(), e, ExitStatus.UNEXPECTED_ERROR);
        }
    }
}
