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
}
