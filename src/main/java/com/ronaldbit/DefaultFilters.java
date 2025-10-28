package com.ronaldbit.mopla;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.Objects;

final class DefaultFilters {
  private DefaultFilters() {}

  static void registerAll(Map<String, Filter> registry) {
    registry.put("upper",  (v,a)-> Objects.toString(v,"").toUpperCase());
    registry.put("lower",  (v,a)-> Objects.toString(v,"").toLowerCase());
    registry.put("trim",   (v,a)-> Objects.toString(v,"").trim());
    registry.put("number", (v,a)-> {
      String pat = (a.length>0 ? a[0] : "#,##0.##");
      DecimalFormat df = new DecimalFormat(pat);
      if (v instanceof Number n) return df.format(n);
      return df.format(new java.math.BigDecimal(Objects.toString(v,"0")));
    });

    // extras tipo Thymeleaf
    registry.put("default", (v,a)-> {
      String s = Objects.toString(v,"");
      return s.isEmpty() ? (a.length>0?a[0]:"") : s;
    });
    registry.put("date", (v,a)-> {
      String p = (a.length>0 ? a[0] : "yyyy-MM-dd");
      DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p);
      if (v == null) return "";
      if (v instanceof LocalDate ld) return ld.format(fmt);
      if (v instanceof LocalDateTime ldt) return ldt.format(fmt);
      if (v instanceof Instant i) return fmt.withZone(ZoneId.systemDefault()).format(i);
      String s = Objects.toString(v,"");
      try { return LocalDate.parse(s).format(fmt); } catch(Exception ignore){}
      try { return LocalDateTime.parse(s).format(fmt); } catch(Exception ignore){}
      return s;
    });
    registry.put("join", (v,a)-> {
      String sep = (a.length>0 ? a[0] : ",");
      if (v instanceof Iterable<?> it) {
        StringBuilder sb = new StringBuilder();
        for (Object o : it) { if (sb.length()>0) sb.append(sep); sb.append(Objects.toString(o,"")); }
        return sb.toString();
      }
      return Objects.toString(v,"");
    });
    registry.put("url",  (v,a)-> URLEncoder.encode(Objects.toString(v,""), StandardCharsets.UTF_8));
    registry.put("json", (v,a)-> TemplateUtils.jsonEscape(Objects.toString(v,"")));

    // v0.4 extra filters
    registry.put("capitalize", (v,a) -> {
      String s = Objects.toString(v, "");
      if (s.isEmpty()) return s;
      return s.substring(0,1).toUpperCase() + (s.length()>1 ? s.substring(1) : "");
    });

    registry.put("truncate", (v,a) -> {
      String s = Objects.toString(v, "");
      int len = 0;
      try { len = (a.length>0) ? Integer.parseInt(a[0]) : 0; } catch(Exception e) { len = 0; }
      if (len <= 0 || s.length() <= len) return s;
      return s.substring(0, Math.max(0, len)) + "...";
    });

    registry.put("replace", (v,a) -> {
      String s = Objects.toString(v, "");
      if (a.length < 2) return s;
      return s.replace(a[0], a[1]);
    });

    registry.put("split", (v,a) -> {
      String s = Objects.toString(v, "");
      String sep = (a.length>0 ? a[0] : ",");
      if (s.isEmpty()) return "";
      return java.util.Arrays.toString(s.split(java.util.regex.Pattern.quote(sep)));
    });
  }
}
