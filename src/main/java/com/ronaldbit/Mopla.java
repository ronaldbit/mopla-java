package com.ronaldbit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mopla Template Engine
 * 
 * Directivas:
 *  - @extend("layout.html") + @section("x") ... @endsection + @yield("x")
 *  - @include("partial.html")
 *  - @foreach(item in items) ... @endforeach
 *  - @if(flag) ... @else ... @endif   y   @if(flag) ... @endif
 *  - @var(name)  (HTML escapado)    |   @raw(name) (sin escape)
 *  - Filtros: @var(name|upper), @var(num|number:"#,##0.00")
 *
 * Orden de procesamiento:
 *   includes -> foreach -> if/else -> if -> yields/vars
 */
public class Mopla {

    /* ===================== Config & Estado ===================== */

    private final Path templatesRoot;
    private boolean devMode = true;            // recarga si el archivo cambia
    private boolean cacheEnabled = true;       // cache de archivos
    private final ConcurrentHashMap<Path, CachedTpl> fileCache = new ConcurrentHashMap<>();
    private final Map<String, Filter> filters = new ConcurrentHashMap<>();

    public Mopla(String templatesPath) {
        this.templatesRoot = Paths.get(templatesPath).toAbsolutePath().normalize();
        // filtros por defecto
        register("upper", v -> Objects.toString(v, "").toUpperCase());
        register("lower", v -> Objects.toString(v, "").toLowerCase());
        register("trim",  v -> Objects.toString(v, "").trim());
        register("number", (v, args) -> {
            String pat = (args.length > 0 ? args[0] : "#,##0.##");
            DecimalFormat df = new DecimalFormat(pat);
            if (v instanceof Number n) return df.format(n);
            return df.format(new java.math.BigDecimal(Objects.toString(v, "0")));
        });
    }

    public Mopla setDevMode(boolean dev)             { this.devMode = dev; return this; }
    public Mopla setCacheEnabled(boolean enabled)    { this.cacheEnabled = enabled; return this; }
    public Mopla register(String name, Filter f)     { filters.put(name, f); return this; }

    /* ===================== Patrones ===================== */

    private static final Pattern EXTEND   = Pattern.compile("@extend\\(\"([^\"]+)\"\\)");
    private static final Pattern SECTION  = Pattern.compile("@section\\(\"([^\"]+)\"\\)([\\s\\S]*?)@endsection");
    private static final Pattern YIELD    = Pattern.compile("@yield\\(\"([^\"]+)\"\\)");
    private static final Pattern INCLUDE  = Pattern.compile("@include\\(\"([^\"]+)\"\\)");
    private static final Pattern FOREACH  = Pattern.compile("@foreach\\(([a-zA-Z0-9_]+)\\s+in\\s+([a-zA-Z0-9_]+)\\)([\\s\\S]*?)@endforeach");
    private static final Pattern IF_ELSE  = Pattern.compile("@if\\(([a-zA-Z0-9_]+)\\)([\\s\\S]*?)@else([\\s\\S]*?)@endif");
    private static final Pattern IF_ONLY  = Pattern.compile("@if\\(([a-zA-Z0-9_]+)\\)([\\s\\S]*?)@endif");
    private static final Pattern VAR      = Pattern.compile("@var\\(([a-zA-Z0-9_]+)(\\|[^)]+)?\\)");
    private static final Pattern RAW      = Pattern.compile("@raw\\(([a-zA-Z0-9_]+)\\)");

    /* ===================== API Pública ===================== */

    /** Render desde archivo (ruta relativa a templatesRoot) */
    public String render(String templateFile, Map<String, Object> vars) throws Exception {
        String content = readFile(templateFile);
        // Soporte @extend → compone layout + sections antes del pipeline
        content = applyExtends(content, vars, new HashSet<>());
        // Pipeline en orden correcto
        return process(content, vars, new HashSet<>());
    }

    /** Render desde string (útil para tests) */
    public String renderString(String templateText, Map<String,Object> vars) throws Exception {
        String content = templateText;
        content = applyExtends(content, vars, new HashSet<>());
        return process(content, vars, new HashSet<>());
    }

    /* ===================== Núcleo de render ===================== */

