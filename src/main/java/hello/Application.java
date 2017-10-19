package hello;

import org.json.simple.parser.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;

@SpringBootApplication
public class Application {

    public static void main(String[] args) throws URISyntaxException, SQLException, ParseException, IOException {
       SpringApplication.run(Application.class, args);
    }
}
