package easymark.subcommands;

import easymark.*;
import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;

import java.io.*;
import java.time.*;
import java.util.*;

public class DebugSeedDatabaseCommand {
    public static void run(CommandLineArgs.DebugSeedDatabase args) {
        try (DatabaseHandle db = DBMS.openWrite()) {
            DBMS.replace(new Database());  // Clear database
        }

        try (DatabaseHandle db = DBMS.openWrite()) {
            Admin admin = new Admin();
            admin.setId(UUID.fromString("613198a7-eb79-468c-88a1-77fcc75b51dc"));

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
            course.setId(UUID.fromString("59f29bcb-978a-48ed-a0ac-7d8425920cdf"));
            course.setName("Debug Course 2020/21");
            course.setAdminId(admin.getId());
            course.setOrdNum(0);
            db.get().getCourses().add(course);

            final String[] CHAPTERS = new String[]{
                    "Complexity",
                    "Algorithms",
                    "Data structures",
                    "Boolean logic",
                    "Java",
            };
            final String[][] ASSIGNMENTS = new String[][]{
                    new String[]{
                            "Algorithmic complexity",
                            "Big O notation",
                    },
                    new String[]{
                            "Binary Search",
                            "Quicksort",
                            "Mergesort",
                            "Dijkstra's algorithm",
                    },
                    new String[]{
                            "Linked list",
                            "Array list",
                            "Map",
                            "Set",
                    },
                    new String[]{
                            "AND, OR, NOT",
                            "Advanced logic gates",
                    },
                    new String[]{
                            "Variables",
                            "Methods",
                            "Objects",
                            "Interfaces",
                    },
            };
            final String[] CHAPTER_IDS = new String[]{
                    "3d279849-2c90-458b-835f-311f50302477",
                    "49a1aa54-e87b-4cbd-b498-84a66fb9d15e",
                    "36423902-f25e-41da-b24b-a6c0f6aed3a2",
                    "db261a4d-0cd5-4261-9d4a-53575b033988",
                    "08c21c44-134a-4e64-8b39-c7e3ffa42f2b",
            };
            final String[] TEST_ASSIGNMENT_IDS = new String[]{
                    "1853c59b-ceed-4779-9114-bb79c7c692ab",
                    "7f0a5d07-f764-47b9-a8bf-bf019e4cb440",
                    "ed1176c0-c738-414f-a847-38c274a95f4f",
            };
            final String[][] ASSIGNMENT_IDS = new String[][]{
                    new String[]{
                            "e6c21a35-b654-4218-9466-482fea2157fa",
                            "5deb0458-1a3c-48fd-8149-30dfdf345777",
                    },
                    new String[]{
                            "6244a9b2-97aa-4b7b-864c-fc8ccb4b8766",
                            "d6c48faa-c885-4876-afda-467053ff775a",
                            "fafd56e1-d6d2-474a-a823-578129ee1823",
                            "65d90405-7a41-4d50-b6d9-cca9dc990b17",
                    },
                    new String[]{
                            "0a770f3a-11f3-4553-b830-c1ccf1de3b48",
                            "5c7c7f24-9ee7-4865-910c-0ea74478a1a0",
                            "909a03f4-6d64-4db9-90ac-4081becce5bd",
                            "c068779c-da69-429c-809d-f5e04344bca0",
                    },
                    new String[]{
                            "b94e6a7b-5afb-49b0-ba3d-9afa5cab7f11",
                            "5fea973c-3945-4465-9615-ebfbf491908c",
                    },
                    new String[]{
                            "9ad60d91-af4b-418d-be85-b7c147b418dc",
                            "deb12bb8-62f1-4108-9487-7aba8707351e",
                            "c44aa9b4-be42-4a22-8dfb-afffb473bf10",
                            "01733023-aa10-4473-8aab-3e675e1f0e18",
                    },
            };
            for (int i = 0; i < CHAPTERS.length; i++) {
                Chapter chapter = new Chapter();
                chapter.setId(UUID.fromString(CHAPTER_IDS[i]));
                chapter.setCourseId(course.getId());
                chapter.setOrdNum(i);
                chapter.setName(CHAPTERS[i]);
                chapter.setDueDate(LocalDate.of(2020, 8, 26).plusMonths(i));
                db.get().getChapters().add(chapter);

                String[] assignmentsForChapter = ASSIGNMENTS[i];
                String[] assignmentIDsForChapter = ASSIGNMENT_IDS[i];
                for (int j = 0; j < assignmentsForChapter.length; j++) {
                    Assignment assignment = new Assignment();
                    assignment.setId(UUID.fromString(assignmentIDsForChapter[j]));
                    assignment.setChapterId(chapter.getId());
                    assignment.setOrdNum(j);
                    assignment.setName(assignmentsForChapter[j]);
                    assignment.setMaxScore((j + 1) * 5);
                    db.get().getAssignments().add(assignment);
                }
                if (i < 3) {
                    Assignment testAssignment = new Assignment();
                    testAssignment.setId(UUID.fromString(TEST_ASSIGNMENT_IDS[i]));
                    testAssignment.setChapterId(chapter.getId());
                    testAssignment.setOrdNum(-2);
                    testAssignment.setName("Test");
                    testAssignment.setMaxScore(10);
                    db.get().getAssignments().add(testAssignment);
                    chapter.setTestAssignmentId(testAssignment.getId());
                }
            }

            final String[] PARTICIPANTS = new String[]{
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
            final String[] PARTICIPANT_IDS = new String[]{
                    "fc0d03c4-389d-4365-8efe-e95a532d2af4",
                    "999fd335-043b-46d9-b01c-0d1f664b36c7",
                    "e44a3327-2cb4-44f5-91d9-70748f893782",
                    "a93fe134-1bd1-4135-acab-e501d8e160ca",
                    "53484c58-08c4-4cb1-9151-48b360d86950",
                    "1ac8ab8f-0e68-4c9c-a053-3be3d802c218",
                    "0dbef766-a5e4-49fd-aa8b-5f2a0ec2628a",
                    "ef8da79d-cca8-4df9-9ca7-2f7956a01ddc",
                    "e4f757e4-9c26-4cda-bcc2-8d60b4a916ba",
                    "e7dbdb8c-d3a4-415b-8dfb-0d390be6ed9b",
                    "e1336130-4064-4e90-baa5-dde5b579fb0d",
                    "483215af-8285-4133-a136-d88656baada0",
                    "185c9e77-609a-4e59-9240-c729e3f6eed0",
                    "5e772b76-a69d-41c4-87a4-b0013d1525be",
                    "3d8c9203-188a-4db0-8801-2211ff708b49",
                    "9d4c33a2-ad8f-4f6e-aa28-ec1bf3210755",
                    "af71c509-96fd-48e6-9ec1-1903942df6bf",
                    "7cb03e35-347b-41ed-b76f-f0e1c8964ac3",
                    "5d12bb1a-7c1c-4c30-9388-a90883fbc3e4",
                    "49b91ca7-135b-4fd6-bdab-e331229ef0b2",
                    "e2fb52ee-5be6-46db-b0ea-a4705a66dfaf",
                    "24c302bf-616f-42bf-8e9e-e5107823169b",
                    "8343f14e-ea75-4fee-9c15-d6d5d2ab2c12",
                    "bcfa60b7-9008-44af-8630-84e593405da8",
                    "5dd3bff9-309d-4a89-8d40-006660c7f606",
                    "b274e056-435d-45bd-b7e0-1b67feb5730d",
                    "b621a8a9-6703-4d35-9ee6-83ba1e2e83af",
            };
            for (int i = 0; i < PARTICIPANTS.length; i++) {
                Participant newParticipant = new Participant();
                newParticipant.setId(UUID.fromString(PARTICIPANT_IDS[i]));
                newParticipant.setCourseId(course.getId());
                String nameSalt = Cryptography.generateEncryptionSalt();
                String encName = Cryptography.encryptData(PARTICIPANTS[i], nameSalt, uek);
                newParticipant.setName(encName);
                newParticipant.setNameSalt(nameSalt);
                newParticipant.setCat(Cryptography.accessTokenFromString(Cryptography.generateAccessToken()));
                db.get().getParticipants().add(newParticipant);
            }

            Participant lastParticipant = new Participant();
            lastParticipant.setId(UUID.fromString("adc6d249-bc3d-4f9d-8421-5440e0d1e991"));
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
