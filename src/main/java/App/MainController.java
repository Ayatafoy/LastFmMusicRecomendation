package App;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;
import App.Exceptions.CannotParseRequestException;
import App.Exceptions.ServerErrorOccurredException;
import App.Exceptions.UnauthorizedUserException;
import App.Interfaces.IAuthService;
import App.Interfaces.IMusicRecomendService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.*;

@RestController
public class MainController {

    private Connection _connection;
    private IMusicRecomendService _musicRecomendService;
    private IAuthService _authService;

    private MainController() throws SQLException, ParseException, IOException, URISyntaxException {
        Driver driver = new com.mysql.jdbc.Driver();
        DriverManager.registerDriver(driver);
        Properties p = new Properties();
        p.setProperty("user", "root");
        p.setProperty("password", "root");
        p.setProperty("useUnicode", "true");
        p.setProperty("characterEncoding", "UTF-8");
        _connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/musicrange", p);
        _musicRecomendService = new MusicRecomendServiceLastFm(_connection);
        _authService = new AuthService(_connection);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/registerUser")
    public void registerUser(@RequestBody String userData) {
        User user = new User();
        JSONObject userJson;
        JSONParser parser = new JSONParser();
        try {
            userJson = (JSONObject) parser.parse(userData);
            convertToUserModel(user, userJson);
            _authService.RegisterUser(user);
        } catch (ParseException e) {
            throw new CannotParseRequestException();
        } catch (SQLException e) {
            throw new ServerErrorOccurredException();
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/getToken")
    public String getToken(@RequestParam(value = "login") String login, @RequestParam(value = "password") String password) {
        try {
            return _authService.GetToken(login, password);
        } catch (SQLException e) {
            throw new ServerErrorOccurredException();
        }
    }

    @RequestMapping(method = RequestMethod.POST, value = "/getAudio")
    public ArrayList<String> getAudio(@RequestBody String trackList, @RequestHeader(value = "token") String token) {
        JSONObject jsonRequest;
        JSONArray mp3list;
        if (_authService.IsUserAuthorised(token)) {
            String usersID = _authService.GetUserIdFromToken(token);
            JSONParser parser = new JSONParser();
            try {
                jsonRequest = (JSONObject) parser.parse(trackList);
                mp3list = (JSONArray) jsonRequest.get("mp3list");
                return _musicRecomendService.GetAudio(usersID, mp3list);
            } catch (ParseException e) {
                throw new CannotParseRequestException();
            } catch (Exception e) {
                throw new ServerErrorOccurredException();
            }
        } else
            throw new UnauthorizedUserException();
    }

    @RequestMapping(method = RequestMethod.POST, value = "/setEvaluation")
    public void setEvaluation(@RequestBody String trackList, @RequestHeader(value = "token") String token) {
        JSONObject jsonRequest;
        JSONArray mp3list;
        if (_authService.IsUserAuthorised(token)) {
            String usersID = _authService.GetUserIdFromToken(token);
            try {
                JSONParser parser = new JSONParser();
                jsonRequest = (JSONObject) parser.parse(trackList);
                mp3list = (JSONArray) jsonRequest.get("mp3list");
                _musicRecomendService.SetEvaluation(usersID, mp3list);
            } catch (ParseException e) {
                throw new CannotParseRequestException();
            } catch (Exception e) {
                throw new ServerErrorOccurredException();
            }
        } else
            throw new UnauthorizedUserException();
    }

    private void convertToUserModel(User user, JSONObject userJson) {
        user.Login = ((String) userJson.get("login")).toLowerCase();
        user.Password = ((String) userJson.get("password"));
        user.Name = ((String) userJson.get("name")).toLowerCase();
        user.Dob = ((String) userJson.get("dob")).toLowerCase();
        user.KindOfInteresting = ((String) userJson.get("KindOfInteresting")).toLowerCase();
        user.Genre = ((String) userJson.get("genre")).toLowerCase();
        user.Location = ((String) userJson.get("location")).toLowerCase();
    }
}
