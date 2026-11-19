package ar.pariz.epen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Abre cada pantalla con datos de ejemplo y toca cada cuadro del resumen:
 * si algo falla al dibujar, la prueba falla. Deja capturas en app/build/capturas.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PantallasTest {

    private lateinit var ctx: Context

    @Before fun sembrar() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        Suministros.reiniciarCache()
        ctx.getSharedPreferences("epen", 0).edit()
            .putInt("registro_version", Registro.VERSION_LECTOR).putBoolean("permiso_avisos_pedido", true).commit()
        ctx.getSharedPreferences("avisos", 0).edit().putLong("nueva_revisada", System.currentTimeMillis()).commit()
        Suministros.guardar(ctx, Suministro("00147351/02", "Terreno", "Eliseo", "Tobiano"))
        Suministros.guardar(ctx, Suministro("118310/1", "Casa", "Eliseo", "Valles de Chapelco"))
        val conc = Lector.conceptos("""
            Cargo fijo 6.200,00
            Energía consumida 14 kWh 9.100,40
            Alumbrado Público 3.450,20
            Tasa municipal 812,30
            I.V.A. 21,00 % 4.380,77
            Ingresos Brutos 1.316,09
            Interés por pago fuera de término 1.000,00
        """.trimIndent())
        // 20 facturas (más de un año) para probar el selector 1 año / 2 años
        Suministros.lote(ctx) {
            for (i in 0 until 20) {
                val mes = 11 - i
                val anio = 2026 + Math.floorDiv(mes - 1, 12)
                val m = Math.floorMod(mes - 1, 12) + 1
                Suministros.registrar(ctx, Comprobante("147351/2", "00007-165045%02d".format(i),
                    "10/%02d/%d".format(m, anio), 18000.0 + i * 650.0, 14 + (i * 7) % 25,
                    origen = "pdf", conceptos = conc))
            }
        }
    }

    private fun capturar(v: View, nombre: String) {
        v.measure(View.MeasureSpec.makeMeasureSpec(1233, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)
        assertTrue("$nombre quedó vacía", v.measuredHeight > 100)
        val b = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight.coerceIn(1, 9000), Bitmap.Config.ARGB_8888)
        b.eraseColor(0xFFF2F5F9.toInt())
        v.draw(Canvas(b))
        val dir = File("build/capturas").apply { mkdirs() }
        File(dir, "$nombre.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun scroll(v: View): ScrollView? {
        if (v is ScrollView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) scroll(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun buscar(v: View, t: String): View? {
        if (v is TextView && v.text.toString() == t) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) buscar(v.getChildAt(i), t)?.let { return it }
        return null
    }

    @Test fun inicio() {
        val a = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assertNotNull(buscar(a.window.decorView, "Terreno"))
        assertNotNull(buscar(a.window.decorView, "Casa"))
        capturar(scroll(a.window.decorView)!!.getChildAt(0), "inicio")
    }

    @Test fun fichaYDesgloses() {
        val i = Intent(ctx, ConexionActivity::class.java).putExtra(ConexionActivity.EXTRA_CLAVE, "147351/2")
        val a = Robolectric.buildActivity(ConexionActivity::class.java, i).setup().get()
        val raiz = a.window.decorView
        capturar(scroll(raiz)!!.getChildAt(0), "conexion")
        for ((t, n) in listOf("Total facturado" to "d_total", "Alumbrado público" to "d_alumbrado",
                "Impuestos y tasas" to "d_impuestos", "Energía consumida" to "d_kwh", "Intereses y recargos" to "d_intereses")) {
            var v = buscar(raiz, t) ?: error("No está el cuadro $t")
            while (!v.isClickable) v = v.parent as View
            v.performClick()
            ShadowLooper.idleMainLooper()
            val d = ShadowDialog.getLatestDialog() as BottomSheetDialog
            val hoja = d.findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet)!!
            capturar((hoja.getChildAt(0) as ViewGroup).getChildAt(0), n)
            d.dismiss()
        }
        // selector de período y de serie
        buscar(raiz, "2 años")!!.performClick()
        buscar(raiz, "Importe ($)")!!.performClick()
        ShadowLooper.idleMainLooper()
        capturar(scroll(raiz)!!.getChildAt(0), "conexion_2anios")
    }

    @Test fun saltarDeConexion() {
        val i = Intent(ctx, ConexionActivity::class.java).putExtra(ConexionActivity.EXTRA_CLAVE, "147351/2")
        val a = Robolectric.buildActivity(ConexionActivity::class.java, i).setup().get()
        buscar(a.window.decorView, "Casa")!!.performClick()
        ShadowLooper.idleMainLooper()
        assertTrue(a.title.toString() == "Casa")
    }

    @Test fun archivoYPortal() {
        val ar = Robolectric.buildActivity(ArchivoActivity::class.java).setup().get()
        assertNotNull(buscar(ar.window.decorView, "Todavía no descargaste facturas"))
        val i = Intent(ctx, PortalActivity::class.java)
            .putExtra(PortalActivity.EXTRA_SECCION, Seccion.ANTERIORES.name)
            .putExtra(PortalActivity.EXTRA_SUMINISTRO, "00147351/02")
        val p = Robolectric.buildActivity(PortalActivity::class.java, i).setup().get()
        assertTrue(p.title.toString() == Seccion.ANTERIORES.titulo)
    }
}
