package com.ronaldbit.mopla;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;

import static com.ronaldbit.mopla.Patterns.*;
import static com.ronaldbit.mopla.TemplateUtils.*;

final class TemplateProcessor {
  private final Path templatesRoot;
  private final TemplateCache cache;
  private final Map<String, Filter> filters;
  private final boolean devMode;
  private final java.util.function.Function<String,String> assetHook;

  TemplateProcessor(Path templatesRoot, TemplateCache cache, Map<String,Filter> filters, boolean devMode, java.util.function.Function<String,String> assetHook) {
    this.templatesRoot = templatesRoot;
    this.cache = cache;
    this.filters = filters;
    this.devMode = devMode;
    this.assetHook = (assetHook == null ? (s->s) : assetHook);
  }

  /* ========== Extends / Includes ========== */

  String renderIncludeRaw(String includeFile) throws Exception {
    Path p = templatesRoot.resolve(includeFile).normalize();
    if (!p.startsWith(templatesRoot)) throw new SecurityException("Include fuera de templates: " + includeFile);
    if (!Files.exists(p)) return "";
    return cache.readFileCached(p);
  }

  String renderInclude(String includeFile, Map<String,Object> vars, Set<Path> includeStack) throws Exception {
    Path p = templatesRoot.resolve(includeFile).normalize();
    if (!p.startsWith(templatesRoot)) throw new SecurityException("Include fuera de templates: " + includeFile);
    if (!Files.exists(p)) return "";
    if (!includeStack.add(p)) throw new IllegalStateException("Ciclo de @include detectado: " + p);
    try {
      String raw = cache.readFileCached(p);
      return process(raw, vars, includeStack);
    } finally {
      includeStack.remove(p);
    }
  }

  String applyExtends(String content, Map<String,Object> vars, Set<Path> includeStack) throws Exception {
    Matcher em = EXTEND.matcher(content);
    if (!em.find()) return content;

    String layoutFile = em.group(1);
    String layout = renderIncludeRaw(layoutFile);

    Map<String,String> sections = new HashMap<>();
    Matcher sm = SECTION.matcher(content);
    while (sm.find()) {
      String name = sm.group(1);
      String body = sm.group(2);
      sections.put(name, process(body, vars, includeStack));
    }
    String composed = replaceAll(layout, YIELD, m -> sections.getOrDefault(m.group(1), ""));
    return applyExtends(composed, vars, includeStack);
  }

  /* ========== Pipeline principal ========== */
  // Orden:
  // 0) Comentarios
  // 1) Includes con args
  // 2) Includes simples
  // 3) Foreach
  // 4) If ... else
  // 5) Unless (inverso)
  // 6) If simple
  // 7) With (scope temporal)
  // 8) Set
  // 9) Yield / Raw / Var

  String process(String content, Map<String,Object> vars, Set<Path> includeStack) throws Exception {

    // 0) comentarios
    content = replaceAll(content, COMMENTS, m -> "");

    // 1) includes con locals: @include("file", k:"v":x:"y")
    content = replaceAll(content, INCLUDE_ARGS, m -> {
      String file = m.group(1);
      Map<String,Object> locals = TemplateUtils.parseNamedArgs(m.group(2));
      Map<String,Object> merged = new HashMap<>(vars);
      merged.putAll(locals);
      return renderInclude(file, merged, includeStack);
    });

    // 2) includes simples
    content = replaceAll(content, INCLUDE, m -> renderInclude(m.group(1), vars, includeStack));

    // 3) foreach
    content = replaceAll(content, FOREACH, m -> {
      String itemName = m.group(1);
      String listName = m.group(2);
      String block    = m.group(3);

      Object listObj = TemplateUtils.resolveVar(vars, listName);
      if (!(listObj instanceof Iterable<?> it)) return "";
      StringBuilder out = new StringBuilder();
      int index = 0;
      for (Object item : it) {
        Map<String,Object> loopVars = new HashMap<>(vars);
        loopVars.put(itemName, item);
        loopVars.put(itemName + "_index", index++);
        out.append(process(block, loopVars, includeStack));
      }
      return out.toString();
    });

    // 4) if ... else ... endif
    content = replaceAll(content, IF_ELSE, m -> {
      String name    = m.group(1);
      String thenB   = m.group(2);
      String elseB   = m.group(3);
      boolean ok = TemplateUtils.truthy(TemplateUtils.resolveVar(vars, name));
      return process(ok ? thenB : elseB, vars, includeStack);
    });

    // 5) unless(cond) ... endunless  (bloque inverso)
    content = replaceAll(content, UNLESS, m -> {
      String name = m.group(1);
      String body = m.group(2);
      boolean ok = TemplateUtils.truthy(TemplateUtils.resolveVar(vars, name));
      return process(ok ? "" : body, vars, includeStack);
    });

    // 6) if ... endif
    content = replaceAll(content, IF_ONLY, m -> {
      String name  = m.group(1);
      String body  = m.group(2);
      if (TemplateUtils.truthy(TemplateUtils.resolveVar(vars, name))) return process(body, vars, includeStack);
      return "";
    });

    // 7) with(locals) ... endwith  (scope temporal)
    content = replaceAll(content, WITH, m -> {
      Map<String,Object> locals = TemplateUtils.parseNamedArgs(m.group(1));
      String body = m.group(2);
      Map<String,Object> scoped = new HashMap<>(vars);
      scoped.putAll(locals); // locals pisan
      return process(body, scoped, includeStack);
    });

    // 8) @set("k","v")
    content = replaceAll(content, SET, m -> {
      String k = m.group(1), v = m.group(2);
      vars.put(k, v);
      return "";
    });

    // asset hook: @asset("path") -> hook(path)
    content = replaceAll(content, ASSET, m -> {
      String p = m.group(1);
      try { return assetHook.apply(p); } catch(Exception e) { return p; }
    });

    // dump (solo en devMode)
    content = replaceAll(content, DUMP, m -> {
      if (!devMode) return "";
      Object val = TemplateUtils.resolveVar(vars, m.group(1));
      String out = TemplateUtils.dump(val);
      return "<pre>" + TemplateUtils.htmlEscape(out) + "</pre>";
    });

    // 9) yields y variables
    content = replaceAll(content, YIELD, m -> {
      Object val = TemplateUtils.resolveVar(vars, m.group(1));
      return Objects.toString(val, "");
    });
    content = replaceAll(content, RAW, m -> {
      Object val = TemplateUtils.resolveVar(vars, m.group(1));
      return Objects.toString(val, "");
    });
    content = replaceAll(content, VAR, m -> {
      String token = m.group(1);
      String pipe  = m.group(2);
      Object val = TemplateUtils.resolveVar(vars, token);

      if (pipe != null && !pipe.isBlank()) {
        String chain = pipe.substring(1);
        for (String rawF : chain.split("\\|")) {
          rawF = rawF.trim();
          if (rawF.isEmpty()) continue;
          String fname;
          String[] fargs = new String[0];
          int colon = rawF.indexOf(':');
          if (colon >= 0) {
            fname = rawF.substring(0, colon).trim();
            fargs = TemplateUtils.parseArgs(rawF.substring(colon+1));
          } else {
            fname = rawF;
          }
          Filter f = filters.get(fname);
          if (f != null) val = f.apply(val, fargs);
        }
      }
      return TemplateUtils.htmlEscape(Objects.toString(val, ""));
    });

    return content;
  }
}
