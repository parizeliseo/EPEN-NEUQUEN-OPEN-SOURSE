# Guía de usuario — EPEN al día ⚡

**Versión 1.1** · App Android no oficial para gestionar tus conexiones del EPEN (Ente Provincial de Energía del Neuquén).

---

## Índice

1. [Qué es y qué hace](#1-qué-es-y-qué-hace)
2. [Instalación](#2-instalación)
3. [Primeros pasos: registrar una conexión](#3-primeros-pasos-registrar-una-conexión)
4. [Pantalla principal](#4-pantalla-principal)
5. [Ficha de la conexión](#5-ficha-de-la-conexión)
6. [Consultar el portal del EPEN](#6-consultar-el-portal-del-epen)
7. [Bajar varias facturas con un solo código](#7-bajar-varias-facturas-con-un-solo-código)
8. [Facturas anteriores y comprobante de referencia](#8-facturas-anteriores-y-comprobante-de-referencia)
9. [Mis facturas](#9-mis-facturas)
10. [Historial de consumo e importes](#10-historial-de-consumo-e-importes)
11. [Pagar](#11-pagar)
12. [Respaldo y cambio de teléfono](#12-respaldo-y-cambio-de-teléfono)
13. [Privacidad](#13-privacidad)
14. [Solución de problemas](#14-solución-de-problemas)
15. [Preguntas frecuentes](#15-preguntas-frecuentes)

---

## 1. Qué es y qué hace

EPEN al día ordena en el celular todo lo que ofrece el portal público [servicios.epen.gov.ar](https://servicios.epen.gov.ar), y agrega un registro propio de tus conexiones y facturas.

| Función | Para qué sirve |
|---|---|
| **Conexiones registradas** | Guardás todas tus conexiones (casa, cabaña, local…) con número, titular, dirección y notas. |
| **Factura vigente** | Descargar o pagar la última factura sin vencer. |
| **Deuda** | Ver facturas vencidas, generar recibos y pagar. |
| **Facturas anteriores** | Reimprimir facturas de hasta 2 años. |
| **Segmentación tarifaria** | Consultar el nivel de subsidio de la conexión. |
| **Bajar todo** | Con un solo código de verificación, descargar todos los comprobantes que muestra el portal. |
| **Registro de comprobantes** | Cada factura que el portal te muestra o que bajás queda anotada con su número, vencimiento, importe y consumo. |
| **Historial** | Gráficos de consumo (kWh) e importe por factura. |
| **Mis facturas** | Archivo de PDF por conexión, disponible sin internet. |
| **Respaldo** | Guardar y restaurar tus conexiones y su registro. |

> **Importante:** la app no es del EPEN. Usa el mismo portal que la página web, con las mismas reglas.

---

## 2. Instalación

1. Entrá a la sección **Releases** del repositorio y descargá el archivo `EPEN-al-dia-v1.1.apk`.
2. Abrilo desde el teléfono. Si Android lo pide, habilitá **"Instalar apps de origen desconocido"** para el navegador o el gestor de archivos que estés usando.
3. Tocá **Instalar**.

**Requisitos:** Android 8.0 o superior, y conexión a internet para consultar el portal.

**Si ya tenías la versión 1.0:** instalá la 1.1 encima. Tus conexiones se pasan solas al nuevo registro, y los PDF que ya habías bajado se leen y se anotan en su conexión la primera vez que abrís la app.

---

## 3. Primeros pasos: registrar una conexión

1. En la pantalla principal tocá **Agregar conexión**.
2. Completá:
   - **Suministro/Contrato** (obligatorio). Está en tu factura, arriba a la derecha. Se puede escribir con o sin ceros: `118310/1` y `00118310/01` son lo mismo, y la app no deja cargar dos veces la misma conexión.
   - **Nombre**: cómo la querés reconocer (Casa, Cabaña, Local).
   - **Titular** y **Dirección**: opcionales, para identificarla rápido.
   - **Notas**: cualquier dato útil (ubicación del medidor, inquilino, etc.).
   - **Comprobante de referencia**: opcional. Solo lo usa la sección *Facturas anteriores* (ver [punto 8](#8-facturas-anteriores-y-comprobante-de-referencia)). Si lo dejás vacío, la app lo completa sola cuando ya tengas alguna factura registrada.
3. Tocá **Guardar**.

Podés registrar todas las conexiones que quieras. Para cambiar o borrar una, entrá a su ficha y usá **⋮ → Editar**.

---

## 4. Pantalla principal

Muestra todas tus conexiones registradas. Cada tarjeta tiene:

- **Nombre** y **N° de conexión**, más la dirección si la cargaste.
- **Importe y vencimiento** de la última factura registrada, si la app ya la conoce.
- Cuántos **comprobantes** y **PDF** tiene guardados.
- Tres botones: **Factura vigente**, **Deuda** y **Ver más** (abre la ficha).

Tocando cualquier parte de la tarjeta también se abre la ficha.

**Menú ⋮ de la pantalla principal:**

- **Mis facturas**: todos los PDF descargados.
- **Guardar respaldo de conexiones**.
- **Restaurar respaldo**.
- **Acerca de**.

---

## 5. Ficha de la conexión

Es el centro de cada conexión. De arriba hacia abajo:

1. **N° de conexión** en grande, con botón **Copiar** (útil para reclamos o trámites), y los datos de titular, dirección y notas.
2. **Última factura registrada**: importe, número, vencimiento y consumo.
3. **Consultar en EPEN**: acceso a las cuatro secciones del portal.
4. **Gráficos** de consumo e importe (aparecen cuando hay al menos dos facturas con datos).
5. **Comprobantes registrados**: todos los que la app vio o descargó, del más nuevo al más viejo. Cada uno indica si es un **PDF** guardado o si solo se **vio en el portal**, y permite **Abrir**, **Compartir** o **Usar de referencia**.

**Menú ⋮ de la ficha:**

- **Editar**: cambiar datos o borrar la conexión. Si borrás una conexión, se borra su registro de comprobantes, pero los PDF quedan en *Mis facturas*.
- **Copiar N° de conexión**.
- **Compartir datos**: envía por WhatsApp, mail, etc. el nombre, número, titular y dirección.
- **Volver a leer mis PDF**: vuelve a extraer importe, vencimiento y consumo de los PDF guardados.

---

## 6. Consultar el portal del EPEN

Al tocar una sección se abre el portal dentro de la app, ya preparado:

1. El **N° de conexión** ya está escrito.
2. Escribí el **código de verificación** de 4 caracteres que muestra la imagen. Si no se lee bien, **tocá la imagen** para que aparezca otro (o **⋮ → Nuevo código**).
3. En **Deuda**, además, tildá la casilla de **aceptación de términos** (la app no la acepta por vos).
4. Tocá **Ingresar**.

Desde ahí navegás el portal normalmente. Lo que la app hace por vos:

- **Cada PDF que abras se guarda solo** en *Mis facturas* y en la carpeta `Descargas/EPEN` del teléfono. Un aviso abajo muestra el número, importe y vencimiento leídos, con el botón **Abrir**.
- **Si ya tenías ese PDF**, no lo duplica: te avisa "Ya la tenías".
- **Los comprobantes que aparecen en pantalla se anotan** en la ficha de la conexión, aunque no los bajes.

**Menú ⋮ dentro del portal:**

| Opción | Uso |
|---|---|
| Bajar todas las de esta página | Ver [punto 7](#7-bajar-varias-facturas-con-un-solo-código). |
| Nuevo código | Cambia la imagen del captcha. |
| Guardar página como PDF | Si el portal muestra el comprobante como página y no como PDF, lo guarda con el sistema de impresión de Android. |
| Mis facturas | Ir al archivo de PDF. |
| Abrir en el navegador | Abre la misma página en Chrome u otro navegador. |
| Volver a empezar | Recarga la sección desde el formulario inicial. |

Deslizando hacia abajo se recarga la página.

> **¿Por qué hay que escribir el código cada vez?** Porque el portal del EPEN no tiene usuario y contraseña: cada consulta se protege con ese código. La app no lo saltea ni lo resuelve sola; solo te deja todo listo para que lo escribas.

---

## 7. Bajar varias facturas con un solo código

Después de ingresar el código, si la página de resultados tiene comprobantes para descargar, aparece abajo a la derecha el botón **Bajar todo (N)**.

1. Tocalo (o usá **⋮ → Bajar todas las de esta página**).
2. La app pide cada comprobante en la misma sesión, uno por uno, sin volver a pedir el código.
3. Al terminar muestra el resumen: cuántas facturas nuevas guardó, cuántas ya tenías y si alguna dio error. Con **Ver** vas a *Mis facturas*.

Es la forma más rápida de juntar el historial completo: entrá a **Facturas anteriores**, escribí el código una vez y tocá **Bajar todo**.

Si la página no entrega PDF, la app te lo avisa. En ese caso abrí el comprobante a mano o usá **Guardar página como PDF**.

---

## 8. Facturas anteriores y comprobante de referencia

El EPEN permite reimprimir facturas de hasta **2 años**. Para eso, el portal puede pedir el número de una factura anterior **que no sea la última emitida**, con alguno de estos formatos:

- N° de factura: `0001-00003217`
- N° de liquidación de servicios públicos: `00007-03125034`

Los dos figuran en el encabezado de tu factura.

**Cómo lo resuelve la app:**

- Cuando tocás **Facturas anteriores** y la conexión tiene varios comprobantes registrados, la app te muestra la lista para que elijas cuál usar de referencia (o "Sin referencia" para escribirlo vos).
- En la ficha, el botón **Usar de referencia** de cada comprobante lo deja fijo; aparece marcado con ★.
- La referencia elegida se completa sola en el formulario del portal.

---

## 9. Mis facturas

Reúne todos los PDF descargados, **agrupados por conexión** y del más reciente al más antiguo. Cada uno indica de qué sección vino (factura vigente, deuda, factura anterior, segmentación), la fecha de descarga y el tamaño.

- **Abrir**: con tu visor de PDF.
- **Compartir**: por WhatsApp, mail, Drive, etc.
- **Borrar**: elimina el PDF de la app. La copia en `Descargas/EPEN` no se toca, y el comprobante sigue anotado en la ficha (como "visto en portal").

Todo funciona **sin internet**.

---

## 10. Historial de consumo e importes

Cada vez que se guarda un PDF, la app intenta leer:

- N° de factura y N° de liquidación
- Fecha de vencimiento
- Total a pagar
- Consumo en kWh
- Período facturado

Con esos datos, la ficha de la conexión muestra:

- **Consumo por factura (kWh)** y **Importe por factura ($)**: barras de hasta las últimas 12 facturas, con la más reciente resaltada en amarillo.
- **Última vs. promedio**: cuánto subió o bajó la última factura respecto del promedio, en porcentaje.

Los gráficos aparecen cuando hay al menos dos facturas con fecha de vencimiento y el dato correspondiente.

> **Sobre la precisión:** el EPEN no publica el formato de sus facturas, así que la lectura es automática y tolerante. Si un dato no está claro, la app lo deja vacío en lugar de inventarlo. Si ves algo raro, abrí el PDF original y avisá en *Issues* del repositorio.

---

## 11. Pagar

Los botones de pago del portal (Mercado Pago, Link, Pago Mis Cuentas, etc.) se abren **en el navegador del teléfono**, fuera de la app. La app nunca ve ni guarda datos de tarjetas o cuentas.

Recordá lo que indica el EPEN: las facturas sin vencer se pueden imprimir y pagar hasta el último día del mes; las vencidas se gestionan desde **Deuda**. En las facturas anteriores, solo las que el portal muestra resaltadas sirven como medio de pago.

---

## 12. Respaldo y cambio de teléfono

**Guardar respaldo:** pantalla principal → **⋮ → Guardar respaldo de conexiones**. Elegí dónde guardarlo (Drive, Descargas…). Se genera un archivo `EPEN-al-dia-respaldo-AAAA-MM-DD.json` con:

- Todas tus conexiones (número, nombre, titular, dirección, notas, referencia).
- Los comprobantes registrados con sus datos (número, vencimiento, importe, consumo).

Los PDF no van en el respaldo, porque ya están en `Descargas/EPEN`.

**Restaurar:** **⋮ → Restaurar respaldo** y elegí el archivo. La restauración **suma** lo que falta; nunca borra ni duplica lo que ya tenés.

---

## 13. Privacidad

- No hay servidores intermedios: la app se comunica **solo** con `servicios.epen.gov.ar`.
- Tus conexiones, comprobantes y PDF quedan **únicamente en tu teléfono**.
- No usa publicidad, analíticas ni cuentas de usuario.
- El código es abierto: cualquiera puede revisarlo en este repositorio.

---

## 14. Solución de problemas

| Problema | Qué hacer |
|---|---|
| "ERROR en el código de verificación" | El código se escribió mal o venció. Tocá la imagen para uno nuevo y volvé a intentar. |
| La sesión se cierra en Deuda | El portal la cierra tras 5 minutos sin uso. Usá **⋮ → Volver a empezar**. |
| "El portal no devolvió un PDF" | La sesión venció o el portal mostró una página en lugar del PDF. Repetí la consulta, o usá **Guardar página como PDF**. |
| No aparece el botón Bajar todo | La página no tiene comprobantes descargables (por ejemplo, una página de error). |
| Los gráficos no aparecen | Hacen falta al menos dos facturas en PDF con datos legibles. Probá **⋮ → Volver a leer mis PDF** en la ficha. |
| Un importe o consumo quedó mal | Abrí el PDF original para confirmarlo y reportalo en *Issues*. |
| No se instala el APK | Revisá que esté habilitado "Instalar apps de origen desconocido" y que el teléfono tenga Android 8 o superior. |
| El portal cambió y algo dejó de funcionar | Usá **⋮ → Abrir en el navegador** mientras tanto, y reportalo en *Issues*. |

---

## 15. Preguntas frecuentes

**¿La app me avisa cuando llega una factura nueva?**
No. Para consultar, el portal exige escribir el código de verificación en cada ingreso, así que no se puede revisar en segundo plano.

**¿Puedo usar la app con conexiones de otras personas?**
Sí, el portal solo pide el número de conexión. Usalo con conexiones propias o con autorización de su titular.

**¿Dónde quedan los PDF?**
Dentro de la app (*Mis facturas*) y en la carpeta `Descargas/EPEN` del teléfono.

**¿Por qué el APK pesa unos 13 MB?**
Incluye la biblioteca que lee el texto de los PDF para armar el historial.

**¿Cómo colaboro o reporto un error?**
Abrí un *Issue* en el repositorio. Si podés, contá en qué sección pasó y qué mostraba la pantalla (sin publicar datos personales).

---

*Proyecto independiente, sin relación con el EPEN. EPEN — Sede Central: La Rioja 385, Neuquén · Tel. (0299) 445-6600.*
