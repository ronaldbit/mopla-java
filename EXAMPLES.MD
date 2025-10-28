## 📙 `EXAMPLES.md`

# 🧪 Ejemplos prácticos de Mopla

---

## 📁 Estructura típica

````

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

````

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
````

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

## 🧩 App.java

```java
import com.ronaldbit.mopla.*;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        Mopla mopla = new Mopla("src/main/resources/templates")
                .setDevMode(true)
                .setCacheEnabled(true);

        // Contexto persistente (tipo Thymeleaf)
        MoplaContext ctx = new MoplaContext();
        ctx.app().put("appName", "Mopla Demo");
        ctx.session().put("usuario", Map.of("nombre", "Ronald"));
        ctx.req().put("csrf", UUID.randomUUID().toString());

        // Variables locales de esta vista
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

## 🧾 Salida

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
 