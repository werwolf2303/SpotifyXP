package com.spotifyxp;


import ch.randelshofer.quaqua.QuaquaLookAndFeel;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.spotifyxp.analytics.Analytics;
import com.spotifyxp.api.GitHubAPI;
import com.spotifyxp.args.ArgParser;
import com.spotifyxp.audio.Quality;
import com.spotifyxp.designs.Theme;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.logging.ConsoleLoggingModules;
import com.spotifyxp.panels.SplashPanel;
import com.spotifyxp.setup.Setup;
import com.spotifyxp.support.LinuxSupportModule;
import com.spotifyxp.updater.Updater;
import com.spotifyxp.updater.UpdaterDialog;
import com.spotifyxp.utils.DoubleArrayList;
import com.spotifyxp.api.SpotifyAPI;
import com.spotifyxp.background.BackgroundService;
import com.spotifyxp.configuration.Config;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.dialogs.LoginDialog;
import com.spotifyxp.listeners.KeyListener;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.Resources;
import com.spotifyxp.utils.StartupTime;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@SuppressWarnings("Convert2Lambda")
public class Initiator {
    static SpotifyAPI api = null;
    static StartupTime startupTime;
    static Thread hook = new Thread(new Runnable() {
        @Override
        public void run() {
            if(!new File(PublicValues.fileslocation + "/" + "LOCKED").delete()) {
                ConsoleLogging.error(PublicValues.language.translate("startup.error.lockdelete"));
            }
        }
    });
    public static void main(String[] args) {
        startupTime = new StartupTime();
        PublicValues.args = args;
        PublicValues.logger.setColored(false);
        PublicValues.logger.setShowTime(false);
        PublicValues.language = new libLanguage();
        PublicValues.language.setLanguageFolder("lang");
        PublicValues.language.setAutoFindLanguage();
        if(!System.getProperty("os.name").toLowerCase().contains("win")) {
            //Is not Windows
            new LinuxSupportModule();
            args = new String[] {"--setup-complete"};
        }
        if(!new File(PublicValues.fileslocation).exists()) {
            if(!new File(PublicValues.fileslocation).mkdir()) {
                ConsoleLogging.changeName("SpotifyAPI");
                ConsoleLogging.error(PublicValues.language.translate("error.configuration.failedcreate"));
            }
        }
        try {
            if(!new File(PublicValues.fileslocation + "/" + "LOCKED").createNewFile()) {
                ConsoleLogging.error(PublicValues.language.translate("startup.error.lockcreate"));
            }
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
        new ArgParser(args);
        if(new File("pom.xml").exists()) {
            PublicValues.debug = true;
            ConsoleLoggingModules modules = new ConsoleLoggingModules("Module");
            modules.setColored(true);
            modules.setShowTime(false);
            PublicValues.logger.setColored(true);
            PublicValues.foundSetupArgument = true;
        }
        //Try to unrestrict security
        try {
            Files.copy(new Resources().readToInputStream("security/local_policy.jar"), Paths.get(System.getProperty("java.home") + "\\jre\\lib\\security\\local_policy.jar"), REPLACE_EXISTING);
            Files.copy(new Resources().readToInputStream("security/US_export_policy.jar"), Paths.get(System.getProperty("java.home") + "\\jre\\lib\\security\\US_export_policy.jar"), REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.copy(new Resources().readToInputStream("security/local_policy.jar"), Paths.get(System.getProperty("java.home") + "\\lib\\security\\local_policy.jar"), REPLACE_EXISTING);
                Files.copy(new Resources().readToInputStream("security/US_export_policy.jar"), Paths.get(System.getProperty("java.home") + "\\lib\\security\\US_export_policy.jar"), REPLACE_EXISTING);
            } catch (IOException ignored) {
                //This will propably fail on Java versions above 1.8
            }
        }
        //---
        PublicValues.config = new Config();
        switch(PublicValues.config.get(ConfigValues.theme.name)) {
            case "QUAQUA":
                PublicValues.theme = Theme.QuaQua;
                break;
            case "MACOSDARK":
                PublicValues.theme = Theme.MacOSDark;
                break;
            case "MACOSLIGHT":
                PublicValues.theme = Theme.MacOSLight;
                break;
            case "LIGHT":
                PublicValues.theme = Theme.LIGHT;
                break;
            case "WINDOWS":
                PublicValues.theme = Theme.WINDOWS;
                break;
            case "LEGACY":
                PublicValues.theme = Theme.LEGACY;
                break;
            case "UGLY":
                PublicValues.theme = Theme.UGLY;
                break;
            default:
                PublicValues.theme = Theme.DARK;
                break;
        }
        boolean unsupported = false;
        switch (PublicValues.theme) {
            case DARK:
                try {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                } catch (UnsupportedLookAndFeelException e) {
                    unsupported = true;
                    ConsoleLogging.Throwable(e);
                }
                break;
            case LIGHT:
                try {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                } catch (UnsupportedLookAndFeelException e) {
                    unsupported = true;
                    ConsoleLogging.Throwable(e);
                }
                break;
            case WINDOWS:
            case LEGACY:
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (RuntimeException | UnsupportedLookAndFeelException | ClassNotFoundException |
                         InstantiationException | IllegalAccessException e) {
                    unsupported = true;
                    ConsoleLogging.Throwable(e);
                }
                break;
            case QuaQua:
                try {
                    UIManager.setLookAndFeel(new QuaquaLookAndFeel());
                } catch (UnsupportedLookAndFeelException e) {
                    unsupported = true;
                    ConsoleLogging.Throwable(e);
                }
                break;
        }
        if(unsupported) {
            ConsoleLogging.error("The theme you selected is not supported! Setting theme to ugly");
        }
        try {
            PublicValues.quality = Quality.valueOf(PublicValues.config.get(ConfigValues.audioquality.name));
        }catch (IllegalArgumentException exception) {
            //This should not happen but when it happens don't crash SpotifyXP
            PublicValues.quality = Quality.NORMAL;
        }
        if(!PublicValues.debug) {
            if (new File(PublicValues.fileslocation + "/" + "LOCKED").exists()) {
                JOptionPane.showConfirmDialog(null, PublicValues.language.translate("ui.startup.alreadystarted"), "SpotifyXP", JOptionPane.OK_CANCEL_OPTION);
                System.exit(0);
            }
        }
        if(!PublicValues.foundSetupArgument) {
            new Setup(); //Start setup because the argument "--setup-complete" was not found
        }
        new SplashPanel().show();
        if(PublicValues.config.get(ConfigValues.username.name).equals("")) {
            new LoginDialog().open(); //Show login dialog if no username is set
        }
        Runtime.getRuntime().addShutdownHook(hook); //Gets executed when SpotifyXP is closing
        api = new SpotifyAPI();
        SpotifyAPI.Player player = new SpotifyAPI.Player(api);
        new KeyListener().start();
        PublicValues.elevated = new SpotifyAPI.OAuthPKCE();
        ContentPanel panel = new ContentPanel(player, api);
        new BackgroundService().start();
        panel.open();
        new Analytics();
        SplashPanel.hide();
        DoubleArrayList updater = new Updater().updateAvailable();
        if(Boolean.parseBoolean(updater.getFirst(0).toString())) {
            String version = ((GitHubAPI.Release)updater.getSecond(0)).version;
            ContentPanel.feedbackupdaterversionfield.setText(PublicValues.language.translate("ui.updater.available") + version);
            new UpdaterDialog();
        }else{
            if(Double.parseDouble((((GitHubAPI.Release) updater.getSecond(0)).version.replace("v", "")))<Double.parseDouble(PublicValues.version.replace("v", ""))) {
                ContentPanel.feedbackupdaterversionfield.setText(PublicValues.language.translate("ui.updater.nightly"));
                ContentPanel.feedbackupdaterdownloadbutton.setVisible(false);
            } else {
                ContentPanel.feedbackupdaterversionfield.setText(PublicValues.language.translate("ui.updater.notavailable"));
            }
        }
        ConsoleLogging.info(PublicValues.language.translate("startup.info.took").replace("{}", startupTime.getHHMMSS()));
    }
}
