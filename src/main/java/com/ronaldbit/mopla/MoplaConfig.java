package com.ronaldbit.mopla;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Helper for initializing Mopla from env/.env, properties or a minimal YAML file.
 *
 * Note: YAML parsing is intentionally minimal (only supports `mopla:` block or `mopla.key: value` simple forms).
 */
public final class MoplaConfig {
  private MoplaConfig() {}

  public static Mopla fromEnv() {
    return fromMap(System.getenv());
  }

  public static Mopla fromEnvFile(Path envFile) throws IOException {
    Map<String,String> map = new HashMap<>(System.getenv());
    if (envFile != null && Files.exists(envFile)) {
      try (BufferedReader r = Files.newBufferedReader(envFile)) {
        String line;
        while ((line = r.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty() || line.startsWith("#")) continue;
          int eq = line.indexOf('=');
          if (eq <= 0) continue;
          String k = line.substring(0, eq).trim();
          String v = line.substring(eq+1).trim();
          // remove optional quotes
          if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length()-1);
          }
          map.put(k, v);
        }
      }
    }
    return fromMap(map);
  }

  public static Mopla fromProperties(Path propsFile) throws IOException {
    Properties p = new Properties();
    if (propsFile != null && Files.exists(propsFile)) {
      try (InputStream in = Files.newInputStream(propsFile)) { p.load(in); }
    }
    return fromProperties(p);
  }

  public static Mopla fromProperties(Properties p) {
    String root = p.getProperty("mopla.templates-root", "src/main/resources/templates");
    boolean dev = Boolean.parseBoolean(p.getProperty("mopla.dev-mode", "true"));
    boolean cache = Boolean.parseBoolean(p.getProperty("mopla.cache-enabled", "true"));
    String assetPrefix = p.getProperty("mopla.asset-prefix", "");
    return Mopla.builder()
        .templatesRoot(root)
        .devMode(dev)
        .cacheEnabled(cache)
        .setAssetHook(path -> assetPrefix + path)
        .build();
  }

  /** Minimal YAML parser for `mopla:` block or dotted keys. */
  public static Mopla fromYaml(Path yamlFile) throws IOException {
    Map<String,String> map = new HashMap<>();
    if (yamlFile != null && Files.exists(yamlFile)) {
      try (BufferedReader r = Files.newBufferedReader(yamlFile)) {
        String line;
        String currentParent = null;
        while ((line = r.readLine()) != null) {
          String t = line.trim();
          if (t.isEmpty() || t.startsWith("#")) continue;
          if (t.endsWith(":") && !t.contains(" ")) { // parent key like `mopla:`
            currentParent = t.substring(0, t.length()-1).trim();
            continue;
          }
          int colon = t.indexOf(':');
          if (colon <= 0) continue;
          String key = t.substring(0, colon).trim();
          String val = t.substring(colon+1).trim();
          if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
            val = val.substring(1, val.length()-1);
          }
          if (currentParent != null && (line.startsWith(" ") || line.startsWith("\t"))) {
            map.put(currentParent + "." + key, val);
          } else {
            map.put(key, val);
          }
        }
      }
    }
    Properties p = new Properties();
    // prefer dotted mopla.* keys, otherwise use direct keys
    p.setProperty("mopla.templates-root", map.getOrDefault("mopla.templates-root", map.getOrDefault("templates-root", "src/main/resources/templates")));
    p.setProperty("mopla.dev-mode", map.getOrDefault("mopla.dev-mode", map.getOrDefault("dev-mode", "true")));
    p.setProperty("mopla.cache-enabled", map.getOrDefault("mopla.cache-enabled", map.getOrDefault("cache-enabled", "true")));
    p.setProperty("mopla.asset-prefix", map.getOrDefault("mopla.asset-prefix", map.getOrDefault("asset-prefix", "")));
    return fromProperties(p);
  }

  private static Mopla fromMap(Map<String, String> env) {
    String root = env.getOrDefault("MOPLA_TEMPLATES_ROOT", env.getOrDefault("mopla.templates-root", env.getOrDefault("templates-root", "src/main/resources/templates")));
    boolean dev = Boolean.parseBoolean(env.getOrDefault("MOPLA_DEV_MODE", env.getOrDefault("mopla.dev-mode", "true")));
    boolean cache = Boolean.parseBoolean(env.getOrDefault("MOPLA_CACHE_ENABLED", env.getOrDefault("mopla.cache-enabled", "true")));
    String assetPrefix = env.getOrDefault("MOPLA_ASSET_PREFIX", env.getOrDefault("mopla.asset-prefix", ""));
    return Mopla.builder()
        .templatesRoot(root)
        .devMode(dev)
        .cacheEnabled(cache)
        .setAssetHook(path -> assetPrefix + path)
        .build();
  }
}
