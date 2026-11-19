package ar.pariz.epen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Resultado de guardar un PDF. */
data class Guardado(val archivo: File, val datos: DatosLeidos, val repetido: Boolean)

/** Descarga los PDF que entrega el portal, los archiva y los registra en su conexión. */
object Descargas {

    fun carpeta(ctx: Context): File = File(ctx.filesDir, "facturas").apply { mkdirs() }

    fun archivos(ctx: Context): List<File> =
        carpeta(ctx).listFiles { f -> f.extension.equals("pdf", true) }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** Clave de conexión a partir del nombre del archivo: EPEN_118310-1_... */
    fun claveDeArchivo(f: File): String =
        f.name.split("_").getOrNull(1)?.replace("-", "/")?.let(Suministros::clave) ?: ""

    /** Baja [url] con las cookies de la sesión del WebView. Se llama fuera del hilo principal. */
    fun bajar(
        ctx: Context, url: String, userAgent: String?, disposition: String?,
        mime: String?, clave: String, seccion: String, referer: String?
    ): Guardado {
        var actual = url
        var con: HttpURLConnection
        var saltos = 0
        while (true) {
            con = (URL(actual).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20000
                readTimeout = 60000
                CookieManager.getInstance().getCookie(actual)?.let { setRequestProperty("Cookie", it) }
                userAgent?.let { setRequestProperty("User-Agent", it) }
                referer?.let { setRequestProperty("Referer", it) }
            }
            val code = con.responseCode
            if (code in 300..399 && saltos < 5) {
                val loc = con.getHeaderField("Location") ?: break
                actual = URL(URL(actual), loc).toString()
                if (!actual.startsWith("https://")) throw IllegalStateException("Redirección no segura")
                saltos++
                continue
            }
            if (code != 200) throw IllegalStateException("El portal respondió $code")
            break
        }
        val bytes = con.inputStream.use { it.readBytes() }
        val sugerido = URLUtil.guessFileName(url, disposition, mime ?: "application/pdf")
        return guardar(ctx, bytes, clave, seccion, sugerido)
    }

    /** Guarda un PDF ya recibido: evita duplicados, lo lee y lo registra en la conexión. */
    fun guardar(ctx: Context, bytes: ByteArray, clave: String, seccion: String, sugerido: String?): Guardado {
        if (bytes.size < 5 || String(bytes, 0, 5, Charsets.ISO_8859_1) != "%PDF-")
            throw IllegalStateException("El portal no devolvió un PDF (puede que haya vencido la sesión).")

        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (bytes.size > 25 * 1024 * 1024) throw IllegalStateException("El PDF es demasiado grande (más de 25 MB).")
        Suministros.archivoConHash(ctx, hash)?.let { nombre ->
            val f = File(carpeta(ctx), nombre)
            if (f.exists()) return Guardado(f, Lector.leerFactura(Lector.textoDePdf(ctx, f, hash)), true)
        }

        val fecha = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        val tmp = File(carpeta(ctx), "tmp_$fecha.pdf").apply { writeBytes(bytes) }
        val datos = Lector.leerFactura(Lector.textoDePdf(ctx, tmp, hash))

        val etiqueta = (datos.numero ?: sugerido.orEmpty().substringBeforeLast(".").ifBlank { "comprobante" })
            .replace(Regex("[^A-Za-z0-9-]"), "").take(20)
        val destino = File(carpeta(ctx), "EPEN_${clave.replace("/", "-")}_${seccion}_${fecha}_$etiqueta.pdf")
        tmp.renameTo(destino)
        copiarADescargas(ctx, destino)

        Suministros.registrar(ctx, Comprobante(
            clave = clave,
            numero = datos.numero ?: "PDF ${fecha.take(10)}",
            vencimiento = datos.vencimiento, total = datos.total, consumoKwh = datos.consumoKwh,
            periodo = datos.periodo, archivo = destino.name, hash = hash, origen = "pdf",
            otroNumero = if (datos.factura != null) datos.liquidacion.orEmpty() else "",
            conceptos = datos.conceptos
        ))
        return Guardado(destino, datos, false)
    }

    /** Hilos para leer PDF en paralelo (la librería de PDF es lo más lento de la app). */
    private val hilos = maxOf(1, minOf(3, Runtime.getRuntime().availableProcessors() - 1))

