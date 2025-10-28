package com.ronaldbit.mopla;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

final class TemplateCache {
  static final class Entry {
    final String text;
    final long lastModified;
    Entry(String t, long lm) { this.text = t; this.lastModified = lm; }
  }

  private final boolean devMode;
  private final boolean cacheEnabled;
  private final ConcurrentHashMap<Path, Entry> cache = new ConcurrentHashMap<>();

  TemplateCache(boolean devMode, boolean cacheEnabled) {
    this.devMode = devMode; this.cacheEnabled = cacheEnabled;
  }

  String readFileCached(Path p) throws IOException {
    if (!cacheEnabled) return Files.readString(p, StandardCharsets.UTF_8);
    long lm = lastModified(p);
    Entry e = cache.get(p);
    if (e == null || (devMode && e.lastModified != lm)) {
      String txt = Files.readString(p, StandardCharsets.UTF_8);
      e = new Entry(txt, lm);
      cache.put(p, e);
    }
    return e.text;
  }

  static long lastModified(Path p) {
    try { return Files.getLastModifiedTime(p).toMillis(); }
    catch (IOException e) { return 0L; }
  }
}
