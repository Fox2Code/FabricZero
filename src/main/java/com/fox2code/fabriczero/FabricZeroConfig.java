package com.fox2code.fabriczero;

import com.fox2code.fabriczero.api.FabricZeroAPI;

import java.io.*;

public class FabricZeroConfig {
    private static final File cfgFile = FabricZeroAPI.getInstance().getFabricZeroConfigFile();
    public static boolean dumpClasses = false;
    public static boolean disableAccessMod = false; // Implement https://github.com/Fox2Code/FabricZero/issues/5

    static {
        load();
    }

    private static void load() {
        if (!cfgFile.exists()) {
            return;
        }
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(cfgFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                int i = line.indexOf('=');
                if (i != -1) {
                    String value = line.substring(i + 1);
                    switch (line.substring(0, i)) {
                        case "dump":
                            dumpClasses = Boolean.parseBoolean(value);
                            break;
                        case "disableAccessMod":
                            disableAccessMod = Boolean.parseBoolean(value);
                            break;
                    }
                }
            }
        } catch (IOException ioe) {
            FabricZeroPlugin.LOGGER.error("Couldn't read config!",ioe);
        }
    }

    public static void save() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(cfgFile)){
            PrintStream printStream = new PrintStream(fileOutputStream);
            printStream.println("dump="+dumpClasses);
            printStream.println("disableAccessMod="+disableAccessMod);
            printStream.flush();
        } catch (IOException ioe) {
            FabricZeroPlugin.LOGGER.error("Couldn't write config!",ioe);
        }
    }
}
