# 🧩 Mopla Template Engine

**Mopla** es un motor de plantillas minimalista para **Java 17+**, inspirado en Blade/Laravel y con ideas de **Thymeleaf**.  
Incluye layouts, includes, bucles, condicionales, **scopes persistentes** (app/session/req), **@with**, **@unless**, **acceso a beans**, **filtros avanzados** y **comentarios**.

Compatible con cualquier proyecto **Java** o **Spring Boot**.

---

## 📦 Instalación (JitPack)

1. Agrega el repositorio JitPack:
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
````

2. Añade la dependencia:

```xml
<dependency>
  <groupId>com.github.ronaldbit</groupId>
  <artifactId>mopla-java</artifactId>
  <version>v0.3.0</version>
</dependency>
```

> También puedes usar `main-SNAPSHOT`.

---

## ⚙️ Configuración rápida

```java
new Mopla("src/main/resources/templates")
  .setDevMode(true)       // recarga si cambia el archivo
  .setCacheEnabled(true)  // cache para producción
  .register("reverse", (v, a) -> new StringBuilder(String.valueOf(v)).reverse().toString());
```

---

## 🧠 Scopes persistentes (tipo Thymeleaf)

Permite mantener datos entre plantillas, como variables globales o de sesión.

```java
MoplaContext ctx = new MoplaContext();
ctx.app().put("appName", "Mi Web");
ctx.session().put("usuario", Map.of("nombre", "Juan"));
ctx.req().put("csrf", "abc123");

String html = mopla.render("home.html", ctx, Map.of("title", "Inicio"));
```

Jerarquía de búsqueda:
`req > session > app > vars locales`

Prefijos disponibles:

* `@var(app.nombreApp)`
* `@var(session.usuario.nombre)`
* `@var(req.csrf)`
* `@var(usuario.nombre)` (sin prefijo → búsqueda en cascada)

---

## 🧩 Directivas principales

| Directiva                            | Descripción                         | Ejemplo |
| ------------------------------------ | ----------------------------------- | ------- |
| `@extend("layout.html")`             | Herencia de layout                  | —       |
| `@section("x") ... @endsection`      | Define contenido para `@yield("x")` | —       |
| `@yield("x")`                        | Inserta la sección del hijo         | —       |
| `@include("file.html")`              | Incluye otra plantilla              | —       |
| `@include("file.html", k:"v":x:"y")` | Include con variables locales       | —       |
| `@foreach(item in list)`             | Itera sobre listas                  | —       |
| `@if(cond) ... @else ... @endif`     | Condicional con else                | —       |
| `@unless(cond) ... @endunless`       | Bloque inverso (if not)             | —       |
| `@with(k:"v":x:"y") ... @endwith`    | Crea un scope temporal              | —       |
| `@set("k","v")`                      | Define variable en el render actual | —       |
| `@var(name)`                         | Inserta variable (HTML escapado)    | —       |
| `@raw(name)`                         | Inserta sin escape                  | —       |
| `@* ... *@`                          | Comentario eliminado                | —       |

> Acceso con puntos: `@var(user.name)` o `@var(order.customer.city)`
> Soporta **Map**, **POJOs** (getX/isX) y campos públicos.

---

## 🧰 Filtros integrados

| Filtro              | Descripción               | Ejemplo                            |
| ------------------- | ------------------------- | ---------------------------------- |
| `upper`             | Mayúsculas                | `@var(title \| upper)`             |
| `lower`             | Minúsculas                | `@var(name \| lower)`              |
| `trim`              | Quita espacios            | `@var(text \| trim)`               |
| `number:"#.00"`     | Formato numérico          | `@var(price \| number:"#.00")`     |
| `default:"N/A"`     | Valor por defecto         | `@var(msg \| default:"-")`         |
| `date:"dd/MM/yyyy"` | Formatea fechas o strings | `@var(fecha \| date:"yyyy-MM-dd")` |
| `join:","`          | Une listas                | `@var(tags \| join:", ")`          |
| `url`               | URL-encode                | `@var(text \| url)`                |
| `json`              | Escape JSON               | `@var(user.name \| json)`          |

---

## 🧱 Seguridad

* `@var()` escapa HTML automáticamente.
* `@raw()` no escapa → úsalo solo si confías en el contenido.
* No permite includes fuera del directorio `templatesRoot`.

---

## 📦 JitPack

Cada tag (por ejemplo `v0.3.0`) genera una build en
👉 [https://jitpack.io/#ronaldbit/mopla-java](https://jitpack.io/#ronaldbit/mopla-java)

```xml
<dependency>
  <groupId>com.github.ronaldbit</groupId>
  <artifactId>mopla-java</artifactId>
  <version>v0.3.0</version>
</dependency>
```

---

## 📚 Ejemplos

Ver 👉 [**EXAMPLES.md**](EXAMPLES.md) para ejemplos completos de layouts, includes y contextos persistentes.

---

## 💡 Tips extra

* Puedes registrar filtros propios:

```java
mopla.register("reverse", (v,a) -> new StringBuilder(String.valueOf(v)).reverse().toString());
```

* O renderizar directamente strings (sin archivos):

```java
String html = mopla.renderString("<p>@var(name|upper)</p>", Map.of("name","Ronald"));
```

* Usa `ctx.clearRequest()` entre peticiones si mantienes Mopla en un servidor web.

---

## 🧑‍💻 Contribuir

```bash
git clone https://github.com/ronaldbit/mopla-java
cd mopla-java
mvn package
```

1. Usa Java 17+
2. Modifica y ejecuta pruebas
3. Envía PR 🚀

---

## 🏷️ Licencia

**MIT License**

Hecho con ❤️ por [@ronaldbit](https://github.com/ronaldbit)


 