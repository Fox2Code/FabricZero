package com.fox2code.fabriczero.access;

import net.minecraft.resource.DefaultResourcePack;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;

public class MCResources {
    public static final byte[] pack_mcmeta, pack_png;
    public static final boolean successful;

    static {
        byte[] tmp_pack_mcmeta = null, tmp_pack_png = null;
        boolean tmp_successful;
        String fileSource = null;
        String modFolder = new File(MCResources.class.getProtectionDomain()
                .getCodeSource().getLocation().getFile()).getAbsoluteFile().getParent();
        try {
            Enumeration<URL> url = DefaultResourcePack.class.getClassLoader().getResources("assets/.mcassetsroot");
            if (!url.hasMoreElements()) {
                System.out.println("No candidates!");
            }
            while (url.hasMoreElements()) {
                String file = url.nextElement().getFile();
                int ex = file.indexOf('!');
                if (ex != -1 && file.startsWith("file:")) {
                    file = file.substring(5, ex);
                    if (file.endsWith(".jar") && !file.startsWith(modFolder)
                            && file.contains("minecraft")) {
                        fileSource = file;
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        if (fileSource == null) {
            fileSource = DefaultResourcePack.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        }
        try(JarFile jarFile = new JarFile(new File(fileSource).getAbsoluteFile())) {
            System.out.println(jarFile.getName());
            tmp_pack_mcmeta =
                    readAllBytes(jarFile.getInputStream(jarFile.getEntry("pack.mcmeta")));
            tmp_pack_png =
                    readAllBytes(jarFile.getInputStream(jarFile.getEntry("pack.png")));
            tmp_successful = true;
        } catch (Throwable e) {
            tmp_successful = false;
            e.printStackTrace();
        }
        pack_mcmeta = tmp_pack_mcmeta;
        pack_png = tmp_pack_png;
        successful = tmp_successful;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        try {
            inputStream.close();
        } catch (IOException ignored) {}

        return buffer.toByteArray();
    }
}
