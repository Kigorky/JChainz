package it.unimi.di.laser.jchainz;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Util {
    private static Properties prop = null;
    final static Logger logger = Logger.getLogger(Util.class);

    private static void config() {
        try (InputStream input = new FileInputStream("./config.properties")) {

            prop = new Properties();

            // load a properties file
            prop.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProp(String p) {
        config();
        return prop.getProperty(p);
    }
}