    /** Lee varios PDF en paralelo. Devuelve (archivo, huella, datos) en el mismo orden. */
    private fun leerEnParalelo(ctx: Context, archivos: List<File>, huellas: Map<File, String> = emptyMap()):
        List<Triple<File, String, DatosLeidos>> {
        if (archivos.isEmpty()) return emptyList()
        val pool = java.util.concurrent.Executors.newFixedThreadPool(minOf(hilos, archivos.size))
        try {
            return archivos.map { f ->
                pool.submit<Triple<File, String, DatosLeidos>> {
                    val h = huellas[f] ?: runCatching { Lector.huella(f) }.getOrDefault("")
                    Triple(f, h, Lector.leerFactura(Lector.textoDePdf(ctx, f, h.ifBlank { null })))
                }
            }.map { it.get() }
        } finally {
            pool.shutdown()
        }
    }

    /** Lee y registra los PDF que todavía no figuran en el registro (p. ej. bajados con la v1.0). */
    fun reindexar(ctx: Context): Int {
        val conocidos = Suministros.todosLosComprobantes(ctx).map { it.archivo }.toSet()
        val nuevos = archivos(ctx).filter { it.name !in conocidos && claveDeArchivo(it).isNotBlank() && !it.name.startsWith("tmp_") }
        if (nuevos.isEmpty()) return 0
        val leidos = leerEnParalelo(ctx, nuevos)
        Suministros.lote(ctx) {
            for ((f, hash, d) in leidos) Suministros.registrar(ctx, Comprobante(
                claveDeArchivo(f), d.numero ?: "PDF ${f.name.split("_").getOrNull(3) ?: ""}", d.vencimiento, d.total,
                d.consumoKwh, d.periodo, f.name, hash, "pdf", f.lastModified(),
                if (d.factura != null) d.liquidacion.orEmpty() else "", d.conceptos
            ))
        }
        return leidos.size
    }

    /**
     * Vuelve a leer todos los PDF guardados (para aplicar reglas nuevas del lector) y une
     * duplicados. Usa el texto ya extraído, así que es rápido aunque haya muchas facturas.
     * Se ejecuta fuera del hilo principal.
     */
    fun actualizarRegistro(ctx: Context): Int {
        reindexar(ctx)
        // restos de descargas interrumpidas
        carpeta(ctx).listFiles { f -> f.name.startsWith("tmp_") && System.currentTimeMillis() - f.lastModified() > 3_600_000 }
            ?.forEach { it.delete() }
        val conArchivo = Suministros.todosLosComprobantes(ctx).filter { it.archivo.isNotBlank() }
            .mapNotNull { c -> File(carpeta(ctx), c.archivo).takeIf { it.exists() }?.let { c to it } }
        val huellas = conArchivo.filter { it.first.hash.isNotBlank() }.associate { it.second to it.first.hash }
        val leidos = leerEnParalelo(ctx, conArchivo.map { it.second }, huellas)
        Suministros.lote(ctx) {
            conArchivo.zip(leidos).forEach { (par, l) ->
                val c = par.first
                val d = l.third
                Suministros.registrar(ctx, c.copy(
                    vencimiento = d.vencimiento.ifBlank { c.vencimiento }, total = d.total ?: c.total,
                    consumoKwh = d.consumoKwh ?: c.consumoKwh, periodo = d.periodo.ifBlank { c.periodo },
                    otroNumero = c.otroNumero.ifBlank { if (d.factura != null) d.liquidacion.orEmpty() else "" },
                    hash = c.hash.ifBlank { l.second },
                    conceptos = d.conceptos
                ))
            }
            Suministros.depurar(ctx)
        }
        Lector.limpiarTextos(ctx, Suministros.todosLosComprobantes(ctx).map { it.hash }.filter { it.isNotBlank() }.toSet())
        return leidos.size
    }

    /** Copia también a Descargas/EPEN para verlo con cualquier gestor de archivos. */
    fun copiarADescargas(ctx: Context, f: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val v = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, f.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EPEN")
                }
                val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(f.readBytes()) }
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EPEN")
                dir.mkdirs()
                f.copyTo(File(dir, f.name), overwrite = true)
            }
        } catch (_: Exception) {
            // Si no se puede copiar, el PDF igual queda en "Mis facturas" dentro de la app.
        }
    }

    fun abrir(ctx: Context, f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(Intent.createChooser(i, "Abrir factura").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun compartir(ctx: Context, f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, "Compartir factura"))
    }
}
