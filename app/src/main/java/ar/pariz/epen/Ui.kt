package ar.pariz.epen

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Pequeños ayudantes para armar la interfaz sin XML. */
fun Context.dp(v: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

val AZUL = Color.parseColor("#0B4F8A")
val AMARILLO = Color.parseColor("#F5B800")
val GRIS_TEXTO = Color.parseColor("#4A5563")

fun Context.vertical(padding: Int = 0): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
}

fun Context.texto(t: String, sp: Float = 15f, bold: Boolean = false, color: Int = Color.BLACK): TextView =
    TextView(this).apply {
        text = t
        textSize = sp
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
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

fun Context.tarjeta(contenido: View): MaterialCardView = MaterialCardView(this).apply {
    radius = dp(14).toFloat()
    cardElevation = dp(2).toFloat()
    setCardBackgroundColor(Color.WHITE)
    addView(contenido)
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(dp(14), dp(8), dp(14), dp(8)) }
}

fun Context.etiqueta(t: String): TextView = texto(t, 12f, true, AZUL).apply {
    setPadding(dp(10), dp(3), dp(10), dp(3))
    background = GradientDrawable().apply {
        cornerRadius = dp(20).toFloat()
        setColor(Color.parseColor("#E3EEF8"))
    }
}

fun LinearLayout.lp(top: Int = 0): LinearLayout.LayoutParams =
    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = context.dp(top) }
