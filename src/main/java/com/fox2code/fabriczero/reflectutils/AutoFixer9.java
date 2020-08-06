package com.fox2code.fabriczero.reflectutils;

import java.util.Objects;

public class AutoFixer9 {
    public static String getCommandLine() {
        return Objects.requireNonNull(ProcessHandle.current())
                .info().commandLine().orElseThrow(NullPointerException::new);
    }
}
