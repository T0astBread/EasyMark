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
            final Admin toRemove = Utils.findAdminBySelector(db, args.adminSelector);

            if (toRemove != null) {
                final Database _db = db.get();

                // Cascading delete admin
                // I want an RDBMS
                _db.getAdmins().remove(toRemove);
                _db.getCourses().stream()
                        .filter(c -> toRemove.getId().equals(c.getAdminId()))
                        .forEach(c -> {
                            _db.getCourses().remove(c);
                            _db.getParticipants().removeIf(p -> p.getCourseId().equals(c.getId()));
                            _db.getChapters().stream()
                                    .filter(ch -> c.getId().equals(ch.getCourseId()))
                                    .forEach(ch -> {
                                        _db.getChapters().remove(ch);
                                        _db.getTestRequests().removeIf(t -> ch.getId().equals(t.getChapterId()));
                                        _db.getAssignments().stream()
                                                .filter(a -> ch.getId().equals(a.getChapterId()))
                                                .forEach(a -> {
                                                    _db.getAssignments().remove(a);
                                                    _db.getAssignmentResults().removeIf(ar -> a.getId().equals(ar.getAssignmentId()));
                                                });
                                    });
                        });

                DBMS.storeUnlocked();
                System.out.println("Removed one matching admin with all associated courses and participants");
            }

        } catch (IOException e) {
            throw new UserFriendlyException("Failed to write to database: " + e.getMessage(), e, ExitStatus.UNEXPECTED_ERROR);
        }
    }
}