    private String process(String content, Map<String,Object> vars, Set<Path> includeStack) throws Exception {
        // 1) includes
        content = replaceAll(content, INCLUDE, m -> renderInclude(m.group(1), vars, includeStack));

        // 2) foreach (crea variables de item antes de reemplazar @var)
        content = replaceAll(content, FOREACH, m -> {
            String itemName = m.group(1);
            String listName = m.group(2);
            String block    = m.group(3);

            Object listObj = vars.get(listName);
            if (!(listObj instanceof Iterable<?> it)) return "";
            StringBuilder out = new StringBuilder();
            int index = 0;
            for (Object item : it) {
                Map<String,Object> loopVars = new HashMap<>(vars);
                loopVars.put(itemName, item);
                loopVars.put(itemName + "_index", index++); // índice opcional
                out.append(process(block, loopVars, includeStack)); // anidado
            }
            return out.toString();
        });

        // 3) if ... else ... endif
        content = replaceAll(content, IF_ELSE, m -> {
            String name    = m.group(1);
            String thenB   = m.group(2);
            String elseB   = m.group(3);
            boolean ok = truthy(vars.get(name));
            return process(ok ? thenB : elseB, vars, includeStack);
        });

        // 4) if ... endif
        content = replaceAll(content, IF_ONLY, m -> {
            String name  = m.group(1);
            String body  = m.group(2);
            if (truthy(vars.get(name))) return process(body, vars, includeStack);
            return "";
        });

        // 5) yields y variables (al final)
        content = replaceAll(content, YIELD, m -> {
            String name = m.group(1);
            Object val = vars.getOrDefault(name, "");
            return Objects.toString(val, "");
        });

        // @raw(nombre) sin escape
        content = replaceAll(content, RAW, m -> {
            String name = m.group(1);
            Object val = vars.getOrDefault(name, "");
            return Objects.toString(val, "");
        });

        // @var(nombre|filtro:arg:arg)
        content = replaceAll(content, VAR, m -> {
            String token = m.group(1);     // nombre
            String pipe  = m.group(2);     // |upper  |number:"#,##0.00"
            Object val = vars.getOrDefault(token, "");
            // aplicar filtros encadenados si los hay
            if (pipe != null && !pipe.isBlank()) {
                String chain = pipe.substring(1); // remove '|'
                for (String rawF : chain.split("\\|")) {
                    rawF = rawF.trim();
                    if (rawF.isEmpty()) continue;
                    String fname;
                    String[] fargs = new String[0];
                    int colon = rawF.indexOf(':');
                    if (colon >= 0) {
                        fname = rawF.substring(0, colon).trim();
                        fargs = parseArgs(rawF.substring(colon+1));
                    } else {
                        fname = rawF;
                    }
                    Filter f = filters.get(fname);
                    if (f != null) {
                        val = f.apply(val, fargs);
                    }
                }
            }
            // escape HTML por defecto
            return htmlEscape(Objects.toString(val, ""));
        });

        return content;
    }

    /* ===================== Extends / Sections / Yield ===================== */

    private String applyExtends(String content, Map<String,Object> vars, Set<Path> includeStack) throws Exception {
        Matcher em = EXTEND.matcher(content);
        if (!em.find()) return content;

        String layoutFile = em.group(1);
        String layout = readFile(layoutFile);

        // Extraer secciones del hijo
        Map<String,String> sections = new HashMap<>();
        Matcher sm = SECTION.matcher(content);
        while (sm.find()) {
            String name = sm.group(1);
            String body = sm.group(2);
            sections.put(name, process(body, vars, includeStack));
        }
        // Rellenar yields del layout
        String composed = replaceAll(layout, YIELD, m -> sections.getOrDefault(m.group(1), ""));
        // Soporte de herencia anidada (raro, pero posible)
        return applyExtends(composed, vars, includeStack);
    }

    /* ===================== Includes ===================== */

    private String renderInclude(String includeFile, Map<String,Object> vars, Set<Path> includeStack) throws Exception {
        Path p = templatesRoot.resolve(includeFile).normalize();
        if (!p.startsWith(templatesRoot)) throw new SecurityException("Include fuera de templates: " + includeFile);
        if (!Files.exists(p)) return "";
        if (!includeStack.add(p)) throw new IllegalStateException("Ciclo de @include detectado: " + p);
        try {
            String raw = readFileCached(p);
            return process(raw, vars, includeStack);
        } finally {
            includeStack.remove(p);
        }
    }

    /* ===================== IO + Caché ===================== */

    private String readFile(String file) throws IOException {
        Path p = templatesRoot.resolve(file).normalize();
        if (!p.startsWith(templatesRoot)) throw new SecurityException("Archivo fuera de templates: " + file);
        return readFileCached(p);
    }

    private String readFileCached(Path p) throws IOException {
        if (!cacheEnabled) return Files.readString(p, StandardCharsets.UTF_8);

        CachedTpl cached = fileCache.get(p);
        long lm = lastModified(p);
        if (cached == null || (devMode && cached.lastModified != lm)) {
            String txt = Files.readString(p, StandardCharsets.UTF_8);
            cached = new CachedTpl(txt, lm);
            fileCache.put(p, cached);
        }
        return cached.text;
    }

    private static long lastModified(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); }
        catch (IOException e) { return 0L; }
    }

    private static class CachedTpl {
        final String text; final long lastModified;
        CachedTpl(String t, long lm) { this.text = t; this.lastModified = lm; }
    }

    /* ===================== Utils ===================== */

    @FunctionalInterface
    public interface Filter {
        String apply(Object value, String... args);
    }

    private interface Replacer { String apply(Matcher m) throws Exception; }

    private static String replaceAll(String input, Pattern pattern, Replacer fn) throws Exception {
        Matcher m = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String rep = fn.apply(m);
            // escapar para appendReplacement
            rep = rep.replace("\\", "\\\\").replace("$", "\\$");
            m.appendReplacement(sb, rep);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n)  return n.intValue() != 0;
        if (v instanceof String s)  return !s.isEmpty() && !"false".equalsIgnoreCase(s);
        return true;
    }

    private static String[] parseArgs(String raw) {
        // soporta args separados por ":"; comillas dobles para cadenas con espacios
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i=0;i<raw.length();i++) {
            char c = raw.charAt(i);
            if (c=='"') { inQuotes = !inQuotes; continue; }
            if (c==':' && !inQuotes) { out.add(cur.toString()); cur.setLength(0); continue; }
            cur.append(c);
        }
        out.add(cur.toString());
        return out.stream().map(String::trim).filter(s->!s.isEmpty()).toArray(String[]::new);
    }

    private static String htmlEscape(String s) {
        StringBuilder sb = new StringBuilder(Math.max(16, s.length()));
        for (int i=0;i<s.length();i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\''-> sb.append("&#39;");
                default  -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
