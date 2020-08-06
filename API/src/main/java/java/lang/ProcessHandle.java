package java.lang;

import java.util.Optional;

public interface ProcessHandle {
    static ProcessHandle current() {
        return null;
    }

    Info info();

    interface Info {
        Optional<String> commandLine();

        Optional<String[]> arguments();
    }
}
