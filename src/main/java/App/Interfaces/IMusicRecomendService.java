package App.Interfaces;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Алексей on 21.10.2017.
 */
public interface IMusicRecomendService {
    ArrayList<String> GetAudio(String userId, JSONArray mp3list) throws SQLException, ParseException;
}
