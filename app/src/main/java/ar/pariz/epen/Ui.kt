package ar.pariz.epen

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/* ---------- Sistema visual de EPEN al día (interfaz armada sin XML) ---------- */

val AZUL = Color.parseColor("#0B4F8A")
val AZUL_OSC = Color.parseColor("#073763")
val AZUL_CLARO = Color.parseColor("#E3EEF8")
val AMARILLO = Color.parseColor("#F5B800")
val AMARILLO_CLARO = Color.parseColor("#FFF4D1")
val VERDE = Color.parseColor("#1E8E5A")
val VERDE_CLARO = Color.parseColor("#E1F4EA")
val ROJO = Color.parseColor("#C0392B")
val ROJO_CLARO = Color.parseColor("#FBE6E3")
val TEXTO = Color.parseColor("#1B2430")
val GRIS_TEXTO = Color.parseColor("#5B6676")
val BORDE = Color.parseColor("#E1E7EF")
val FONDO = Color.parseColor("#F2F5F9")

private val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
private val ES_AR = Locale("es", "AR")

fun Context.dp(v: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

fun pesos(v: Double): String = NumberFormat.getCurrencyInstance(ES_AR).format(v)

/** "$ 1,2 M" / "$ 845 mil" para mosaicos chicos. */
fun pesosCorto(v: Double): String = when {
    v >= 1_000_000 -> "$ " + String.format(ES_AR, "%.1f M", v / 1_000_000)
    v >= 100_000 -> "$ " + String.format(ES_AR, "%.0f mil", v / 1_000)
    else -> pesos(v)
}

fun miles(v: Int): String = NumberFormat.getIntegerInstance(ES_AR).format(v)

fun Context.vertical(padding: Int = 0): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
}

fun Context.fila(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

fun Context.texto(t: String, sp: Float = 15f, bold: Boolean = false, color: Int = TEXTO): TextView =
    TextView(this).apply {
        text = t
        textSize = sp
        setTextColor(color)
        if (bold) typeface = if (sp >= 20f) Typeface.DEFAULT_BOLD else MEDIUM
        setLineSpacing(0f, 1.1f)
    }

fun Context.boton(t: String, relleno: Boolean = true, onClick: () -> Unit): MaterialButton {
    val estilo = if (relleno) com.google.android.material.R.attr.materialButtonStyle
    else com.google.android.material.R.attr.materialButtonOutlinedStyle
    return MaterialButton(this, null, estilo).apply {
        text = t
        isAllCaps = false
        setOnClickListener { onClick() }
    }
}

/** Tarjeta plana con borde fino, el estilo de toda la app. */
fun Context.tarjeta(contenido: View, margenVertical: Int = 6): MaterialCardView = MaterialCardView(this).apply {
    radius = dp(16).toFloat()
    cardElevation = 0f
    strokeWidth = dp(1)
    strokeColor = BORDE
    setCardBackgroundColor(Color.WHITE)
    addView(contenido)
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(dp(16), dp(margenVertical), dp(16), dp(margenVertical)) }
}

fun Context.etiqueta(t: String, fondo: Int = AZUL_CLARO, color: Int = AZUL): TextView = texto(t, 12f, true, color).apply {
    setPadding(dp(10), dp(4), dp(10), dp(4))
    background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(fondo) }
}

fun Context.icono(res: Int, color: Int, tam: Int = 22): ImageView = ImageView(this).apply {
    setImageResource(res)
    imageTintList = ColorStateList.valueOf(color)
    layoutParams = LinearLayout.LayoutParams(dp(tam), dp(tam))
}

/** Ícono dentro de un círculo de color suave. */
fun Context.iconoCircular(res: Int, color: Int, fondo: Int, tam: Int = 40): FrameLayout = FrameLayout(this).apply {
    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(fondo) }
    val i = icono(res, color, tam / 2 + 2)
    addView(i, FrameLayout.LayoutParams(dp(tam / 2 + 2), dp(tam / 2 + 2), Gravity.CENTER))
    layoutParams = LinearLayout.LayoutParams(dp(tam), dp(tam))
}

