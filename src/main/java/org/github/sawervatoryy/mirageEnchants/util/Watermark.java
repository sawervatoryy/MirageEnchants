package org.github.sawervatoryy.mirageEnchants.util;

import java.util.logging.Logger;

/** Watermark студии. MirageStudio | Автор: sawervatoryy | https://discord.gg/mQbZT3x3Q */
public final class Watermark {
    public static final String STUDIO = "MirageStudio";
    public static final String AUTHOR = "sawervatoryy";
    public static final String DISCORD = "https://discord.gg/mQbZT3x3Q";

    private Watermark() {}

    public static void banner(Logger log, String version) {
        log.info("==================================================");
        log.info("  MirageEnchants v" + version);
        log.info("  " + STUDIO + " - Автор: " + AUTHOR);
        log.info("  Discord: " + DISCORD);
        log.info("==================================================");
    }
}
