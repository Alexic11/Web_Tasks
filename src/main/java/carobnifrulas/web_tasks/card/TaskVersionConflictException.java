package carobnifrulas.web_tasks.card;

public class TaskVersionConflictException extends RuntimeException {

    public TaskVersionConflictException() {
        super("Task je u međuvremenu promijenjen od strane drugog korisnika.");
    }

    public TaskVersionConflictException(String message) {
        super(message);
    }
}
