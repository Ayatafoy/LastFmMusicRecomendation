package App;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.Properties;

public class GetDataVk {
    private static void getAudio(String userID, Statement st,
                                 String ACCESS_TOKEN)
            throws URISyntaxException, IOException, ParseException, SQLException {
        String USER_ID = userID;
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https").setHost("api.vk.com")
                .setPath("/method/audio.get")
                .setParameter("oid", USER_ID)
                .setParameter("access_token", ACCESS_TOKEN);
        URI uri = builder.build();
        HttpGet httpget = new HttpGet(uri);
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = null;
            try {
                instream = entity.getContent();
                String responseAsString = IOUtils.toString(instream);
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser
                        .parse(responseAsString);
                JSONArray mp3list = (JSONArray) jsonResponse
                        .get("response");
                if (mp3list != null) {
                    for (int i = 0; i < mp3list.size(); i++) {
                        JSONObject mp3 = (JSONObject) mp3list.get(i);
                        String artistName = ((String) mp3.get("artist"))
                                .toLowerCase().replace("\'", "");
                        String title = ((String) mp3.get("title"))
                                .toLowerCase().replace("\'", "");
                        int genreID = 18;
                        if (mp3.get("genre") != null)
                            genreID = Integer.parseInt(mp3.get("genre")
                                    .toString());
                        String sql = "INSERT Into musicrange.usersdata(idUsersData, artist, title, genreID) VALUES " + "('" + userID + "', '" + artistName
                                + "', '" + title + "', '" + genreID + "')";
                        try {
                            st.execute(sql);
                        } catch (SQLException e) {
                            System.out.println(sql);
                            System.out.println(e.getMessage());
                            continue;
                        }
                    }
                }
            } finally {
                if (instream != null)
                    instream.close();
            }
        }
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
        connection = DriverManager
                .getConnection("jdbc:mysql://localhost:3306/musicrange", p);
        return connection;
    }

    private void getUsers(String city, String ageFrom, String ageTo,
                          String id, String ACCESS_TOKEN)
            throws URISyntaxException, IOException, ParseException, SQLException {
        URIBuilder builder = new URIBuilder();
        builder.setScheme("https").setHost("api.vk.com")
                .setPath("/method/users.search")
                .setParameter("hometown", city)
                .setParameter("count", "1000")
                .setParameter("sex", id)
                .setParameter("age_From", ageFrom)
                .setParameter("age_to", ageTo)
                .setParameter("access_token", ACCESS_TOKEN);
        URI uri = builder.build();
        HttpGet httpget = new HttpGet(uri);
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = null;
            try {
                instream = entity.getContent();
                String responseAsString = IOUtils.toString(instream);
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser
                        .parse(responseAsString);
                JSONArray userslist = (JSONArray) jsonResponse
                        .get("response");
                Connection connection = getConnection();
                Statement st = connection.createStatement();
                for (int i = 1; i < userslist.size(); i++) {
                    JSONObject user = (JSONObject) userslist.get(i);
                    try {
                        System.out.println(user.get("uid") + "\n");
                        getAudio(String.valueOf(user.get("uid")),
                                st, ACCESS_TOKEN);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
                connection.close();
            } finally {
                if (instream != null)
                    instream.close();
            }

        }
    }
}
