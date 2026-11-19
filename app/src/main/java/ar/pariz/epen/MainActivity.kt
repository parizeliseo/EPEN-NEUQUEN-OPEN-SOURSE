package ar.pariz.epen

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/** Inicio: resumen general y todas las conexiones registradas. */
class MainActivity : Pantalla() {

    private lateinit var lista: LinearLayout
    private var leyendo = false

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
            Suministros.depurar(this)
            Toast.makeText(this, "$nuevas conexión(es) nuevas, $comps comprobante(s) procesados", Toast.LENGTH_LONG).show()
            dibujar()
        } catch (e: Exception) {
            Toast.makeText(this, "El archivo no es un respaldo de EPEN al día", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)

        lista = vertical().apply { setPadding(0, 0, 0, dp(100)) }
        val fab = ExtendedFloatingActionButton(this).apply {
            text = "Agregar conexión"
            isAllCaps = false
            setIconResource(R.drawable.ic_bolt)
            setOnClickListener { Editor.abrir(this@MainActivity, null) { dibujar() } }
        }
        setContentView(FrameLayout(this).apply {
            addView(ScrollView(context).apply { addView(lista); isFillViewport = true })
            addView(fab, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply { setMargins(dp(16), dp(16), dp(16), dp(20)) })
        })

        Avisos.programar(this)
        pedirPermisoAvisos()
        if (savedInstanceState == null) actualizarEnSegundoPlano()
    }

    private val permisoAvisos = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** En Android 13+ los avisos necesitan permiso; se pide una sola vez. */
    private fun pedirPermisoAvisos() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        val p = getSharedPreferences("epen", Context.MODE_PRIVATE)
        if (p.getBoolean("permiso_avisos_pedido", false)) return
        p.edit().putBoolean("permiso_avisos_pedido", true).apply()
        permisoAvisos.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Cada vez que se abre: relee PDF nuevos o todos si cambió el lector, y busca versión nueva. */
    private fun actualizarEnSegundoPlano() {
        leyendo = true
        thread {
            val cambio = runCatching { Registro.actualizarSiHaceFalta(this) }.getOrDefault(false)
            runCatching { Avisos.revisarVencimientos(this) }
            if (System.currentTimeMillis() - Avisos.ultimaRevisionVersion(this) > 6 * 3600_000L)
                Avisos.guardarVersion(this, Avisos.versionNueva(this))
            runOnUiThread { leyendo = false; if (!isFinishing) dibujar() }
            if (cambio) runOnUiThread { Toast.makeText(this, "Facturas actualizadas", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun onResume() { super.onResume(); dibujar() }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Mis facturas").setIcon(R.drawable.ic_doc).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.add(0, 5, 1, "Volver a leer mis facturas")
        menu.add(0, 6, 1, "Avisos de vencimiento").setCheckable(true).setChecked(Avisos.activos(this))
        menu.add(0, 2, 2, "Guardar respaldo de conexiones")
        menu.add(0, 3, 3, "Restaurar respaldo")
        menu.add(0, 4, 4, "Acerca de")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> startActivity(Intent(this, ArchivoActivity::class.java))
            2 -> exportar.launch("EPEN-al-dia-respaldo-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json")
            3 -> importar.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            5 -> {
                leyendo = true; dibujar()
                thread {
                    val n = Descargas.actualizarRegistro(this)
                    getSharedPreferences("epen", Context.MODE_PRIVATE).edit().putInt("registro_version", Registro.VERSION_LECTOR).apply()
                    runOnUiThread { leyendo = false; dibujar(); Toast.makeText(this, "$n factura(s) leídas", Toast.LENGTH_SHORT).show() }
                }
            }
            6 -> {
                val nuevo = !Avisos.activos(this)
                Avisos.activar(this, nuevo)
                item.isChecked = nuevo
                if (nuevo && !Avisos.puedeNotificar(this)) MaterialAlertDialogBuilder(this)
                    .setTitle("Permitir notificaciones")
                    .setMessage("Para recibir el aviso con sonido el día del vencimiento, permití las notificaciones de EPEN al día.")
                    .setPositiveButton("Abrir ajustes") { _, _ ->
                        startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName))
                    }
                    .setNegativeButton("Ahora no", null).show()
                else Toast.makeText(this, if (nuevo) "Avisos activados: el día anterior y el día del vencimiento" else "Avisos desactivados", Toast.LENGTH_LONG).show()
            }
            4 -> MaterialAlertDialogBuilder(this)
                .setTitle("EPEN al día")
                .setMessage(
                    "Versión ${packageManager.getPackageInfo(packageName, 0).versionName}\n\n" +
                        "App no oficial y de código abierto para consultar el portal de servicios del EPEN " +
                        "(servicios.epen.gov.ar).\n\nTus conexiones y facturas quedan solo en este teléfono. " +
                        "El código de verificación lo escribís vos en cada consulta, tal cual la imagen."
                )
                .setPositiveButton("Entendido", null).show()
        }
        return true
    }

    private fun dibujar() {
        lista.removeAllViews()
        val sums = Suministros.listar(this)

        if (sums.isEmpty()) {
            lista.addView(bandaBienvenida())
            lista.addView(tarjeta(vertical(24).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(iconoCircular(R.drawable.ic_bolt, AZUL, AZUL_CLARO, 64))
                addView(texto("Registrá tu primera conexión", 19f, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(14), 0, 0) })
                addView(texto("La encontrás en tu factura como \"Suministro/Contrato\", por ejemplo 118310/1. " +
                    "Podés registrar todas las que quieras.", 14f, color = GRIS_TEXTO)
                    .apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(18)) })
                addView(boton("Agregar conexión") { Editor.abrir(this@MainActivity, null) { dibujar() } },
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                addView(boton("Restaurar un respaldo", relleno = false) {
                    importar.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }, lp(6))
            }, 0).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = -dp(12) })
            return
        }

        val datos = sums.map { it to Suministros.comprobantes(this, it.clave) }
        lista.addView(bandaResumen(datos))
        Avisos.versionGuardada(this)?.let { v -> lista.addView(bannerVersion(v)) }
        if (leyendo) lista.addView(texto("Leyendo tus facturas…", 12.5f, color = GRIS_TEXTO)
            .apply { setPadding(dp(20), dp(12), dp(20), 0) })
        lista.addView(titulo("Mis conexiones", "${sums.size}"))
        datos.forEach { (s, comps) -> lista.addView(tarjetaConexion(s, comps)) }
    }

    private fun bannerVersion(v: Avisos.Version): View = tarjeta(fila().apply {
        setPadding(dp(16), dp(14), dp(12), dp(14))
        addView(iconoCircular(R.drawable.ic_bolt, Color.parseColor("#8A6400"), AMARILLO_CLARO, 40))
        addView(vertical().apply {
            setPadding(dp(12), 0, dp(8), 0)
            addView(texto("Nueva versión ${v.nombre}", 15f, true))
            addView(texto("Descargala e instalala encima; no perdés tus datos.", 12.5f, color = GRIS_TEXTO))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(boton("Bajar") { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(v.url))) })
    }, 10)

    private fun bandaBienvenida(): View = vertical().apply {
        setBackgroundColor(AZUL)
        setPadding(dp(20), dp(8), dp(20), dp(30))
        addView(texto("Tu energía, en orden", 22f, true, Color.WHITE))
        addView(texto("Facturas, deuda e historial de tus conexiones del EPEN.", 14f, color = Color.parseColor("#CCFFFFFF")), lp(4))
    }

    /** Franja azul con los totales de todas las conexiones en el último año. */
    private fun bandaResumen(datos: List<Pair<Suministro, List<Comprobante>>>): View = vertical().apply {
        setBackgroundColor(AZUL)
        setPadding(dp(20), dp(6), dp(20), dp(20))
        val rs = datos.map { ResumenAnual.de(it.second) }
        val total = rs.sumOf { it.total }
        val kwh = rs.sumOf { it.kwh }
        val proximas = datos.mapNotNull { (_, c) -> c.firstOrNull()?.vencimiento?.let(::diasHasta) }.filter { it in 0..10 }.size

        addView(texto("Último año · todas las conexiones", 12.5f, color = Color.parseColor("#B3FFFFFF")))
        addView(texto(if (total > 0) pesos(total) else "Sin facturas aún", 28f, true, Color.WHITE))
        addView(fila().apply {
            setPadding(0, dp(12), 0, 0)
            fun dato(valor: String, rotulo: String) = vertical().apply {
                addView(texto(valor, 16f, true, Color.WHITE))
                addView(texto(rotulo, 12f, color = Color.parseColor("#B3FFFFFF")))
            }
            addView(dato("${miles(kwh)} kWh", "consumidos"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(dato("${datos.size}", if (datos.size == 1) "conexión" else "conexiones"),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(dato("$proximas", "vencen pronto"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
    }

    private fun tarjetaConexion(s: Suministro, comps: List<Comprobante>): View {
        val ult = comps.firstOrNull { it.total != null || it.vencimiento.isNotBlank() }
        val r = ResumenAnual.de(comps)
        val v = vertical(16).apply {
            addView(fila().apply {
                gravity = Gravity.TOP
                addView(iconoCircular(R.drawable.ic_bolt, AZUL, AZUL_CLARO, 44))
                addView(vertical().apply {
                    setPadding(dp(12), 0, dp(8), 0)
                    addView(texto(s.nombre, 17f, true).apply { maxLines = 1 })
                    addView(texto("N° ${s.numero}", 13.5f, true, AZUL))
                    if (s.direccion.isNotBlank()) addView(texto(s.direccion, 12.5f, color = GRIS_TEXTO).apply { maxLines = 1 })
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(vertical().apply {
                    gravity = Gravity.END
                    addView(texto(ult?.total?.let { pesos(it) } ?: "—", 16f, true).apply { gravity = Gravity.END })
                    ult?.vencimiento?.let { chipVencimiento(it) }?.let { chip ->
                        addView(chip, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                            .apply { topMargin = dp(4) })
                    }
                })
            })
            if (r.facturas > 0) addView(fila().apply {
                setPadding(0, dp(12), 0, 0)
                fun mini(valor: String, rotulo: String) = vertical().apply {
                    addView(texto(valor, 13.5f, true))
                    addView(texto(rotulo, 11.5f, color = GRIS_TEXTO))
                }
                addView(mini("${miles(r.kwh)} kWh", "último año"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(mini(pesosCorto(r.total), "facturado"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(mini("${comps.size}", "comprobantes"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            })
            addView(fila().apply {
                setPadding(0, dp(12), 0, 0)
                addView(boton("Factura") { lanzar(s, Seccion.FACTURA) },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(boton("Deuda", relleno = false) { lanzar(s, Seccion.DEUDA) },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
                addView(boton("Ver todo", relleno = false) { abrirConexion(s) },
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) })
            })
        }
        return tarjeta(v).apply {
            isClickable = true
            setOnClickListener { abrirConexion(s) }
        }
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
