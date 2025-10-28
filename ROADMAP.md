# 🗺️ Roadmap — Mopla Template Engine

Estado actual: **v0.4.0**  
Meta: mantener Mopla **simple, rápido y seguro**, con integración fluida en proyectos Java/Spring.

---

## ✅ Hecho (v0.4.0)
- Scopes persistentes tipo Thymeleaf: `app`, `session`, `req` (cascada `req > session > app > vars`)
- Directivas nuevas: `@with(...)`, `@include("x", k:"v")`, `@unless(...)`, `@set("k","v")`, `@* ... *@`
- Acceso con puntos y a **POJOs** (getX/isX/campo)
- Filtros core y extras: `default`, `date`, `join`, `url`, `json` (además de `upper/lower/trim/number`)
- Refactor modular: `Patterns`, `TemplateUtils`, `TemplateProcessor`, `TemplateCache`, `DefaultFilters`, `MoplaContext`
- README dividido: `README.md` + `EXAMPLES.md`
- Mejoras v0.4 implementadas:
	- Nuevos filtros: `capitalize`, `truncate:length`, `replace:from:to`, `split:sep`.
	- `Mopla.builder()` fluent para configuración rápida.
	- `@asset("path")` con hook extensible (`Mopla.setAssetHook(...)`).
	- Macro de depuración `@dump(x)` (solo en `devMode`) y `TemplateUtils.dump(...)`.
	- Infra: `TemplateProcessor` y `TemplateCache` actualizados para soportar devMode/assetHook.
	- Compilación verificada (build local) y cambios integrados en el repo.

---

## 🧭 Principios
- **DX primero**: API clara, cero magia oculta.
- **Sin dependencia pesada**: solo estándar + utilitarios mínimos.
- **Seguro por defecto**: `@var` escapa HTML; `@raw` explícito.
- **Backwards compatible**: evitar cambios rompientes; si son inevitables, guías de migración.

---

## 🧩 Versionado
- `v0.x`: iteraciones rápidas (posibles cambios menores en API).
- `v1.0`: API estable con integración Spring Boot.

---

## ✅ v0.4 — Ergonomía & filtros (Short term)
**Objetivo:** hacer Mopla más cómodo en el día a día sin tocar el core.

**Alcance**
- ✅ Nuevos filtros: `capitalize`, `truncate:length`, `replace:from:to`, `split:sep`
- ✅ Macro de depuración `@dump(x)` (solo devMode)
- ✅ Utilidad `Mopla.builder()` (fluent) para config rápida
- ✅ `@asset("path")` (hook extensible para versionado/hashing de assets)
- ✅ `TemplateProfiler` opcional (tiempos de cada etapa en dev)

**Criterios de aceptación**
- Documentación en `EXAMPLES.md`
- Tests básicos por filtro / directiva
- Sin cambios rompientes

**Completado:** 2025-11-10

---

## 🌱 v0.5 — Integración Spring (MVP)
**Objetivo:** usar Mopla como motor de vistas en Spring MVC.

**Alcance**
- [ ] `MoplaView` y `MoplaViewResolver` (similar a ThymeleafViewResolver)
- [ ] Adaptador de `MoplaContext` desde `HttpSession` y `WebRequest`
- [ ] Soporte `classpath:` en `templatesRoot`
- [ ] Ejemplo `spring-boot-demo` (módulo sample)

**Criterios de aceptación**
- Demo corriendo con `@Controller` devolviendo nombres de vista
- Docs rápidas en `EXAMPLES.md` (sección Spring)

**Notas**
- Mantener Mopla independiente; el starter será un módulo opcional.

---

## ⚙️ v0.6 — CLI & herramientas
**Objetivo:** mejorar DX fuera del IDE.

**Alcance**
- [ ] `mopla-cli`: `render <tpl> --data <file.json> --root <dir>`
- [ ] `--watch` para recarga en caliente (dev)
- [ ] Modo “lint”: detectar includes fuera de root y loops vacíos

**Criterios de aceptación**
- Binario ejecutable (maven plugin o script)
- Ejemplos en `EXAMPLES.md`

---

## 🧠 v0.7 — i18n & mensajes
**Objetivo:** soporte ligero de internacionalización.

**Alcance**
- [ ] `@msg("key")` con `ResourceBundle` (`messages_xx.properties`)
- [ ] `@msg("key", k:"v")` (reemplazos simples)
- [ ] Selección de locale (por `ctx.session.locale` o parámetro en Mopla)

**Criterios de aceptación**
- Ejemplo en `EXAMPLES.md`
- Fall-back a `messages.properties`

---

## 🧩 v0.8 — Componentes reutilizables
**Objetivo:** composición más expresiva sin perder simplicidad.

**Alcance**
- [ ] `@component("card", title:"...", body:"...")` ↔ `components/card.html`
- [ ] Slots mínimos: `@slot("header") ... @endslot`
- [ ] Cache de componentes renderizados (opcional)

**Criterios de aceptación**
- API declarativa simple
- Docs y ejemplos

---

## 🚀 v0.9 — Precompilado ligero
**Objetivo:** rendimiento en producción.

**Alcance**
- [ ] Tokenización simple (evitar regex repetitivo en cada render)
- [ ] `mopla.compileAll(root)` → caché en `target/mopla-cache`
- [ ] Medición con `TemplateProfiler` (comparativa vs runtime puro)

**Criterios de aceptación**
- Mismo output que el motor actual
- Toggle para activar/desactivar

---

## 🎯 v1.0 — Estable
**Objetivo:** API consolidada + Spring Starter oficial.

**Alcance**
- [ ] `mopla-spring-boot-starter` (auto-config)
- [ ] Guía de migración 0.x → 1.0
- [ ] Estabilizar directivas y filtros “core”
- [ ] Política de compatibilidad semántica

**Criterios de aceptación**
- Demo end-to-end (Spring Boot + Mopla + i18n + componentes)
- Docs completas y revisadas

---

## 🧪 Calidad & contribuciones
- PRs con tests y ejemplos de uso.
- Estilo: Java 17, sin dependencias innecesarias.
- Revisa `GOOD FIRST ISSUE` y `help wanted` en Issues.

---

## 📝 Notas abiertas (backlog)
- [ ] `@csrf` y `@error(field)` (helpers web)
- [ ] `ThreadLocal` para `MoplaContext` (modo servidor)
- [ ] Loaders alternos: JAR/HTTP/FS mixto
- [ ] Cache distribuida (Redis/Hazelcast)
- [ ] Extensión VS Code (syntax highlight)

---

## 📅 Cómo planificamos
- Lanzamientos menores cada 2–4 semanas.
- Changelog por release.
- Todo lo que no entre en la versión planificada pasa a la siguiente sin bloquear.

