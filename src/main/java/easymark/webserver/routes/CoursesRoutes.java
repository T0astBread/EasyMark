package easymark.webserver.routes;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import io.javalin.*;
import io.javalin.http.*;

import java.util.*;
import java.util.stream.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.*;

public class CoursesRoutes {
    public static void configure(Javalin app) {

        app.get("/courses/:id", ctx -> {
            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            UUID entityId = ctx.sessionAttribute(SessionKeys.ENTITY_ID);

            Optional<Course> course;
            List<Chapter> chapters;
            Map<UUID, List<Assignment>> assignmentsPerChapter;
            Map<UUID, Assignment> testAssignmentPerChapter;
            try (DatabaseHandle db = DBMS.openRead()) {
                course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny();

                if (course.isEmpty())
                    throw new NotFoundResponse("Course not found");
                if (!course.get().getAdminId().equals(entityId))
                    throw new ForbiddenResponse("You are not the admin of this course");

                assignmentsPerChapter = new HashMap<>();
                testAssignmentPerChapter = new HashMap<>();
                chapters = db.get().getChapters()
                        .stream()
                        .filter(chapter -> chapter.getCourseId().equals(courseId))
                        .peek(chapter -> {
                            List<Assignment> assignments = db.get().getAssignments()
                                    .stream()
                                    .filter(assignment -> assignment.getChapterId().equals(chapter.getId()))
                                    .filter(assignment -> {
                                        boolean isTestAssignment = assignment.getId().equals(chapter.getTestAssignmentId());
                                        if (isTestAssignment)
                                            testAssignmentPerChapter.put(chapter.getId(), assignment);
                                        return !isTestAssignment;
                                    })
                                    .sorted(Comparator.comparingInt(Assignment::getOrdNum))
                                    .collect(Collectors.toUnmodifiableList());
                            assignmentsPerChapter.put(chapter.getId(), assignments);
                        })
                        .sorted(Comparator.comparingInt(Chapter::getOrdNum))
                        .collect(Collectors.toUnmodifiableList());
            }

            ctx.render("pages/courses_show.peb", Map.of(
                    ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx),
                    ModelKeys.COURSE, course.get(),
                    ModelKeys.CHAPTERS, chapters,
                    ModelKeys.ASSIGNMENTS_PER_CHAPTER, assignmentsPerChapter,
                    ModelKeys.TEST_ASSIGNMENT_PER_CHAPTER, testAssignmentPerChapter
            ));
        }, roles(UserRole.ADMIN));


