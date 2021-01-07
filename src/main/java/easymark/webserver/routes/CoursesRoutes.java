package easymark.webserver.routes;

import easymark.*;
import easymark.database.*;
import easymark.database.models.*;
import easymark.webserver.*;
import easymark.webserver.constants.*;
import easymark.webserver.sessions.*;
import io.javalin.*;
import io.javalin.http.*;

import java.text.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import static easymark.webserver.WebServerUtils.*;
import static io.javalin.core.security.SecurityUtil.*;

public class CoursesRoutes {
    public static void configure(Javalin app, SessionManager sessionManager) {

        new CommonRouteBuilder("courses")
                .withShow(roles(UserRole.ADMIN), (ctx, courseId) -> {
                    Session session = getSession(sessionManager, ctx);
                    UUID entityId = session.getUserId();

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
                })

                .withCreateOrdered(
                        roles(UserRole.ADMIN),
                        Database::getCourses,
                        Course::getAdminId,
                        (db, ctx, groupId) -> {
                            Course newCourse = new Course();
                            newCourse.setAdminId(groupId);
                            newCourse.setName(ctx.formParam(FormKeys.NAME));
                            logActivity(db, getSession(sessionManager, ctx),
                                    "Course created: [b]" + newCourse.getName() + "[/b]");
                            return newCourse;
                        })

                .withEdit(roles(UserRole.ADMIN), (ctx, courseId) -> {
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
                })

                .withUpdate(roles(UserRole.ADMIN), (ctx, courseId) -> {
                    try (DatabaseHandle db = DBMS.openWrite()) {
                        Course course = db.get().getCourses()
                                .stream()
                                .filter(c -> c.getId().equals(courseId))
                                .findAny()
                                .orElseThrow(() -> new NotFoundResponse("Course not found"));
                        course.setName(ctx.formParam(FormKeys.NAME));
                        logActivity(db.get(), getSession(sessionManager, ctx),
                                "Course updated: [b]" + course.getName() + "[/b]");
                        DBMS.store();
                    }

                    ctx.redirect("/courses/" + courseId);
                })

                .withUpdateOrder(
                        roles(UserRole.ADMIN),
                        Database::getCourses,
                        Course::getAdminId)

                .withConfirmDelete(roles(UserRole.ADMIN), (ctx, entityId) -> {
                    String cancelUrl = ctx.queryParam(QueryKeys.CANCEL_URL);
                    String redirectUrl = ctx.queryParam(QueryKeys.RECIRECT_URL);
                    String deleteUrl = "/courses/" + entityId + "/delete";

                    if (redirectUrl == null || cancelUrl == null)
                        throw new BadRequestResponse();

                    String entityName;
                    try (DatabaseHandle db = DBMS.openRead()) {
                        entityName = "course \"" + db.get().getCourses()
                                .stream()
                                .filter(course -> course.getId().equals(entityId))
                                .findAny()
                                .orElseThrow(NotFoundResponse::new)
                                .getName() + "\" including all associated participants, chapters, test requests, assignments and assignment results";
                    }

                    ctx.render("pages/confirm-delete.peb", Map.of(
                            ModelKeys.DELETE_URL, deleteUrl,
                            ModelKeys.DELETE_ENTITY_NAME, entityName,
                            ModelKeys.REDIRECT_URL, redirectUrl,
                            ModelKeys.CANCEL_URL, cancelUrl,
                            ModelKeys.CSRF_TOKEN, makeCSRFToken(ctx)
                    ));
                })

                .withDeleteOrdered(
                        roles(UserRole.ADMIN),
                        Database::getCourses,
                        Course::getAdminId,
                        (db, courseId, ctx) -> {
                            Course course = db.getCourses()
                                    .stream()
                                    .filter(c -> c.getId().equals(courseId))
                                    .findAny()
                                    .orElseThrow(NotFoundResponse::new);
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
                            db.getCourses().remove(course);
                            logActivity(db, getSession(sessionManager, ctx),
                                    "Course deleted: [b]" + course.getName() + "[/b]");
                            return course.getAdminId();
                        })

                .applyTo(app);


        app.get("/courses/:id/grading", ctx -> {
            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            Session session = getSession(sessionManager, ctx);
            UUID entityId = session.getUserId();
            String uek = null;
            try {
                uek = getUek(ctx, session);
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
            Map<UUID, Float> scorePerParticipant;
            Map<UUID, Float> maxScorePerParticipant;
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
                                    .sorted(Comparator.comparingInt(assignment -> assignment.getOrdNum() == -2 ? Integer.MAX_VALUE : assignment.getOrdNum()))
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

                            float totalScore = (float) assignmentResults.stream()
                                    .mapToDouble(AssignmentResult::getScore)
                                    .sum();
                            float maxScore = (float) assignmentResults.stream()
                                    .mapToDouble(assignmentResult -> db.get().getAssignments()
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
                        .sorted((p1, p2) -> {
                            // Sort by group, then last name

                            boolean p1GroupIsEmpty = p1.getGroup() == null || p1.getGroup().isBlank();
                            boolean p2GroupIsEmpty = p2.getGroup() == null || p2.getGroup().isBlank();
                            if (p1GroupIsEmpty) {
                                if (p2GroupIsEmpty)
                                    return Utils.compareLastNames(p1, p2, namePerParticipant);
                                else return 1;
                            }
                            if (p2GroupIsEmpty)
                                return -1;

                            int groupComparison = Collator.getInstance().compare(p1.getGroup(), p2.getGroup());
                            if (groupComparison != 0) return groupComparison;

                            return Utils.compareLastNames(p1, p2, namePerParticipant);
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
                Course course = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Course not found"));

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
                            float scoreFormValue;
                            try {
                                scoreFormValue = Float.parseFloat(scoreFormValueStr);
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
                logActivity(db.get(), getSession(sessionManager, ctx),
                        "Grading updated: [b]" + course.getName() + "[/b]");
                DBMS.store();
            }

            ctx.redirect("/courses/" + courseId + "/grading");
        }, roles(UserRole.ADMIN));

        app.get("/courses/:id/grades-csv", ctx -> {
            UUID courseId;
            try {
                courseId = UUID.fromString(ctx.pathParam(PathParams.ID));
            } catch (Exception e) {
                throw new BadRequestResponse("Bad request");
            }

            String uek = getUek(ctx, getSession(sessionManager, ctx));

            String courseName;
            Map<UUID, List<AssignmentResult>> assignmentResultsPerParticipant;
            Map<UUID, Float> maxScorePerAssignment;
            Map<UUID, String> namePerParticipant = new HashMap<>();
            Set<UUID> participantIDs = new HashSet<>();
            Set<UUID> assignmentIDs = new HashSet<>();
            try (DatabaseHandle db = DBMS.openRead()) {
                courseName = db.get().getCourses()
                        .stream()
                        .filter(c -> c.getId().equals(courseId))
                        .findAny()
                        .orElseThrow(() -> new NotFoundResponse("Course not found"))
                        .getName();
                for (Participant participant : db.get().getParticipants()) {
                    if (!participant.getCourseId().equals(courseId))
                        continue;
                    participantIDs.add(participant.getId());
                    try {
                        String name = Cryptography.decryptData(participant.getName(), participant.getNameSalt(), uek);
                        namePerParticipant.put(participant.getId(), name);
                    } catch (Exception e) {
                        throw new InternalServerErrorResponse("Failed to decrypt student names\n" + e.getClass().getName() + ": " + e.getMessage());
                    }
                }
                assignmentResultsPerParticipant = db.get().getAssignmentResults()
                        .stream()
                        .filter(ar -> participantIDs.contains(ar.getParticipantId()))
                        .peek(ar -> assignmentIDs.add(ar.getAssignmentId()))
                        .collect(Collectors.groupingBy(AssignmentResult::getParticipantId));
                maxScorePerAssignment = db.get().getAssignments()
                        .stream()
                        .filter(a -> assignmentIDs.contains(a.getId()))
                        .collect(Collectors.toMap(Entity::getId, Assignment::getMaxScore));
            }
            String fileContents = participantIDs
                    .stream()
                    .map(participantId -> {
                        List<AssignmentResult> assignmentResults = assignmentResultsPerParticipant.getOrDefault(participantId, Collections.EMPTY_LIST);
                        float totalScore = (float) assignmentResults.stream()
                                .mapToDouble(AssignmentResult::getScore)
                                .sum();
                        float maxScore = (float) assignmentResults.stream()
                                .mapToDouble(ar -> maxScorePerAssignment.get(ar.getAssignmentId()))
                                .sum();
                        Utils.GradingInfo gradingInfo = Utils.gradingInfo(totalScore, maxScore);
                        String name = namePerParticipant.get(participantId);
                        return String.join(Utils.CSV_DELIMITER, name, Float.toString(gradingInfo.ratioPercent), Float.toString(gradingInfo.grade));
                    })
                    .collect(Collectors.joining("\n")) + "\n";

            String fileName = courseName.replaceAll("\\s", "_") + "__" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + "__grades.csv";
            ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            ctx.header("Content-Type", "text/csv");
            ctx.result(fileContents);
        }, roles(UserRole.ADMIN));
    }
}
