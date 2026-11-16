package ar.pariz.epen

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/** Pantalla principal: todas las conexiones registradas, con su última factura conocida. */
class MainActivity : AppCompatActivity() {

    private lateinit var lista: LinearLayout
    private val pesos = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    private val exportar = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@registerForActivityResult
        try {
            contentResolver.openOutputStream(uri)?.use { it.write(Suministros.exportar(this).toByteArray()) }
            Toast.makeText(this, "Respaldo guardado", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo guardar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val importar = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val txt = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            val (nuevas, comps) = Suministros.importar(this, txt)
            Toast.makeText(this, "$nuevas conexión(es) nuevas, $comps comprobante(s) procesados", Toast.LENGTH_LONG).show()
            dibujar()
        } catch (e: Exception) {
            Toast.makeText(this, "El archivo no es un respaldo de EPEN al día", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

        lista = vertical().apply { setPadding(0, dp(8), 0, dp(96)) }
        val fab = ExtendedFloatingActionButton(this).apply {
            text = "Agregar conexión"
            isAllCaps = false
            setOnClickListener { Editor.abrir(this@MainActivity, null) { dibujar() } }
        }
        setContentView(FrameLayout(this).apply {
            addView(ScrollView(context).apply { addView(lista) })
            addView(fab, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply { setMargins(dp(16), dp(16), dp(16), dp(24)) })
        })

        // PDF bajados con la versión anterior: se leen y registran una sola vez.
        if (savedInstanceState == null) thread {
            if (Descargas.reindexar(this) > 0) runOnUiThread { dibujar() }
        }
    }

    override fun onResume() { super.onResume(); dibujar() }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Mis facturas").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, 2, 1, "Guardar respaldo de conexiones")
        menu.add(0, 3, 2, "Restaurar respaldo")
        menu.add(0, 4, 3, "Acerca de")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> startActivity(Intent(this, ArchivoActivity::class.java))
            2 -> exportar.launch("EPEN-al-dia-respaldo-" +
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json")
            3 -> importar.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            4 -> MaterialAlertDialogBuilder(this)
                .setTitle("EPEN al día")
                .setMessage(
                    "App no oficial para consultar el portal de servicios del EPEN (servicios.epen.gov.ar).\n\n" +
                        "Tus conexiones y comprobantes quedan registrados solo en este teléfono; podés guardar un " +
                        "respaldo desde el menú. El código de verificación (captcha) lo escribís vos en cada consulta, " +
                        "como en la web, y con un solo código podés bajar todas las facturas que liste la página."
                )
                .setPositiveButton("Entendido", null).show()
        }
        return true
    }

    private fun dibujar() {
        lista.removeAllViews()
        val sums = Suministros.listar(this)

        if (sums.isEmpty()) {
            lista.addView(tarjeta(vertical(24).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(texto("⚡", 48f).apply { gravity = Gravity.CENTER })
                addView(texto("Registrá tu conexión", 20f, true).apply { gravity = Gravity.CENTER })
                addView(texto(
                    "La encontrás en tu factura como \"Suministro/Contrato\", por ejemplo 118310/1. Podés registrar todas las que quieras.",
                    15f, color = GRIS_TEXTO
                ).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(16)) })
                addView(boton("Agregar conexión") { Editor.abrir(this@MainActivity, null) { dibujar() } })
                addView(boton("Restaurar un respaldo", relleno = false) {
                    importar.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }, lp(6))
            }))
            return
        }

        lista.addView(texto("${sums.size} conexión(es) registradas", 13f, color = GRIS_TEXTO)
            .apply { setPadding(dp(18), dp(6), dp(18), 0) })
        for (s in sums) lista.addView(tarjeta(fila(s)).apply {
            isClickable = true
            setOnClickListener { abrirConexion(s) }
        })
    }

    private fun fila(s: Suministro): LinearLayout = vertical(16).apply {
        val comps = Suministros.comprobantes(context, s.clave)
        val ult = comps.firstOrNull { it.total != null || it.vencimiento.isNotBlank() }

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(vertical().apply {
                addView(texto(s.nombre, 19f, true))
                addView(texto("N° ${s.numero}", 15f, true, AZUL))
                if (s.direccion.isNotBlank()) addView(texto(s.direccion, 13f, color = GRIS_TEXTO))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (ult?.total != null) addView(vertical().apply {
                gravity = Gravity.END
                addView(texto(pesos.format(ult.total), 16f, true).apply { gravity = Gravity.END })
                if (ult.vencimiento.isNotBlank()) addView(texto("vence ${ult.vencimiento}", 12f, color = GRIS_TEXTO))
            })
        })

        val pdfs = comps.count { it.archivo.isNotBlank() }
        addView(texto(
            if (comps.isEmpty()) "Sin comprobantes todavía" else "${comps.size} comprobante(s) · $pdfs PDF",
            12f, color = GRIS_TEXTO
        ), lp(6))

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
            addView(boton("Factura vigente") { lanzar(s, Seccion.FACTURA) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(boton("Deuda", relleno = false) { lanzar(s, Seccion.DEUDA) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
            addView(boton("Ver más", relleno = false) { abrirConexion(s) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
        })
    }

    private fun abrirConexion(s: Suministro) =
        startActivity(Intent(this, ConexionActivity::class.java).putExtra(ConexionActivity.EXTRA_CLAVE, s.clave))

    private fun lanzar(s: Suministro, sec: Seccion) {
        startActivity(Intent(this, PortalActivity::class.java)
            .putExtra(PortalActivity.EXTRA_SECCION, sec.name)
            .putExtra(PortalActivity.EXTRA_SUMINISTRO, s.numero)
            .putExtra(PortalActivity.EXTRA_ALIAS, s.alias)
            .putExtra(PortalActivity.EXTRA_FACTURA, s.ultimaFactura))
    }
}
