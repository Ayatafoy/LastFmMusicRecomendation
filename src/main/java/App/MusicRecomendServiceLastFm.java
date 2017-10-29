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
    public HashMap<String, HashMap<String, Integer>> usersArtistPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> usersAlbumPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> usersTagPlays = new HashMap<>();
    public HashMap<String, HashMap<String, TrackInfo>> usersTracksPlays = new HashMap<>();
    private Connection _connection;

    public MusicRecomendServiceLastFm(Connection connection) throws SQLException, ParseException {
        _connection = connection;
        Statement statement = _connection.createStatement();
        ResultSet usersTracksResultSet = statement.executeQuery("SELECT * from userstracks");
        while (usersTracksResultSet.next()) {
            String userLogin = usersTracksResultSet.getString(2);
            TrackInfo track = new TrackInfo();
            track.FullName = usersTracksResultSet.getString(3);
            track.ArtistName = usersTracksResultSet.getString(4);
            track.AlbumFullName = usersTracksResultSet.getString(5);
            track.TagList = usersTracksResultSet.getString(6);
            track.PlaysCount = usersTracksResultSet.getInt(7);
            if (usersTracksPlays.get(userLogin) == null) {
                usersTracksPlays.put(userLogin, new HashMap<>());
            }
            usersTracksPlays.get(userLogin).put(track.FullName, track);
        }
        setTrackAtributData(statement, "usersartists",  usersArtistPlays);
        setTrackAtributData(statement, "usersalbums", usersAlbumPlays);
        setTrackAtributData(statement, "userstags", usersTagPlays);
    }

    private void setTrackAtributData(Statement statement, String tableName, HashMap<String, HashMap<String, Integer>> trackAtributhashTable) throws SQLException {
        ResultSet trackAtributResultSet = statement.executeQuery("SELECT * from " + tableName + "");
        while (trackAtributResultSet.next()) {
            String userLogin = trackAtributResultSet.getString(2),
                    atributName = trackAtributResultSet.getString(3);
            int plays = trackAtributResultSet.getInt(4);
            if (trackAtributhashTable.get(userLogin) == null) {
                trackAtributhashTable.put(userLogin, new HashMap<>());
            }
            trackAtributhashTable.get(userLogin).put(atributName, plays);
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
                TrackInfo track = new TrackInfo();
                track.FullName = ((String) mp3.get("trackName")).toLowerCase();
                track.ArtistName = ((String) mp3.get("artistName")).toLowerCase();
                track.AlbumFullName = ((String) mp3.get("albumName")).toLowerCase();
                track.TagList = ((String) mp3.get("tagName")).toLowerCase();
                track.PlaysCount = Math.toIntExact((long) mp3.get("playCount"));
                if (usersTracksPlays.get(userLogin).get(track.FullName) == null || usersTracksPlays.get(userLogin)
                        .get(track.FullName).PlaysCount != track.PlaysCount) {
                    usersTracksPlays.get(userLogin).put(track.FullName, track);
                    sql = "INSERT Into lastfmdb" +
                            ".userstracks(UserName, TrackFullName, ArtistName, AlbumFullName, TagNames, PlaysCount)" +
                            " VALUES  ('"
                            + userLogin + "', '"
                            + track.FullName + "', '"
                            + track.ArtistName + "', '"
                            + track.AlbumFullName + "', '"
                            + track.TagList + "', '"
                            + track.PlaysCount + "')";
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
        HashMap<String, Integer> friendsRange = new HashMap<>();//range(usersTracksPlays, usersLogin);
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
                                score = userTrackEvaluation + friendTrackEvaluation;
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
        i = 0;
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