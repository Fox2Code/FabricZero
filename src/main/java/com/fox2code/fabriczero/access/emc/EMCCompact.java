package com.fox2code.fabriczero.access.emc;

import com.fox2code.fabriczero.FabricZeroPlugin;
import com.fox2code.fabriczero.reflectutils.ReflectedClass;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

public class EMCCompact {
    private static final URLClassLoader urlClassLoader;

    static {
        final ClassLoader classLoader = FabricLauncherBase.getLauncher().getTargetClassLoader();
        if (classLoader instanceof URLClassLoader) {
            urlClassLoader = (URLClassLoader) classLoader;
        } else {
            URLClassLoader urlClassLoaderTmp = null;
            try {
                final URLClassLoader urlContainer = (URLClassLoader)
                        ReflectedClass.of(classLoader).get0("urlLoader");
                urlClassLoaderTmp = new URLClassLoader(new URL[0],classLoader) {
                    @Override
                    public Class<?> loadClass(String name) throws ClassNotFoundException {
                        return classLoader.loadClass(name);
                    }

                    @Override
                    public Enumeration<URL> getResources(String name) throws IOException {
                        return classLoader.getResources(name);
                    }

                    @Override
                    public InputStream getResourceAsStream(String name) {
                        return classLoader.getResourceAsStream(name);
                    }

                    @Nullable
                    @Override
                    public URL getResource(String name) {
                        return classLoader.getResource(name);
                    }

                    @Override
                    public Enumeration<URL> findResources(String name) throws IOException {
                        return urlContainer.findResources(name);
                    }

                    @Override
                    public URL findResource(String name) {
                        return urlContainer.findResource(name);
                    }

                    @Override
                    public URL[] getURLs() {
                        return urlContainer.getURLs();
                    }

                    @Override
                    protected void addURL(URL url) {
                        FabricLauncherBase.getLauncher().propose(url);
                    }
                };
            } catch (ReflectiveOperationException e) {
                FabricZeroPlugin.LOGGER.error(e);
            }
            urlClassLoader = urlClassLoaderTmp;
        }
    }

    public static URLClassLoader newInstance(URL[] urls,ClassLoader parent) {
        if (urlClassLoader == null) {
            return URLClassLoader.newInstance(urls, parent);
        }
        try {
            for (URL url:urls) {
                FabricLauncherBase.getLauncher().propose(url);
            }
            return urlClassLoader;
        } catch (Throwable throwable) {
            FabricZeroPlugin.LOGGER.error(throwable);
            return URLClassLoader.newInstance(urls, parent);
        }
    }
}
