package hello;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import hello.CryptoPro.TokenGenerator;
import javafx.util.Pair;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class MainController {

    private Connection _connection;
    private Statement _statement;
    public MusicRecomendManager _musicRecomendManager;
    private HashMap<String, Pair<LocalDateTime, Integer>> _tokens = new HashMap<>();

    private MainController() throws SQLException, ParseException, IOException, URISyntaxException {
        Driver driver = new com.mysql.jdbc.Driver();
        DriverManager.registerDriver(driver);
        Properties p = new Properties();
        p.setProperty("user", "root");
        p.setProperty("password", "root");
        p.setProperty("useUnicode", "true");
        p.setProperty("characterEncoding", "UTF-8");
        _connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/musicrange", p);
        _statement = _connection.createStatement();
        _musicRecomendManager = new MusicRecomendManager(_connection);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/registerUser")
    public void registerUser(@RequestBody String userData) {
        String login;
        String password;
        String name;
        String dob;
        String kindOfInteresting;
        String genre;
        String location;
        JSONObject userJson;
        JSONParser parser = new JSONParser();
        try {
            userJson = (JSONObject) parser.parse(userData);
            login = ((String) userJson.get("login")).toLowerCase();
        } catch (Exception e) {
            throw new CannotParseRequestException();
        }
        String sql = "select * from musicrange.users where login = '" + login + "';";
        ResultSet resultSet;
        try {
            resultSet = _statement.executeQuery(sql);
        } catch (SQLException e) {
            throw new ServerErrorOccurredException();
        }
        try {
            if (resultSet.next()) {
                throw new UserAlreadyExistException(login);
            }
        } catch (SQLException e) {
            throw new ServerErrorOccurredException();
        }
        try {
            password = ((String) userJson.get("password"));
            name = ((String) userJson.get("name")).toLowerCase();
            dob = ((String) userJson.get("dob")).toLowerCase();
            kindOfInteresting = ((String) userJson.get("KindOfInteresting")).toLowerCase();
            genre = ((String) userJson.get("genre")).toLowerCase();
            location = ((String) userJson.get("location")).toLowerCase();
            sql = "insert into musicrange.users (Login, Password, Name, DOB, KindOfInteresting, Genre, Location)  VALUES " +
                    "('" + login + "', '" + DigestUtils.sha256Hex(password) + "', '" + name + "', '" + dob + "', '" +
                    kindOfInteresting + "', '" + genre + "', '" + location + "')";
        } catch (Exception e) {
            throw new CannotParseRequestException();
        }
        try {
            _statement.execute(sql);
        } catch (SQLException e) {
            throw new ServerErrorOccurredException();
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/getToken")
    public String getToken(@RequestParam(value = "login") String login, @RequestParam(value = "password") String password) {
        String sql = "select idUsers, password from musicrange.users where login = '" + login + "';";
        ResultSet resultSet;
        try {
            Statement statement = _connection.createStatement();
            resultSet = statement.executeQuery(sql);
        } catch (SQLException e) {
            throw new ServerErrorOccurredException();
        }
        try {
            if (resultSet.next()) {
                int userId = resultSet.getInt(1);
                if (DigestUtils.sha256Hex(password).equals(resultSet.getString(2))) {
                    String token = TokenGenerator.generateToken(login);
                    Pair<LocalDateTime, Integer> tokenInfo = new Pair(LocalDateTime.now(), userId);
                    for(Map.Entry<String, Pair<LocalDateTime, Integer>> entry : _tokens.entrySet()) {
                        String oldToken = entry.getKey();
                        int value = entry.getValue().getValue();
                        if (value == userId){
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

    @RequestMapping(method = RequestMethod.POST, value = "/getAudio")
    public ArrayList<String> getAudio(@RequestBody String trackList, @RequestHeader(value = "token") String token) {
        String sql;
        JSONObject jsonRequest;
        JSONArray mp3list;
        if (_tokens.get(token) != null && ChronoUnit.MINUTES.between(_tokens.get(token).getKey(), LocalDateTime.now()) <= 5) {
            try {
                JSONParser parser = new JSONParser();
                jsonRequest = (JSONObject) parser.parse(trackList);
                mp3list = (JSONArray) jsonRequest.get("mp3list");
            } catch (Exception e) {
                throw new CannotParseRequestException();
            }
            if (mp3list != null) {
                String usersID = _tokens.get(token).getValue().toString();
                ArrayList<MusicRecomendManager.MyPair> userLegacyTrackList = _musicRecomendManager.friendsListsArr.get(usersID);
                if (userLegacyTrackList == null) {
                    _musicRecomendManager.friendsListsArr.put(usersID, new ArrayList<>());
                    _musicRecomendManager.friendsArtistCount.put(usersID, new HashMap<>());
                    _musicRecomendManager.friendsGenderCount.put(usersID, new HashMap<>());
                    _musicRecomendManager.friends.put(usersID, new HashMap<>());
                }
                for (int i = 0; i < mp3list.size(); i++) {
                    try {
                        JSONObject mp3 = (JSONObject) mp3list.get(i);
                        String artistName = ((String) mp3.get("artist")).toLowerCase();
                        String title = ((String) mp3.get("title")).toLowerCase();
                        int genreID = 18;
                        if (mp3.get("genre") != null)
                            genreID = Integer.parseInt(mp3.get("genre").toString());
                        if (_musicRecomendManager.friendsArtistCount.get(usersID).get(artistName) == null) {
                            _musicRecomendManager.friendsArtistCount.get(usersID).put(artistName, 0.0);
                            _musicRecomendManager.friends.get(usersID).put(artistName, new ArrayList<>());
                        }
                        if (_musicRecomendManager.friendsGenderCount.get(usersID).get(genreID) == null){
                            _musicRecomendManager.friendsGenderCount.get(usersID).put(genreID, 0.0);
                        }
                        boolean flag = false;
                        for (MusicRecomendManager.MyPair track : userLegacyTrackList) {
                            if (artistName.equals(track.getLeft()) && title.equals(track.getRight())) {
                                flag = true;
                                break;
                            }
                        }
                        if (!flag){
                            _musicRecomendManager.friendsListsArr.get(usersID).add(new MusicRecomendManager.MyPair(artistName, title));
                            _musicRecomendManager.friends.get(usersID).get(artistName).add(new MusicRecomendManager.MyPair(title, genreID));
                            sql = "INSERT Into musicrange.usersdata(idUsersData, artist, title, genreID) VALUES " +
                                    "('" + usersID + "', '" + artistName + "', '" + title + "', '" + genreID + "')";
                            try {
                                _statement.execute(sql);
                            } catch (SQLException e) {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        throw new CannotParseRequestException();
                    }
                }
                try {
                    return _musicRecomendManager.searchAction(_tokens.get(token).getValue().toString());
                } catch (URISyntaxException e) {
                    throw new ServerErrorOccurredException();
                } catch (SQLException e) {
                    throw new ServerErrorOccurredException();
                } catch (ParseException e) {
                    throw new ServerErrorOccurredException();
                } catch (IOException e) {
                    throw new ServerErrorOccurredException();
                }
            }
        }
        else
            throw new UnauthorizedUserException();
        return null;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/setEvaluation")
    public void setEvaluation(@RequestBody String trackList, @RequestHeader(value = "token") String token)  {
        String sql;
        JSONObject jsonRequest;
        JSONArray mp3list;
        if (_tokens.get(token) != null && ChronoUnit.MINUTES.between(_tokens.get(token).getKey(), LocalDateTime.now()) <= 5) {
            String usersID = _tokens.get(token).getValue().toString();
            try {
                JSONParser parser = new JSONParser();
                jsonRequest = (JSONObject) parser.parse(trackList);
                mp3list = (JSONArray) jsonRequest.get("mp3list");
            } catch (Exception e) {
                throw new CannotParseRequestException();
            }
            if (mp3list != null) {
                for (int i = 0; i < mp3list.size(); i++) {
                    try {
                        JSONObject mp3 = (JSONObject) mp3list.get(i);
                        String artistName = ((String) mp3.get("artist")).toLowerCase();
                        String title = ((String) mp3.get("title")).toLowerCase();
                        int genreID = 18;
                        if (mp3.get("genre") != null)
                            genreID = Integer.parseInt(mp3.get("genre").toString());
                        int evaluation = Integer.parseInt(mp3.get("evaluation").toString());
                        sql = "select * from musicrange.userEvaluation where idUser = '" + usersID + "' and artist = '" + artistName +
                                "' and title = '" + title + "';";
                        ResultSet resultSet;
                        try {
                            Statement statement = _connection.createStatement();
                            resultSet = statement.executeQuery(sql);
                            if (resultSet.next()) {
                                sql = "update musicrange.userevaluation set userevaluation = '" + evaluation +
                                        "' where idUser = '" + usersID + "' and artist = '" + artistName +
                                        "' and title = '" + title + "';";
                            }
                            else
                                sql = "INSERT Into musicrange.userEvaluation(idUser, artist, title, genreID, userEvaluation) VALUES " +
                                        "('" + usersID + "', '" + artistName + "', '" + title + "', '" + genreID + "', '" + evaluation + "')";
                            _statement.execute(sql);
                        } catch (Exception e) {
                            continue;
                        }

                    } catch (Exception e) {
                        throw new CannotParseRequestException();
                    }
                }
            }
        }
        else
            throw new UnauthorizedUserException();
    }

    @ResponseStatus(HttpStatus.FOUND)
    class UserAlreadyExistException extends RuntimeException {

        public UserAlreadyExistException(String login) {
            super("User '" + login + "' is already exists.");
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    class ServerErrorOccurredException extends RuntimeException {

        public ServerErrorOccurredException() {
            super("Server error occurred!");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    class CannotParseRequestException extends RuntimeException {

        public CannotParseRequestException() {
            super("Data format is not correct!");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    class UnauthorizedUserException extends RuntimeException {

        public UnauthorizedUserException() {
            super("Authorisation field!");
        }
    }
}