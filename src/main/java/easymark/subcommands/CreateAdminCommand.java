package easymark.subcommands;

import easymark.*;
import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.errors.*;

import java.io.*;

public class CreateAdminCommand {
    public static void run(CommandLineArgs.CreateAdmin args) throws UserFriendlyException {
        Admin newAdmin = new Admin();
        Cryptography.AdminCreationSecrets secrets = Cryptography.generateAdminSecrets();

        newAdmin.setAccessToken(secrets.accessToken);
        newAdmin.setIek(secrets.iek);
        newAdmin.setIekSalt(secrets.iekSalt);

        try(DatabaseHandle db = DBMS.openWrite()) {
            db.get().getAdmins().add(newAdmin);
            DBMS.storeUnlocked();
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to add admin to database: " + e.getMessage(), e, ExitStatus.UNEXPECTED_ERROR);
        }

        System.out.println("Generated new admin (" + newAdmin.getId() + ")");
        System.out.println("Access Token: " + secrets.accessTokenStr);
    }
}
