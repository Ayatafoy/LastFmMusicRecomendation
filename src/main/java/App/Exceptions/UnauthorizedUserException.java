package App.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Алексей on 21.10.2017.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedUserException extends RuntimeException {

    public UnauthorizedUserException() {
        super("Authorisation field!");
    }
}
