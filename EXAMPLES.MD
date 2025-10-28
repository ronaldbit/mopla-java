## 📙 `EXAMPLES.md`

# 🧪 Ejemplos prácticos de Mopla

---

## 📁 Estructura típica

````

src/
└─ main/
# 🧪 Ejemplos prácticos de Mopla

Este archivo muestra ejemplos concretos para usar Mopla: layouts, includes, filtros (incl. los nuevos v0.4), `Mopla.builder()`, `@asset` y `@dump`.

---

## 📁 Estructura típica

```
src/
└─ main/
│   └─ java/
│       └─ com/example/App.java
└─ resources/
    └─ templates/
        ├─ layout.html
        ├─ home.html
        ├─ header.html
        └─ footer.html

```

---

## 🧩 layout.html

```html
<html>
<head><title>@var(title)</title></head>
<body>
  @include("header.html")
  <main>@yield("content")</main>
  @include("footer.html")
</body>
</html>
```

---

## 🧩 home.html

```html
@extend("layout.html")

@section("content")

<h1>@var(title|upper)</h1>

@if(showMessage)
  <p>@var(message)</p>
@else
  <p>No hay mensaje</p>
@endif

<ul>
@foreach(user in users)
  <li>@var(user)</li>
@endforeach
</ul>

@unless(admin)
  <p>Acceso limitado</p>
@endunless

@with(msg:"¡Hola!", extra:"dato temporal")
  <div>@var(msg) - @var(extra)</div>
@endwith

@endsection
```

---

## 🧩 header.html

```html
<header>
  <h2>@var(app.appName|default:"Mi App")</h2>
  Usuario: @var(session.usuario.nombre|default:"Invitado")
</header>
```

---

## 🧩 footer.html

```html
<footer>
  <small>CSRF: @var(req.csrf)</small>
</footer>
```

---

## 🧩 App.java (ejemplo de uso)

```java
import com.ronaldbit.mopla.*;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        Mopla mopla = new Mopla("src/main/resources/templates")
                .setDevMode(true)
                .setCacheEnabled(true);

        MoplaContext ctx = new MoplaContext();
        ctx.app().put("appName", "Mopla Demo");
        ctx.session().put("usuario", Map.of("nombre", "Ronald"));
        ctx.req().put("csrf", UUID.randomUUID().toString());

        Map<String,Object> vars = new HashMap<>();
        vars.put("title", "Usuarios");
        vars.put("showMessage", true);
        vars.put("message", "Listado cargado con éxito");
        vars.put("users", List.of("Ronald", "User2", "Pedro"));
        vars.put("admin", false);

        String html = mopla.render("home.html", ctx, vars);
        System.out.println(html);
    }
}
```

---

## 🧾 Salida esperada

```html
<html>
<head><title>Usuarios</title></head>
<body>
  <header>
    <h2>Mopla Demo</h2>
    Usuario: Ronald
  </header>
  <main>
    <h1>USUARIOS</h1>
    <p>Listado cargado con éxito</p>
    <ul>
      <li>Ronald</li>
      <li>User2</li>
      <li>Pedro</li>
    </ul>
    <p>Acceso limitado</p>
    <div>¡Hola! - dato temporal</div>
  </main>
  <footer>
    <small>CSRF: 49e6e...f41</small>
  </footer>
</body>
</html>
```

---

## ✨ Ejemplos v0.4 (filtros, builder, @asset, @dump)

### Filtros nuevos

```html
<p>Título: @var(title|capitalize)</p>
<p>Resumen: @var(longText|truncate:20)</p>
<p>Reemplazo: @var(text|replace:foo:bar)</p>
<p>Split como texto: @var(csv|split:,)</p>
```

> Nota: `split` devuelve una representación de array como String (por compatibilidad con la firma actual de filtros).

### Uso del builder (fluent)

```java
Mopla m = Mopla.builder()
    .templatesRoot("src/main/resources/templates")
    .devMode(true)
    .cacheEnabled(true)
    .setAssetHook(path -> "/static/" + path + "?v=123")
    .putGlobal("site", "Mi Site")
    .build();

String html = m.render("home.html", Map.of("title","hola mundo"));
```

### `@asset` — hook para versionado / hashing

Plantilla:
```html
<link rel="stylesheet" href="@asset(\"css/app.css\")">
```

Con el `assetHook` en el builder el resultado puede ser:

```html
<link rel="stylesheet" href="/static/css/app.css?v=123">
```

### `@dump` — depuración rápida (solo en `devMode`)

Plantilla:
```html
@dump(user)
```

Salida en `devMode=true`:

```html
<pre>{name: "Juan", roles: ["admin","user"]}</pre>
```

### Registrar un filtro custom y `renderString`

```java
mopla.register("reverse", (v,a) -> new StringBuilder(String.valueOf(v)).reverse().toString());
String out = mopla.renderString("<p>@var(name|reverse)</p>", Map.of("name","Ronald"));
// => <p>dlanoR</p>
```

---

## ▶️ Try it (rápido)

Desde la raíz del proyecto:

```bash
mvn -q -DskipTests package
java -cp target/classes:target/dependency/* com.example.App
```


