package info.kgeorgiy.ja.slastin.walk;

public class WalkException extends RuntimeException {
    WalkException(final String message) {
        super(message);
    }

    public static void throwOut(final String message, final String errorMessage) throws WalkException {
        throw new WalkException(message + ": " + errorMessage);
    }
}