        app.get("/courses/:id/grading", ctx -> {
            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            UUID entityId = ctx.sessionAttribute(SessionKeys.ENTITY_ID);
            String uek = null;
            try {
                uek = getUekFromContext(ctx);
            } catch (Exception e) {
            }
            String _uek = uek;

            Optional<Course> course;
            List<Chapter> chapters;
            List<Assignment> assignments;
            Map<UUID, List<Assignment>> assignmentsPerChapter;
            List<Participant> participants;
            Map<UUID, Map<UUID, AssignmentResult>> assignmentResultsPerAssignmentPerParticipant;
            Map<UUID, String> namePerParticipant;
            Map<UUID, Integer> scorePerParticipant;
            Map<UUID, Integer> maxScorePerParticipant;
            Map<UUID, String> ratioPerParticipant;
            Map<UUID, String> gradePerParticipant;
            try (DatabaseHandle db = DBMS.openRead()) {
                course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny();

                if (course.isEmpty())
                    throw new NotFoundResponse("Course not found");
                if (!course.get().getAdminId().equals(entityId))
                    throw new ForbiddenResponse("You are not the admin of this course");

                assignmentsPerChapter = new HashMap<>();
                assignments = new ArrayList<>();
                chapters = db.get().getChapters()
                        .stream()
                        .filter(chapter -> chapter.getCourseId().equals(courseId))
                        .peek(chapter -> {
                            List<Assignment> assignmentsForParticipant = db.get().getAssignments()
                                    .stream()
                                    .filter(assignment -> assignment.getChapterId().equals(chapter.getId()))
                                    .sorted(Comparator.comparingInt(Assignment::getOrdNum))
                                    .collect(Collectors.toUnmodifiableList());
                            assignments.addAll(assignmentsForParticipant);
                            assignmentsPerChapter.put(chapter.getId(), assignmentsForParticipant);
                        })
                        .sorted(Comparator.comparingInt(Chapter::getOrdNum))
                        .collect(Collectors.toUnmodifiableList());

                assignmentResultsPerAssignmentPerParticipant = new HashMap<>();
                namePerParticipant = new HashMap<>();
                scorePerParticipant = new HashMap<>();
                maxScorePerParticipant = new HashMap<>();
                ratioPerParticipant = new HashMap<>();
                gradePerParticipant = new HashMap<>();
                participants = db.get().getParticipants()
                        .stream()
//                        .flatMap(participant -> List.of(participant, participant, participant, participant, participant, participant, participant, participant).stream())
                        .filter(participant -> participant.getCourseId().equals(courseId))
                        .peek(participant -> {
                            List<AssignmentResult> assignmentResults = db.get().getAssignmentResults()
                                    .stream()
                                    .filter(assignmentResult -> assignmentResult.getParticipantId().equals(participant.getId()))
                                    .collect(Collectors.toUnmodifiableList());

                            Map<UUID, AssignmentResult> assignmentResultPerAssignment = new HashMap<>();
                            assignmentResultsPerAssignmentPerParticipant.put(participant.getId(), assignmentResultPerAssignment);
                            for (AssignmentResult assignmentResult : assignmentResults)
                                assignmentResultPerAssignment.put(assignmentResult.getAssignmentId(), assignmentResult);

                            if (_uek != null) {
                                try {
                                    String name = Cryptography.decryptData(participant.getName(), participant.getNameSalt(), _uek);
                                    namePerParticipant.put(participant.getId(), name);
                                } catch (Exception e) {
                                    namePerParticipant.put(participant.getId(), "Decryption failure");
                                }
                            } else {
                                namePerParticipant.put(participant.getId(), "Decryption failure");
                            }

                            int totalScore = assignmentResults.stream()
                                    .mapToInt(AssignmentResult::getScore)
                                    .sum();
                            int maxScore = assignmentResults.stream()
                                    .mapToInt(assignmentResult -> db.get().getAssignments()
                                            .stream()
                                            .filter(assignment -> assignment.getId().equals(assignmentResult.getAssignmentId()))
                                            .findAny()
                                            .orElseThrow()
                                            .getMaxScore())
                                    .sum();
                            scorePerParticipant.put(participant.getId(), totalScore);
                            maxScorePerParticipant.put(participant.getId(), maxScore);

                            Utils.GradingInfo gradingInfo = Utils.gradingInfo(totalScore, maxScore);
                            ratioPerParticipant.put(participant.getId(), gradingInfo.ratioPercentStr);
                            gradePerParticipant.put(participant.getId(), gradingInfo.gradeStr);
                        })
                        .collect(Collectors.toUnmodifiableList());
            }
            final int assignmentCount = assignmentsPerChapter.values()
                    .stream()
                    .mapToInt(List::size)
                    .sum();

            Map<String, Object> model = new HashMap<>();
            model.put(ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx));
            model.put(ModelKeys.COURSE, course.get());
            model.put(ModelKeys.CHAPTERS, chapters);
            model.put(ModelKeys.PARTICIPANTS, participants);
            model.put(ModelKeys.ASSIGNMENTS, assignments);
            model.put(ModelKeys.ASSIGNMENT_RESULT_PER_ASSIGNMENT_PER_PARTICIPANT, assignmentResultsPerAssignmentPerParticipant);
            model.put(ModelKeys.ASSIGNMENT_COUNT, assignmentCount);
            model.put(ModelKeys.ASSIGNMENTS_PER_CHAPTER, assignmentsPerChapter);
            model.put(ModelKeys.NAME_PER_PARTICIPANT, namePerParticipant);
            model.put(ModelKeys.SCORE_PER_PARTICIPANT, scorePerParticipant);
            model.put(ModelKeys.MAX_SCORE_PER_PARTICIPANT, maxScorePerParticipant);
            model.put(ModelKeys.RATIO_PER_PARTICIPANT, ratioPerParticipant);
            model.put(ModelKeys.GRADE_PER_PARTICIPANT, gradePerParticipant);
            ctx.render("pages/courses_grading.peb", model);
        }, roles(UserRole.ADMIN));


        app.post("/courses/:id/grading", ctx -> {
            Map<String, List<String>> formParams = ctx.formParamMap();

            if (!checkCSRFToken(ctx, getFormParam(formParams, FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            try (DatabaseHandle db = DBMS.openWrite()) {
                for (Participant participant : db.get().getParticipants()) {
                    String warning = getFormParam(formParams, participant.getId() + "-warning");
                    String group = getFormParam(formParams, participant.getId() + "-group");
                    String notes = getFormParam(formParams, participant.getId() + "-notes");
                    participant.setWarning(warning);
                    participant.setGroup(group);
                    participant.setNotes(notes);

                    List<Chapter> chapters = db.get().getChapters()
                            .stream()
                            .filter(chapter -> chapter.getCourseId().equals(courseId))
                            .collect(Collectors.toUnmodifiableList());
                    List<Assignment> assignments = db.get().getAssignments()
                            .stream()
                            .filter(assignment -> chapters.stream()
                                    .anyMatch(chapter -> chapter.getId().equals(assignment.getChapterId())))
                            .collect(Collectors.toUnmodifiableList());
                    for (Assignment assignment : assignments) {
                        String scoreFormKey = participant.getId() + "-score-" + assignment.getId();
                        String scoreFormValueStr = getFormParam(formParams, scoreFormKey);

                        if (scoreFormValueStr == null || scoreFormValueStr.isBlank()) {
                            db.get().getAssignmentResults().removeIf(ar ->
                                    ar.getParticipantId().equals(participant.getId()) &&
                                            ar.getAssignmentId().equals(assignment.getId()));
                        } else {
                            int scoreFormValue;
                            try {
                                scoreFormValue = Integer.parseInt(scoreFormValueStr);
                            } catch (IllegalArgumentException e) {
                                throw new BadRequestResponse();
                            }
                            Optional<AssignmentResult> assignmentResult = db.get().getAssignmentResults()
                                    .stream()
                                    .filter(ar ->
                                            ar.getParticipantId().equals(participant.getId()) &&
                                                    ar.getAssignmentId().equals(assignment.getId()))
                                    .findAny();
                            if (assignmentResult.isPresent()) {
                                assignmentResult.get().setScore(scoreFormValue);
                            } else {
                                AssignmentResult newAssignmentResult = new AssignmentResult();
                                newAssignmentResult.setParticipantId(participant.getId());
                                newAssignmentResult.setAssignmentId(assignment.getId());
                                newAssignmentResult.setScore(scoreFormValue);
                                db.get().getAssignmentResults().add(newAssignmentResult);
                            }
                        }
                    }
                }
                DBMS.store();
            }

            ctx.redirect("/courses/" + courseId + "/grading");
        }, roles(UserRole.ADMIN));


        app.get("/courses/:id/edit", ctx -> {
            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            Course course;
            try (DatabaseHandle db = DBMS.openRead()) {
                course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Course not found"));
            }

            ctx.render("pages/courses_edit.peb", Map.of(
                    ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx),
                    ModelKeys.COURSE, course
            ));
        });

        app.post("/courses/:id/save", ctx -> {
            if (!checkCSRFToken(ctx, ctx.formParam(FormKeys.CSRF_TOKEN)))
                throw new ForbiddenResponse();

            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            try (DatabaseHandle db = DBMS.openWrite()) {
                Course course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Course not found"));
                course.setName(ctx.formParam(FormKeys.NAME));
                DBMS.store();
            }

            ctx.redirect("/courses/" + courseId);
        });

        GenericOrdListRoutes.configure(
                app,
                "courses",

                (db, ctx, groupId) -> {
                    Course newCourse = new Course();
                    newCourse.setAdminId(groupId);
                    newCourse.setName(ctx.formParam(FormKeys.NAME));
                    return newCourse;
                },

                (db, entityId) -> "course \"" + db.getCourses()
                        .stream()
                        .filter(course -> course.getId().equals(entityId))
                        .findAny()
                        .orElseThrow(NotFoundResponse::new)
                        .getName() + "\" including all associated participants, chapters, test requests, assignments and assignment results",

                (db, courseId, ctx) -> {
                    db.getChapters().removeAll(db.getChapters()
                            .stream()
                            .filter(chapter -> courseId.equals(chapter.getCourseId()))
                            .peek(chapter -> {
                                db.getTestRequests()
                                        .removeIf(testRequest -> testRequest.getChapterId().equals(chapter.getId()));
                                db.getAssignments().removeAll(db.getAssignments()
                                        .stream()
                                        .filter(assignment -> assignment.getChapterId().equals(chapter.getId()))
                                        .peek(assignment -> db.getAssignmentResults()
                                                .removeIf(ar -> ar.getAssignmentId().equals(assignment.getId())))
                                        .collect(Collectors.toUnmodifiableSet()));
                            })
                            .collect(Collectors.toUnmodifiableSet()));
                    db.getParticipants().removeIf(participant -> participant.getCourseId().equals(courseId));
                    db.getCourses().removeIf(course -> course.getId().equals(courseId));
                },

                Database::getCourses,
                Course::getAdminId
        );
    }

    private static String getFormParam(Map<String, List<String>> formParams, String key) {
        List<String> vals = formParams.getOrDefault(key, null);
        return vals == null ? null : vals.get(0);
    }
}
