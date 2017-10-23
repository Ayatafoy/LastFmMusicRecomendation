package App.Interfaces;

import App.User;

import java.sql.SQLException;

/**
 * Created by Алексей on 21.10.2017.
 */
public interface IAuthService {
    void RegisterUser(User user) throws SQLException;

    String GetToken(String login, String password) throws SQLException;

    boolean IsUserAuthorised(String token);

    String GetUserIdFromToken(String token);
}
