package ar.pariz.epen

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Mis facturas": todos los PDF bajados, agrupados por suministro, disponibles sin conexión. */
class ArchivoActivity : Pantalla() {

    private lateinit var lista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Mis facturas"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        lista = vertical().apply { setPadding(0, dp(8), 0, dp(24)) }
        setContentView(ScrollView(this).apply { addView(lista) })
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean { menu.agregarInicio(); return true }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == MENU_INICIO) { irAlInicio(); return true }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() { super.onResume(); dibujar() }

    private val nombreSeccion = mapOf(
        "factura" to "Factura vigente", "deuda" to "Deuda / recibo",
        "fact_ant" to "Factura anterior", "subsidio" to "Segmentación", "fact_ant_list" to "Factura anterior"
    )

    private fun dibujar() {
        lista.removeAllViews()
        val archivos = Descargas.archivos(this)
        if (archivos.isEmpty()) {
            lista.addView(tarjeta(vertical(24).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(texto("Todavía no descargaste facturas", 17f, true))
                addView(texto(
                    "Cuando bajes un PDF desde cualquier sección, queda guardado acá (y también en Descargas/EPEN) para verlo sin conexión.",
                    14f, color = GRIS_TEXTO
                ).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) })
            }))
            return
        }

        archivos.groupBy { Descargas.claveDeArchivo(it) }.forEach { (clave, fs) ->
            val s = Suministros.buscar(this, clave)
            val titulo = s?.let { "${it.nombre} · ${it.numero}" } ?: "Conexión $clave"
            lista.addView(texto(titulo, 15f, true, AZUL).apply { setPadding(dp(18), dp(14), dp(18), dp(2)) })
            fs.forEach { lista.addView(tarjeta(fila(it))) }
        }
    }

    private fun fila(f: File): LinearLayout = vertical(14).apply {
        val partes = f.nameWithoutExtension.split("_")
        val sec = nombreSeccion[partes.getOrNull(2)] ?: "Comprobante"
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "AR")).format(Date(f.lastModified()))
        val kb = (f.length() / 1024).coerceAtLeast(1)

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(etiqueta(sec))
            addView(texto("  Bajada el $fecha · $kb KB", 12f, color = GRIS_TEXTO))
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
            addView(boton("Abrir") { Descargas.abrir(this@ArchivoActivity, f) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(boton("Compartir", relleno = false) { Descargas.compartir(this@ArchivoActivity, f) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
            addView(boton("Borrar", relleno = false) { borrar(f) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
        })
    }

    private fun borrar(f: File) {
        MaterialAlertDialogBuilder(this)
            .setMessage("¿Borrar este PDF de la app? La copia en Descargas/EPEN no se toca.")
            .setPositiveButton("Borrar") { _, _ -> f.delete(); Suministros.olvidarArchivo(this, f.name); dibujar() }
            .setNegativeButton("Cancelar", null).show()
    }
}
