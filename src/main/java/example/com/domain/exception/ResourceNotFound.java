package example.com.domain.exception;

public class ResourceNotFound extends RuntimeException{
    public ResourceNotFound(String notFoundMessage) {
        super(notFoundMessage);
    }
}
