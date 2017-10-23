package App.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Алексей on 21.10.2017.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ServerErrorOccurredException extends RuntimeException {

    public ServerErrorOccurredException() {
        super("Server error occurred!");
    }
}