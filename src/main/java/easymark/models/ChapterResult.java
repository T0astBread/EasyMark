package easymark.models;

import java.util.*;

public class ChapterResult extends Entity {
    private UUID participantId;
    private UUID chapterId;
    private boolean hasCompletedTest;

    public UUID getParticipantId() {
        return participantId;
    }

    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }

    public UUID getChapterId() {
        return chapterId;
    }

    public void setChapterId(UUID chapterId) {
        this.chapterId = chapterId;
    }

    public boolean isHasCompletedTest() {
        return hasCompletedTest;
    }

    public void setHasCompletedTest(boolean hasCompletedTest) {
        this.hasCompletedTest = hasCompletedTest;
    }
}
