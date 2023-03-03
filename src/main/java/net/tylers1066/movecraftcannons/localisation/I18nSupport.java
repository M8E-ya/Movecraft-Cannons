package net.tylers1066.movecraftcannons.localisation;

import net.tylers1066.movecraftcannons.MovecraftCannons;
import net.tylers1066.movecraftcannons.config.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

public class I18nSupport {
    private static Properties langFile;

    public static void init() {
        langFile = new Properties();

        File langDirectory = new File(MovecraftCannons.getInstance().getDataFolder().getAbsolutePath() + "/localisation");
        if (!langDirectory.exists()) {
            langDirectory.mkdirs();
        }


        try (InputStream stream = new FileInputStream(langDirectory.getAbsolutePath() + "/mc-cannonslang_" + Config.Locale + ".properties")) {
            langFile.load(stream);
        } catch (IOException ex) {
            ex.printStackTrace();
            MovecraftCannons.getInstance().getLogger().log(Level.SEVERE, "Critical Error in localisation system!");
            MovecraftCannons.getInstance().getServer().shutdown();
        }
    }

    public static String getInternationalisedString(String key) {
        String ret = langFile.getProperty(key);
        if (ret != null) {
            return ret;
        }
        else {
            return key;
        }
    }
}
