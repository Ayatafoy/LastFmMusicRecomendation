package App;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Driver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Properties;

import static sun.net.www.protocol.http.HttpURLConnection.userAgent;

/**
 * Created by Алексей on 12.10.2017.
 */
public class GetDataFromLastFm {
    public static void main(String[] args) throws IOException, SQLException {
        Caller.getInstance().setUserAgent(userAgent);
        String key = "62ebdf830024a3ee8e82da8d1de1531c"; //этот ключ из примеров last.fm
        BufferedReader TSVFile = new BufferedReader(new FileReader("Two_Million_LastFM_User_Profiles.tsv"));
        String dataRow = TSVFile.readLine(); // Read first line.
        Connection connection = getConnection();
        Statement st = connection.createStatement();
        int count = 0;
        while (dataRow != null) {
            try {
                dataRow = TSVFile.readLine();
                String[] dataArray = dataRow.split("\t");
                String user = dataArray[1];
                System.out.println("User:   " + count);
                User userInfo = User.getInfo(user, key);
                String sqlInsertUser;
                String userInfoLogin = user.replaceAll("'", ""),
                        password = "4b216db4f13d1fcc41fe260a2fc6bfffcc50b7b313aba2c9f7f60ecb0e36ce98";
                if (userInfo != null) {
                    String country = (userInfo.getCountry() != null) ? userInfo.getCountry().replaceAll("'", "") : "",
                            gender = (userInfo.getGender() != null) ? userInfo.getGender().replaceAll("'", "") : "";
                    sqlInsertUser = "INSERT Into lastfmdb" +
                            ".users(Login, Country, Age, Gender, Password) VALUES " + "('"
                            + userInfoLogin + "', '" + country + "', '" + userInfo.getAge() + "', '" + gender + "', '" + password + "')";
                } else {
                    sqlInsertUser = "INSERT Into lastfmdb" +
                            ".users(Login, Country, Age, Gender, Password) VALUES " + "('"
                            + userInfoLogin + "', '" + "" + "', '" + 0 + "', '" + "" + "', '" + password + "')";
                }
                try {
                    st.execute(sqlInsertUser);
                } catch (SQLException e) {
                    System.out.println(sqlInsertUser);
                    System.out.println(e.getMessage());
                    continue;
                }
                Collection<Track> tracks = User.getTopTracks(user, 100, key);
                for (Track track : tracks) {
                    String tagNames = "";
                    for (String tagName : track.getTags()) {
                        tagNames += tagName + "   ";
                    }
                    String userLogin = user.replaceAll("'", ""),
                            artist = (track.getArtist() != null) ? track.getArtist().replaceAll("'", "") : "",
                            name = (track.getName() != null) ? track.getName().replaceAll("'", "") : "",
                            album = (track.getAlbum() != null) ? track.getAlbum().replaceAll("'", "") : "";
                    String sql = "INSERT Into lastfmdb" +
                            ".userstracks(Login, TrackFullName, ArtistName, AlbumFullName, TagNames, PlaysCount) " +
                            "VALUES " + "('" +
                            userLogin + "', '" +
                            artist + "   " + name + "', '" +
                            artist + "', '" +
                            artist + "   " + album + "', '" +
                            tagNames + "', '" +
                            track.playcount + "')";
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        System.out.println(sql);
                        System.out.println(e.getMessage());
                        continue;
                    }
                }
                Collection<Artist> artists = User.getTopArtists(user, key);
                for (Artist artist : artists) {
                    String userLogin = user.replaceAll("'", ""),
                            artistName = (artist.getName() != null) ? artist.getName().replaceAll("'", "") : "";
                    String sql = "INSERT Into lastfmdb.usersartists(Login, ArtistName, PlaysCount) VALUES " +
                            "('" + userLogin + "', '" + artistName + "', '" + artist.playcount + "')";
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        System.out.println(sql);
                        System.out.println(e.getMessage());
                        continue;
                    }
                }
                Collection<Album> albums = User.getTopAlbums(user, key);
                for (Album album : albums) {
                    String userLogin = user.replaceAll("'", ""),
                            artistName = (album.getArtist() != null) ? album.getArtist().replaceAll("'", "") : "",
                            albumName = (album.getName() != null) ? album.getName().replaceAll("'", "") : "";
                    String sql = "INSERT Into lastfmdb.usersalbums(Login, AlbumFullName, PlaysCount) VALUES " +
                            "('" + userLogin + "', '" + (artistName + "   " + albumName) + "', '" + album.playcount + "')";
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        System.out.println(sql);
                        System.out.println(e.getMessage());
                        continue;
                    }
                }
                Collection<Tag> tags = User.getTopTags(user, key);
                for (Tag tag : tags) {
                    String userLogin = user.replaceAll("'", ""),
                            tagName = (tag.getName() != null) ? tag.getName().replaceAll("'", "") : "";
                    String sql = "INSERT Into lastfmdb.userstags(Login, TagName, TrackCount) VALUES " +
                            "('" + userLogin + "', '" + tagName + "', '" + tag.getCount() + "')";
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        System.out.println(sql);
                        System.out.println(e.getMessage());
                        continue;
                    }
                }
                count++;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                continue;
            }
        }
        TSVFile.close();
    }

    private static Connection getConnection() throws SQLException {
        Connection connection;
        Driver driver = new com.mysql.jdbc.Driver();
        DriverManager.registerDriver(driver);
        Properties p = new Properties();
        p.setProperty("user", "root");
        p.setProperty("password", "root");
        p.setProperty("useUnicode", "true");
        p.setProperty("characterEncoding", "UTF-8");
        connection = (Connection) DriverManager
                .getConnection("jdbc:mysql://localhost:3306/lastfmdb", p);
        return connection;
    }
}
