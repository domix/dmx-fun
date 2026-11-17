---
theme: ../theme
title: From exceptions to signatures you can trust
titleTemplate: '%s · dmx-fun'
info: |
  A 45-minute talk on error handling in Java 25 with dmx-fun.
  English and Spanish from one source: press L to switch, or open with ?lang=es.
colorSchema: dark
fonts:
  sans: Inter Variable
  mono: JetBrains Mono
  provider: none
drawings:
  persist: false
transition: fade
layout: cover
---

# <span class="gradient-title">From exceptions to<br>signatures you can trust</span>

<Tk k="cover.subtitle" />

<!--
EN: Open with ?lang=es for a Spanish room. Press L at any time to switch languages; D for light mode.
ES: Abrir con ?lang=es para una sala en español. L cambia el idioma en cualquier momento; D pone el modo claro.
-->

---
layout: default
---

# <T en="The signature that lies" es="La firma que miente" />

```java
User findUser(String id)
```

<p class="mt-8"><Tk k="hook.question" /></p>

<v-click>

<p><Tk k="hook.answer" /></p>

</v-click>

<!--
EN: Hook. Ask the room before clicking. Someone always says "it throws", someone else "it returns null".
ES: Gancho. Preguntar a la sala antes de hacer clic. Alguien siempre dice "lanza", otro "devuelve null".
-->

---

# <T en="Chapter 0: twenty lines, four lies" es="Capítulo 0: veinte líneas, cuatro mentiras" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter00/EnrollmentService.java#enroll java {2-4|9-11|12-14|15-19}

<!--
EN: Same enroll as tutorial chapter 0. Click through: exception, null, exception, wrapped cause.
ES: El mismo enroll del capítulo 0 del tutorial. Clic a clic: excepción, null, excepción, causa envuelta.
-->

---

# <T en="Return instead of throw" es="Devolver en vez de lanzar" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter01/EnrollmentError.java#error java

<!--
EN: First, name the failures. Sealed interface, one record per case, data a caller can act on.
ES: Primero, nombrar los fallos. Interfaz sellada, un record por caso, datos sobre los que el caller puede actuar.
-->

---

# <T en="The caller handles every case, or it does not compile" es="El caller cubre todos los casos, o no compila" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter01/EnrollmentMessages.java#describe java

<!--
EN: Delete any case: compile error. This is the compiler doing the stack trace's job.
ES: Borra cualquier case: error de compilación. El compilador hace el trabajo del stack trace.
-->

---
layout: section
---

# <T en="Two tracks" es="Dos vías" />

---

# <T en="One chain, first failure wins" es="Una cadena, el primer fallo gana" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter02/EnrollmentService.java#enroll java {2|3-6|7|8-11}

<p class="mt-6"><Tk k="tracks.caption" /></p>

<!--
EN: Walk the chain line by line with the clicks. Blue is the success track, amber the failure track.
ES: Recorrer la cadena línea a línea con los clics. Azul es la vía del éxito, ámbar la del fallo.
-->

---

# <T en="All the errors at once" es="Todos los errores a la vez" />

<<< @/snippets/samples/enrollment-tutorial/src/main/java/dmx/fun/tutorial/chapter05/FormValidation.java#guards java

<!--
EN: Rules as values. allOf runs every rule and keeps every failure. ofCatching is chapter 4 at rule scale.
ES: Reglas como valores. allOf ejecuta todas y conserva todos los fallos. ofCatching es el capítulo 4 a escala de regla.
-->

---
layout: section
---

# <T en="Try it on one endpoint" es="Pruébala en un solo endpoint" />

<p><Tk k="closing.ask" /></p>

<p class="mt-8">
  <a href="https://domix.github.io/dmx-fun/guide/tutorial" target="_blank">domix.github.io/dmx-fun/guide/tutorial</a>
</p>

<!--
EN: The ask has to be specific. Point at the tutorial and the good-first-issue list.
ES: La petición tiene que ser concreta. Señalar el tutorial y la lista de good first issue.
-->
