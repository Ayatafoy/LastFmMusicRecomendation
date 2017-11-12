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

    public HashMap<String, HashMap<String, Double>> usersTracksPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Double>> usersArtistPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Double>> usersAlbumPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Double>> usersTagPlays = new HashMap<>();
    public HashMap<String, HashMap<String, TrackInfo>> usersTracksInfoPlays = new HashMap<>();
    private Connection _connection;

    public MusicRecomendServiceLastFm(Connection connection) throws SQLException, ParseException {
        _connection = connection;
        Statement statement = _connection.createStatement();
        ResultSet usersTracksResultSet = statement.executeQuery("SELECT * from userstracks where iduserstracks < 100096");
        while (usersTracksResultSet.next()) {
            String userLogin = usersTracksResultSet.getString(2);
            TrackInfo track = new TrackInfo();
            track.FullName = usersTracksResultSet.getString(3);
            track.ArtistName = usersTracksResultSet.getString(4);
            track.AlbumFullName = usersTracksResultSet.getString(5);
            track.TagList = usersTracksResultSet.getString(6);
            track.PlaysCount = usersTracksResultSet.getInt(7);
            if (usersTracksPlays.get(userLogin) == null) {
                usersTracksInfoPlays.put(userLogin, new HashMap<>());
                usersTracksPlays.put(userLogin, new HashMap<>());
            }
            usersTracksInfoPlays.get(userLogin).put(track.FullName, track);
            usersTracksPlays.get(userLogin).put(track.FullName, track.PlaysCount);
        }
        setTrackAtributData(statement, "usersartists", usersArtistPlays, "idusersartists", 73462);
        setTrackAtributData(statement, "usersalbums", usersAlbumPlays, "idusersalbums", 73850);
        setTrackAtributData(statement, "userstags", usersTagPlays, "iduserstags", 7022);
    }

    private void setTrackAtributData(Statement statement, String tableName, HashMap<String, HashMap<String, Double>> trackAtributhashTable, String columnName, int maxCount) throws SQLException {
        ResultSet trackAtributResultSet = statement.executeQuery("SELECT * from " + tableName + " where " + columnName + " < " + String.valueOf(maxCount));
        while (trackAtributResultSet.next()) {
            String userLogin = trackAtributResultSet.getString(2),
                    atributValue = trackAtributResultSet.getString(3);
            double plays = trackAtributResultSet.getDouble(4);
            if (trackAtributhashTable.get(userLogin) == null) {
                trackAtributhashTable.put(userLogin, new HashMap<>());
            }
            trackAtributhashTable.get(userLogin).put(atributValue, plays);
        }
    }

    @Override
    public ArrayList<String> GetAudio(String usersLogin, JSONArray mp3list) throws ParseException, SQLException {
        SetNewUserTracks(usersLogin, mp3list);
        return searchAction(usersLogin);
    }

    private void SetNewUserTracks(String userLogin, JSONArray mp3list) throws SQLException {
        if (mp3list != null) {
            HashMap userLegacyTrackList = usersTracksPlays.get(userLogin);
            if (userLegacyTrackList == null) {
                setNewHashMapsToNewUser(userLogin);
                for (int i = 0; i < mp3list.size(); i++) {
                    JSONObject mp3 = (JSONObject) mp3list.get(i);
                    TrackInfo track = new TrackInfo();
                    parseJsonToTrackInfo(mp3, track);
                    updateUserTrackData(userLogin, track);
                    updateUserArtistData(userLogin, track);
                    updateUserAlbumData(userLogin, track);
                    for (String tagName : track.TagList.split("   ")) {
                        updateUserTagData(userLogin, tagName);
                    }
                }
                insertNewTrackInfoToDb(userLogin);
            }
        }
    }

    private void setNewHashMapsToNewUser(String userLogin) {
        usersTracksPlays.put(userLogin, new HashMap<>());
        usersArtistPlays.put(userLogin, new HashMap<>());
        usersAlbumPlays.put(userLogin, new HashMap<>());
        usersTagPlays.put(userLogin, new HashMap<>());
        usersTracksInfoPlays.put(userLogin, new HashMap<>());
    }

    private void updateUserTrackData(String userLogin, TrackInfo track) throws SQLException {
        if (usersTracksPlays.get(userLogin).get(track.FullName) == null) {
            usersTracksPlays.get(userLogin).put(track.FullName, track.PlaysCount);
            usersTracksInfoPlays.get(userLogin).put(track.FullName, track);
        }
    }

    private void updateUserArtistData(String userLogin, TrackInfo track) throws SQLException {
        if (usersArtistPlays.get(userLogin).get(track.ArtistName) == null) {
            usersArtistPlays.get(userLogin).put(track.ArtistName, track.PlaysCount);
        } else {
            double oldPlayCountValue = usersArtistPlays.get(userLogin).get(track.ArtistName);
            double newPlayCountValue = oldPlayCountValue + track.PlaysCount;
            usersArtistPlays.get(userLogin).put(track.ArtistName, newPlayCountValue);
        }
    }

    private void updateUserAlbumData(String userLogin, TrackInfo track) throws SQLException {
        if (!track.AlbumFullName.trim().equals(track.AlbumFullName.trim())) {
            if (usersAlbumPlays.get(userLogin).get(track.AlbumFullName) == null) {
                usersAlbumPlays.get(userLogin).put(track.AlbumFullName, track.PlaysCount);
            } else {
                double oldPlayCountValue = usersAlbumPlays.get(userLogin).get(track.AlbumFullName);
                double newPlayCountValue = oldPlayCountValue + track.PlaysCount;
                usersAlbumPlays.get(userLogin).put(track.AlbumFullName, newPlayCountValue);
            }
        }
    }

    private void updateUserTagData(String userLogin, String tagName) throws SQLException {
        if (tagName != null && !tagName.equals("")) {
            if (usersTagPlays.get(userLogin).get(tagName) == null) {
                usersTagPlays.get(userLogin).put(tagName, 1.0);
            } else {
                double oldPlayCountValue = usersTagPlays.get(userLogin).get(tagName);
                double newPlayCountValue = oldPlayCountValue + 1;
                usersTagPlays.get(userLogin).put(tagName, newPlayCountValue);
            }
        }
    }

    private void insertNewTrackInfoToDb(String userLogin) throws SQLException {
        Statement statement = _connection.createStatement();
        String sqlInsertToUsersTracks;
        String sqlInsertToUsersArtists;
        String sqlInsertToUsersAlbums;
        String sqlInsertToUsersTags;
        for (String trackFullName:usersTracksPlays.get(userLogin).keySet()) {
            TrackInfo track = usersTracksInfoPlays.get(userLogin).get(trackFullName);
            sqlInsertToUsersTracks = "INSERT Into lastfmdb" +
                    ".userstracks(Login, TrackFullName, ArtistName, AlbumFullName, TagNames, PlaysCount)" +
                    " VALUES  ('"
                    + userLogin + "', '"
                    + track.FullName + "', '"
                    + track.ArtistName + "', '"
                    + track.AlbumFullName + "', '"
                    + track.TagList + "', '"
                    + track.PlaysCount + "')";
            statement.execute(sqlInsertToUsersTracks);
        }
        for (String artistName:usersArtistPlays.get(userLogin).keySet()) {
            double playCount = usersArtistPlays.get(userLogin).get(artistName);
            sqlInsertToUsersArtists = "INSERT Into lastfmdb" +
                    ".usersartists(Login, ArtistName, PlaysCount)" +
                    " VALUES  ('"
                    + userLogin + "', '"
                    + artistName + "', '"
                    + playCount + "')";
            statement.execute(sqlInsertToUsersArtists);
        }
        for (String albumName:usersAlbumPlays.get(userLogin).keySet()) {
            double playCount = usersAlbumPlays.get(userLogin).get(albumName);
            sqlInsertToUsersAlbums = "INSERT Into lastfmdb" +
                    ".usersalbums(Login, AlbumFullName, PlaysCount)" +
                    " VALUES  ('"
                    + userLogin + "', '"
                    + albumName + "', '"
                    + playCount + "')";
            statement.execute(sqlInsertToUsersAlbums);
        }
        for (String tagName:usersTagPlays.get(userLogin).keySet()) {
            double playCount = usersTagPlays.get(userLogin).get(tagName);
            sqlInsertToUsersTags = "INSERT Into lastfmdb" +
                    ".userstags(Login, TagName, PlaysCount)" +
                    " VALUES  ('"
                    + userLogin + "', '"
                    + tagName + "', '"
                    + playCount + "')";
            statement.execute(sqlInsertToUsersTags);
        }
    }

    private void parseJsonToTrackInfo(JSONObject mp3, TrackInfo track) {
        track.FullName = ((String) mp3.get("TrackFullName")).toLowerCase();
        track.ArtistName = ((String) mp3.get("ArtistName")).toLowerCase();
        track.AlbumFullName = track.ArtistName + ((String) mp3.get("AlbumFullName")).toLowerCase();
        track.TagList = ((String) mp3.get("TagNames")).toLowerCase();
        track.PlaysCount = Math.toIntExact((long) mp3.get("PlaysCount"));
    }

    private ArrayList<String> searchAction(String usersLogin) throws SQLException, ParseException {
        ArrayList<String> resultListContainsControlUserTracks = new ArrayList<>();
        ArrayList<String> outResult = new ArrayList<>();
        HashMap<String, Double> friendsRangeByTracks = range(usersTracksPlays, usersLogin);
        HashMap<String, Double> friendsRangeByArtists = range(usersArtistPlays,usersLogin);
        HashMap<String, Double> friendsRangeByAlbums = range(usersAlbumPlays, usersLogin);
        HashMap<String, Double> friendsRangeByTags = range(usersTagPlays, usersLogin);
        ArrayList<String> sortFriendsListByTracks = firstHundredItems(friendsRangeByTracks);
        ArrayList<String> sortFriendsListByArtists = firstHundredItems(friendsRangeByArtists);
        ArrayList<String> sortFriendsListByAlbums = firstHundredItems(friendsRangeByAlbums);
        ArrayList<String> sortFriendsListByTags = firstHundredItems(friendsRangeByTags);
        ArrayList<String> mostSimilarUsersByAllAttributes = findMostSimilarUsers(sortFriendsListByTracks, sortFriendsListByArtists);
        if (mostSimilarUsersByAllAttributes != null) {
            resultListContainsControlUserTracks = recTrack(mostSimilarUsersByAllAttributes, usersLogin);
        }

        int i = 0, k = 0;
        while (i < Math.min(100, resultListContainsControlUserTracks.size())) {
            String track = resultListContainsControlUserTracks.get(i);
            if (!outResult.contains(track) && usersTracksPlays.get(usersLogin).get(track) == null)
                outResult.add(track);
            else
                k++;
            i++;
        }
        System.out.println(k);
        return outResult;
    }

    private ArrayList<String> findMostSimilarUsers(ArrayList<String> sortFriendsListByTracks, ArrayList<String> sortFriendsListByArtists) {
        ArrayList<String> friendsList = new ArrayList<>();
        for (String friendName:sortFriendsListByTracks) {
            if (sortFriendsListByArtists.contains(friendName)){
                friendsList.add(friendName);
            }
        }
        return friendsList;
    }

    //пересекаем списки треков и считаем оценку результата
    private HashMap<String, Double> range(HashMap<String, HashMap<String, Double>> trackAtributhashTable, String userLogin) {
        HashMap<String, Double> friendsRange = new HashMap<>();
        for (String friendName : trackAtributhashTable.keySet()) {
            friendsRange.put(friendName, 0.0);
            if (!userLogin.equals(friendName)) {
                for (String trackName : trackAtributhashTable.get(userLogin).keySet()) {
                    if (trackAtributhashTable.get(friendName).get(trackName) != null) {
                        double userTrackEvaluation = trackAtributhashTable.get(userLogin).get(trackName),
                                friendTrackEvaluation = trackAtributhashTable.get(friendName).get(trackName);
                        double diff = Math.abs(userTrackEvaluation - friendTrackEvaluation),
                                score = userTrackEvaluation + friendTrackEvaluation;
                        friendsRange.put(friendName, friendsRange.get(friendName) + score - diff);
                    }
                }
            }
        }
        return friendsRange;
    }

    //преобразовываем последовательность к виду: max = 1, остальные по убыванию > 1, чтобы мы работали с единым порядком числа для всех
    private static <T> HashMap<T, Double> normOrderMagnitude(HashMap<T, Double> map) {
        double x, max = 0;
        for (T cell : map.keySet()) {
            x = map.get(cell);
            if (max < x)
                max = x;
        }
        for (T cell : map.keySet()) {
            map.put(cell, map.get(cell) / max);
        }
        return map;
    }

    private <T> ArrayList<String> firstHundredItems(HashMap<T, Double> items) {
        ArrayList<MyPair> tempFriendsList = new ArrayList<>();
        ArrayList<String> sortedFriendsList = new ArrayList<>();
        MyPair pair;
        for (T item : items.keySet()) {
            pair = new MyPair(item, items.get(item));
            tempFriendsList.add(pair);
        }
        int k = tempFriendsList.size();
        for (int i = 0; i < Math.min(100, tempFriendsList.size()); i++) {
            for (int j = 0; j < k - i - 1; j++) {
                if ((double) tempFriendsList.get(k - 1 - j).right > (double) tempFriendsList.get(k - 2 - j).right) {
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
        HashMap<String, Double> controlUserArtistEvaluation = normOrderMagnitude(usersArtistPlays.get(userLogin));
        HashMap<String, Double> controlUserAlbumEvaluation = normOrderMagnitude(usersAlbumPlays.get(userLogin));
        HashMap<String, Double> controlUserTagEvaluation = normOrderMagnitude(usersTagPlays.get(userLogin));
        ArrayList<ArrayList> listOfTopUsersTracks = new ArrayList<>();
        ArrayList<String> resultList = new ArrayList<>();
        int i = 0;
        for (String friendName:sortedFriendsList) {
            HashMap<String, Double> controlUserFriendsTrackEvaluation = new HashMap<>();
            for (String trackFullName : usersTracksInfoPlays.get(friendName).keySet()) {
                TrackInfo friendTrackInfo = usersTracksInfoPlays.get(friendName).get(trackFullName);
                double controlUserArtistEvaluationOfTrack = 0.0;
                double controlUserAlbumEvaluationOfTrack = 0.0;
                double controlUserTagEvaluationOfTrack = 0.0;
                if (controlUserArtistEvaluation.get(friendTrackInfo.ArtistName) != null)
                    controlUserArtistEvaluationOfTrack = controlUserArtistEvaluation.get(friendTrackInfo.ArtistName);
                if (controlUserAlbumEvaluation.get(friendTrackInfo.AlbumFullName) != null)
                    controlUserAlbumEvaluationOfTrack = controlUserAlbumEvaluation.get(friendTrackInfo.AlbumFullName);
                for (String tagName:friendTrackInfo.TagList.split("   ")) {
                    if (controlUserTagEvaluation.get(tagName) != null)
                        controlUserTagEvaluationOfTrack += controlUserTagEvaluation.get(tagName);
                }
                double finalTrackEvaluation = controlUserArtistEvaluationOfTrack + controlUserAlbumEvaluationOfTrack + controlUserTagEvaluationOfTrack;
                controlUserFriendsTrackEvaluation.put(trackFullName, finalTrackEvaluation);
            }
            listOfTopUsersTracks.add(i, firstHundredItems(controlUserFriendsTrackEvaluation));
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