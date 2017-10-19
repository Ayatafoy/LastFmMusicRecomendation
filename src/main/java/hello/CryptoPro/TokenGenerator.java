package hello.CryptoPro;

/**
 * Created by Алексей on 28.04.2017.
 */
import org.apache.commons.codec.digest.DigestUtils;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class TokenGenerator {

    protected static SecureRandom random = new SecureRandom();

    public static synchronized String generateToken( String username ) {
        long longToken = Math.abs( random.nextLong() );
        String random = Long.toString( longToken, 16 );
        return DigestUtils.sha256Hex((new Date() + ":"  + username + ":" + random ));
    }
}