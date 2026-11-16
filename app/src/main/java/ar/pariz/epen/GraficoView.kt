package ar.pariz.epen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/** Barras simples: un valor por factura, de la más vieja a la más nueva. */
class GraficoView(ctx: Context) : View(ctx) {

    private var valores: List<Pair<String, Double>> = emptyList()
    private var unidad = ""

    private val barra = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AZUL }
    private val ultima = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AMARILLO }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GRIS_TEXTO; textSize = ctx.dp(10).toFloat(); textAlign = Paint.Align.CENTER
    }
    private val guia = Paint().apply { color = Color.parseColor("#E5E9F0"); strokeWidth = ctx.dp(1).toFloat() }

    fun mostrar(datos: List<Pair<String, Double>>, unidad: String) {
        valores = datos.takeLast(12)
        this.unidad = unidad
        invalidate()
    }

    override fun onMeasure(w: Int, h: Int) =
        setMeasuredDimension(MeasureSpec.getSize(w), context.dp(170))

    override fun onDraw(c: Canvas) {
        if (valores.isEmpty()) return
        val arriba = context.dp(18).toFloat()
        val abajo = height - context.dp(20).toFloat()
        val max = valores.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
        c.drawLine(0f, abajo, width.toFloat(), abajo, guia)
        val ancho = width.toFloat() / valores.size
        val r = context.dp(4).toFloat()
        valores.forEachIndexed { i, (etiqueta, v) ->
            val x0 = i * ancho + ancho * 0.18f
            val x1 = (i + 1) * ancho - ancho * 0.18f
            val y = abajo - ((abajo - arriba) * (v / max)).toFloat()
            c.drawRoundRect(RectF(x0, y, x1, abajo), r, r, if (i == valores.lastIndex) ultima else barra)
            val cx = (x0 + x1) / 2
            c.drawText(etiqueta, cx, height - context.dp(5).toFloat(), txt)
            if (valores.size <= 8 || i == valores.lastIndex)
                c.drawText(abreviar(v), cx, y - context.dp(4), txt)
        }
    }

    private fun abreviar(v: Double): String = when {
        v >= 1_000_000 -> "%.1fM".format(v / 1_000_000)
        v >= 10_000 -> "%.0fk".format(v / 1_000)
        else -> "%.0f".format(v)
    }
}
