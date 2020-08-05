package easymark.errors;

public class UserFriendlyException extends Exception {
    private final Integer proposedExitStatus;

    public UserFriendlyException(String message) {
        super(message);
        this.proposedExitStatus = null;
    }

    public UserFriendlyException(String message, Integer proposedExitStatus) {
        super(message);
        this.proposedExitStatus = proposedExitStatus;
    }

    public UserFriendlyException(String message, Throwable cause) {
        super(message, cause);
        this.proposedExitStatus = null;
    }

    public UserFriendlyException(String message, Throwable cause, Integer proposedExitStatus) {
        super(message, cause);
        this.proposedExitStatus = proposedExitStatus;
    }

    public Integer getProposedExitStatus() {
        return proposedExitStatus;
    }
}
