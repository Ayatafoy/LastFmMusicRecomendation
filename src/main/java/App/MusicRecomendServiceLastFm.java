package App;

import App.Interfaces.IMusicRecomendService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class MusicRecomendServiceLastFm implements IMusicRecomendService {

    public ArrayList<String> resultList = new ArrayList<>();
    public HashMap<String, HashMap<String, Integer>> usersTracksPlays = new HashMap<>();
    private Connection _connection;

    public MusicRecomendServiceLastFm(Connection connection) throws SQLException, ParseException {
        _connection = connection;
        Statement statement = _connection.createStatement();
        ResultSet usersTracksResultSet = statement.executeQuery("SELECT * from userstracks");
        while (usersTracksResultSet.next()) {
            String userLogin = usersTracksResultSet.getString(1),
                    trackFullName = usersTracksResultSet.getString(3);
            int plays = usersTracksResultSet.getInt(4);
            if (usersTracksPlays.get(userLogin) == null) {
                usersTracksPlays.put(userLogin, new HashMap<>());
            }
            usersTracksPlays.get(userLogin).put(trackFullName, plays);
        }
    }

    @Override
    public ArrayList<String> GetAudio(String usersLogin, JSONArray mp3list) throws ParseException, SQLException {
        SetNewUserTracks(usersLogin, mp3list);
        return searchAction(usersLogin);
    }

    private void SetNewUserTracks(String userLogin, JSONArray mp3list) throws SQLException {
        String sql;
        Statement statement = _connection.createStatement();
        if (mp3list != null) {
            HashMap userLegacyTrackList = usersTracksPlays.get(userLogin);
            if (userLegacyTrackList == null) {
                usersTracksPlays.put(userLogin, new HashMap<>());
            }
            for (int i = 0; i < mp3list.size(); i++) {
                JSONObject mp3 = (JSONObject) mp3list.get(i);
                String trackName = ((String) mp3.get("trackName")).toLowerCase();
                int playCount = (int) mp3.get("playCount");
                if (usersTracksPlays.get(userLogin).get(trackName) != playCount) {
                    usersTracksPlays.get(userLogin).put(trackName, playCount);
                    sql = "INSERT Into lastfm.userstracks(UserName, TrackFullName, PlaysCount) VALUES " +
                            "('" + userLogin + "', '" + trackName + "', '" + playCount + "')";
                    try {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        break;
                    }
                }
            }
        }
    }

    private ArrayList<String> searchAction(String usersLogin) throws SQLException, ParseException {
        HashMap<String, Integer> friendsRange = range(usersTracksPlays, usersLogin);
        ArrayList<String> sortTrackList = Sort(friendsRange);
        if (sortTrackList != null) {
            recTrack(sortTrackList, usersLogin);
        }
        ArrayList<String> outResult = new ArrayList<>();

        int i = 0;
        while (outResult.size() != 100) {
            String track = resultList.get(i);
            if (!outResult.contains(track) && usersTracksPlays.get(usersLogin).get(track) == null)
                outResult.add(track);
            i++;
        }
        return outResult;
    }

    //пересекаем списки треков и считаем оценку результата
    private HashMap<String, Integer> range(HashMap<String, HashMap<String, Integer>> userTracksPlays, String userLogin) {
        HashMap<String, Integer> friendsRange = new HashMap<>();
        for (String friendName : userTracksPlays.keySet()) {
            friendsRange.put(friendName, 0);
            if (!userLogin.equals(friendName)) {
                for (String trackName : userTracksPlays.get(userLogin).keySet()) {
                    if (userTracksPlays.get(friendName).get(trackName) != null) {
                        Integer userTrackEvaluation = userTracksPlays.get(userLogin).get(trackName),
                                friendTrackEvaluation = userTracksPlays.get(friendName).get(trackName);
                        Integer diff = Math.abs(userTrackEvaluation - friendTrackEvaluation),
                                score = friendTrackEvaluation + friendTrackEvaluation;
                        friendsRange.put(friendName, friendsRange.get(friendName) + score - diff);
                    }
                }
            }
        }
        return friendsRange;
    }

    private <T> ArrayList<String> Sort(HashMap<T, Integer> friends) {
        ArrayList<MyPair> tempFriendsList = new ArrayList<>();
        ArrayList<String> sortedFriendsList = new ArrayList<>();
        MyPair pair;
        for (T friend : friends.keySet()) {
            pair = new MyPair(friend, friends.get(friend));
            tempFriendsList.add(pair);
        }
        int k = tempFriendsList.size();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < k - i - 1; j++) {
                if ((int) tempFriendsList.get(k - 1 - j).right > (int) tempFriendsList.get(k - 2 - j).right) {
                    pair = tempFriendsList.get(k - 1 - j);
                    tempFriendsList.set(k - 1 - j, tempFriendsList.get(k - 2 - j));
                    tempFriendsList.set(k - 2 - j, pair);
                }
            }
            sortedFriendsList.add(i, tempFriendsList.get(i).getLeft().toString());
        }
        return sortedFriendsList;
    }

    //ранжируем списки аудиозаписей пользователей с учетом моих предпочтений, выводим список рекомендаций аудиозаписей
    private ArrayList<String> recTrack(ArrayList<String> sortedFriendsList, String userLogin) {
        ArrayList<ArrayList> listOfTopUsersTracks = new ArrayList<>();
        int i = 0;
        for (String friendName : sortedFriendsList) {
            listOfTopUsersTracks.add(new ArrayList());
            for (String trackFullName : usersTracksPlays.get(friendName).keySet()) {
                listOfTopUsersTracks.get(i).add(trackFullName);
            }
            i++;
        }
        //list(0).sublist(0), list(0).sublist(1), list(1).sublist(0), list(0).sublist(2), list(1).sublist(1), list(2).sublist(0)
        while (i < listOfTopUsersTracks.size()) {
            int j = 0;
            while (j <= i) {
                if (i - j < listOfTopUsersTracks.get(j).size() && j < listOfTopUsersTracks.size()) {
                    resultList.add(listOfTopUsersTracks.get(j).get(i - j).toString());
                }
                j++;
            }
            i++;
        }
        return resultList;
    }
}