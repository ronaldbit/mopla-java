package com.ronaldbit.mopla;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

import static com.ronaldbit.mopla.TemplateUtils.*;

public class Mopla {

  /* ===== Config ===== */
  private final Path templatesRoot;
  private boolean devMode = true;
  private boolean cacheEnabled = true;

  /* ===== Infra ===== */
  private final Map<String, Filter> filters = new ConcurrentHashMap<>();
  private final Map<String,Object> engineGlobals = new ConcurrentHashMap<>();
  private TemplateCache cache;
  private TemplateProcessor processor;
  // Hook extensible para assets (v0.4). Por defecto identidad.
  private Function<String,String> assetHook = s -> s;

  public Mopla(String templatesPath) {
    this.templatesRoot = Paths.get(templatesPath).toAbsolutePath().normalize();
    DefaultFilters.registerAll(filters);
    rebuildInfra();
  }

  public Mopla setDevMode(boolean dev)          { this.devMode = dev; rebuildInfra(); return this; }
  public Mopla setCacheEnabled(boolean enabled) { this.cacheEnabled = enabled; rebuildInfra(); return this; }
  public Mopla register(String name, Filter f)  { filters.put(name, f); return this; }

  public Mopla setAssetHook(Function<String,String> hook) { this.assetHook = hook == null ? (s->s) : hook; rebuildInfra(); return this; }

  public Mopla putGlobal(String k, Object v)    { engineGlobals.put(k, v); return this; }
  public Mopla removeGlobal(String k)           { engineGlobals.remove(k); return this; }

  private void rebuildInfra() {
    this.cache = new TemplateCache(devMode, cacheEnabled);
    this.processor = new TemplateProcessor(templatesRoot, cache, filters, devMode, assetHook);
  }

  /** Builder fluent para v0.4 */
  public static Builder builder() { return new Builder(); }

  public static class Builder {
    private String templatesRoot = ".";
    private boolean devMode = true;
    private boolean cacheEnabled = true;
    private final Map<String,Filter> registers = new HashMap<>();
    private final Map<String,Object> globals = new HashMap<>();
    private Function<String,String> assetHook = s->s;

    public Builder templatesRoot(String path) { this.templatesRoot = path; return this; }
    public Builder devMode(boolean dev) { this.devMode = dev; return this; }
    public Builder cacheEnabled(boolean en) { this.cacheEnabled = en; return this; }
    public Builder register(String name, Filter f) { this.registers.put(name, f); return this; }
    public Builder putGlobal(String k, Object v) { this.globals.put(k, v); return this; }
    public Builder setAssetHook(Function<String,String> hook) { this.assetHook = hook == null ? (s->s) : hook; return this; }

    public Mopla build() {
      Mopla m = new Mopla(this.templatesRoot);
      m.setDevMode(this.devMode);
      m.setCacheEnabled(this.cacheEnabled);
      m.setAssetHook(this.assetHook);
      this.registers.forEach(m::register);
      this.globals.forEach(m::putGlobal);
      return m;
    }
  }

  /* ===== API pública ===== */

  /** Render desde archivo con variables locales (compatibilidad). */
  public String render(String templateFile, Map<String,Object> vars) throws Exception {
    Map<String,Object> merged = new HashMap<>(engineGlobals);
    if (vars != null) merged.putAll(vars);
    String content = readFile(templateFile);
    content = processor.applyExtends(content, merged, new HashSet<>());
    return processor.process(content, merged, new HashSet<>());
  }

  /** Render con contexto (app/session/req) + locales que pisan al contexto. */
  public String render(String templateFile, MoplaContext ctx, Map<String,Object> vars) throws Exception {
    Map<String,Object> merged = mergedFromContext(ctx, vars);
    String content = readFile(templateFile);
    content = processor.applyExtends(content, merged, new HashSet<>());
    return processor.process(content, merged, new HashSet<>());
  }

  /** Render desde String (tests). */
  public String renderString(String templateText, Map<String,Object> vars) throws Exception {
    Map<String,Object> merged = new HashMap<>(engineGlobals);
    if (vars != null) merged.putAll(vars);
    String content = templateText;
    content = processor.applyExtends(content, merged, new HashSet<>());
    return processor.process(content, merged, new HashSet<>());
  }

  public String renderString(String templateText, MoplaContext ctx, Map<String,Object> vars) throws Exception {
    Map<String,Object> merged = mergedFromContext(ctx, vars);
    String content = templateText;
    content = processor.applyExtends(content, merged, new HashSet<>());
    return processor.process(content, merged, new HashSet<>());
  }

  /* ===== IO ===== */

  private String readFile(String file) throws IOException {
    // Soporte para classpath:resource (ej. classpath:templates/home.html)
    if (file != null && file.startsWith("classpath:")) {
      String res = file.substring("classpath:".length());
      InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(res.startsWith("/") ? res.substring(1) : res);
      if (in == null) return "";
      try (in) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }
    Path p = templatesRoot.resolve(file).normalize();
    if (!p.startsWith(templatesRoot)) throw new SecurityException("Archivo fuera de templates: " + file);
    return cache.readFileCached(p);
  }

  /* ===== Merge de contexto ===== */

  private Map<String,Object> mergedFromContext(MoplaContext ctx, Map<String,Object> locals) {
    Map<String,Object> merged = new HashMap<>();
    // prefijos que habilitan @var(app.x), @var(session.u), @var(req.csrf)
    ctx.app().forEach((k,v)-> merged.put("app."+k, v));
    ctx.session().forEach((k,v)-> merged.put("session."+k, v));
    ctx.req().forEach((k,v)-> merged.put("req."+k, v));
    // globals del engine
    merged.putAll(engineGlobals);
    // locales pisan todo
    if (locals != null) merged.putAll(locals);
    return merged;
  }
}
