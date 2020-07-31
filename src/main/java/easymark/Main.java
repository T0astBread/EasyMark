package easymark;

import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import io.javalin.*;

import java.io.*;

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

//        try {
//            DBMS.load();
//        } catch (IOException e) {
//            System.out.println("Failed to load database");
//            e.printStackTrace();
//            System.exit(UNEXPECTED_ERROR);
//        }

        try (DatabaseHandle dbHandle = DBMS.openWrite()) {
            Database db = dbHandle.get();

            Admin devAdmin = new Admin();

            String adminAccessTokenStr = Cryptography.generateAccessToken();
            System.out.println(adminAccessTokenStr);
            AccessToken adminAccessToken = Cryptography.accessTokenFromString(adminAccessTokenStr);
            devAdmin.setAccessToken(adminAccessToken);

            String uek = Cryptography.generateUEK();
            String iekSalt = Cryptography.generateEncryptionSalt();
            String iek = Cryptography.encryptUEK(uek, iekSalt, adminAccessTokenStr);
            devAdmin.setIek(iek);
            devAdmin.setIekSalt(iekSalt);

            db.getAdmins().add(devAdmin);

            Course course = new Course();
            course.setAdminId(devAdmin.getId());
            course.setName("MyCourse 2020");
            db.getCourses().add(course);

            Chapter chapter1 = new Chapter();
            chapter1.setCourseId(course.getId());
            chapter1.setOrdNum(0);
            chapter1.setName("Chapter 1");
            chapter1.setDescription("This is\nchapter 1");
            db.getChapters().add(chapter1);

            Participant participant1 = new Participant();
            String partNameSalt = Cryptography.generateEncryptionSalt();
            participant1.setName(Cryptography.encryptData("My Name", partNameSalt, uek));
            participant1.setNameSalt(partNameSalt);
            participant1.setCourseId(course.getId());
            String strTokenPart = Cryptography.generateAccessToken();
            System.out.println(strTokenPart);
            AccessToken participantAccessToken = Cryptography.accessTokenFromString(strTokenPart);
            participant1.setCat(participantAccessToken);
            db.getParticipants().add(participant1);

            try {
                DBMS.storeUnlocked();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(UNEXPECTED_ERROR);
            }
        }

        Javalin server = WebServer.create();
        server.start(WebServer.PORT);
    }

    private static void handleUserFriendlyException(UserFriendlyException e) {
        e.printStackTrace();
        System.err.println(e.getMessage());
        System.exit(USER_ERROR);
    }
}
