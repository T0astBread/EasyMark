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
                chapters = db.get().getChapters()
                        .stream()
                        .filter(chapter -> chapter.getCourseId().equals(courseId))
                        .peek(chapter -> {
                            List<Assignment> assignments = db.get().getAssignments()
                                    .stream()
                                    .filter(assignment -> assignment.getChapterId().equals(chapter.getId()))
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
                    ModelKeys.ASSIGNMENTS_PER_CHAPTER, assignmentsPerChapter
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
            Map<UUID, Float> ratioPerParticipant;
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
                                String name = Cryptography.decryptData(participant.getName(), participant.getNameSalt(), _uek);
                                namePerParticipant.put(participant.getId(), name);
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
                            float ratio = ((float)totalScore) / ((float) maxScore);
                            scorePerParticipant.put(participant.getId(), totalScore);
                            maxScorePerParticipant.put(participant.getId(), maxScore);
                            ratioPerParticipant.put(participant.getId(), ratio);
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
            ctx.render("pages/courses_grading.peb", model);
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
}
