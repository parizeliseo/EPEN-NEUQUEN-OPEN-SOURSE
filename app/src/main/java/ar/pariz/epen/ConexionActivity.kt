package ar.pariz.epen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.concurrent.thread

/** Ficha de una conexión: datos, accesos al portal, resumen, consumo y comprobantes registrados. */
class ConexionActivity : AppCompatActivity() {

    companion object { const val EXTRA_CLAVE = "clave" }

    private lateinit var clave: String
    private lateinit var cuerpo: LinearLayout
    private val pesos = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clave = intent.getStringExtra(EXTRA_CLAVE) ?: run { finish(); return }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        cuerpo = vertical().apply { setPadding(0, dp(6), 0, dp(32)) }
        setContentView(ScrollView(this).apply { addView(cuerpo) })
    }

    override fun onResume() { super.onResume(); dibujar() }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Editar")
        menu.add(0, 2, 1, "Copiar N° de conexión")
        menu.add(0, 3, 2, "Compartir datos")
        menu.add(0, 4, 3, "Volver a leer mis PDF")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val s = Suministros.buscar(this, clave) ?: return true
        when (item.itemId) {
            1 -> Editor.abrir(this, s) { nueva ->
                if (nueva == null) finish() else { clave = nueva.clave; dibujar() }
            }
            2 -> copiar(s.numero)
            3 -> startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, fichaTexto(s)), "Compartir conexión"))
            4 -> releerPdfs()
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

    private fun releerPdfs() {
        Toast.makeText(this, "Leyendo PDF…", Toast.LENGTH_SHORT).show()
        thread {
            for (c in Suministros.comprobantes(this, clave).filter { it.archivo.isNotBlank() }) {
                val f = File(Descargas.carpeta(this), c.archivo)
                if (!f.exists()) continue
                val d = Lector.leerFactura(Lector.textoDePdf(this, f))
                Suministros.registrar(this, c.copy(
                    vencimiento = d.vencimiento, total = d.total, consumoKwh = d.consumoKwh, periodo = d.periodo
                ))
            }
            val extra = Descargas.reindexar(this)
            runOnUiThread {
                dibujar()
                Toast.makeText(this, if (extra > 0) "Listo, $extra PDF nuevos registrados" else "Listo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dibujar() {
        val s = Suministros.buscar(this, clave) ?: run { finish(); return }
        val comps = Suministros.comprobantes(this, clave)
        title = s.nombre
        supportActionBar?.subtitle = "Sum./Contrato ${s.numero}"
        cuerpo.removeAllViews()

        // ----- datos de la conexión
        cuerpo.addView(tarjeta(vertical(16).apply {
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(vertical().apply {
                    addView(texto("N° de conexión", 12f, color = GRIS_TEXTO))
                    addView(texto(s.numero, 24f, true, AZUL))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(boton("Copiar", relleno = false) { copiar(s.numero) })
            })
            if (s.titular.isNotBlank()) addView(texto("Titular: ${s.titular}", 14f), lp(8))
            if (s.direccion.isNotBlank()) addView(texto("Dirección: ${s.direccion}", 14f), lp(2))
            if (s.notas.isNotBlank()) addView(texto(s.notas, 13f, color = GRIS_TEXTO), lp(6))
            if (s.titular.isBlank() && s.direccion.isBlank())
                addView(boton("Completar titular y dirección", relleno = false) {
                    Editor.abrir(this@ConexionActivity, s) { dibujar() }
                }, lp(8))
        }))

        // ----- resumen de la última factura conocida
        val ultima = comps.firstOrNull { it.total != null || it.vencimiento.isNotBlank() }
        if (ultima != null) cuerpo.addView(tarjeta(vertical(16).apply {
            addView(texto("Última factura registrada", 12f, color = GRIS_TEXTO))
            addView(texto(ultima.total?.let { pesos.format(it) } ?: "Importe sin leer", 26f, true))
            val det = listOfNotNull(
                "N° ${ultima.numero}",
                ultima.vencimiento.takeIf { it.isNotBlank() }?.let { "vence $it" },
                ultima.consumoKwh?.let { "$it kWh" }
            ).joinToString(" · ")
            addView(texto(det, 13f, color = GRIS_TEXTO))
        }))

        // ----- accesos al portal
        cuerpo.addView(tarjeta(vertical(16).apply {
            addView(texto("Consultar en EPEN", 15f, true))
            for (sec in Seccion.values()) {
                addView(boton(sec.titulo, relleno = sec == Seccion.FACTURA) { abrirSeccion(s, sec, comps) },
                    lp(if (sec == Seccion.FACTURA) 10 else 4))
                addView(texto(sec.detalle, 12f, color = GRIS_TEXTO).apply { setPadding(dp(4), 0, 0, dp(2)) })
            }
        }))

        // ----- gráficos
        val conDatos = comps.filter { it.orden > 0 }.sortedBy { it.orden }
        val kwh = conDatos.filter { it.consumoKwh != null }.map { etiquetaMes(it) to it.consumoKwh!!.toDouble() }
        val montos = conDatos.filter { it.total != null }.map { etiquetaMes(it) to it.total!! }
        if (kwh.size >= 2) cuerpo.addView(tarjeta(grafico("Consumo por factura (kWh)", kwh, "kWh")))
        if (montos.size >= 2) cuerpo.addView(tarjeta(grafico("Importe por factura ($)", montos, "$")))
        if (kwh.size < 2 && montos.size < 2) cuerpo.addView(tarjeta(vertical(16).apply {
            addView(texto("Historial", 15f, true))
            addView(texto(
                "Cuando tengas dos o más facturas en PDF, acá vas a ver la evolución del consumo y del importe. " +
                    "En \"Facturas anteriores\" podés bajar todas las de los últimos 2 años con un solo código.",
                13f, color = GRIS_TEXTO
            ), lp(4))
        }))

        // ----- comprobantes registrados
        cuerpo.addView(texto("Comprobantes registrados (${comps.size})", 15f, true, AZUL)
            .apply { setPadding(dp(18), dp(14), dp(18), dp(4)) })
        if (comps.isEmpty()) cuerpo.addView(texto(
            "Todavía ninguno. Se anotan solos cuando consultás el portal o bajás un PDF.",
            13f, color = GRIS_TEXTO
        ).apply { setPadding(dp(18), 0, dp(18), 0) })
        comps.forEach { cuerpo.addView(tarjeta(filaComprobante(s, it))) }
    }

    private fun etiquetaMes(c: Comprobante): String =
        Regex("""\d{2}/(\d{2})/\d{2}(\d{2})""").find(c.vencimiento)?.destructured?.let { (m, a) -> "$m/$a" } ?: "?"

    private fun grafico(titulo: String, datos: List<Pair<String, Double>>, unidad: String) = vertical(16).apply {
        addView(texto(titulo, 15f, true))
        val prom = datos.map { it.second }.average()
        val ult = datos.last().second
        val dif = if (prom > 0) ((ult - prom) / prom * 100) else 0.0
        val fmt = { v: Double -> if (unidad == "$") pesos.format(v) else "%.0f kWh".format(v) }
        addView(texto("Última ${fmt(ult)} · promedio ${fmt(prom)} (${if (dif >= 0) "+" else ""}${"%.0f".format(dif)}%)",
            12f, color = GRIS_TEXTO), lp(2))
        addView(GraficoView(context).apply { mostrar(datos, unidad) }, lp(10))
    }

    private fun filaComprobante(s: Suministro, c: Comprobante) = vertical(14).apply {
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(texto(c.numero, 16f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(etiqueta(if (c.archivo.isNotBlank()) "PDF" else "visto en portal"))
        })
        val det = listOfNotNull(
            c.vencimiento.takeIf { it.isNotBlank() }?.let { "Vence $it" },
            c.total?.let { pesos.format(it) },
            c.consumoKwh?.let { "$it kWh" },
            c.periodo.takeIf { it.isNotBlank() }?.let { "Período $it" }
        ).joinToString(" · ")
        if (det.isNotBlank()) addView(texto(det, 13f, color = GRIS_TEXTO), lp(2))

        val acciones = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, 0) }
        val f = File(Descargas.carpeta(context), c.archivo)
        if (c.archivo.isNotBlank() && f.exists()) {
            acciones.addView(boton("Abrir") { Descargas.abrir(this@ConexionActivity, f) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            acciones.addView(boton("Compartir", relleno = false) { Descargas.compartir(this@ConexionActivity, f) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
        }
        if (Suministros.facturaValida(c.numero)) acciones.addView(
            boton("Usar de referencia", relleno = false) {
                Suministros.guardar(this@ConexionActivity, s.copy(ultimaFactura = c.numero), s.clave)
                Toast.makeText(context, "Se usará ${c.numero} en Facturas anteriores", Toast.LENGTH_SHORT).show()
                dibujar()
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply { marginStart = dp(8) })
        if (acciones.childCount > 0) addView(acciones)
        if (c.numero == s.ultimaFactura) addView(texto("★ Referencia para Facturas anteriores", 12f, true, AZUL), lp(4))
    }

    /** En "Facturas anteriores" EPEN pide un comprobante que no sea el último: se ofrece elegirlo. */
    private fun abrirSeccion(s: Suministro, sec: Seccion, comps: List<Comprobante>) {
        if (sec != Seccion.ANTERIORES) { lanzar(s, sec, s.ultimaFactura); return }
        val opciones = comps.map { it.numero }.filter { Suministros.facturaValida(it) }.distinct()
        if (opciones.size <= 1) { lanzar(s, sec, s.ultimaFactura.ifBlank { opciones.firstOrNull().orEmpty() }); return }
        val etiquetas = (opciones.map { n ->
            val c = comps.first { it.numero == n }
            n + (c.vencimiento.takeIf { it.isNotBlank() }?.let { "  (vence $it)" } ?: "")
        } + "Sin referencia (lo escribo yo)").toTypedArray()
        val elegido = opciones.indexOf(s.ultimaFactura).takeIf { it >= 0 } ?: opciones.size.coerceAtMost(1)
        var sel = elegido
        MaterialAlertDialogBuilder(this)
            .setTitle("Comprobante de referencia")
            .setSingleChoiceItems(etiquetas, elegido) { _, w -> sel = w }
            .setPositiveButton("Consultar") { _, _ ->
                lanzar(s, sec, if (sel < opciones.size) opciones[sel] else "")
            }
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
