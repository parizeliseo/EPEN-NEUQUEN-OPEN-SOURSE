package ar.pariz.epen

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Una conexión (suministro/contrato) de EPEN registrada en la app. */
data class Suministro(
    val numero: String,              // como lo pide EPEN: 118310/1 o 00118310/01
    val alias: String = "",          // "Casa", "Cabaña"…
    val titular: String = "",
    val direccion: String = "",
    val notas: String = "",
    val ultimaFactura: String = "",  // comprobante de referencia para "Facturas anteriores"
    val creado: Long = System.currentTimeMillis()
) {
    /** Clave sin ceros de relleno: 00118310/01 y 118310/1 son la misma conexión. */
    val clave: String get() = Suministros.clave(numero)
    val nombre: String get() = alias.ifBlank { "Conexión $numero" }

    fun json(): JSONObject = JSONObject()
        .put("numero", numero).put("alias", alias).put("titular", titular)
        .put("direccion", direccion).put("notas", notas)
        .put("ultimaFactura", ultimaFactura).put("creado", creado)

    companion object {
        fun de(o: JSONObject) = Suministro(
            o.getString("numero"), o.optString("alias"), o.optString("titular"),
            o.optString("direccion"), o.optString("notas"), o.optString("ultimaFactura"),
            o.optLong("creado", System.currentTimeMillis())
        )
    }
}

/**
 * Un comprobante conocido de una conexión: lo que la app vio en el portal o leyó de un PDF.
 * [numero] es el N° de factura (0001-00003217) o de liquidación (00007-03125034).
 */
data class Comprobante(
    val clave: String,               // clave de la conexión
    val numero: String,
    val vencimiento: String = "",    // dd/mm/aaaa
    val total: Double? = null,
    val consumoKwh: Int? = null,
    val periodo: String = "",
    val archivo: String = "",        // nombre del PDF en Mis facturas, si se bajó
    val hash: String = "",
    val origen: String = "portal",   // "portal" o "pdf"
    val visto: Long = System.currentTimeMillis(),
    val otroNumero: String = "",     // el otro número de la misma factura (liquidación o factura)
    val conceptos: List<Concepto> = emptyList()
) {
    fun json(): JSONObject = JSONObject()
        .put("clave", clave).put("numero", numero).put("vencimiento", vencimiento)
        .put("total", total ?: JSONObject.NULL).put("consumoKwh", consumoKwh ?: JSONObject.NULL)
        .put("periodo", periodo).put("archivo", archivo).put("hash", hash)
        .put("origen", origen).put("visto", visto).put("otroNumero", otroNumero)
        .put("conceptos", JSONArray().apply {
            conceptos.forEach { put(JSONObject().put("n", it.nombre).put("i", it.importe).put("c", it.categoria.name)) }
        })

    fun tieneNumero(n: String) = n.isNotBlank() && (numero == n || otroNumero == n)

    fun suma(cat: Categoria): Double = conceptos.filter { it.categoria == cat }.sumOf { it.importe }

    /** Fecha de vencimiento como aaaammdd para ordenar; 0 si no se conoce. */
    val orden: Long
        get() = Regex("""(\d{2})/(\d{2})/(\d{4})""").find(vencimiento)
            ?.destructured?.let { (d, m, a) -> "$a$m$d".toLong() } ?: 0L

    companion object {
        fun de(o: JSONObject) = Comprobante(
            o.getString("clave"), o.getString("numero"), o.optString("vencimiento"),
            if (o.isNull("total")) null else o.optDouble("total"),
            if (o.isNull("consumoKwh")) null else o.optInt("consumoKwh"),
            o.optString("periodo"), o.optString("archivo"), o.optString("hash"),
            o.optString("origen", "portal"), o.optLong("visto"),
            o.optString("otroNumero"),
            (o.optJSONArray("conceptos") ?: JSONArray()).let { a ->
                (0 until a.length()).map { a.getJSONObject(it) }.map {
                    Concepto(it.optString("n"), it.optDouble("i"),
                        runCatching { Categoria.valueOf(it.optString("c")) }.getOrDefault(Categoria.OTRO))
                }
            }
        )
    }
}

/** Registro persistente (archivo JSON en el almacenamiento privado de la app). */
object Suministros {
    private const val ARCHIVO = "registro.json"
    private val lock = Any()

    fun clave(numero: String): String = numero.split("/").joinToString("/") {
        it.trimStart('0').ifEmpty { "0" }
    }

    /** Valida el formato que acepta el portal: números separados por una "/". */
    fun normalizar(entrada: String): String? {
        val limpio = entrada.trim().replace(" ", "").replace("-", "/")
        val partes = limpio.split("/")
        if (partes.size != 2) return null
        if (partes.any { it.isEmpty() || !it.all(Char::isDigit) }) return null
        return "${partes[0]}/${partes[1]}"
    }

    fun facturaValida(f: String): Boolean =
        f.isEmpty() || Regex("""^\d{4,5}-\d{8}$""").matches(f.trim())

    // ---------- lectura / escritura ----------
    private fun archivo(ctx: Context) = File(ctx.filesDir, ARCHIVO)

