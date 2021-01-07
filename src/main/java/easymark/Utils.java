package easymark;

import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.errors.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.*;

import java.security.*;
import java.text.*;
import java.util.*;
import java.util.stream.*;

public class Utils {
    public static final SecureRandom RANDOM = new SecureRandom();
    public static final PasswordEncoder PASSWORD_ENCODER;
    public static final String CSV_DELIMITER = ";";
    public static final String DEBUG_ADMIN_AT = "977d1f28171b17a71b25f69df08a690224ecf8c637d5e2a8";
    public static final String DEBUG_PARTICIPANT_AT = "454e7007a3ed86b9639794453546977dcf8e63c8720d169d";

    static {
        final String BCRYPT_PASSWORD_ENCODER = "bcrypt";
        PASSWORD_ENCODER = new DelegatingPasswordEncoder(
                BCRYPT_PASSWORD_ENCODER, Map.of(
                BCRYPT_PASSWORD_ENCODER, new BCryptPasswordEncoder()
        ));
    }

    public static Admin findAdminBySelector(DatabaseHandle db, AdminSelector adminSelector) throws UserFriendlyException {
        Admin found = null;

        if (adminSelector instanceof AdminSelector.ByID) {
            AdminSelector.ByID selector = (AdminSelector.ByID) adminSelector;
            System.out.println("Looking up admin by ID: " + selector.id);
            List<Admin> admins = db.get().getAdmins();
            Optional<Admin> admin = admins.stream()
                    .filter(a -> a.getId().equals(selector.id))
                    .findAny();
            if (admin.isPresent())
                found = admin.get();
            else
                System.out.println("No matching admin found");

        } else if (adminSelector instanceof AdminSelector.ByAdministeredCourse) {
            AdminSelector.ByAdministeredCourse selector = (AdminSelector.ByAdministeredCourse) adminSelector;
            System.out.println("Looking up admin by course: " + selector.courseName);
            Optional<Course> course = db.get()
                    .getCourses()
                    .stream()
                    .filter(c -> selector.courseName.equals(c.getName()))
                    .findAny();
            if (course.isPresent()) {
                UUID adminId = course.get().getAdminId();
                Optional<Admin> admin = db.get()
                        .getAdmins()
                        .stream()
                        .filter(a -> a.getId().equals(adminId))
                        .findAny();

                if (admin.isEmpty())
                    throw new UserFriendlyException("Database corruption: Course admin not in database", ExitStatus.UNEXPECTED_ERROR);

                found = admin.get();
            } else {
                System.out.println("Course not found: " + selector.courseName);
            }
        }
        return found;
    }

    public static void deleteResourcesOfAdmin(Database db, UUID adminId) {
        List<Course> courses = db.getCourses()
                .stream()
                .filter(course -> course.getAdminId().equals(adminId))
                .peek(course -> {
                    List<Chapter> chapters = db.getChapters()
                            .stream()
                            .filter(ch -> ch.getCourseId().equals(course.getId()))
                            .peek(chapter -> {
                                db.getAssignments()
                                        .removeIf(assignment -> assignment.getChapterId().equals(chapter.getId()));
                            })
                            .collect(Collectors.toUnmodifiableList());
                    db.getChapters().removeAll(chapters);

                    List<Participant> participants = db.getParticipants()
                            .stream()
                            .filter(participant -> participant.getCourseId().equals(course.getId()))
                            .peek(participant -> {
                                db.getTestRequests()
                                        .removeIf(testRequest -> testRequest.getParticipantId().equals(participant.getId()));
                                db.getAssignmentResults()
                                        .removeIf(assignmentResult -> assignmentResult.getParticipantId().equals(participant.getId()));
                            })
                            .collect(Collectors.toUnmodifiableList());
                    db.getParticipants().removeAll(participants);
                })
                .collect(Collectors.toUnmodifiableList());
        db.getCourses().removeAll(courses);
        db.getActivityLogItems()
                .removeIf(li -> li.getAdminId().equals(adminId));
    }

    public static GradingInfo gradingInfo(float totalScore, float maxScore) {
        if (maxScore > 0) {
            float ratio = totalScore / maxScore;
            float ratioPercent = Math.round(ratio * 10000) / 100f;

            float grade = (ratio * 100 - 106.25f) / -12.5f;
            grade = Math.max(.5f, Math.min(5.5f, grade));
            grade = Math.round(grade * 100) / 100f;

            return new GradingInfo(totalScore, maxScore, ratio, ratioPercent, grade, Float.toString(ratioPercent), Float.toString(grade));
        } else {
            return new GradingInfo(totalScore, maxScore, 0f, 0f, 3f, "-", "-");
        }
    }

    public static String getLastName(Participant participant, Map<UUID, String> namePerParticipant) {
        String name = namePerParticipant.getOrDefault(participant.getId(), "");
        int lastNameStart = name.lastIndexOf(" ");
        return lastNameStart == -1 ? name : name.substring(lastNameStart);
    }

    public static int compareLastNames(Participant p1, Participant p2, Map<UUID, String> namePerParticipant) {
        String p1LastName = getLastName(p1, namePerParticipant);
        String p2LastName = getLastName(p2, namePerParticipant);
        return Collator.getInstance().compare(p1LastName, p2LastName);
    }

    public static class GradingInfo {
        public final float score;
        public final float maxScore;
        public final float ratio;
        public final float ratioPercent;
        public final float grade;
        public final String ratioPercentStr;
        public final String gradeStr;

        public GradingInfo(float score, float maxScore, float ratio, float ratioPercent, float grade, String ratioPercentStr, String gradeStr) {
            this.score = score;
            this.maxScore = maxScore;
            this.ratio = ratio;
            this.ratioPercent = ratioPercent;
            this.grade = grade;
            this.ratioPercentStr = ratioPercentStr;
            this.gradeStr = gradeStr;
        }
    }

    public static class Pair<L, R> {
        public final L left;
        public final R right;

        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }
    }
}
