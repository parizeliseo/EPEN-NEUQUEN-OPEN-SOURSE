package ar.pariz.epen

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

/**
 * Base de todas las pantallas. Desde Android 15 las apps se dibujan de borde a borde
 * (detrás de la barra de estado y la de navegación), y desde Android 16 no se puede
 * desactivar. Esta base pone una barra superior propia y corre el contenido según los
 * bordes del sistema y el teclado, así nada queda tapado en ninguna versión.
 */
open class Pantalla : AppCompatActivity() {

    protected lateinit var barra: MaterialToolbar
    private lateinit var contenedor: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),          // íconos claros sobre el azul
            // íconos oscuros abajo; en Android 8.0 (sin íconos oscuros) se usa un velo gris para que se vean
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.argb(0x99, 0x1B, 0x24, 0x30))
        )
        super.onCreate(savedInstanceState)

        val tema = ContextThemeWrapper(this, androidx.appcompat.R.style.ThemeOverlay_AppCompat_Dark_ActionBar)
        val alto = TypedValue().let {
            if (theme.resolveAttribute(android.R.attr.actionBarSize, it, true))
                TypedValue.complexToDimensionPixelSize(it.data, resources.displayMetrics) else dp(56)
        }
        barra = MaterialToolbar(tema).apply {
            setTitleTextColor(Color.WHITE)
            setSubtitleTextColor(Color.parseColor("#CCFFFFFF"))
            popupTheme = com.google.android.material.R.style.ThemeOverlay_Material3_Light
        }
        // La barra va dentro de una franja azul que crece con la barra de estado.
        val franja = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(AZUL)
            addView(barra, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, alto))
        }
        contenedor = FrameLayout(this)
        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(FONDO)
            addView(franja, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(contenedor, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { _, ins ->
            val b = ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val teclado = ins.getInsets(WindowInsetsCompat.Type.ime())
            franja.setPadding(b.left, b.top, b.right, 0)
            contenedor.setPadding(b.left, 0, b.right, maxOf(b.bottom, teclado.bottom))
            WindowInsetsCompat.CONSUMED
        }
        super.setContentView(raiz)
        setSupportActionBar(barra)
    }

    /** El contenido de cada pantalla va debajo de la barra. */
    override fun setContentView(view: View?) {
        if (view == null) return
        contenedor.removeAllViews()
        contenedor.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }
}
