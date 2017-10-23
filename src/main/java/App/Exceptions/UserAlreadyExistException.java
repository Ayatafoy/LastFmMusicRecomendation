package App.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Алексей on 21.10.2017.
 */
@ResponseStatus(HttpStatus.FOUND)
public class UserAlreadyExistException extends RuntimeException {

    public UserAlreadyExistException(String login) {
        super("User '" + login + "' is already exists.");
    }
}
