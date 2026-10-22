package com.ppms.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thread-safe identifier generator with prefixing and high-watermark tracking.
 */
public class IdGenerator {

    private static final Pattern NUMBER_EXTRACTOR = Pattern.compile("(\\d+)$");

    private final String prefix;
    private final AtomicLong counter;

    public IdGenerator(String prefix, long initialValue) {
        this.prefix = prefix != null ? prefix : "";
        this.counter = new AtomicLong(Math.max(initialValue, 1000));
    }

    /**
     * Generates the next sequential ID (e.g. PRD-1001, PRD-1002).
     */
    public String nextId() {
        return prefix + counter.incrementAndGet();
    }

    /**
     * Updates the internal counter if the provided candidate ID has a higher numeric sequence.
     * Prevents duplicate IDs across restarts or imports.
     */
    public void observeId(String existingId) {
        if (existingId == null) return;
        Matcher matcher = NUMBER_EXTRACTOR.matcher(existingId.trim());
        if (matcher.find()) {
            try {
                long num = Long.parseLong(matcher.group(1));
                counter.updateAndGet(current -> Math.max(current, num));
            } catch (NumberFormatException ignored) {
                // If not numeric suffix, ignore for numeric sequence
            }
        }
    }

    public long getCurrentSequence() {
        return counter.get();
    }
}
