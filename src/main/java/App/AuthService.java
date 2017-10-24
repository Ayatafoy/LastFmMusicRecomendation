package App;

import App.Exceptions.ServerErrorOccurredException;
import App.Exceptions.UnauthorizedUserException;
import App.Exceptions.UserAlreadyExistException;
import App.Interfaces.IAuthService;
import javafx.util.Pair;
import org.apache.commons.codec.digest.DigestUtils;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Алексей on 21.10.2017.
 */
public class AuthService implements IAuthService {

    private HashMap<String, Pair<LocalDateTime, String>> _tokens = new HashMap<>();
    private SecureRandom random = new SecureRandom();
    private Connection _connection;

    public AuthService(Connection connection) {
        _connection = connection;
    }

    @Override
    public void RegisterUser(User user) throws SQLException {
        Statement statement = _connection.createStatement();
        String sql = "select * from musicrange.users where login = '" + user.Login + "';";
        ResultSet resultSet;
        resultSet = statement.executeQuery(sql);
        if (resultSet.next()) {
            throw new UserAlreadyExistException(user.Login);
        }
        sql = "insert into musicrange.users (Login, Password, Name, DOB, KindOfInteresting, Genre, Location)  VALUES " +
                "('" + user.Login + "', '" + DigestUtils.sha256Hex(user.Password) + "', '" + user.Name + "', '" + user.Dob + "', '" +
                user.KindOfInteresting + "', '" + user.Genre + "', '" + user.Location + "')";
        statement.execute(sql);
    }

    @Override
    public String GetToken(String login, String password) throws SQLException {
        String sql = "select password from musicrange.users where login = '" + login + "';";
        ResultSet resultSet;
        Statement statement = _connection.createStatement();
        resultSet = statement.executeQuery(sql);
        try {
            if (resultSet.next()) {
                if (DigestUtils.sha256Hex(password).equals(resultSet.getString(2))) {
                    String token = generateToken(login);
                    Pair<LocalDateTime, String> tokenInfo = new Pair(LocalDateTime.now(), login);
                    for (Map.Entry<String, Pair<LocalDateTime, String>> entry : _tokens.entrySet()) {
                        String oldToken = entry.getKey();
                        String value = entry.getValue().getValue();
                        if (value.equals(login)) {
                            _tokens.remove(oldToken);
                            break;
                        }
                    }
                    _tokens.put(token, tokenInfo);
                    return token;
                }
            }
        } catch (Exception e) {
            throw new ServerErrorOccurredException();
        }
        throw new UnauthorizedUserException();
    }

    @Override
    public boolean IsUserAuthorised(String token) {
        return _tokens.get(token) != null
                && ChronoUnit.MINUTES.between(_tokens.get(token).getKey(), LocalDateTime.now()) <= 5;
    }

    @Override
    public String GetUserLoginFromToken(String token) {
        return _tokens.get(token).getValue().toString();
    }

    private synchronized String generateToken(String username) {
        long longToken = Math.abs(random.nextLong());
        String random = Long.toString(longToken, 16);
        return DigestUtils.sha256Hex((new Date() + ":" + username + ":" + random));
    }
}
