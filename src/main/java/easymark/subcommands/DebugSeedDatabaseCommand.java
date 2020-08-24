package easymark.subcommands;

import easymark.*;
import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;

import java.io.*;
import java.time.*;

public class DebugSeedDatabaseCommand {
    public static void run(CommandLineArgs.DebugSeedDatabase args) {
        try (DatabaseHandle db = DBMS.openWrite()) {
            DBMS.replace(new Database());  // Clear database
        }

        try (DatabaseHandle db = DBMS.openWrite()) {
            Admin admin = new Admin();

            String rawAdminCat = Utils.DEBUG_ADMIN_AT;
            AccessToken adminAT = Cryptography.accessTokenFromString(rawAdminCat);
            admin.setAccessToken(adminAT);

            String uek = Cryptography.generateUEK();
            String iekSalt = Cryptography.generateEncryptionSalt();
            String iek = Cryptography.encryptUEK(uek, iekSalt, rawAdminCat);
            admin.setIekSalt(iekSalt);
            admin.setIek(iek);

            db.get().getAdmins().add(admin);

            Course course = new Course();
            course.setName("Debug Course 2020/21");
            course.setAdminId(admin.getId());
            course.setOrdNum(0);
            db.get().getCourses().add(course);

            int nrChapters = (int) (Math.random() * 6) + 3;
            for (int i = 0; i < nrChapters; i++) {
                Chapter chapter = new Chapter();
                chapter.setCourseId(course.getId());
                chapter.setOrdNum(i);
                chapter.setName("Chapter " + i);
                chapter.setDueDate(LocalDate.now().plusMonths(i));
                db.get().getChapters().add(chapter);

                int nrAssignments = (int) (Math.random() * 7) + 2;
                for (int j = 0; j < nrAssignments; j++) {
                    Assignment assignment = new Assignment();
                    assignment.setChapterId(chapter.getId());
                    assignment.setOrdNum(j);
                    assignment.setName("Assignment " + i + ", " + j + " " + ":".repeat(j));
                    assignment.setMaxScore(((int) (Math.random() * 9) + 1) * 5);
                    db.get().getAssignments().add(assignment);
                }
                if (i < 3) {
                    Assignment testAssignment = new Assignment();
                    testAssignment.setChapterId(chapter.getId());
                    testAssignment.setOrdNum(nrAssignments);
                    testAssignment.setName("Test");
                    testAssignment.setMaxScore(10);
                    db.get().getAssignments().add(testAssignment);
                    chapter.setTestAssignmentId(testAssignment.getId());
                }
            }

            final String[] NAMES = new String[]{
                    "Raf GINGLE",
                    "Debbie TRENT",
                    "Wenda SECOMBE",
                    "Paul BRITTLES",
                    "King HEARDMAN",
                    "Justine DOCHE",
                    "Tasia LOVERING",
                    "Willow LANGLANDS",
                    "Zacharia KIMBLEY",
                    "Katharyn BENION",
                    "Sonny ROJEL",
                    "Malena FOCHS",
                    "Randal KORDAS",
                    "Thane NICHOLL",
                    "Frannie LUCIANI",
                    "Aube SALVADOR",
                    "Claudia TREBBETT",
                    "Henrietta TROUGHTON",
                    "Jared IZACHIK",
                    "Minda LUNDBERG",
                    "Franny KIENLEIN",
                    "Farleigh OLDMEADOW",
                    "Lambert MCGINNELL",
                    "Nikolas LARCIERE",
                    "Hedvige SWEEDLAND",
                    "Kirbee DUTHIE",
            };
            for (String name : NAMES) {
                Participant newParticipant = new Participant();
                newParticipant.setCourseId(course.getId());
                String nameSalt = Cryptography.generateEncryptionSalt();
                String encName = Cryptography.encryptData(name, nameSalt, uek);
                newParticipant.setName(encName);
                newParticipant.setNameSalt(nameSalt);
                newParticipant.setCat(Cryptography.accessTokenFromString(Cryptography.generateAccessToken()));
                db.get().getParticipants().add(newParticipant);
            }

            Participant lastParticipant = new Participant();
            lastParticipant.setCourseId(course.getId());
            String nameSalt = Cryptography.generateEncryptionSalt();
            String encName = Cryptography.encryptData("Demo PARTICIPANT", nameSalt, uek);
            lastParticipant.setName(encName);
            lastParticipant.setNameSalt(nameSalt);
            lastParticipant.setCat(Cryptography.accessTokenFromString(Utils.DEBUG_PARTICIPANT_AT));
            db.get().getParticipants().add(lastParticipant);

            DBMS.store();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
