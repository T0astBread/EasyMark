package easymark.cli;

import java.util.*;

public class AdminSelector {

    public static class ByID extends AdminSelector {
        public final UUID id;

        public ByID(UUID id) {
            this.id = id;
        }
    }

    public static class ByAdministeredCourse extends AdminSelector {
        public final String courseName;

        public ByAdministeredCourse(String courseName) {
            this.courseName = courseName;
        }
    }
}
