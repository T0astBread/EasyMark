package easymark;

import easymark.cli.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.errors.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.*;

import java.security.*;
import java.util.*;

public class Utils {
    public static final SecureRandom RANDOM = new SecureRandom();
    public static final PasswordEncoder PASSWORD_ENCODER;
    public static final String CSV_DELIMITER = ";";

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

    public static GradingInfo gradingInfo(int totalScore, int maxScore) {
        if (maxScore > 0) {
            float ratio = ((float) totalScore) / ((float) maxScore);
            float ratioPercent = Math.round(ratio * 10000) / 100f;

            float grade = (ratio * 100 - 106.25f) / -12.5f;
            grade = Math.max(.5f, Math.min(5, grade));
            grade = Math.round(grade * 100) / 100f;

            return new GradingInfo(totalScore, maxScore, ratio, ratioPercent, grade, Float.toString(ratioPercent), Float.toString(grade));
        } else {
            return new GradingInfo(totalScore, maxScore, 0f, 0f, 3f, "-", "-");
        }
    }

    public static class GradingInfo {
        public final int score;
        public final int maxScore;
        public final float ratio;
        public final float ratioPercent;
        public final float grade;
        public final String ratioPercentStr;
        public final String gradeStr;

        public GradingInfo(int score, int maxScore, float ratio, float ratioPercent, float grade, String ratioPercentStr, String gradeStr) {
            this.score = score;
            this.maxScore = maxScore;
            this.ratio = ratio;
            this.ratioPercent = ratioPercent;
            this.grade = grade;
            this.ratioPercentStr = ratioPercentStr;
            this.gradeStr = gradeStr;
        }
    }
}
