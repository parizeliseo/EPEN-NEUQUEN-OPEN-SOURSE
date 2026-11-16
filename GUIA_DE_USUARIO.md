# Guía de usuario — EPEN al día ⚡

**Versión 1.4** · App Android no oficial para gestionar tus conexiones del EPEN (Ente Provincial de Energía del Neuquén).

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
10. [Historial y resumen del último año](#10-historial-y-resumen-del-último-año)
11. [Pagar](#11-pagar)
12. [Respaldo y cambio de teléfono](#12-respaldo-y-cambio-de-teléfono)
13. [Privacidad](#13-privacidad)
14. [Solución de problemas](#14-solución-de-problemas)
15. [Preguntas frecuentes](#15-preguntas-frecuentes)

Anexo: [Avisos y actualización automática](#10-bis-avisos-y-actualización-automática)

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
| **Resumen del último año** | Total facturado, kWh consumidos, impuestos y tasas, alumbrado público e intereses de los últimos 12 meses. |
| **Detalle por factura** | Cada concepto de la factura agrupado por rubro. |
| **Desglose de cada cuadro** | Tocando cualquier cuadro del resumen se ve en qué conceptos y en qué facturas se fue ese gasto. |
| **Aviso de vencimiento** | Notificación con sonido el día anterior y el mismo día del vencimiento. |
| **Actualización automática** | Relee los PDF sola y avisa cuando hay una versión nueva de la app. |
| **Historial** | Gráficos de consumo (kWh) e importe por factura. |
| **Mis facturas** | Archivo de PDF por conexión, disponible sin internet. |
| **Respaldo** | Guardar y restaurar tus conexiones y su registro. |

> **Importante:** la app no es del EPEN. Usa el mismo portal que la página web, con las mismas reglas.

---

## 2. Instalación

1. Entrá a la sección **Releases** del repositorio y descargá el archivo `EPEN-al-dia-v1.4.apk`.
2. Abrilo desde el teléfono. Si Android lo pide, habilitá **"Instalar apps de origen desconocido"** para el navegador o el gestor de archivos que estés usando.
3. Tocá **Instalar**.

**Requisitos:** Android 8.0 o superior, y conexión a internet para consultar el portal.

**Si ya tenías una versión anterior:** instalá la nueva encima. Tus conexiones se pasan solas al nuevo registro, y los PDF que ya habías bajado se leen y se anotan en su conexión la primera vez que abrís la app.

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

Arriba, en la franja azul, está el **resumen de todas tus conexiones** en el último año:

- Total facturado.
- kWh consumidos.
- Cantidad de conexiones.
- Cuántas facturas vencen en los próximos 10 días.

Debajo, una tarjeta por conexión con:

- **Nombre**, **N° de conexión** y dirección.
- **Importe de la última factura** y un aviso de vencimiento: *Vence el…* (azul), *Vence en N días* (amarillo, a 7 días o menos), *Vence hoy* (rojo) o *Venció el…* (gris).
- **kWh y total facturado del último año**, y cantidad de comprobantes.
- Botones **Factura**, **Deuda** y **Ver todo** (abre la ficha). Tocando la tarjeta también se abre la ficha.

**Menú ⋮ de la pantalla principal:**

- **Mis facturas** (ícono de documento): todos los PDF descargados.
- **Volver a leer mis facturas**: relee los PDF guardados y recalcula totales.
- **Guardar respaldo de conexiones** y **Restaurar respaldo**.
- **Acerca de** (muestra la versión).

### Moverse por la app

- El botón **Inicio** (ícono de casa, arriba a la derecha) aparece en la ficha de la conexión, en el portal y en *Mis facturas*: te lleva directo a la pantalla principal.
- En la ficha, la fila de **chips con los nombres de tus conexiones** (arriba, sobre la franja azul) permite saltar de una conexión a otra sin volver al inicio.
- Dentro del portal, **⋮ → Ficha de la conexión** te devuelve a la ficha de esa conexión.

---

## 5. Ficha de la conexión

Es el centro de cada conexión. De arriba hacia abajo:

1. **Selector de conexiones** (si tenés más de una) y **franja azul** con el N° de conexión en grande, el botón **Copiar** y los datos de titular, dirección y notas.
2. **Último año**: el período cubierto (los últimos 12 meses), el total facturado, la cantidad de facturas y cuatro mosaicos:
   - **Energía consumida**: kWh totales y promedio por factura.
   - **Impuestos y tasas**: IVA, ingresos brutos, tasas municipales, leyes y fondos provinciales, percepciones.
   - **Alumbrado público**.
   - **Intereses y recargos**: intereses por pago fuera de término, recargos y punitorios. Si no hubo, lo indica en verde.

   Cada rubro muestra qué porcentaje representa del total. **Tocando el total o cualquier cuadro** se abre su desglose (ver [punto 10](#10-historial-y-resumen-del-último-año)).
3. **Última factura**: importe, aviso de vencimiento, número, consumo y el botón **Ver detalle**.
4. **Consultar en EPEN**: cuatro accesos (Factura vigente, Deuda, Anteriores y Segmentación).
5. **Historial**: gráfico de las últimas 12 facturas, con selector **Consumo (kWh)** / **Importe ($)**.
6. **Comprobantes**: todos los registrados, del más nuevo al más viejo, con mes, número, kWh, importe y si es PDF o solo se vio en el portal. Tocando uno se abre su **detalle**.

**Detalle de una factura** (hoja que se abre desde abajo):

- Importe, aviso de vencimiento, números de factura y liquidación, período y consumo.
- **Detalle por rubro**: cada concepto leído del PDF (cargo fijo, energía, alumbrado, IVA, tasas, intereses…) con su importe y el subtotal de cada rubro.
- Botones **Abrir PDF**, **Compartir**, **Usar como referencia para Anteriores** y **Compartir texto leído (para soporte)**.

**Menú ⋮ de la ficha:**

- **Editar conexión**: cambiar datos o borrarla. Si la borrás, se borra su registro de comprobantes, pero los PDF quedan en *Mis facturas*.
- **Copiar N° de conexión**.
- **Compartir datos**: envía el nombre, número, titular y dirección.
- **Volver a leer mis facturas**.

---

## 6. Consultar el portal del EPEN

Al tocar una sección se abre el portal dentro de la app, ya preparado:

1. El **N° de conexión** ya está escrito.
2. Escribí el **código de verificación** de 4 caracteres **tal cual aparece en la imagen**: el portal distingue mayúsculas de minúsculas (si ves "jp3b", escribí jp3b en minúscula). Si no se lee bien, **tocá la imagen** para que aparezca otro (o **⋮ → Nuevo código**).
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

## 10. Historial y resumen del último año

Cada vez que se guarda un PDF, la app lee:

- N° de factura y N° de liquidación (quedan unidos en un solo comprobante).
- Fecha de vencimiento, total a pagar, consumo en kWh y período.
- **Cada concepto** con su importe, clasificado en: *Energía y cargos*, *Impuestos y tasas*, *Alumbrado público*, *Intereses y recargos* u *Otros*.

**Resumen del último año.** Suma las facturas cuyo vencimiento cae en los últimos 12 meses (por ejemplo, del 15/11/25 al 15/11/26). Cada factura se cuenta **una sola vez**, aunque figure con dos números o la hayas bajado más de una vez. Impuestos, alumbrado e intereses salen del detalle de cada PDF: si alguna factura del período solo se vio en el portal y no se bajó, la app avisa que los rubros pueden estar incompletos.

**Desglose de cada cuadro.** Tocando un cuadro del resumen se abre una hoja con:

- **Total facturado**: en qué se fue el dinero (energía y cargos, impuestos y tasas, alumbrado público, intereses, otros) y la lista de facturas del año. Si la suma de los conceptos leídos no coincide con el total de las facturas, la app lo advierte en rojo.
- **Impuestos y tasas**, **Alumbrado público** e **Intereses y recargos**: el total del rubro, **por concepto** (por ejemplo IVA, ingresos brutos, tasa municipal) y **por factura**, con barras para comparar.
- **Energía consumida**: los kWh de cada factura, el promedio, el máximo y el mínimo, y cuánto se pagó de energía y cargos fijos por kWh.

Tocando cualquier factura de la lista se abre su detalle completo.

**Gráfico del historial.**

- Muestra hasta 12 facturas, con una escala en números redondos.
- La línea amarilla punteada es el **promedio**.
- La barra de la última factura va en azul fuerte.
- **Tocando una barra** se ve su mes y su valor.
- Arriba se indica cuánto subió o bajó la última factura respecto del promedio.

> **Sobre la precisión:** el EPEN no publica el formato de sus facturas, así que la lectura es automática y tolerante. Si un dato no está claro, la app lo deja vacío en lugar de inventarlo. Si un rubro te parece mal clasificado, abrí el detalle de esa factura, tocá **Compartir texto leído (para soporte)** y mandalo en un *Issue* del repositorio: con eso se ajusta la lectura.

---

## 10 bis. Avisos y actualización automática

**Aviso de vencimiento con sonido.** La app avisa con una notificación sonora:

- **el día anterior** ("Mañana vence la factura de Terreno por $ …"), y
- **el mismo día del vencimiento** ("Hoy vence…").

La notificación tiene el botón **Pagar / descargar**, que abre la factura vigente de esa conexión. Toma el vencimiento de la última factura registrada de cada conexión, así que conviene bajar cada factura nueva cuando llega. Si ya la pagaste, ignorá el aviso.

- La primera vez que abrís la app, Android pide permiso para mostrar notificaciones: tocá **Permitir**.
- Se activan o desactivan desde la pantalla principal: **⋮ → Avisos de vencimiento**.
- El sonido y la vibración se ajustan en los ajustes de notificaciones de Android, canal *Vencimientos de facturas*.

**Actualización automática.** Sin que tengas que hacer nada:

- Cada vez que abrís la app, y también en segundo plano varias veces por día, se leen los PDF nuevos y se unen facturas repetidas.
- Cuando una versión nueva de la app mejora la lectura de facturas, todos tus PDF se vuelven a leer solos.
- Una vez por día la app revisa si hay una versión nueva publicada en GitHub. Si la hay, aparece un aviso en la pantalla principal (y una notificación) con el botón **Bajar**: se instala encima sin perder datos.

Lo único que no puede hacerse en segundo plano es **consultar el portal del EPEN**, porque cada consulta exige escribir el código de verificación.

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
| "ERROR en el código de verificación" | El código se escribió mal (revisá mayúsculas y minúsculas) o venció. Tocá la imagen para uno nuevo y volvé a intentar. |
| La sesión se cierra en Deuda | El portal la cierra tras 5 minutos sin uso. Usá **⋮ → Volver a empezar**. |
| "El portal no devolvió un PDF" | La sesión venció o el portal mostró una página en lugar del PDF. Repetí la consulta, o usá **Guardar página como PDF**. |
| No aparece el botón Bajar todo | La página no tiene comprobantes descargables (por ejemplo, una página de error). |
| Los gráficos no aparecen | Hacen falta al menos dos facturas en PDF con datos legibles. Probá **⋮ → Volver a leer mis facturas**. |
| No suena el aviso de vencimiento | Revisá que estén activados (⋮ → Avisos de vencimiento) y que Android permita las notificaciones de la app con sonido. Algunos teléfonos con ahorro de batería agresivo demoran los avisos: excluí la app del ahorro de batería. |
| Impuestos, alumbrado o intereses en $ 0 | Esos rubros salen del PDF: bajá las facturas del año desde **Anteriores** (con **Bajar todo**) y, si siguen en cero, compartí el texto leído para ajustar la lectura. |
| Una factura aparecía dos veces | Desde la 1.3 se unen solas; si quedara alguna, usá **⋮ → Volver a leer mis facturas**. |
| Un importe o consumo quedó mal | Abrí el PDF original para confirmarlo y reportalo en *Issues*. |
| No se instala el APK | Revisá que esté habilitado "Instalar apps de origen desconocido" y que el teléfono tenga Android 8 o superior. |
| El portal cambió y algo dejó de funcionar | Usá **⋮ → Abrir en el navegador** mientras tanto, y reportalo en *Issues*. |

---

## 15. Preguntas frecuentes

**¿La app me avisa cuando llega una factura nueva?**
Avisa el día anterior y el día del vencimiento de las facturas que ya están registradas. No puede detectar sola una factura nueva en el portal, porque cada consulta exige escribir el código de verificación.

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
