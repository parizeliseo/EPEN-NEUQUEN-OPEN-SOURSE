package ar.pariz.epen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.log10
import kotlin.math.pow

/**
 * Gráfico de barras de las últimas 12 facturas: escala con líneas guía, promedio
 * punteado, meses abajo y la última factura resaltada. Tocando una barra se ve su valor.
 */
class GraficoView(ctx: Context) : View(ctx) {

    private var datos: List<Pair<String, Double>> = emptyList()
    private var formato: (Double) -> String = { "%.0f".format(it) }
    private var elegida = -1
    private val rect = RectF()   // reutilizado en cada dibujo (sin crear objetos en onDraw)

    companion object { const val MAXIMO = 24 }

    private val barra = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9DB9D6") }
    private val barraUltima = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AZUL }
    private val barraElegida = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AMARILLO }
    private val guia = Paint().apply { color = BORDE; strokeWidth = ctx.dp(1).toFloat() }
    private val promedio = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AMARILLO; strokeWidth = ctx.dp(2).toFloat(); style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(ctx.dp(6).toFloat(), ctx.dp(4).toFloat()), 0f)
    }
    private val eje = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GRIS_TEXTO; textSize = ctx.dp(10).toFloat(); textAlign = Paint.Align.RIGHT
    }
    private val mes = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GRIS_TEXTO; textSize = ctx.dp(10).toFloat(); textAlign = Paint.Align.CENTER
    }
    private val globo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXTO }
    private val globoTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = ctx.dp(12).toFloat(); textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    }

    /** [datos]: (etiqueta del mes, valor) de la más vieja a la más nueva; se muestran hasta [MAXIMO]. */
    fun mostrar(datos: List<Pair<String, Double>>, formato: (Double) -> String) {
        this.datos = datos.takeLast(MAXIMO)
        this.formato = formato
        elegida = this.datos.lastIndex
        invalidate()
    }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(MeasureSpec.getSize(w), context.dp(210))

    private val izq get() = context.dp(44).toFloat()
    private fun ancho() = (width - izq) / datos.size.coerceAtLeast(1)

    /** Tope de la escala redondeado (1, 2, 2.5 o 5 × 10ⁿ). */
    private fun tope(max: Double): Double {
        if (max <= 0) return 1.0
        val p = 10.0.pow(kotlin.math.floor(log10(max)))
        return listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * p }.first { it >= max }
    }

    override fun onDraw(c: Canvas) {
        if (datos.isEmpty()) return
        val arriba = context.dp(34).toFloat()
        val abajo = height - context.dp(22).toFloat()
        val max = tope(datos.maxOf { it.second })
        val prom = datos.map { it.second }.average()

        // líneas guía y escala
        val div = divisiones(max)
        for (i in 0..div) {
            val y = abajo - (abajo - arriba) * i / div
            c.drawLine(izq, y, width.toFloat(), y, guia)
            c.drawText(corto(max * i / div), izq - context.dp(6), y + context.dp(3), eje)
        }

        val w = ancho()
        val r = context.dp(5).toFloat()
        datos.forEachIndexed { i, (etq, v) ->
            val x0 = izq + i * w + w * 0.2f
            val x1 = izq + (i + 1) * w - w * 0.2f
            val y = abajo - ((abajo - arriba) * (v / max)).toFloat()
            val p = when (i) { elegida -> barraElegida; datos.lastIndex -> barraUltima; else -> barra }
            rect.set(x0, y, x1, abajo); c.drawRoundRect(rect, r, r, p)
            c.drawRect(x0, (y + r).coerceAtMost(abajo), x1, abajo, p) // base recta
            val paso = when { datos.size <= 8 -> 1; datos.size <= 16 -> 2; else -> 3 }
            if ((datos.lastIndex - i) % paso == 0)
                run {
                    val mitad = mes.measureText(etq) / 2
                    val cx = ((x0 + x1) / 2).coerceIn(izq + mitad, width - mitad) // no se corta en los bordes
                    c.drawText(etq, cx, height - context.dp(6).toFloat(), mes)
                }
        }

        val yp = abajo - ((abajo - arriba) * (prom / max)).toFloat()
        c.drawLine(izq, yp, width.toFloat(), yp, promedio)

        // globo con el valor de la barra elegida
        if (elegida in datos.indices) {
            val (etq, v) = datos[elegida]
            val txt = "$etq · ${formato(v)}"
            val tw = globoTxt.measureText(txt) + context.dp(16)
            val cx = (izq + elegida * w + w / 2).coerceIn(izq + tw / 2, width - tw / 2)
            val y = abajo - ((abajo - arriba) * (v / max)).toFloat()
            val top = (y - context.dp(30)).coerceAtLeast(0f)
            rect.set(cx - tw / 2, top, cx + tw / 2, top + context.dp(24)); c.drawRoundRect(rect, r, r, globo)
            c.drawText(txt, cx, top + context.dp(16), globoTxt)
        }
    }

    /** Cantidad de líneas guía para que la escala quede en números redondos. */
    private fun divisiones(max: Double): Int {
        val primero = max / 10.0.pow(kotlin.math.floor(log10(max)))
        return when (primero) { 2.0 -> 4; 2.5 -> 5; 5.0 -> 5; else -> 5 }
    }

    private fun corto(v: Double): String = when {
        v >= 1_000_000 -> "%.1fM".format(v / 1_000_000)
        v >= 1_000 -> if (v % 1_000 == 0.0) "${(v / 1_000).toInt()}k" else "%.1fk".format(v / 1_000)
        v == kotlin.math.floor(v) -> "%.0f".format(v)
        else -> "%.1f".format(v)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (datos.isEmpty()) return false
        if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val i = ((e.x - izq) / ancho()).toInt().coerceIn(0, datos.lastIndex)
            if (i != elegida) { elegida = i; invalidate() }
            return true
        }
        parent?.requestDisallowInterceptTouchEvent(false)
        return true
    }
}
