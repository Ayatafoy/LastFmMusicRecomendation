package App;

import App.Interfaces.IMusicRecomendService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import java.sql.*;
import java.util.*;

class MusicRecomendService implements IMusicRecomendService {

    public HashMap<String, HashMap<String, ArrayList<MyPair>>> friends = new HashMap<>();
    public HashMap<String, HashMap<String, Double>> friendsTrackListRange = new HashMap<>();
    public HashMap<String, HashMap<String, Double>> friendsArtistCount = new HashMap<>();
    public HashMap<String, HashMap<Integer, Double>> friendsGenderCount = new HashMap<>();
    public HashMap<String, ArrayList<MyPair>> friendsListsArr = new HashMap<>();
    public ArrayList<MyPair<MyPair>> result = new ArrayList<>();
    private Connection _connection;

    public MusicRecomendService(Connection connection) throws SQLException, ParseException{
        _connection = connection;
        Statement statement = _connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * from usersdata");
        while (resultSet.next()) {
            String friendName = resultSet.getString(2),
                    artistName = resultSet.getString(3),
                    title = resultSet.getString(4);
            int genreID = resultSet.getInt(5);
            if (friends.get(friendName) == null)  {
                friendsArtistCount.put(friendName, new HashMap<>());
                friendsGenderCount.put(friendName, new HashMap<>());
                friendsListsArr.put(friendName, new ArrayList<>());
                friends.put(friendName, new HashMap<>());
            }
            if (friendsArtistCount.get(friendName).get(artistName) == null) {
                friendsArtistCount.get(friendName).put(artistName, 0.0);
                friends.get(friendName).put(artistName, new ArrayList<>());
            }
            if (friendsGenderCount.get(friendName).get(genreID) == null){
                friendsGenderCount.get(friendName).put(genreID, 0.0);
            }
            friendsListsArr.get(friendName).add(new MyPair(artistName, title));
            friends.get(friendName).get(artistName).add(new MyPair(title, genreID));
        }
    }

    @Override
    public ArrayList<String> GetAudio(String userId, JSONArray mp3list) throws ParseException, SQLException{
        SetNewUserTracks(userId, mp3list);
        return searchAction(userId);
    }

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