fun Context.titulo(t: String, extra: String? = null): LinearLayout = fila().apply {
    setPadding(dp(20), dp(18), dp(20), dp(6))
    addView(texto(t, 16f, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    if (extra != null) addView(texto(extra, 12f, color = GRIS_TEXTO))
}

fun Context.separador(): View = View(this).apply {
    setBackgroundColor(BORDE)
    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
}

/** Mosaico de dato: ícono, valor grande, rótulo y un detalle opcional. */
fun Context.mosaico(res: Int, color: Int, fondo: Int, valor: String, rotulo: String, detalle: String? = null): LinearLayout =
    vertical(14).apply {
        background = GradientDrawable().apply { cornerRadius = dp(14).toFloat(); setColor(fondo) }
        addView(icono(res, color, 22))
        addView(texto(valor, 19f, true).apply { setPadding(0, dp(8), 0, 0); maxLines = 1 })
        addView(texto(rotulo, 12.5f, color = GRIS_TEXTO).apply { maxLines = 2 })
        if (detalle != null) addView(texto(detalle, 11.5f, true, color).apply { setPadding(0, dp(2), 0, 0) })
    }

/** Grilla de dos columnas con separación pareja. */
fun Context.grilla(celdas: List<View>, separacion: Int = 10): LinearLayout = vertical().apply {
    celdas.chunked(2).forEachIndexed { i, par ->
        addView(fila().apply {
            gravity = Gravity.TOP
            par.forEachIndexed { j, v ->
                addView(v, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (j == 1) marginStart = dp(separacion)
                })
            }
            if (par.size == 1) addView(View(context), LinearLayout.LayoutParams(0, 0, 1f).apply { marginStart = dp(separacion) })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { if (i > 0) topMargin = dp(separacion) })
    }
}

/** Botón de acceso en forma de mosaico (ícono + título + bajada). */
fun Context.acceso(res: Int, tituloT: String, bajada: String, destacado: Boolean, onClick: () -> Unit): LinearLayout =
    vertical(14).apply {
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(if (destacado) AZUL else Color.WHITE)
            if (!destacado) setStroke(dp(1), BORDE)
        }
        isClickable = true
        isFocusable = true
        foreground = context.getDrawable(android.R.drawable.list_selector_background)
        setOnClickListener { onClick() }
        addView(icono(res, if (destacado) Color.WHITE else AZUL, 22))
        addView(texto(tituloT, 14.5f, true, if (destacado) Color.WHITE else TEXTO).apply { setPadding(0, dp(8), 0, 0) })
        addView(texto(bajada, 12f, color = if (destacado) Color.parseColor("#CCFFFFFF") else GRIS_TEXTO).apply { maxLines = 2 })
    }

fun LinearLayout.lp(top: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = context.dp(top) }

/* ---------- Fechas ---------- */

private val fmtFecha = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }

/** Días desde hoy hasta la fecha (negativo si ya pasó); null si no se puede leer. */
fun diasHasta(fecha: String): Long? = runCatching {
    val d = fmtFecha.parse(fecha) ?: return null
    val hoy = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    TimeUnit.MILLISECONDS.toDays(d.time - hoy.timeInMillis)
}.getOrNull()

/** Chip de estado del vencimiento: vence hoy / en N días / venció. */
fun Context.chipVencimiento(fecha: String): TextView? {
    val d = diasHasta(fecha) ?: return null
    return when {
        d < 0 -> etiqueta("Venció el ${fecha.take(5)}", Color.parseColor("#EEF1F5"), GRIS_TEXTO)
        d == 0L -> etiqueta("Vence hoy", ROJO_CLARO, ROJO)
        d <= 7 -> etiqueta("Vence en $d día${if (d == 1L) "" else "s"}", AMARILLO_CLARO, Color.parseColor("#8A6400"))
        else -> etiqueta("Vence el ${fecha.take(5)}", AZUL_CLARO, AZUL)
    }
}

private val MESES = arrayOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** "nov 26" a partir de dd/mm/aaaa. */
fun mesCorto(fecha: String): String =
    Regex("""\d{2}/(\d{2})/\d{2}(\d{2})""").find(fecha)?.destructured?.let { (m, a) ->
        (MESES.getOrNull(m.toInt() - 1) ?: m) + " " + a
    } ?: "—"

/* ---------- Navegación ---------- */

const val MENU_INICIO = 900

/** Vuelve a la lista de conexiones cerrando las pantallas abiertas encima. */
fun android.app.Activity.irAlInicio() {
    startActivity(android.content.Intent(this, MainActivity::class.java)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
    finish()
}

/** Botón "Inicio" con ícono, siempre visible en la barra superior. */
fun android.view.Menu.agregarInicio() {
    add(0, MENU_INICIO, 0, "Inicio").setIcon(R.drawable.ic_home)
        .setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
}
