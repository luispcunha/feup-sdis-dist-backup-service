package message;

@SuppressWarnings("serial")
public class InvalidMessageException extends Exception {
    public InvalidMessageException(String message) {
        super(message);
    }
}