    private fun leer(ctx: Context): Pair<MutableList<Suministro>, MutableList<Comprobante>> = synchronized(lock) {
        val f = archivo(ctx)
        if (!f.exists()) migrarDesdePreferencias(ctx)
        val o = try { JSONObject(f.readText()) } catch (e: Exception) { JSONObject() }
        val s = o.optJSONArray("suministros") ?: JSONArray()
        val c = o.optJSONArray("comprobantes") ?: JSONArray()
        (0 until s.length()).map { Suministro.de(s.getJSONObject(it)) }.toMutableList() to
            (0 until c.length()).map { Comprobante.de(c.getJSONObject(it)) }.toMutableList()
    }

    private fun escribir(ctx: Context, s: List<Suministro>, c: List<Comprobante>) = synchronized(lock) {
        val o = JSONObject()
            .put("version", 2)
            .put("suministros", JSONArray().apply { s.forEach { put(it.json()) } })
            .put("comprobantes", JSONArray().apply { c.forEach { put(it.json()) } })
        val tmp = File(ctx.filesDir, "$ARCHIVO.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(archivo(ctx))
    }

    /** La versión 1.0 guardaba los suministros en SharedPreferences. */
    private fun migrarDesdePreferencias(ctx: Context) {
        val prefs = ctx.getSharedPreferences("epen", Context.MODE_PRIVATE)
        val viejo = try { JSONArray(prefs.getString("suministros", "[]")) } catch (e: Exception) { JSONArray() }
        val lista = (0 until viejo.length()).map { Suministro.de(viejo.getJSONObject(it)) }
        escribir(ctx, lista, emptyList())
    }

    // ---------- conexiones ----------
    fun listar(ctx: Context): List<Suministro> = leer(ctx).first.sortedBy { it.nombre.lowercase() }

    fun buscar(ctx: Context, clave: String): Suministro? = leer(ctx).first.firstOrNull { it.clave == clave }

    /** Guarda o reemplaza. Devuelve false si ya existe otra conexión con el mismo número. */
    fun guardar(ctx: Context, s: Suministro, claveAnterior: String? = null): Boolean = synchronized(lock) {
        val (lista, comps) = leer(ctx)
        if (lista.any { it.clave == s.clave && it.clave != claveAnterior }) return false
        lista.removeAll { it.clave == claveAnterior || it.clave == s.clave }
        lista.add(s)
        // si cambió el número, los comprobantes pasan a la nueva clave
        val nuevos = if (claveAnterior != null && claveAnterior != s.clave)
            comps.map { if (it.clave == claveAnterior) it.copy(clave = s.clave) else it } else comps
        escribir(ctx, lista, nuevos)
        true
    }

    fun borrar(ctx: Context, clave: String) = synchronized(lock) {
        val (lista, comps) = leer(ctx)
        escribir(ctx, lista.filterNot { it.clave == clave }, comps.filterNot { it.clave == clave })
    }

    // ---------- comprobantes ----------
    fun comprobantes(ctx: Context, clave: String): List<Comprobante> =
        leer(ctx).second.filter { it.clave == clave }
            .sortedWith(compareByDescending<Comprobante> { it.orden }.thenByDescending { it.numero })

    fun todosLosComprobantes(ctx: Context): List<Comprobante> = leer(ctx).second

    /** Agrega o completa un comprobante: nunca pisa datos conocidos con vacíos. */
    fun registrar(ctx: Context, nuevo: Comprobante) = synchronized(lock) {
        val (lista, comps) = leer(ctx)
        val i = comps.indexOfFirst {
            it.clave == nuevo.clave && (it.tieneNumero(nuevo.numero) || it.tieneNumero(nuevo.otroNumero))
        }
        if (i < 0) comps.add(nuevo) else {
            val v = comps[i]
            comps[i] = v.copy(
                vencimiento = nuevo.vencimiento.ifBlank { v.vencimiento },
                total = nuevo.total ?: v.total,
                consumoKwh = nuevo.consumoKwh ?: v.consumoKwh,
                periodo = nuevo.periodo.ifBlank { v.periodo },
                archivo = nuevo.archivo.ifBlank { v.archivo },
                hash = nuevo.hash.ifBlank { v.hash },
                origen = if (nuevo.origen == "pdf" || v.origen == "pdf") "pdf" else v.origen,
                visto = maxOf(v.visto, nuevo.visto),
                otroNumero = v.otroNumero.ifBlank {
                    listOf(nuevo.numero, nuevo.otroNumero).firstOrNull { it.isNotBlank() && it != v.numero }.orEmpty()
                },
                conceptos = nuevo.conceptos.ifEmpty { v.conceptos }
            )
        }
        escribir(ctx, lista, comps)
    }

    /**
     * Une comprobantes repetidos de una misma factura (la v1.1 registraba aparte el N° de
     * factura y el de liquidación). Se consideran la misma factura si coinciden conexión,
     * vencimiento e importe. Devuelve cuántos se unieron.
     */
    fun depurar(ctx: Context): Int = synchronized(lock) {
        val (lista, comps) = leer(ctx)
        val salida = mutableListOf<Comprobante>()
        var unidos = 0
        for (c in comps.sortedByDescending { it.archivo.isNotBlank() }) {
            val j = salida.indexOfFirst { o ->
                o.clave == c.clave && (
                    o.tieneNumero(c.numero) ||
                        (c.vencimiento.isNotBlank() && c.total != null &&
                            o.vencimiento == c.vencimiento && o.total == c.total)
                    )
            }
            if (j < 0) { salida.add(c); continue }
            val o = salida[j]
            salida[j] = o.copy(
                otroNumero = o.otroNumero.ifBlank { c.numero.takeIf { it != o.numero }.orEmpty() },
                consumoKwh = o.consumoKwh ?: c.consumoKwh,
                periodo = o.periodo.ifBlank { c.periodo },
                conceptos = o.conceptos.ifEmpty { c.conceptos }
            )
            unidos++
        }
        if (unidos > 0) escribir(ctx, lista, salida)
        unidos
    }

    fun archivoConHash(ctx: Context, hash: String): String? =
        leer(ctx).second.firstOrNull { it.hash == hash && it.archivo.isNotBlank() }?.archivo

    fun olvidarArchivo(ctx: Context, nombre: String) = synchronized(lock) {
        val (lista, comps) = leer(ctx)
        escribir(ctx, lista, comps.map { if (it.archivo == nombre) it.copy(archivo = "", hash = "") else it })
    }

    // ---------- respaldo ----------
    fun exportar(ctx: Context): String {
        val (s, c) = leer(ctx)
        return JSONObject()
            .put("app", "EPEN al día").put("version", 2).put("exportado", System.currentTimeMillis())
            .put("suministros", JSONArray().apply { s.forEach { put(it.json()) } })
            .put("comprobantes", JSONArray().apply { c.forEach { put(it.json().put("archivo", "").put("hash", "")) } })
            .toString(2)
    }

    /** Une un respaldo con lo que ya hay. Devuelve (conexiones nuevas, comprobantes procesados). */
    fun importar(ctx: Context, texto: String): Pair<Int, Int> = synchronized(lock) {
        val o = JSONObject(texto)
        val (lista, _) = leer(ctx)
        var nuevas = 0
        val s = o.optJSONArray("suministros") ?: JSONArray()
        for (i in 0 until s.length()) {
            val x = Suministro.de(s.getJSONObject(i))
            if (lista.none { it.clave == x.clave }) { lista.add(x); nuevas++ }
        }
        escribir(ctx, lista, leer(ctx).second)
        val c = o.optJSONArray("comprobantes") ?: JSONArray()
        for (i in 0 until c.length()) registrar(ctx, Comprobante.de(c.getJSONObject(i)))
        nuevas to c.length()
    }
}

/** Totales de los últimos 12 meses de una conexión, contando cada factura una sola vez. */
data class ResumenAnual(
    val facturas: Int,
    val kwh: Int,
    val total: Double,
    val impuestos: Double,
    val alumbrado: Double,
    val intereses: Double,
    val energia: Double,
    val conDesglose: Int,          // facturas de las que se pudo leer el detalle
    val desde: String,
    val hasta: String
) {
    companion object {
        fun de(comps: List<Comprobante>, hoy: java.util.Calendar = java.util.Calendar.getInstance()): ResumenAnual {
            val fmt = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
            val hasta = fmt.format(hoy.time).toLong()
            val inicio = (hoy.clone() as java.util.Calendar).apply { add(java.util.Calendar.YEAR, -1) }
            val desde = fmt.format(inicio.time).toLong()
            val anio = comps.filter { it.orden in (desde + 1)..hasta }
            val vista = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            return ResumenAnual(
                facturas = anio.size,
                kwh = anio.sumOf { it.consumoKwh ?: 0 },
                total = anio.sumOf { it.total ?: 0.0 },
                impuestos = anio.sumOf { it.suma(Categoria.IMPUESTO) },
                alumbrado = anio.sumOf { it.suma(Categoria.ALUMBRADO) },
                intereses = anio.sumOf { it.suma(Categoria.INTERES) },
                energia = anio.sumOf { it.suma(Categoria.ENERGIA) },
                conDesglose = anio.count { it.conceptos.isNotEmpty() },
                desde = vista.format(inicio.time),
                hasta = vista.format(hoy.time)
            )
        }
    }
}

/** Secciones del portal servicios.epen.gov.ar que usa la app. */
enum class Seccion(val menu: String, val titulo: String, val detalle: String) {
    FACTURA("factura", "Factura vigente", "Descargar o pagar la última factura sin vencer"),
    DEUDA("deuda", "Deuda", "Ver facturas vencidas, generar recibos y pagar"),
    ANTERIORES("fact_ant", "Facturas anteriores", "Reimprimir facturas de hasta 2 años"),
    SEGMENTACION("subsidio", "Segmentación tarifaria", "Consultar el nivel de subsidio del suministro");

    val url get() = "https://servicios.epen.gov.ar/index.php?menu=$menu"
}
