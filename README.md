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
- **Historial**: gráfico interactivo de consumo o importe, de 1 o 2 años, con escala y promedio.
- **Desglose tocando cada cuadro**: por concepto y por factura.
- **Aviso sonoro de vencimiento**: el día anterior y el día del vencimiento.
- **Actualización automática**: relee los PDF en segundo plano y avisa de nuevas versiones publicadas en GitHub.
- **Navegación rápida**: botón Inicio en todas las pantallas y selector para pasar de una conexión a otra.
- **Mis facturas**: archivo de PDF por conexión, sin duplicados, disponible sin conexión y copiado en `Descargas/EPEN`.
- **Facturas anteriores asistidas**: elegís el comprobante de referencia de una lista en lugar de buscarlo en papel.
- **Respaldo**: guardar y restaurar conexiones y registro en un archivo JSON.
- Los pagos se abren en el navegador del teléfono.

El **captcha lo escribe el usuario** en cada consulta, igual que en la web: la app no lo saltea ni lo resuelve automáticamente.

## Privacidad

Sin servidores intermedios, sin cuentas, sin publicidad ni analíticas. Los datos quedan solo en el teléfono y la app habla directamente con `servicios.epen.gov.ar`.

## Instalar

Descargá `EPEN-al-dia-vX.Y.apk` desde **[Releases](../../releases/latest)** y habilitá "Instalar apps de origen desconocido". Requiere Android 8.0 o superior (compilada para Android 17).

## Compilar

```bash
./gradlew testDebugUnitTest    # pruebas: lector, registro, capacidad y pantallas
./gradlew assembleRelease       # APK en app/build/outputs/apk/release/
```

Cada push a `main` compila y prueba con GitHub Actions. Al publicar una etiqueta `vX.Y`, el APK se adjunta solo a la Release.

## Rendimiento

Medido con las pruebas automáticas (`RegistroTest`), 20 conexiones con 1000 facturas:

| | v1.4 | v1.5 |
|---|---|---|
| Registrar 1000 facturas | 4,3 s | 0,13 s |
| Armar la pantalla de inicio | 81 ms | 23 ms |

Las pruebas `PantallasTest` abren cada pantalla y cada desglose con datos de ejemplo, y dejan capturas en `app/build/capturas` (también se publican como artefacto de cada compilación en GitHub Actions).

Tecnología: Kotlin, Android Gradle Plugin 9.5, SDK 37, Material 1.14, WorkManager 2.12, PDFBox-Android, R8.

## Estructura

| Archivo | Qué hace |
|---|---|
| `MainActivity.kt` | Lista de conexiones, respaldo y restauración |
| `ConexionActivity.kt` | Ficha de la conexión: datos, resumen, gráficos y comprobantes |
| `PortalActivity.kt` | Portal del EPEN en WebView, autocompletado, descarga individual y en lote |
| `Suministros.kt` | Registro de conexiones y comprobantes: en memoria, con índice por conexión, lotes y escritura atómica |
| `Lector.kt` | Lectura de facturas (PDF) con caché de texto, conceptos por rubro y páginas de resultados |
| `Pantalla.kt` | Base de las pantallas: barra superior y bordes del sistema (borde a borde) |
| `Avisos.kt` | Avisos de vencimiento, revisión en segundo plano y nuevas versiones |
| `Descargas.kt` | Descarga, deduplicación por SHA-256 y archivo de PDF |
| `ArchivoActivity.kt` | Mis facturas |

## Colaborar

Los errores y sugerencias van en **Issues**. Si el portal cambia o una factura se lee mal, contá en qué sección pasó, sin publicar datos personales.

## Aviso

Proyecto independiente, sin relación con el EPEN.
