package ar.pariz.epen

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

/** Rubros en los que se agrupan los conceptos de una factura. */
enum class Categoria(val titulo: String) {
    IMPUESTO("Impuestos y tasas"), ALUMBRADO("Alumbrado público"), INTERES("Intereses y recargos"),
    ENERGIA("Energía y cargos"), OTRO("Otros")
}

/** Un renglón de la factura: "Alumbrado Público  1.234,56". */
data class Concepto(val nombre: String, val importe: Double, val categoria: Categoria)

/** Datos que se pudieron leer de una factura o de una página del portal. */
data class DatosLeidos(
    val factura: String? = null,      // 0001-00003217
    val liquidacion: String? = null,  // 00007-03125034
    val vencimiento: String = "",
    val total: Double? = null,
    val consumoKwh: Int? = null,
    val periodo: String = "",
    val conceptos: List<Concepto> = emptyList()
) {
    /** El número que mejor identifica al comprobante (el portal acepta cualquiera de los dos). */
    val numero: String? get() = factura ?: liquidacion
}

/**
 * Lectura de facturas del EPEN. El formato no es público, así que las reglas son
 * deliberadamente tolerantes: si un dato no aparece claro, queda vacío en vez de inventarse.
 */
object Lector {

    private val reFactura = Regex("""(?<!\d)(\d{4})-(\d{8})(?!\d)""")
    private val reLiquidacion = Regex("""(?<!\d)(\d{5})-(\d{8})(?!\d)""")
    private val reFecha = Regex("""(?<!\d)(\d{2})/(\d{2})/(\d{4})(?!\d)""")
    private val reVenc = Regex("""venc\w*\.?[^0-9]{0,60}?(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
    private val reTotal = Regex(
        """total\s*(?:a\s*pagar|factura|liquidaci\S*|general)?[^0-9\n]{0,25}?\$?\s*(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})""",
        RegexOption.IGNORE_CASE
    )
    private val reKwh = Regex("""(\d{1,3}(?:\.\d{3})+|\d{1,6})\s*kwh""", RegexOption.IGNORE_CASE)
    private val reConsumo = Regex(
        """consumo[^0-9\n]{0,40}?(\d{1,3}(?:\.\d{3})+|\d{1,6})(?:\s*kwh)?""", RegexOption.IGNORE_CASE
    )
    private val rePeriodo = Regex(
        """per[ií]odo[^0-9\n]{0,25}?(\d{2}/\d{2}/\d{4}\s*(?:al|a|-)\s*\d{2}/\d{2}/\d{4}|\d{1,2}/\d{4}|\d{2}/\d{2}/\d{4})""",
        RegexOption.IGNORE_CASE
    )

    fun importe(s: String): Double? = s.replace(".", "").replace(",", ".").toDoubleOrNull()

    /** Lee una sola factura (texto de un PDF). */
    fun leerFactura(texto: String): DatosLeidos {
        val t = texto.replace(' ', ' ')
        val factura = reFactura.find(t)?.value
        val liq = reLiquidacion.find(t)?.value
        val venc = reVenc.find(t)?.groupValues?.get(1) ?: ""
        // El total a pagar suele ser el último "Total" del comprobante.
        val total = reTotal.findAll(t).mapNotNull { importe(it.groupValues[1]) }.filter { it > 0 }.lastOrNull()
        val kwh = (reConsumo.findAll(t) + reKwh.findAll(t))
            .mapNotNull { it.groupValues[1].replace(".", "").toIntOrNull() }
            .firstOrNull { it in 1..200_000 }
        val periodo = rePeriodo.find(t)?.groupValues?.get(1)?.replace(Regex("\\s+"), " ") ?: ""
        return DatosLeidos(factura, liq, venc, total, kwh, periodo, conceptos(t))
    }

    // ---------- conceptos ----------
    private val reRenglon = Regex(
        """^(.*?\p{L}.*?)\s+\$?\s*(-?\d{1,3}(?:\.\d{3})*,\d{2}|-?\d+,\d{2})\s*$"""
    )
    private val reExcluir = Regex(
        """total|subtotal|saldo|a\s*pagar|vencim|venc\.|importe\s+en\s+letras|son\s+pesos|""" +
            """per[ií]odo|fecha|kwh|lectura|medidor|factor|tarifa\s+[a-z0-9]|cuit|c\.?u\.?i\.?t|n[°º]|hoja|""" +
            """neto|gravad|imponible|exent|hist[oó]ric|promedio|anterior|cuota\s+\d|\bcae\b|c[oó]d\.?\s*barra|""" +
            """redondeo|importe\s+(total|neto)|monto\s+total""",
        RegexOption.IGNORE_CASE
    )
    private val reAlumbrado = Regex("""alumb|a\.\s?p\.\s?(?:munic|pub)""", RegexOption.IGNORE_CASE)
    private val reInteres = Regex("""inter[eé]s|recargo|punitorio|mora\b|actualizaci[oó]n""", RegexOption.IGNORE_CASE)
    private val reImpuesto = Regex(
        """\biva\b|i\.v\.a|impuesto|ingresos\s*brutos|ii\.?\s*bb|\btasa|\bley\b|fondo|contribuci|percepci|""" +
            """municipal|provincial|tribut|\bfnee\b|\bfeesp\b|\bcammesa\b|gravamen|retenci""",
        RegexOption.IGNORE_CASE
    )
    private val reEnergia = Regex("""energ|cargo|consumo|potencia|bonificaci|subsidio|descuento|segmentaci""", RegexOption.IGNORE_CASE)

    fun categoria(nombre: String): Categoria = when {
        reAlumbrado.containsMatchIn(nombre) -> Categoria.ALUMBRADO
        reInteres.containsMatchIn(nombre) -> Categoria.INTERES
        reImpuesto.containsMatchIn(nombre) -> Categoria.IMPUESTO
        reEnergia.containsMatchIn(nombre) -> Categoria.ENERGIA
        else -> Categoria.OTRO
    }

    /**
     * Toma los renglones "descripción ... importe". El talón de pago suele repetir
     * renglones, así que cada concepto se cuenta una sola vez.
     */
    fun conceptos(texto: String): List<Concepto> {
        val vistos = linkedMapOf<String, Concepto>()
        for (linea in texto.lines()) {
            val m = reRenglon.find(linea.trim()) ?: continue
            var nombre = m.groupValues[1].replace(Regex("""\s+"""), " ").trim()
            // quita cantidades y precios unitarios que quedan pegados al nombre
            nombre = nombre.replace(Regex("""(\s+(-?[\d.,%$]+|kwh|kw|unid\.?|\$))+$""", RegexOption.IGNORE_CASE), "")
                .trim(' ', ':', '-', '$')
            if (nombre.length < 3 || reExcluir.containsMatchIn(nombre)) continue
            val importe = importe(m.groupValues[2]) ?: continue
            if (importe == 0.0) continue
            val clave = nombre.lowercase() + "|" + importe
            if (clave !in vistos) vistos[clave] = Concepto(nombre.take(60), importe, categoria(nombre))
        }
        return vistos.values.toList()
    }

    /**
     * Lee una página de resultados del portal, que puede listar varios comprobantes.
     * Toma cada número de comprobante y busca vencimiento e importe en su misma fila.
     */
    fun leerPagina(texto: String): List<DatosLeidos> {
        val filas = texto.replace(' ', ' ').lines().map { it.trim() }.filter { it.isNotEmpty() }
        val out = linkedMapOf<String, DatosLeidos>()
        filas.forEachIndexed { i, fila ->
            val nums = reLiquidacion.findAll(fila).map { it.value }.toList() +
                reFactura.findAll(fila).map { it.value }.filter { f -> reLiquidacion.findAll(fila).none { it.value.endsWith(f) } }
            if (nums.isEmpty()) return@forEachIndexed
            // Primero la misma fila; si no está ahí, las dos siguientes que no sean otro comprobante.
            val siguientes = filas.drop(i + 1).take(2).takeWhile { !reFactura.containsMatchIn(it) && !reLiquidacion.containsMatchIn(it) }
            val reMonto = Regex("""\$\s*(\d{1,3}(?:\.\d{3})*,\d{2}|\d+,\d{2})""")
            val fecha = reFecha.findAll(fila).lastOrNull()?.value
                ?: siguientes.firstNotNullOfOrNull { reFecha.find(it)?.value } ?: ""
            val monto = (reMonto.find(fila) ?: siguientes.firstNotNullOfOrNull { reMonto.find(it) })
                ?.groupValues?.get(1)?.let(::importe)
            for (n in nums.distinct()) {
                if (n in out) continue
                val esLiq = reLiquidacion.matches(n)
                out[n] = DatosLeidos(
                    factura = if (esLiq) null else n,
                    liquidacion = if (esLiq) n else null,
                    vencimiento = fecha, total = monto
                )
            }
        }
        return out.values.toList()
    }

    private var iniciado = false

    /** Extrae el texto de un PDF. Devuelve "" si no se puede leer (PDF escaneado, dañado, etc.). */
    fun textoDePdf(ctx: Context, f: File): String = try {
        if (!iniciado) { PDFBoxResourceLoader.init(ctx.applicationContext); iniciado = true }
        PDDocument.load(f).use { doc -> PDFTextStripper().apply { sortByPosition = true }.getText(doc) }
    } catch (e: Throwable) { "" }
}
