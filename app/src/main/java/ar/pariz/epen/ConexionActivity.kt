package ar.pariz.epen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import kotlin.concurrent.thread

/** Ficha de una conexión: resumen del último año, última factura, accesos, historial y comprobantes. */
class ConexionActivity : AppCompatActivity() {

    companion object { const val EXTRA_CLAVE = "clave" }

    private lateinit var clave: String
    private lateinit var cuerpo: LinearLayout
    private var verImporte = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clave = intent.getStringExtra(EXTRA_CLAVE) ?: run { finish(); return }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        cuerpo = vertical().apply { setPadding(0, 0, 0, dp(32)) }
        setContentView(ScrollView(this).apply { addView(cuerpo); isFillViewport = true })
    }

    override fun onResume() { super.onResume(); dibujar() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_CLAVE)?.let { clave = it }
    }
    override fun onSupportNavigateUp(): Boolean { irAlInicio(); return true }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.agregarInicio()
        menu.add(0, 1, 0, "Editar conexión")
        menu.add(0, 2, 1, "Copiar N° de conexión")
        menu.add(0, 3, 2, "Compartir datos")
        menu.add(0, 4, 3, "Volver a leer mis facturas")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == MENU_INICIO) { irAlInicio(); return true }
        val s = Suministros.buscar(this, clave) ?: return true
        when (item.itemId) {
            1 -> Editor.abrir(this, s) { nueva -> if (nueva == null) finish() else { clave = nueva.clave; dibujar() } }
            2 -> copiar(s.numero)
            3 -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, fichaTexto(s)), "Compartir conexión"))
            4 -> releer()
        }
        return true
    }

    private fun copiar(t: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("Conexión EPEN", t))
        Toast.makeText(this, "Copiado: $t", Toast.LENGTH_SHORT).show()
    }

    private fun fichaTexto(s: Suministro) = buildString {
        appendLine("EPEN – ${s.nombre}")
        appendLine("Suministro/Contrato: ${s.numero}")
        if (s.titular.isNotBlank()) appendLine("Titular: ${s.titular}")
        if (s.direccion.isNotBlank()) appendLine("Dirección: ${s.direccion}")
    }.trim()

    private fun releer() {
        Toast.makeText(this, "Leyendo tus facturas…", Toast.LENGTH_SHORT).show()
        thread {
            val n = Descargas.actualizarRegistro(this)
            getSharedPreferences("epen", 0).edit().putInt("registro_version", Registro.VERSION_LECTOR).apply()
            runOnUiThread { dibujar(); Toast.makeText(this, "Listo: $n factura(s) leídas", Toast.LENGTH_SHORT).show() }
        }
    }

    // ===================================================================================
    private fun dibujar() {
        val s = Suministros.buscar(this, clave) ?: run { finish(); return }
        val comps = Suministros.comprobantes(this, clave)
        title = s.nombre
        supportActionBar?.subtitle = null
        cuerpo.removeAllViews()

        cuerpo.addView(cabecera(s))
        cuerpo.addView(resumenAnual(comps))
        ultimaFactura(comps)?.let { cuerpo.addView(it) }

        cuerpo.addView(titulo("Consultar en EPEN"))
        cuerpo.addView(vertical().apply {
            setPadding(dp(16), 0, dp(16), 0)
            addView(grilla(listOf(
                acceso(R.drawable.ic_receipt, "Factura vigente", "Descargar o pagar", true) { abrirSeccion(s, Seccion.FACTURA, comps) },
                acceso(R.drawable.ic_alert, "Deuda", "Vencidas y recibos", false) { abrirSeccion(s, Seccion.DEUDA, comps) },
                acceso(R.drawable.ic_history, "Anteriores", "Hasta 2 años", false) { abrirSeccion(s, Seccion.ANTERIORES, comps) },
                acceso(R.drawable.ic_scale, "Segmentación", "Nivel de subsidio", false) { abrirSeccion(s, Seccion.SEGMENTACION, comps) }
            )))
        })

        historial(comps)?.let { cuerpo.addView(titulo("Historial")); cuerpo.addView(it) }

        cuerpo.addView(titulo("Comprobantes", if (comps.isEmpty()) null else "${comps.size}"))
        if (comps.isEmpty()) {
            cuerpo.addView(tarjeta(vertical(18).apply {
                addView(texto("Todavía no hay comprobantes", 15f, true))
                addView(texto("Se anotan solos cuando consultás el portal o bajás una factura. " +
                    "En \"Anteriores\" podés traer los últimos 2 años con un solo código.", 13f, color = GRIS_TEXTO), lp(4))
            }))
        } else {
            val lista = vertical()
            comps.forEachIndexed { i, c ->
                if (i > 0) lista.addView(separador())
                lista.addView(filaComprobante(s, c))
            }
            cuerpo.addView(tarjeta(lista))
        }
    }

    /** Franja azul que continúa la barra superior: nombre, número y datos de la conexión. */
    /** Fila de chips para saltar entre conexiones sin volver al inicio. */
    private fun selectorConexiones(actual: Suministro): View? {
        val todas = Suministros.listar(this)
        if (todas.size < 2) return null
        val fila = fila().apply { setPadding(dp(16), dp(2), dp(16), dp(10)) }
        for (o in todas) {
            val activa = o.clave == actual.clave
            fila.addView(texto(o.nombre, 13.5f, true, if (activa) AZUL else Color.WHITE).apply {
                setPadding(dp(14), dp(7), dp(14), dp(7))
                maxLines = 1
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(if (activa) Color.WHITE else Color.parseColor("#26FFFFFF"))
                }
                if (!activa) setOnClickListener {
                    clave = o.clave
                    verImporte = false
                    dibujar()
                    (cuerpo.parent as? ScrollView)?.scrollTo(0, 0)
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = dp(8) })
        }
        return android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(AZUL)
            addView(fila)
            post {
                val i = todas.indexOfFirst { it.clave == actual.clave }
                fila.getChildAt(i)?.let { smoothScrollTo((it.left - dp(16)).coerceAtLeast(0), 0) }
            }
        }
    }

    private fun cabecera(s: Suministro): View = vertical().apply {
        setBackgroundColor(AZUL)
        selectorConexiones(s)?.let { addView(it) }
        addView(cabeceraDatos(s))
    }

    private fun cabeceraDatos(s: Suministro): View = vertical().apply {
        setBackgroundColor(AZUL)
        setPadding(dp(20), dp(4), dp(20), dp(22))
        addView(texto("N° de suministro / contrato", 12f, color = Color.parseColor("#B3FFFFFF")))
        addView(fila().apply {
            addView(texto(s.numero, 26f, true, Color.WHITE),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(fila().apply {
                setPadding(dp(12), dp(6), dp(12), dp(6))
                background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(Color.parseColor("#26FFFFFF")) }
                addView(icono(R.drawable.ic_copy, Color.WHITE, 16))
                addView(texto("Copiar", 13f, true, Color.WHITE).apply { setPadding(dp(6), 0, 0, 0) })
                setOnClickListener { copiar(s.numero) }
            })
        })
        val datos = listOf(s.titular, s.direccion).filter { it.isNotBlank() }.joinToString(" · ")
        if (datos.isNotBlank()) addView(texto(datos, 14f, color = Color.parseColor("#E6FFFFFF")), lp(6))
        if (s.notas.isNotBlank()) addView(texto(s.notas, 12.5f, color = Color.parseColor("#B3FFFFFF")), lp(2))
    }

    /** Totales de los últimos 12 meses: lo pagado, los kWh y los rubros que pidió el usuario. */
    private fun resumenAnual(comps: List<Comprobante>): View {
        val r = ResumenAnual.de(comps)
        val contenido = vertical(18).apply {
            addView(fila().apply {
                addView(texto("Último año", 16f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(texto("${r.desde.take(5)}/${r.desde.takeLast(2)} – ${r.hasta.take(5)}/${r.hasta.takeLast(2)}", 12f, color = GRIS_TEXTO))
            })
            if (r.facturas == 0) {
                addView(texto("Cuando tengas facturas de los últimos 12 meses, acá vas a ver cuánto pagaste en total, " +
                    "los kWh consumidos y cuánto fue de impuestos, alumbrado público e intereses.", 13f, color = GRIS_TEXTO), lp(6))
                return@apply
            }
            val anio = ResumenAnual.delPeriodo(comps)
            addView(vertical().apply {
                setPadding(0, dp(10), 0, dp(4))
                addView(texto("Total facturado", 12.5f, color = GRIS_TEXTO))
                addView(texto(pesos(r.total), 30f, true))
                addView(texto("${r.facturas} factura${if (r.facturas == 1) "" else "s"} en el período · ver desglose ›", 12.5f, color = AZUL))
                tocable { desgloseTotal(anio, r) }
            })

            val pct = { v: Double -> if (r.total > 0 && v > 0) "${"%.1f".format(v / r.total * 100).replace('.', ',')}% del total" else null }
            val violeta = Color.parseColor("#6A3FB5")
            val ocre = Color.parseColor("#8A6400")
            addView(grilla(listOf(
                mosaico(R.drawable.ic_bolt, AZUL, AZUL_CLARO, "${miles(r.kwh)} kWh", "Energía consumida",
                    if (r.facturas > 0) "≈ ${miles(r.kwh / r.facturas)} kWh por factura" else null)
                    .tocable { desgloseKwh(anio) },
                mosaico(R.drawable.ic_percent, violeta, Color.parseColor("#EFE8FA"),
                    pesosCorto(r.impuestos), "Impuestos y tasas", pct(r.impuestos))
                    .tocable { desgloseRubro(Categoria.IMPUESTO, violeta, anio) },
                mosaico(R.drawable.ic_lamp, ocre, AMARILLO_CLARO,
                    pesosCorto(r.alumbrado), "Alumbrado público", pct(r.alumbrado))
                    .tocable { desgloseRubro(Categoria.ALUMBRADO, ocre, anio) },
                mosaico(R.drawable.ic_clock, if (r.intereses > 0) ROJO else VERDE, if (r.intereses > 0) ROJO_CLARO else VERDE_CLARO,
                    pesosCorto(r.intereses), "Intereses y recargos", pct(r.intereses) ?: "Sin intereses 👍")
                    .tocable { desgloseRubro(Categoria.INTERES, if (r.intereses > 0) ROJO else VERDE, anio) }
            )), lp(14))
            addView(texto("Tocá cada cuadro para ver el detalle", 11.5f, color = GRIS_TEXTO).apply {
                gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0)
            })

            if (r.conDesglose < r.facturas) addView(texto(
                if (r.conDesglose == 0)
                    "Impuestos, alumbrado e intereses salen del detalle de cada PDF. Bajá las facturas en PDF " +
                        "(o tocá ⋮ → Volver a leer mis facturas) para completarlos."
                else "El detalle se leyó de ${r.conDesglose} de ${r.facturas} facturas; los rubros pueden estar incompletos.",
                12f, color = GRIS_TEXTO
            ), lp(10))
        }
        return tarjeta(contenido, 0).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = -dp(10) // se apoya sobre la franja azul
        }
    }

    // ---------------- desgloses del último año ----------------

    private fun <T : View> T.tocable(accion: () -> Unit): T = apply {
        isClickable = true
        isFocusable = true
        foreground = context.getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener { accion() }
    }

    /** Barra horizontal proporcional para las listas de desglose. */
    private fun barra(fraccion: Double, color: Int): View = LinearLayout(this).apply {
        background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(Color.parseColor("#EEF2F6")) }
        addView(View(context).apply {
            background = GradientDrawable().apply { cornerRadius = dp(3).toFloat(); setColor(color) }
        }, LinearLayout.LayoutParams(0, dp(6), fraccion.toFloat().coerceIn(0.02f, 1f)))
        addView(View(context), LinearLayout.LayoutParams(0, dp(6), (1 - fraccion).toFloat().coerceIn(0f, 0.98f)))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6)).apply { topMargin = dp(4) }
    }

    private fun hoja(armar: LinearLayout.(BottomSheetDialog) -> Unit) {
        val h = BottomSheetDialog(this)
        val v = vertical(20)
        v.armar(h)
        h.setContentView(ScrollView(this).apply { addView(v) })
        h.behavior.peekHeight = resources.displayMetrics.heightPixels * 3 / 4
        h.show()
    }

    private fun LinearLayout.encabezadoHoja(icono: Int, color: Int, tituloT: String, valor: String, bajada: String) {
        addView(fila().apply {
            addView(iconoCircular(icono, color, Color.parseColor("#F1F4F8"), 40))
            addView(vertical().apply {
                setPadding(dp(12), 0, 0, 0)
                addView(texto(tituloT, 13f, color = GRIS_TEXTO))
                addView(texto(valor, 24f, true))
            })
        })
        addView(texto(bajada, 12.5f, color = GRIS_TEXTO), lp(6))
    }

    /** Fila de una factura dentro de un desglose: mes, número, valor y barra. Tocándola abre su detalle. */
    private fun LinearLayout.filaFactura(h: BottomSheetDialog, c: Comprobante, valor: String, fraccion: Double, color: Int, nota: String? = null) {
        val s = Suministros.buscar(context, clave) ?: return
        addView(vertical().apply {
            setPadding(0, dp(10), 0, dp(10))
            addView(fila().apply {
                addView(texto(mesCorto(c.vencimiento), 14f, true), LinearLayout.LayoutParams(dp(64), LinearLayout.LayoutParams.WRAP_CONTENT))
                addView(texto(nota ?: c.numero, 12.5f, color = GRIS_TEXTO).apply { maxLines = 1 },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(texto(valor, 14f, true))
                addView(icono(R.drawable.ic_chevron, GRIS_TEXTO, 16))
            })
            addView(barra(fraccion, color))
            tocable { h.dismiss(); detalle(s, c) }
        })
    }

    private fun desgloseRubro(cat: Categoria, color: Int, anio: List<Comprobante>) = hoja { h ->
        val total = anio.sumOf { it.suma(cat) }
        val icono = when (cat) {
            Categoria.IMPUESTO -> R.drawable.ic_percent; Categoria.ALUMBRADO -> R.drawable.ic_lamp
            Categoria.INTERES -> R.drawable.ic_clock; else -> R.drawable.ic_receipt
        }
        val conDetalle = anio.count { it.conceptos.isNotEmpty() }
        encabezadoHoja(icono, color, "${cat.titulo} · último año", pesos(total),
            "Suma de ${anio.size} factura${if (anio.size == 1) "" else "s"}" +
                (if (conDetalle < anio.size) " (detalle leído de $conDetalle)" else "") +
                (anio.sumOf { it.total ?: 0.0 }.takeIf { it > 0 && total > 0 }?.let { " · ${"%.1f".format(total / it * 100).replace('.', ',')}% de lo facturado" } ?: ""))

        // por concepto
        val porConcepto = anio.flatMap { it.conceptos }.filter { it.categoria == cat }
            .groupBy { it.nombre.lowercase().replace(Regex("""\s+"""), " ") }
            .map { (_, l) -> l.first().nombre to l.sumOf { it.importe } }
            .sortedByDescending { it.second }
        addView(texto("Por concepto", 15f, true), lp(18))
        if (porConcepto.isEmpty()) {
            addView(texto(
                if (conDetalle == 0) "Todavía no hay facturas en PDF con detalle en este período. Bajalas desde \"Anteriores\" con \"Bajar todo\"."
                else "No se encontraron conceptos de este rubro en las facturas leídas. Si tu factura sí los tiene, " +
                    "abrí una factura y tocá \"Compartir texto leído\" para ajustar la lectura.",
                13f, color = GRIS_TEXTO), lp(6))
        } else porConcepto.forEach { (nombre, imp) ->
            addView(vertical().apply {
                setPadding(0, dp(8), 0, dp(4))
                addView(fila().apply {
                    addView(texto(nombre, 13.5f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(texto(pesos(imp), 13.5f, true))
                })
                addView(barra(if (total > 0) imp / total else 0.0, color))
            })
        }

        // por factura
        addView(texto("Por factura", 15f, true), lp(20))
        val max = anio.maxOfOrNull { it.suma(cat) }?.takeIf { it > 0 } ?: 1.0
        anio.forEachIndexed { i, c ->
            if (i > 0) addView(separador())
            val v = c.suma(cat)
            filaFactura(h, c, if (c.conceptos.isEmpty()) "sin detalle" else pesos(v), v / max, color,
                c.conceptos.filter { it.categoria == cat }.joinToString(", ") { it.nombre }.ifBlank { null })
        }
    }

    private fun desgloseKwh(anio: List<Comprobante>) = hoja { h ->
        val total = anio.sumOf { it.consumoKwh ?: 0 }
        val con = anio.filter { it.consumoKwh != null }
        encabezadoHoja(R.drawable.ic_bolt, AZUL, "Energía consumida · último año", "${miles(total)} kWh",
            if (con.isEmpty()) "Sin datos de consumo todavía"
            else "Promedio ${miles(total / con.size)} kWh por factura · máximo ${miles(con.maxOf { it.consumoKwh!! })} · mínimo ${miles(con.minOf { it.consumoKwh!! })}")
        val energia = anio.sumOf { it.suma(Categoria.ENERGIA) }
        if (energia > 0 && total > 0) addView(texto(
            "Energía y cargos fijos: ${pesos(energia)} en el año (≈ ${pesos(energia / total)} por kWh, con cargos fijos incluidos)",
            12.5f, color = GRIS_TEXTO), lp(4))
        addView(texto("Por factura", 15f, true), lp(18))
        val max = con.maxOfOrNull { it.consumoKwh!! }?.toDouble() ?: 1.0
        anio.forEachIndexed { i, c ->
            if (i > 0) addView(separador())
            filaFactura(h, c, c.consumoKwh?.let { "${miles(it)} kWh" } ?: "—", (c.consumoKwh ?: 0) / max, AZUL,
                c.periodo.takeIf { it.isNotBlank() }?.let { "Período $it" })
        }
    }

    private fun desgloseTotal(anio: List<Comprobante>, r: ResumenAnual) = hoja { h ->
        encabezadoHoja(R.drawable.ic_receipt, AZUL, "Total facturado · último año", pesos(r.total),
            "${anio.size} facturas, del ${r.desde} al ${r.hasta}")
        addView(texto("En qué se fue", 15f, true), lp(18))
        val colores = mapOf(Categoria.ENERGIA to AZUL, Categoria.IMPUESTO to Color.parseColor("#6A3FB5"),
            Categoria.ALUMBRADO to AMARILLO, Categoria.INTERES to ROJO, Categoria.OTRO to GRIS_TEXTO)
        val conDetalle = anio.filter { it.conceptos.isNotEmpty() }
        val baseDetalle = conDetalle.sumOf { it.total ?: 0.0 }
        if (conDetalle.isEmpty()) addView(texto("Hace falta el detalle de las facturas en PDF para separar por rubro.", 13f, color = GRIS_TEXTO), lp(6))
        else {
            for (cat in Categoria.values()) {
                val v = conDetalle.sumOf { it.suma(cat) }
                if (v == 0.0) continue
                addView(vertical().apply {
                    setPadding(0, dp(8), 0, dp(4))
                    addView(fila().apply {
                        addView(texto(cat.titulo, 13.5f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                        addView(texto(pesos(v), 13.5f, true))
                    })
                    addView(barra(if (baseDetalle > 0) v / baseDetalle else 0.0, colores[cat]!!))
                    if (cat != Categoria.ENERGIA && cat != Categoria.OTRO) tocable { h.dismiss(); desgloseRubro(cat, colores[cat]!!, anio) }
                })
            }
            val suma = conDetalle.sumOf { c -> c.conceptos.sumOf { it.importe } }
            if (baseDetalle > 0 && kotlin.math.abs(suma - baseDetalle) / baseDetalle > 0.05) addView(texto(
                "Atención: la suma de los conceptos leídos (${pesos(suma)}) no coincide con el total de esas facturas " +
                    "(${pesos(baseDetalle)}). Es probable que algún renglón se haya leído mal.", 12f, color = ROJO), lp(8))
        }
        addView(texto("Por factura", 15f, true), lp(20))
        val max = anio.maxOfOrNull { it.total ?: 0.0 }?.takeIf { it > 0 } ?: 1.0
        anio.forEachIndexed { i, c ->
            if (i > 0) addView(separador())
            filaFactura(h, c, c.total?.let { pesos(it) } ?: "—", (c.total ?: 0.0) / max, AZUL)
        }
    }

    private fun ultimaFactura(comps: List<Comprobante>): View? {
        val u = comps.firstOrNull { it.total != null || it.vencimiento.isNotBlank() } ?: return null
        return tarjeta(vertical(18).apply {
            addView(fila().apply {
                addView(texto("Última factura", 16f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                chipVencimiento(u.vencimiento)?.let { addView(it) }
            })
            addView(texto(u.total?.let { pesos(it) } ?: "Importe sin leer", 26f, true), lp(8))
            addView(texto(listOfNotNull(
                "N° ${u.numero}",
                u.vencimiento.takeIf { it.isNotBlank() }?.let { "vence $it" },
                u.consumoKwh?.let { "$it kWh" }
            ).joinToString(" · "), 13f, color = GRIS_TEXTO))
            addView(boton("Ver detalle", relleno = false) { detalle(Suministros.buscar(this@ConexionActivity, clave)!!, u) }, lp(12))
        }, 10)
    }

    private fun historial(comps: List<Comprobante>): View? {
        val datos = comps.filter { it.orden > 0 }.sortedBy { it.orden }
        val kwh = datos.filter { it.consumoKwh != null }.map { mesCorto(it.vencimiento) to it.consumoKwh!!.toDouble() }
        val importes = datos.filter { it.total != null }.map { mesCorto(it.vencimiento) to it.total!! }
        if (kwh.size < 2 && importes.size < 2) return null
        if (kwh.size < 2) verImporte = true

        val resumen = texto("", 13f, color = GRIS_TEXTO)
        val grafico = GraficoView(this)
        fun pintar() {
            val serie = if (verImporte) importes else kwh
            val fmt: (Double) -> String = if (verImporte) { v -> pesos(v) } else { v -> "${miles(v.toInt())} kWh" }
            grafico.mostrar(serie, fmt)
            val ult = serie.last().second
            val prom = serie.map { it.second }.average()
            val dif = if (prom > 0) (ult - prom) / prom * 100 else 0.0
            resumen.text = "Última ${fmt(ult)} · promedio ${fmt(prom)} (${if (dif >= 0) "+" else "−"}${"%.0f".format(kotlin.math.abs(dif))}%)"
        }

        val selector = MaterialButtonToggleGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
            val b1 = boton("Consumo (kWh)", relleno = false) {}.apply { id = View.generateViewId(); isEnabled = kwh.size >= 2 }
            val b2 = boton("Importe ($)", relleno = false) {}.apply { id = View.generateViewId(); isEnabled = importes.size >= 2 }
            addView(b1); addView(b2)
            check(if (verImporte) b2.id else b1.id)
            addOnButtonCheckedListener { _, id, marcado -> if (marcado) { verImporte = id == b2.id; pintar() } }
        }
        pintar()
        return tarjeta(vertical(18).apply {
            addView(selector, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(resumen, lp(10))
            addView(grafico, lp(8))
            addView(fila().apply {
                setPadding(0, dp(6), 0, 0)
                addView(View(context).apply { background = GradientDrawable().apply { setColor(AMARILLO) } },
                    LinearLayout.LayoutParams(dp(14), dp(3)))
                addView(texto("  promedio · tocá una barra para ver su valor", 11.5f, color = GRIS_TEXTO))
            })
        })
    }

    private fun filaComprobante(s: Suministro, c: Comprobante): View = fila().apply {
        setPadding(dp(16), dp(12), dp(12), dp(12))
        isClickable = true
        foreground = context.getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener { detalle(s, c) }

        addView(vertical().apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(AZUL_CLARO) }
            val m = mesCorto(c.vencimiento).split(" ")
            addView(texto(m.getOrElse(0) { "—" }, 13f, true, AZUL).apply { gravity = Gravity.CENTER })
            addView(texto(m.getOrElse(1) { "" }, 11f, color = AZUL).apply { gravity = Gravity.CENTER })
        }, LinearLayout.LayoutParams(dp(46), dp(46)))

        addView(vertical().apply {
            setPadding(dp(12), 0, dp(8), 0)
            addView(texto(c.numero, 14.5f, true).apply { maxLines = 1 })
            addView(texto(listOfNotNull(
                c.consumoKwh?.let { "$it kWh" },
                if (c.archivo.isNotBlank()) "PDF" else "visto en portal",
                if (s.ultimaFactura.isNotBlank() && c.tieneNumero(s.ultimaFactura)) "★ referencia" else null
            ).joinToString(" · "), 12f, color = GRIS_TEXTO))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        addView(texto(c.total?.let { pesos(it) } ?: "", 14.5f, true))
        addView(icono(R.drawable.ic_chevron, GRIS_TEXTO, 18).apply {
            (layoutParams as LinearLayout.LayoutParams).marginStart = dp(4)
        })
    }

    /** Hoja inferior con el desglose de una factura y sus acciones. */
    private fun detalle(s: Suministro, c: Comprobante) {
        val hoja = BottomSheetDialog(this)
        val v = vertical(20)
        v.addView(fila().apply {
            addView(vertical().apply {
                addView(texto("Factura ${mesCorto(c.vencimiento)}", 13f, color = GRIS_TEXTO))
                addView(texto(c.total?.let { pesos(it) } ?: "Importe sin leer", 26f, true))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            chipVencimiento(c.vencimiento)?.let { addView(it) }
        })
        val datos = listOfNotNull(
            "N° ${c.numero}", c.otroNumero.takeIf { it.isNotBlank() }?.let { "N° $it" },
            c.vencimiento.takeIf { it.isNotBlank() }?.let { "Vencimiento $it" },
            c.periodo.takeIf { it.isNotBlank() }?.let { "Período $it" },
            c.consumoKwh?.let { "Consumo $it kWh" }
        )
        datos.forEach { v.addView(texto(it, 13.5f, color = GRIS_TEXTO), v.lp(2)) }

        if (c.conceptos.isNotEmpty()) {
            v.addView(texto("Detalle", 15f, true), v.lp(16))
            for (cat in Categoria.values()) {
                val items = c.conceptos.filter { it.categoria == cat }
                if (items.isEmpty()) continue
                v.addView(fila().apply {
                    setPadding(0, dp(10), 0, dp(2))
                    addView(texto(cat.titulo, 13f, true, AZUL), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(texto(pesos(items.sumOf { it.importe }), 13f, true, AZUL))
                })
                items.forEach { k ->
                    v.addView(fila().apply {
                        setPadding(dp(8), dp(2), 0, dp(2))
                        addView(texto(k.nombre, 13f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                        addView(texto(pesos(k.importe), 13f, color = GRIS_TEXTO))
                    })
                }
            }
            v.addView(texto("Detalle leído automáticamente del PDF; ante cualquier duda, el PDF es el que vale.",
                11.5f, color = GRIS_TEXTO), v.lp(10))
        } else if (c.archivo.isBlank()) {
            v.addView(texto("Para ver el detalle (impuestos, alumbrado, intereses) bajá esta factura en PDF desde " +
                "\"Anteriores\" o \"Factura vigente\".", 13f, color = GRIS_TEXTO), v.lp(14))
        }

        val f = File(Descargas.carpeta(this), c.archivo)
        val acciones = vertical().apply { setPadding(0, dp(16), 0, 0) }
        if (c.archivo.isNotBlank() && f.exists()) {
            acciones.addView(fila().apply {
                addView(boton("Abrir PDF") { Descargas.abrir(this@ConexionActivity, f) },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(boton("Compartir", relleno = false) { Descargas.compartir(this@ConexionActivity, f) },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
            })
        }
        if (Suministros.facturaValida(c.numero) && c.numero != s.ultimaFactura) acciones.addView(
            boton("Usar como referencia para Anteriores", relleno = false) {
                Suministros.guardar(this, s.copy(ultimaFactura = c.numero), s.clave)
                Toast.makeText(this, "Se usará ${c.numero} en Facturas anteriores", Toast.LENGTH_SHORT).show()
                hoja.dismiss(); dibujar()
            }, acciones.lp(6))
        if (c.archivo.isNotBlank() && f.exists()) acciones.addView(
            boton("Compartir texto leído (para soporte)", relleno = false) { compartirTexto(f) }, acciones.lp(6))
        v.addView(acciones)

        hoja.setContentView(ScrollView(this).apply { addView(v) })
        hoja.show()
    }

    /** Comparte el texto que la app leyó del PDF, para calibrar la lectura si algo salió mal. */
    private fun compartirTexto(f: File) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Compartir texto leído")
            .setMessage("Se comparte el texto de esta factura tal como lo leyó la app (incluye tus datos de la factura). " +
                "Sirve para reportar una lectura incorrecta.")
            .setPositiveButton("Compartir") { _, _ ->
                thread {
                    val t = Lector.textoDePdf(this, f).ifBlank { "(No se pudo leer texto de este PDF)" }
                    runOnUiThread {
                        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, t), "Compartir texto"))
                    }
                }
            }
            .setNegativeButton("Cancelar", null).show()
    }

    /** En "Anteriores" EPEN pide un comprobante que no sea el último: se ofrece elegirlo. */
    private fun abrirSeccion(s: Suministro, sec: Seccion, comps: List<Comprobante>) {
        if (sec != Seccion.ANTERIORES) { lanzar(s, sec, s.ultimaFactura); return }
        val opciones = comps.flatMap { listOf(it.numero, it.otroNumero) }.filter { it.isNotBlank() && Suministros.facturaValida(it) }.distinct()
        if (opciones.size <= 1) { lanzar(s, sec, s.ultimaFactura.ifBlank { opciones.firstOrNull().orEmpty() }); return }
        val etiquetas = (opciones.map { n ->
            val c = comps.first { it.tieneNumero(n) }
            n + (c.vencimiento.takeIf { it.isNotBlank() }?.let { "  (vence $it)" } ?: "")
        } + "Sin referencia (lo escribo yo)").toTypedArray()
        val elegido = opciones.indexOf(s.ultimaFactura).takeIf { it >= 0 } ?: opciones.size.coerceAtMost(1)
        var sel = elegido
        MaterialAlertDialogBuilder(this)
            .setTitle("Comprobante de referencia")
            .setSingleChoiceItems(etiquetas, elegido) { _, w -> sel = w }
            .setPositiveButton("Consultar") { _, _ -> lanzar(s, sec, if (sel < opciones.size) opciones[sel] else "") }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun lanzar(s: Suministro, sec: Seccion, factura: String) {
        startActivity(Intent(this, PortalActivity::class.java)
            .putExtra(PortalActivity.EXTRA_SECCION, sec.name)
            .putExtra(PortalActivity.EXTRA_SUMINISTRO, s.numero)
            .putExtra(PortalActivity.EXTRA_ALIAS, s.alias)
            .putExtra(PortalActivity.EXTRA_FACTURA, factura))
    }
}
