package com.fox2code.fabriczero.reflectutils;

import java.util.Objects;
import java.util.Optional;

public class AutoFixer9 {
    @SuppressWarnings("unchecked")
    public static String getCommandLine() throws ReflectiveOperationException {
        final Class<?> ProcessHandle = Class.forName("java.lang.ProcessHandle");
        final Class<?> ProcessHandle$Info = Class.forName("java.lang.ProcessHandle$Info");
        return ((Optional<String>)ProcessHandle$Info.getDeclaredMethod("commandLine").invoke(
                ProcessHandle.getDeclaredMethod("info").invoke(
                Objects.requireNonNull(ProcessHandle.getDeclaredMethod("current").invoke(null)))
                )).orElseThrow(NullPointerException::new);
    }
}
