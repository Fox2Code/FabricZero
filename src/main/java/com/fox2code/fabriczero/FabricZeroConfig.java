package com.fox2code.fabriczero;

import com.fox2code.fabriczero.api.FabricZeroAPI;

import java.io.*;

public class FabricZeroConfig {
    private static final File cfgFile = FabricZeroAPI.getInstance().getFabricZeroConfigFile();
    private static final int configRev = 1;
    public static boolean dumpClasses = false;
    public static boolean disableAccessMod = false; // Implement https://github.com/Fox2Code/FabricZero/issues/5
    public static boolean disableStringRedirect = false;
    public static boolean disableUnsafeRedirect = false;

    static {
        if (load()) {
            save();
        }
    }

    private static boolean load() {
        if (!cfgFile.exists()) {
            return true;
        }
        boolean update = false, flag2 = true;
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(cfgFile))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
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
                        case "disableStringRedirect":
                            disableStringRedirect = Boolean.parseBoolean(value);
                            break;
                        case "disableUnsafeRedirect":
                            disableUnsafeRedirect = Boolean.parseBoolean(value);
                            break;
                        case "configRev":
                            if (flag2) {
                                flag2 = false;
                                update = parseIntSafe(value) < configRev;
                            } else update = true;
                    }
                }
            }
        } catch (IOException ioe) {
            FabricZeroPlugin.LOGGER.error("Couldn't read config!",ioe);
        }
        return flag2 || update;
    }

    public static void save() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(cfgFile)){
            PrintStream printStream = new PrintStream(fileOutputStream);
            printStream.println("# Fabriczero Config. (Java version: "+System.getProperty("java.version")+")");
            printStream.println("# Note: UnsafeRedirect is only effective on Java9+");
            printStream.println("dump="+dumpClasses);
            printStream.println("disableAccessMod="+disableAccessMod);
            printStream.println("disableStringRedirect="+disableStringRedirect);
            printStream.println("disableUnsafeRedirect="+disableUnsafeRedirect);
            printStream.println("configRev="+configRev);
            printStream.flush();
        } catch (IOException ioe) {
            FabricZeroPlugin.LOGGER.error("Couldn't write config!",ioe);
        }
    }

    private static int parseIntSafe(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception e) {
            return Boolean.parseBoolean(text)?1:0;
        }
    }
}
