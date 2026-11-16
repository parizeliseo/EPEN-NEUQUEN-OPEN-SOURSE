# EPEN al día ⚡

App Android **no oficial y de código abierto** para gestionar tus conexiones del **EPEN (Ente Provincial de Energía del Neuquén)** desde el celular, usando el portal público [servicios.epen.gov.ar](https://servicios.epen.gov.ar).

📘 **[Guía de usuario completa](GUIA_DE_USUARIO.md)** · 📦 **[Descargar el APK](../../releases/latest)**

## Funciones

- **Registro de conexiones**: todas las que quieras, con N° de suministro/contrato, nombre, titular, dirección y notas. No se duplican aunque cargues el número con o sin ceros.
- **Las cuatro secciones del portal**: factura vigente, deuda (vencidas, recibos y pago), facturas anteriores (hasta 2 años) y segmentación tarifaria. La app completa tu conexión y te deja directo en el código de verificación.
- **Bajar todo con un solo código**: descarga todos los comprobantes que liste la página de resultados en la misma sesión.
- **Registro de comprobantes**: cada factura que ves o bajás queda anotada en su conexión con número, vencimiento, importe y consumo.
- **Lectura de PDF**: extrae N° de factura y liquidación, vencimiento, total, kWh y período.
- **Resumen del último año**: total facturado, kWh, impuestos y tasas, alumbrado público e intereses, por conexión y en total.
- **Detalle por factura**: cada concepto del PDF agrupado por rubro.
- **Historial**: gráfico interactivo de consumo o importe, con escala y promedio.
- **Navegación rápida**: botón Inicio en todas las pantallas y selector para pasar de una conexión a otra.
- **Mis facturas**: archivo de PDF por conexión, sin duplicados, disponible sin conexión y copiado en `Descargas/EPEN`.
- **Facturas anteriores asistidas**: elegís el comprobante de referencia de una lista en lugar de buscarlo en papel.
- **Respaldo**: guardar y restaurar conexiones y registro en un archivo JSON.
- Los pagos se abren en el navegador del teléfono.

El **captcha lo escribe el usuario** en cada consulta, igual que en la web: la app no lo saltea ni lo resuelve automáticamente.

## Privacidad

Sin servidores intermedios, sin cuentas, sin publicidad ni analíticas. Los datos quedan solo en el teléfono y la app habla directamente con `servicios.epen.gov.ar`.

## Instalar

Descargá `EPEN-al-dia-vX.Y.apk` desde **[Releases](../../releases/latest)** y habilitá "Instalar apps de origen desconocido". Requiere Android 8.0 o superior.

## Compilar

```bash
./gradlew testReleaseUnitTest   # pruebas del lector de facturas
./gradlew assembleRelease       # APK en app/build/outputs/apk/release/
```

Cada push a `main` compila y prueba con GitHub Actions. Al publicar una etiqueta `vX.Y`, el APK se adjunta solo a la Release.

## Estructura

| Archivo | Qué hace |
|---|---|
| `MainActivity.kt` | Lista de conexiones, respaldo y restauración |
| `ConexionActivity.kt` | Ficha de la conexión: datos, resumen, gráficos y comprobantes |
| `PortalActivity.kt` | Portal del EPEN en WebView, autocompletado, descarga individual y en lote |
| `Suministros.kt` | Registro persistente de conexiones y comprobantes |
| `Lector.kt` | Lectura de facturas (PDF) y de páginas de resultados |
| `Descargas.kt` | Descarga, deduplicación por SHA-256 y archivo de PDF |
| `ArchivoActivity.kt` | Mis facturas |

## Colaborar

Los errores y sugerencias van en **Issues**. Si el portal cambia o una factura se lee mal, contá en qué sección pasó, sin publicar datos personales.

## Aviso

Proyecto independiente, sin relación con el EPEN.
