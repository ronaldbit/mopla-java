package com.ronaldbit.mopla;

import java.util.regex.Pattern;

/** Patrones centralizados. Permitimos puntos en nombres (app.x, a.b.c). */
final class Patterns {
  private Patterns() {}

  // Extends / Sections / Yield
  static final Pattern EXTEND   = Pattern.compile("@extend\\(\"([^\"]+)\"\\)");
  static final Pattern SECTION  = Pattern.compile("@section\\(\"([^\"]+)\"\\)([\\s\\S]*?)@endsection");
  static final Pattern YIELD    = Pattern.compile("@yield\\(\"([^\"]+)\"\\)");

  // Includes: sin args y con args (locals)
  static final Pattern INCLUDE       = Pattern.compile("@include\\(\"([^\"]+)\"\\)");
  static final Pattern INCLUDE_ARGS  = Pattern.compile("@include\\(\"([^\"]+)\",\\s*([^\\)]+)\\)");

  // Bucles y condicionales (listName con puntos)
  static final Pattern FOREACH  = Pattern.compile("@foreach\\(([a-zA-Z0-9_]+)\\s+in\\s+([a-zA-Z0-9_\\.]+)\\)([\\s\\S]*?)@endforeach");
  static final Pattern IF_ELSE  = Pattern.compile("@if\\(([a-zA-Z0-9_\\.]+)\\)([\\s\\S]*?)@else([\\s\\S]*?)@endif");
  static final Pattern IF_ONLY  = Pattern.compile("@if\\(([a-zA-Z0-9_\\.]+)\\)([\\s\\S]*?)@endif");

  // Bloque inverso
  static final Pattern UNLESS   = Pattern.compile("@unless\\(([a-zA-Z0-9_\\.]+)\\)([\\s\\S]*?)@endunless");

  // Variables
  static final Pattern VAR      = Pattern.compile("@var\\(([a-zA-Z0-9_\\.]+)(\\|[^)]+)?\\)");
  static final Pattern RAW      = Pattern.compile("@raw\\(([a-zA-Z0-9_\\.]+)\\)");

  // Asset hook: extensible para versionado/hashing
  static final Pattern ASSET    = Pattern.compile("@asset\\(\"([^\"]+)\"\\)");

  // Dump para depuración (solo en devMode)
  static final Pattern DUMP     = Pattern.compile("@dump\\(([a-zA-Z0-9_\\.]+)\\)");

  // Set en el scope actual (request del render)
  static final Pattern SET      = Pattern.compile("@set\\(\"([^\"]+)\",\\s*\"([^\"]*)\"\\)");

  // With (scope temporal de bloque) con named-args: k:"v":x:"y"
  static final Pattern WITH     = Pattern.compile("@with\\(([^)]*)\\)([\\s\\S]*?)@endwith");

  // Comentarios a eliminar
  static final Pattern COMMENTS = Pattern.compile("@\\*([\\s\\S]*?)\\*@");
}
