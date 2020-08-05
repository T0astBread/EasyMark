package easymark.subcommands;

import easymark.*;
import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.errors.*;

import java.io.*;

public class ResetAdminTokenCommand {
    public static void run(CommandLineArgs.ResetAdminToken args) throws UserFriendlyException {
        try (DatabaseHandle db = DBMS.openWrite()) {
            Admin toReset = Utils.findAdminBySelector(db, args.adminSelector);

            if (toReset != null) {
                Cryptography.AdminCreationSecrets secrets = Cryptography.generateAdminSecrets();

                toReset.setAccessToken(secrets.accessToken);
                toReset.setIek(secrets.iek);
                toReset.setIekSalt(secrets.iekSalt);

                db.get().getCourses().stream()
                        .filter(course -> toReset.getId().equals(course.getAdminId()))
                        .forEach(course -> {
                            db.get().getParticipants().stream()
                                    .filter(participant -> course.getId().equals(participant.getCourseId()))
                                    .forEach(participant -> {
                                        participant.setName(null);
                                        participant.setNameSalt(null);
                                    });
                        });

                DBMS.storeUnlocked();
                System.out.println("New access token: " + secrets.accessTokenStr);
            }

        } catch (IOException e) {
            throw new UserFriendlyException("Failed to write to database: " + e.getMessage(), e, ExitStatus.UNEXPECTED_ERROR);
        }
    }
}
