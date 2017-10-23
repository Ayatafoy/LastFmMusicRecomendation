package App.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Алексей on 21.10.2017.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CannotParseRequestException extends RuntimeException {

    public CannotParseRequestException() {
        super("Data format is not correct!");
    }
}