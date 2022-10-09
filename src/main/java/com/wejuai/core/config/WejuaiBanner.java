package com.wejuai.core.config;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

/**
 * Default Banner implementation which writes the 'Wejuai' banner.
 */
public class WejuaiBanner implements Banner {

    private static final String[] BANNER = {"",
            " __          ________     _ _    _         _____",
            " \\ \\        / /  ____|   | | |  | |  /\\   |_   _|",
            "  \\ \\  /\\  / /| |__      | | |  | | /  \\    | |",
            "   \\ \\/  \\/ / |  __| _   | | |  | |/ /\\ \\   | |",
            "    \\  /\\  /  | |___| |__| | |__| / ____ \\ _| |_",
            "     \\/  \\/   |______\\____/ \\____/_/    \\_\\_____|",
            ""};

    private static final String SPRING_BOOT = " :: Spring Boot :: ";

    private static final int STRAP_LINE_SIZE = 42;

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream printStream) {
        for (String line : BANNER) {
            printStream.println(line);
        }
        String version = SpringBootVersion.getVersion();
        version = (version != null) ? " (v" + version + ")" : "";
        StringBuilder padding = new StringBuilder();
        while (padding.length() < STRAP_LINE_SIZE - (version.length() + SPRING_BOOT.length())) {
            padding.append(" ");
        }

        printStream.println(AnsiOutput.toString(AnsiColor.GREEN, SPRING_BOOT, AnsiColor.DEFAULT, padding.toString(),
                AnsiStyle.FAINT, version));
        printStream.println();
    }

}
