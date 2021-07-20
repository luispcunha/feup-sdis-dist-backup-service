package file;

@SuppressWarnings("serial")
public class FileSizeException extends Exception {
    private String path;

    public FileSizeException(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "The file " + path + " is too large.";
    }
}