    private void SetNewUserTracks(String userId, JSONArray mp3list) throws SQLException {
        String sql;
        Statement statement = _connection.createStatement();
        if (mp3list != null) {
            ArrayList<MyPair> userLegacyTrackList = friendsListsArr.get(userId);
            if (userLegacyTrackList == null) {
                friendsListsArr.put(userId, new ArrayList<>());
                friendsArtistCount.put(userId, new HashMap<>());
                friendsGenderCount.put(userId, new HashMap<>());
                friends.put(userId, new HashMap<>());
            }
            for (int i = 0; i < mp3list.size(); i++) {
                JSONObject mp3 = (JSONObject) mp3list.get(i);
                String artistName = ((String) mp3.get("artist")).toLowerCase();
                String title = ((String) mp3.get("title")).toLowerCase();
                int genreID = 18;
                if (mp3.get("genre") != null)
                    genreID = Integer.parseInt(mp3.get("genre").toString());
                if (friendsArtistCount.get(userId).get(artistName) == null) {
                    friendsArtistCount.get(userId).put(artistName, 0.0);
                    friends.get(userId).put(artistName, new ArrayList<>());
                }
                if (friendsGenderCount.get(userId).get(genreID) == null) {
                    friendsGenderCount.get(userId).put(genreID, 0.0);
                }
                boolean flag = false;
                for (MyPair track : userLegacyTrackList) {
                    if (artistName.equals(track.getLeft()) && title.equals(track.getRight())) {
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    friendsListsArr.get(userId).add(new MyPair(artistName, title));
                    friends.get(userId).get(artistName).add(new MyPair(title, genreID));
                    sql = "INSERT Into musicrange.usersdata(idUsersData, artist, title, genreID) VALUES " +
                            "('" + userId + "', '" + artistName + "', '" + title + "', '" + genreID + "')";
                    try {
                        statement.execute(sql);
                    } catch (SQLException e) {
                        break;
                    }
                }
            }
        }
    }

    private ArrayList<String> searchAction(String userId) throws SQLException, ParseException {
        HashMap<Integer, Double> map1, myMap1 = null;
        HashMap<String, Double> map2, myMap2 = null;
        HashMap<String, ArrayList<MyPair>> normTrackList;
        //на каждой итерации получаем более точные предпочтения пользователей относительно музыкального жанра и исполнителей с учетом всех метрик оценки
        for (int i = 0; i < 10; i++) {
            int count = 0;
            for (String friendName : friends.keySet()) {
                count++;
                HashMap<String, Double> rangeList = new HashMap<>();
                normTrackList = normGender(friends.get(friendName));
                if (i == 0) {
                    map1 = countGender(normTrackList);
                    if (map1.get(18) != null)
                        friendsGenderCount.get(friendName).put(18, map1.get(18));
                    map2 = countArtist(normTrackList);
                    //добавляем условие того что список твой и редактируем map1 и map2 так как знаем кол-во треков исполнителя и треков в определенном жанре
                    //и можем этот трек кинуть в параметре в count, а там мы либо вычтем эту оценку либо прибавим, либо ничего не сделаем
                } else {
                    map1 = new HashMap<>();
                    map2 = new HashMap<>();
                    for (int genreID : friendsGenderCount.get(friendName).keySet()) {
                        map1.put(genreID, friendsGenderCount.get(friendName).get(genreID));
                    }

                    for (String artistName : friendsArtistCount.get(friendName).keySet()) {
                        map2.put(artistName, friendsArtistCount.get(friendName).get(artistName));
                    }
                }

                if (friendName.equals(userId)) {
                    myMap1 = new HashMap<>();
                    myMap2 = new HashMap<>();
                    for (int genreID : map1.keySet()) {
                        myMap1.put(genreID, map1.get(genreID));
                    }
                    for (String artistName : map2.keySet()) {
                        myMap2.put(artistName, map2.get(artistName));
                    }
                }
                for (int j = 0; j < friendsListsArr.get(friendName).size(); j++) {
                    MyPair mp3 = friendsListsArr.get(friendName).get(j);
                    String artistName = (String) mp3.getLeft();
                    int genreID = (int) normTrackList.get(artistName).get(0).getRight();
                    String title = (String) mp3.getRight();
                    double score = map1.get(genreID) + map2.get(artistName) + (1.0 - ((double) j / (double) friendsListsArr.get(friendName).size()));
                    rangeList.put(artistName + " " + title, score);
                    if (j == 0) {
                        for (int genreID1 : friendsGenderCount.get(friendName).keySet()) {
                            if (genreID1 != 18)
                                friendsGenderCount.get(friendName).put(genreID1, 0.0);
                        }
                        for (String artistName1 : friendsArtistCount.get(friendName).keySet()) {
                            friendsArtistCount.get(friendName).put(artistName1, 0.0);
                        }
                    }
                    friendsArtistCount.get(friendName).put(artistName, friendsArtistCount.get(friendName).get(artistName) + score);
                    if (genreID != 18)
                        friendsGenderCount.get(friendName).put(genreID, friendsGenderCount.get(friendName).get(genreID) + score);
                }
                if (count == friends.size() - 1 && i == 9) {
                    Statement statement = _connection.createStatement();
                    ResultSet resultSet = statement.executeQuery("SELECT * from userevaluation");
                    while (resultSet.next()) {
                        String currentUserId = resultSet.getString(2);
                        String artistName = resultSet.getString(3);
                        String title = resultSet.getString(4);
                        int genreID = Integer.parseInt(resultSet.getString(5));
                        double score = Double.parseDouble( String.valueOf((resultSet.getInt(6) - 5.0) / 1.666666666666));
                        if (friends.get(currentUserId) == null)  {
                            friendsArtistCount.put(currentUserId, new HashMap<>());
                            friendsGenderCount.put(currentUserId, new HashMap<>());
                            friendsListsArr.put(currentUserId, new ArrayList<>());
                            friends.put(currentUserId, new HashMap<>());
                        }
                        if (friendsArtistCount.get(currentUserId).get(artistName) == null) {
                            friendsArtistCount.get(currentUserId).put(artistName, 0.0);
                            friends.get(currentUserId).put(artistName, new ArrayList<>());
                            friendsListsArr.get(currentUserId).add(new MyPair(artistName, title));
                            friends.get(currentUserId).get(artistName).add(new MyPair(title, genreID));
                        }
                        if (friendsGenderCount.get(currentUserId).get(genreID) == null){
                            friendsGenderCount.get(currentUserId).put(genreID, 0.0);
                        }
                        friendsArtistCount.get(currentUserId).put(artistName, friendsArtistCount.get(currentUserId).get(artistName) + score);
                        if (genreID != 18)
                            friendsGenderCount.get(currentUserId).put(genreID, friendsGenderCount.get(currentUserId).get(genreID) + score);
                    }
                }
                friendsTrackListRange.put(friendName, rangeList);
                friendsGenderCount.put(friendName, normOrderMagnitude(friendsGenderCount.get(friendName)));
                friendsArtistCount.put(friendName, normOrderMagnitude(friendsArtistCount.get(friendName)));
            }
        }
        HashMap<String, Double> friendsRange = range(friendsTrackListRange, String.valueOf(userId));//пересекаем списки треков и считаем оценку результата
        HashMap<String, Double> friendsRange1 = range(friendsArtistCount, String.valueOf(userId));//пересекаем списки исполнителей и считаем оценку результата
        friendsRange = normOrderMagnitude(friendsRange);
        friendsRange1 = normOrderMagnitude(friendsRange1);
        for (String friendName : friendsRange.keySet()) {
            friendsRange.put(friendName, friendsRange.get(friendName) + friendsRange1.get(friendName));
        }
        ArrayList<MyPair> sortTrackList = Sort(friendsRange);

        if (sortTrackList != null && myMap1 != null && myMap2 != null) {
            result = recTrack(sortTrackList, myMap1, myMap2, userId);
        }
        ArrayList<String> outResult = new ArrayList<>();

        int i = 0;
        while (outResult.size() != 100){
            String track = result.get(i).getRight().getLeft().toString() + " " + String.valueOf(result.get(i).getLeft());
            if (!outResult.contains(track) && friendsTrackListRange.get(userId).get(track) == null)
                outResult.add(track);
            i++;
        }
        return outResult;
    }

    //преобразовываем последовательность к виду: max = 1, остальные по убыванию > 1, чтобы мы работали с единым порядком числа для всех
    private <T> HashMap<T, Double> normOrderMagnitude(HashMap<T, Double> map) {
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

    //аудиозаписи одного исполнителя приводим к одному жанру, чтобы не было разноброда
    private HashMap<String, ArrayList<MyPair>> normGender(HashMap<String, ArrayList<MyPair>> audios) {
        int x;
        for (String trackArtist : audios.keySet()) {
            int max = 0, k = 0;
            ArrayList<MyPair> list = audios.get(trackArtist);
            HashMap<Integer, Double> map = new HashMap<Integer, Double>();
            for (int i = 0; i < list.size(); i++) {
                if (map.get(list.get(i).right) == null)
                    map.put((int) list.get(i).right, (double) 1);
                else {
                    x = map.get(list.get(i).right).intValue() + 1;
                    if ((int) list.get(i).right != 18)
                        map.put((int) list.get(i).right, (double) x);
                    if (max < x) {
                        max = x;
                        k = (int) list.get(i).right;
                    }
                }
            }
            if (k != 0)
                for (int i = 0; i < list.size(); i++) {
                    audios.get(trackArtist).set(i, new MyPair(list.get(i).left, k));
                }
        }
        return audios;
    }

    //функция определяет предпочтения позьзвателя относительно музыкального жанра
    private HashMap<Integer, Double> countGender(HashMap<String, ArrayList<MyPair>> audios) {
        HashMap<Integer, Double> map = new HashMap<>();
        for (String trackArtist : audios.keySet()) {
            ArrayList<MyPair> list = audios.get(trackArtist);
            for (int i = 0; i < list.size(); i++) {
                if (map.get(list.get(i).right) == null)
                    map.put((int) list.get(i).right, (double) 1);
                else {
                    if ((int) list.get(i).right != 18)
                        map.put((int) list.get(i).right, (double) map.get(list.get(i).right).intValue() + 1);
                }
            }
        }
        return normOrderMagnitude(map);
    }

    //функция определяет предпочтения позьзвателя относительно исполнителей
    private HashMap<String, Double> countArtist(HashMap<String, ArrayList<MyPair>> audios) {
        HashMap<String, Double> map = new HashMap<String, Double>();
        for (String trackArtist : audios.keySet()) {
            map.put(trackArtist, (double) audios.get(trackArtist).size());
        }
        return normOrderMagnitude(map);
    }

    private <T> ArrayList<MyPair> Sort(HashMap<T, Double> audios) {
        ArrayList<MyPair> SortedTrackList = new ArrayList<MyPair>();
        MyPair pair;
        for (T trackArtist : audios.keySet()) {
            pair = new MyPair(trackArtist, audios.get(trackArtist));
            SortedTrackList.add(pair);
        }
        int k = SortedTrackList.size();
        for (int i = 0; i < k - 1; i++) {
            for (int j = 0; j < k - i - 1; j++) {
                if ((double) SortedTrackList.get(k - 1 - j).right > (double) SortedTrackList.get(k - 2 - j).right) {
                    pair = SortedTrackList.get(k - 1 - j);
                    SortedTrackList.set(k - 1 - j, SortedTrackList.get(k - 2 - j));
                    SortedTrackList.set(k - 2 - j, pair);
                }
            }
        }
        return SortedTrackList;
    }

    //ранжируем пользователей по похожести списков аудиозаписей
    private HashMap<String, Double> range(HashMap<String, HashMap<String, Double>> map, String myName) {
        HashMap<String, Double> friendsRange = new HashMap<>();
        HashMap<String, ArrayList<MyPair>> result = new HashMap<>();//используется только в закомменченной вариации алгоритма
        double mySum = 0, friendSum = 0;
        for (String trackName : map.get(myName).keySet()) {
            mySum += map.get(myName).get(trackName);
        }
        for (String friendName : map.keySet()) {
            result.put(friendName, new ArrayList<>());
            for (String trackName : map.get(friendName).keySet()) {
                friendSum += map.get(friendName).get(trackName);
            }
            mySum = 1;//параметр игнора объема коллекции аудиозаписей
            friendSum = 1;//параметр игнора объема коллекции аудиозаписей
            friendsRange.put(friendName, 0.0);
            if (!myName.equals(friendName)) {
                for (String trackName : map.get(myName).keySet()) {
                    if (map.get(friendName).get(trackName) != null) {
                        double diff = Math.abs(map.get(friendName).get(trackName) / friendSum - map.get(myName).get(trackName) / mySum),
                                score = map.get(friendName).get(trackName) / friendSum + map.get(myName).get(trackName) / mySum;
                        result.get(friendName).add(new MyPair(trackName, friendsRange.get(friendName) + score - diff));
                        friendsRange.put(friendName, friendsRange.get(friendName) + score - diff);
                    }
                }
            }
        }
        return friendsRange;
    }

    //ранжируем списки аудиозаписей пользователей с учетом моих предпочтений, выводим список рекомендаций аудиозаписей
    private ArrayList<MyPair<MyPair>> recTrack(ArrayList<MyPair> sortTrackList, HashMap<Integer, Double> myMap1, HashMap<String, Double> myMap2, String userId) {
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