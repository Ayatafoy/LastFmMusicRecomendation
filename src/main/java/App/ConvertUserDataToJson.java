package App;

import java.sql.*;
import java.util.Properties;

/**
 * Created by Алексей on 11.11.2017.
 */
public class ConvertUserDataToJson {
    public static void main(String[] args) throws SQLException {
        String result = "{\"mp3list\" : [";
        Driver driver = new com.mysql.jdbc.Driver();
        DriverManager.registerDriver(driver);
        Properties p = new Properties();
        p.setProperty("user", "root");
        p.setProperty("password", "root");
        p.setProperty("useUnicode", "true");
        p.setProperty("characterEncoding", "UTF-8");
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/lastfmdb", p);
        Statement statement = connection.createStatement();
        ResultSet usersTracksResultSet = statement.executeQuery("SELECT * FROM lastfmdb.userstracks where Login = 'Ishkur88'");
        while (usersTracksResultSet.next()) {
            result += "{";
            result += "\"TrackFullName\":\"" + usersTracksResultSet.getString(3) + "\", ";
            result += "\"ArtistName\":\"" + usersTracksResultSet.getString(4) + "\", ";
            result += "\"AlbumFullName\":\"\", ";
            result += "\"TagNames\":\"\", ";
            result += "\"PlaysCount\":" + usersTracksResultSet.getString(7);
            result += "}, ";
        }
        result = result.substring(0, result.length() - 2);
        result += "]}";
        System.out.println(result);
    }
}
