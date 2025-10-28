package com.ronaldbit.mopla;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TemplateUtils {
  private TemplateUtils() {}

  interface Replacer { String apply(Matcher m) throws Exception; }

  static String replaceAll(String input, Pattern pattern, Replacer fn) throws Exception {
    Matcher m = pattern.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String rep = fn.apply(m);
      // seguro para appendReplacement
      rep = rep.replace("\\", "\\\\").replace("$", "\\$");
      m.appendReplacement(sb, rep);
    }
    m.appendTail(sb);
    return sb.toString();
  }

  static boolean truthy(Object v) {
    if (v == null) return false;
    if (v instanceof Boolean b) return b;
    if (v instanceof Number n)  return n.intValue()!=0;
    if (v instanceof String s)  return !s.isEmpty() && !"false".equalsIgnoreCase(s);
    return true;
  }

  static String[] parseArgs(String raw) {
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

  /** Named-args tipo: k:"v":x:"y":flag:"true". Valores sin comillas se toman tal cual. */
  static Map<String,Object> parseNamedArgs(String raw) {
    Map<String,Object> out = new LinkedHashMap<>();
    if (raw == null || raw.isBlank()) return out;
    // partimos por ':' preservando comillas simples/dobles
    List<String> tokens = new ArrayList<>();
    StringBuilder cur = new StringBuilder();
    boolean inQuotes = false;
    for (int i=0;i<raw.length();i++) {
      char c = raw.charAt(i);
      if (c=='"') { inQuotes = !inQuotes; continue; }
      if (c==':' && !inQuotes) { tokens.add(cur.toString().trim()); cur.setLength(0); continue; }
      cur.append(c);
    }
    tokens.add(cur.toString().trim());
    // ahora tokens en pares key, value
    for (int i=0;i+1<tokens.size(); i+=2) {
      String key = tokens.get(i);
      String val = tokens.get(i+1);
      if (val.startsWith("\"") && val.endsWith("\"") && val.length()>=2) {
        val = val.substring(1, val.length()-1);
      }
      out.put(key, val);
    }
    return out;
  }

  static String htmlEscape(String s) {
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

  static String jsonEscape(String s) {
    StringBuilder sb = new StringBuilder(Math.max(16, s.length()));
    for (int i=0;i<s.length();i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\' -> sb.append("\\\\");
        case '"'  -> sb.append("\\\"");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default   -> {
          if (c < 0x20) sb.append(String.format("\\u%04x", (int)c));
          else sb.append(c);
        }
      }
    }
    return sb.toString();
  }

  static String dump(Object o) {
    if (o == null) return "null";
    if (o instanceof Map<?,?> m) {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      boolean first = true;
      for (Map.Entry<?,?> e : m.entrySet()) {
        if (!first) sb.append(", "); first = false;
        sb.append(Objects.toString(e.getKey(), "")); sb.append(": "); sb.append(dump(e.getValue()));
      }
      sb.append("}");
      return sb.toString();
    }
    if (o instanceof Iterable<?> it) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      boolean first = true;
      for (Object e : it) { if (!first) sb.append(", "); first = false; sb.append(dump(e)); }
      sb.append("]");
      return sb.toString();
    }
    if (o.getClass().isArray()) {
      int len = java.lang.reflect.Array.getLength(o);
      List<String> parts = new ArrayList<>();
      for (int i=0;i<len;i++) parts.add(dump(java.lang.reflect.Array.get(o,i)));
      return "[" + String.join(", ", parts) + "]";
    }
    return Objects.toString(o, "");
  }

  /** Devuelve value navegando por "a.b.c" soportando Map y beans (getX()/isX()). */
  static Object dotGet(Object base, String path) {
    if (base == null || path == null || path.isEmpty()) return null;
    String[] parts = path.split("\\.");
    Object cur = base;
    for (String p : parts) {
      if (cur == null) return null;
      if (cur instanceof Map<?,?> m) {
        cur = m.get(p);
      } else {
        cur = beanGet(cur, p);
      }
    }
    return cur;
  }

  /** Introspección segura de beans: busca getX(), isX() o campo público 'x'. */
  static Object beanGet(Object bean, String prop) {
    Class<?> c = bean.getClass();
    String cap = prop.substring(0,1).toUpperCase() + (prop.length()>1 ? prop.substring(1) : "");
    String[] getters = new String[] { "get"+cap, "is"+cap };
    for (String g : getters) {
      try {
        Method m = c.getMethod(g);
        if (!Modifier.isPublic(m.getModifiers())) continue;
        if (m.getParameterCount()==0) return m.invoke(bean);
      } catch (NoSuchMethodException ignored) {
      } catch (Exception e) {
        return null;
      }
    }
    // Campo público
    try {
      Field f = c.getField(prop);
      if (Modifier.isPublic(f.getModifiers())) return f.get(bean);
    } catch (Exception ignored) {}
    return null;
  }

  /** Cascada req > session > app > local; respeta prefijos explícitos (app./session./req.). */
  static Object resolveVar(Map<String,Object> vars, String token) {
    if (token.startsWith("app.") || token.startsWith("session.") || token.startsWith("req."))
      return dotGet(vars, token)!=null ? dotGet(vars, token) : vars.getOrDefault(token,"");

    Object v = dotGet(vars, "req." + token);
    if (v != null) return v;
    v = dotGet(vars, "session." + token);
    if (v != null) return v;
    v = dotGet(vars, "app." + token);
    if (v != null) return v;

    Object d = dotGet(vars, token);
    return d != null ? d : vars.getOrDefault(token, "");
  }
}
