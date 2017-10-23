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

    public HashMap<String, HashMap<String, ArrayList<MyPair>>> users = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> friendsTrackListRange = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> usersTracksPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> usersArtistsPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> usersTagsPlays = new HashMap<>();
    public HashMap<String, HashMap<String, Integer>> usersAlbumsCount = new HashMap<>();
    public HashMap<String, ArrayList<MyPair>> usersTrackLists = new HashMap<>();
    public ArrayList<MyPair<MyPair>> result = new ArrayList<>();
    private Connection _connection;

    public MusicRecomendServiceLastFm(Connection connection) throws SQLException, ParseException{
        _connection = connection;
        Statement statement = _connection.createStatement();
        ResultSet usersTracksResultSet = statement.executeQuery("SELECT * from userstracks");
        while (usersTracksResultSet.next()) {
            String userLogin = usersTracksResultSet.getString(1),
                    trackFullName = usersTracksResultSet.getString(3);
            int plays = usersTracksResultSet.getInt(4);
            if (usersTracksPlays.get(userLogin) == null)  {
                usersTracksPlays.put(userLogin, new HashMap<>());
            }
            usersTracksPlays.get(userLogin).put(trackFullName, plays);
        }
    }

    @Override
    public ArrayList<String> GetAudio(String usersLogin, JSONArray mp3list) throws ParseException, SQLException{
        SetNewUserTracks(usersLogin, mp3list);
        return searchAction(usersLogin);
    }

    @Override
    public void SetEvaluation(String userId, JSONArray mp3list) throws SQLException, ParseException {
        Statement statement = _connection.createStatement();
        String sql;
        if (mp3list != null) {
            for (int i = 0; i < mp3list.size(); i++) {
                JSONObject mp3 = (JSONObject) mp3list.get(i);
                String artistName = ((String) mp3.get("artist")).toLowerCase();
                String title = ((String) mp3.get("title")).toLowerCase();
                int genreID = 18;
                if (mp3.get("genre") != null)
                    genreID = Integer.parseInt(mp3.get("genre").toString());
                int evaluation = Integer.parseInt(mp3.get("evaluation").toString());
                sql = "select * from musicrange.userEvaluation where idUser = '" + userId + "' and artist = '" + artistName +
                        "' and title = '" + title + "';";
                ResultSet resultSet;
                try {
                    Statement newStatement = _connection.createStatement();
                    resultSet = newStatement.executeQuery(sql);
                    if (resultSet.next()) {
                        sql = "update musicrange.userevaluation set userevaluation = '" + evaluation +
                                "' where idUser = '" + userId + "' and artist = '" + artistName +
                                "' and title = '" + title + "';";
                    } else
                        sql = "INSERT Into musicrange.userEvaluation(idUser, artist, title, genreID, userEvaluation) VALUES " +
                                "('" + userId + "', '" + artistName + "', '" + title + "', '" + genreID + "', '" + evaluation + "')";
                    statement.execute(sql);
                } catch (Exception e) {
                    continue;
                }
            }
        }
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
        HashMap<String, Integer> friendsRange = range(usersTracksPlays, usersLogin);//пересекаем списки треков и считаем оценку результата
        ArrayList<MyPair> sortTrackList = Sort(friendsRange);
        if (sortTrackList != null) {
            result = recTrack(sortTrackList, usersLogin);
        }
        ArrayList<String> outResult = new ArrayList<>();

        int i = 0;
        while (outResult.size() != 100){
            String track = result.get(i).getRight().getLeft().toString() + " " + String.valueOf(result.get(i).getLeft());
            if (!outResult.contains(track) && friendsTrackListRange.get(userLogin).get(track) == null)
                outResult.add(track);
            i++;
        }
        return outResult;
    }

    //ранжируем пользователей по похожести списков аудиозаписей
    private HashMap<String, Integer> range(HashMap<String, HashMap<String, Integer>> map, String myName) {
        HashMap<String, Integer> friendsRange = new HashMap<>();
        for (String friendName : map.keySet()) {
            friendsRange.put(friendName, 0);
            if (!myName.equals(friendName)) {
                for (String trackName : map.get(myName).keySet()) {
                    if (map.get(friendName).get(trackName) != null) {
                        Integer diff = Math.abs(map.get(friendName).get(trackName) - map.get(myName).get(trackName)),
                                score = map.get(friendName).get(trackName) + map.get(myName).get(trackName);
                        friendsRange.put(friendName, friendsRange.get(friendName) + score - diff);
                    }
                }
            }
        }
        return friendsRange;
    }

    private <T> ArrayList<MyPair> Sort(HashMap<T, Integer> audios) {
        ArrayList<MyPair> SortedTrackList = new ArrayList<>();
        MyPair pair;
        for (T trackArtist : audios.keySet()) {
            pair = new MyPair(trackArtist, audios.get(trackArtist));
            SortedTrackList.add(pair);
        }
        int k = SortedTrackList.size();
        for (int i = 0; i < k - 1; i++) {
            for (int j = 0; j < k - i - 1; j++) {
                if ((int) SortedTrackList.get(k - 1 - j).right > (int) SortedTrackList.get(k - 2 - j).right) {
                    pair = SortedTrackList.get(k - 1 - j);
                    SortedTrackList.set(k - 1 - j, SortedTrackList.get(k - 2 - j));
                    SortedTrackList.set(k - 2 - j, pair);
                }
            }
        }
        return SortedTrackList;
    }

    //ранжируем списки аудиозаписей пользователей с учетом моих предпочтений, выводим список рекомендаций аудиозаписей
    private ArrayList<MyPair<MyPair>> recTrack(ArrayList<MyPair> sortTrackList, String userLogin) {
        HashMap<String, ArrayList<MyPair>> normTrackList;
        ArrayList<ArrayList<MyPair>> result = new ArrayList<>();
        for (int i = 0; i < sortTrackList.size(); i++) {
            HashMap<MyPair<MyPair>, Double> rangeList = new HashMap<>();
            normTrackList = normGender(friends.get(sortTrackList.get(i).getLeft()));
            for (int j = 0; j < friendsListsArr.get(sortTrackList.get(i).getLeft()).size(); j++) {
                MyPair mp3 = friendsListsArr.get(sortTrackList.get(i).getLeft()).get(j);
                String artistName = (String) mp3.getLeft();
                int genreID = (int) normTrackList.get(artistName).get(0).getRight();
                String title = (String) mp3.getRight();
                double score;
                if (myMap1.get(genreID) != null && myMap2.get(artistName) != null)
                    score = myMap1.get(genreID) + myMap2.get(artistName) + (1.0 - ((double) j / (double) friendsListsArr.get(sortTrackList.get(i).getLeft()).size()));
                else if (myMap1.get(genreID) == null && myMap2.get(artistName) == null) {
                    score = 1.0 - ((double) j / (double) friendsListsArr.get(sortTrackList.get(i).getLeft()).size());
                } else if (myMap1.get(genreID) == null) {
                    score = myMap2.get(artistName) + (1.0 - ((double) j / (double) friendsListsArr.get(sortTrackList.get(i).getLeft()).size()));
                } else
                    score = myMap1.get(genreID) + (1.0 - ((double) j / (double) friendsListsArr.get(sortTrackList.get(i).getLeft()).size()));
                rangeList.put(new MyPair(title, new MyPair(artistName, genreID)), score);
            }
            result.add(Sort(rangeList));
        }
        int i = 0;
        ArrayList<MyPair<MyPair>> resultList = new ArrayList<>();
        while (i < result.size()) {
            int j = 0;
            while (j <= i) {
                if (i - j < result.get(j).size() && j < result.size()) {
                    resultList.add((MyPair<MyPair>) result.get(j).get(i - j).getLeft());
                }
                j++;
            }
            i++;
        }
        return resultList;
    }


}