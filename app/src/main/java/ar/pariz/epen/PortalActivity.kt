package ar.pariz.epen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Abre una sección del portal del EPEN, completa la conexión por vos y deja el cursor
 * en el captcha. Con un solo captcha se pueden bajar todos los comprobantes que liste
 * la página de resultados; cada uno queda registrado en la conexión.
 */
class PortalActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SECCION = "seccion"
        const val EXTRA_SUMINISTRO = "suministro"
        const val EXTRA_ALIAS = "alias"
        const val EXTRA_FACTURA = "factura"
    }

    private lateinit var web: WebView
    private lateinit var progreso: ProgressBar
    private lateinit var ayuda: TextView
    private lateinit var fab: ExtendedFloatingActionButton
    private lateinit var seccion: Seccion
    private lateinit var suministro: String
    private lateinit var clave: String
    private var factura = ""

    // Las descargas se procesan de a una para no mezclar el registro.
    private val cola = Executors.newSingleThreadExecutor()
    private var enLote = false
    private val nuevos = AtomicInteger()
    private val repetidos = AtomicInteger()
    private val errores = AtomicInteger()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seccion = Seccion.valueOf(intent.getStringExtra(EXTRA_SECCION) ?: Seccion.FACTURA.name)
        suministro = intent.getStringExtra(EXTRA_SUMINISTRO) ?: ""
        clave = Suministros.clave(suministro)
        factura = intent.getStringExtra(EXTRA_FACTURA) ?: ""
        val alias = intent.getStringExtra(EXTRA_ALIAS).orEmpty()
        title = seccion.titulo
        supportActionBar?.subtitle = listOf(alias, suministro).filter { it.isNotBlank() }.joinToString(" · ")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progreso = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false; max = 100
        }
        ayuda = texto(textoAyuda(), 13f, color = GRIS_TEXTO).apply {
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setBackgroundColor(0xFFFFF7DB.toInt())
        }
        web = WebView(this)
        val refresco = SwipeRefreshLayout(this).apply {
            addView(web)
            setOnRefreshListener { web.reload(); isRefreshing = false }
        }
        fab = ExtendedFloatingActionButton(this).apply {
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { bajarTodas() }
        }
        val zona = FrameLayout(this).apply {
            addView(refresco)
            addView(fab, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.END
            ).apply { setMargins(dp(16), dp(16), dp(16), dp(24)) })
        }
        setContentView(vertical().apply {
            addView(progreso, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)))
            addView(ayuda)
            addView(zona, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        })

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportMultipleWindows(false) // window.open / target=_blank abren acá mismo
            javaScriptCanOpenWindowsAutomatically = true
        }
        web.addJavascriptInterface(Puente(), "EpenApp")
        web.webViewClient = Cliente()
        web.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView, p: Int) {
                progreso.progress = p
                progreso.visibility = if (p >= 100) View.GONE else View.VISIBLE
            }
        }
        web.setDownloadListener { url, ua, disp, mime, _ -> descargar(url, ua, disp, mime) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        if (savedInstanceState == null) web.loadUrl(seccion.url) else web.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    private fun textoAyuda(): String = when (seccion) {
        Seccion.DEUDA -> "Ya completé tu conexión. Leé y aceptá los términos, escribí el código de la imagen y tocá Ingresar."
        Seccion.ANTERIORES -> "Ya completé tu conexión" +
            (if (factura.isNotBlank()) " y el comprobante de referencia $factura" else "") +
            ". Escribí el código y tocá Ingresar. Después podés bajar todas las facturas de la lista con un toque."
        else -> "Ya completé tu conexión. Escribí el código tal cual la imagen (respetando minúsculas) y tocá Ingresar."
    }

    // ---------- Menú ----------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.agregarInicio()
        menu.add(0, 7, 0, "Ficha de la conexión")
        menu.add(0, 6, 0, "Bajar todas las de esta página")
        menu.add(0, 1, 1, "Nuevo código")
        menu.add(0, 2, 2, "Guardar página como PDF")
        menu.add(0, 3, 3, "Mis facturas")
        menu.add(0, 4, 4, "Abrir en el navegador")
        menu.add(0, 5, 5, "Volver a empezar")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            MENU_INICIO -> irAlInicio()
            7 -> {
                startActivity(Intent(this, ConexionActivity::class.java)
                    .putExtra(ConexionActivity.EXTRA_CLAVE, clave)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            }
            1 -> web.evaluateJavascript("window.recargar_captcha && recargar_captcha();", null)
            2 -> imprimirPagina()
            3 -> startActivity(Intent(this, ArchivoActivity::class.java))
            4 -> web.url?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
            5 -> web.loadUrl(seccion.url)
            6 -> bajarTodas()
        }
        return true
    }

    /** Respaldo: si el portal muestra el comprobante como página, se guarda con el diálogo de impresión. */
    private fun imprimirPagina() {
        val pm = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val nombre = "EPEN_${clave.replace("/", "-")}_${seccion.menu}"
        pm.print(nombre, web.createPrintDocumentAdapter(nombre), PrintAttributes.Builder().build())
    }

    private fun bajarTodas() {
        enLote = true
        nuevos.set(0); repetidos.set(0); errores.set(0)
        fab.isEnabled = false
        fab.text = "Bajando…"
        web.evaluateJavascript("window.__epenBajarTodas ? __epenBajarTodas() : EpenApp.finLote(0)", null)
    }

    // ---------- Navegación ----------
    private inner class Cliente : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
            val uri = req.url
            val esEpen = uri.host?.endsWith("epen.gov.ar") == true
            if (uri.scheme == "https" && esEpen) {
                if (uri.path?.endsWith(".pdf", true) == true) {
                    descargar(uri.toString(), view.settings.userAgentString, null, "application/pdf")
                    return true
                }
                return false
            }
            // Pagos (Mercado Pago, Link, Pago Mis Cuentas, etc.) y cualquier otro sitio: navegador del sistema.
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                Toast.makeText(this@PortalActivity, "No hay app para abrir ese enlace", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            progreso.visibility = View.VISIBLE
            fab.visibility = View.GONE
        }

        override fun onPageFinished(view: WebView, url: String) {
            view.evaluateJavascript(scriptPagina(), null)
            val enFormulario = url.contains("menu=${seccion.menu}") && !url.contains("_list")
            ayuda.visibility = if (enFormulario) View.VISIBLE else View.GONE
        }
    }

    private fun js(s: String) = s.replace("\\", "\\\\").replace("'", "\\'")

    /**
     * 1) Acomoda la página al celular y completa el formulario de la sección.
     * 2) En páginas de resultados: registra los comprobantes que aparecen, cuenta los
     *    que se pueden bajar y deja lista la función para bajarlos todos en la misma sesión.
     * 3) Los formularios sin captcha (botones de descargar/imprimir) se envían por fetch:
     *    si vuelve un PDF se guarda, si vuelve HTML se muestra.
     */
    private fun scriptPagina(): String = """
        (function(){
          if (window.__epenApp) return; window.__epenApp = true;
          var st = document.createElement('style');
          st.textContent = '#header{display:none!important} body{background:#F3F6FA!important}' +
            '.main.container{border:none!important;max-width:100%!important}' +
            'input.form-control{font-size:18px!important} #security_code{text-transform:none!important;letter-spacing:4px;min-width:120px;font-size:22px!important}';
          document.head && document.head.appendChild(st);

          var s = document.querySelector('input[name=sumcto]');
          if (s && !s.value) s.value = '${js(suministro)}';
          var f = document.getElementById('factura');
          if (f && !f.value && '${js(factura)}') f.value = '${js(factura)}';
          var c = document.getElementById('security_code');
          if (c) {
            c.setAttribute('autocomplete','off'); c.setAttribute('autocapitalize','none');
            c.setAttribute('autocorrect','off'); c.setAttribute('spellcheck','false');
            c.placeholder = 'tal cual la imagen';
            var img = document.getElementById('capt') || document.querySelector('img[src*=captcha]');
            if (img) { img.style.width='160px'; img.style.height='auto'; img.style.cursor='pointer';
              img.title='Tocá para otro código'; img.onclick=function(){ window.recargar_captcha && recargar_captcha(); }; }
            setTimeout(function(){ (document.getElementById('acepto') || c).scrollIntoView({block:'center'}); }, 300);
          }

          function aBase64(blob, cb){ var r = new FileReader(); r.onloadend = function(){ cb(String(r.result).split(',')[1]); }; r.readAsDataURL(blob); }

          // Pide algo en la misma sesión; si es PDF lo manda a la app. Devuelve true si fue PDF.
          function traer(url, opts){
            opts.credentials = 'include';
            return fetch(url, opts).then(function(r){
              var tipo = (r.headers.get('content-type') || '').toLowerCase();
              var disp = r.headers.get('content-disposition') || '';
              return r.blob().then(function(b){
                if (tipo.indexOf('pdf') >= 0 || /\.pdf/i.test(disp)) {
                  return new Promise(function(ok){ aBase64(b, function(d){ EpenApp.guardarPdf(d, disp, url); ok({pdf:true}); }); });
                }
                return b.text().then(function(t){ return {pdf:false, html:t}; });
              });
            });
          }

          function pedidoDeFormulario(form, boton){
            var url = new URL(form.getAttribute('action') || location.href, location.href);
            var datos = new FormData(form);
            if (boton && boton.name) datos.append(boton.name, boton.value || '');
            var metodo = (form.getAttribute('method') || 'GET').toUpperCase();
            var opts = { method: metodo };
            if (metodo === 'GET') datos.forEach(function(v,k){ url.searchParams.set(k, v); }); else opts.body = datos;
            return { url: url, opts: opts };
          }

          // Enlaces y formularios de la página que probablemente entreguen un comprobante.
          var navegacion = /^(\.?\/?(index\.php)?)?\?menu=[a-z0-9_]*$/i;
          var pista = /pdf|imprim|descarg|reimpr|factura|liquid|comprob|recibo|cup[oó]n|ver/i;
          function candidatos(){
            var out = [];
            document.querySelectorAll('a[href]').forEach(function(a){
              var h = a.getAttribute('href') || '';
              if (!h || h.charAt(0) === '#' || /^(javascript|mailto|tel):/i.test(h) || navegacion.test(h)) return;
              var u; try { u = new URL(h, location.href); } catch(e) { return; }
              if (u.host !== location.host || /captcha|\.(css|js|jpg|png|gif)$/i.test(u.pathname)) return;
              if (pista.test(h) || pista.test(a.textContent) || a.querySelector('img')) out.push({tipo:'a', url:u.toString()});
            });
            document.querySelectorAll('form').forEach(function(fm){
              if (fm.querySelector('[name=security_code]')) return;
              var u = new URL(fm.getAttribute('action') || location.href, location.href);
              if (u.host !== location.host) return;
              out.push({tipo:'form', form:fm});
            });
            var vistos = {};
            return out.filter(function(x){ var k = x.tipo === 'a' ? x.url : Math.random(); if (vistos[k]) return false; vistos[k] = 1; return true; });
          }

          window.__epenBajarTodas = function(){
            var lista = candidatos(), i = 0, pdfs = 0;
            function siguiente(){
              if (i >= lista.length) { EpenApp.finLote(pdfs); return; }
              var x = lista[i++], p;
              if (x.tipo === 'a') p = traer(x.url, { method: 'GET' });
              else { var r = pedidoDeFormulario(x.form, x.form.querySelector('[type=submit][name]')); p = traer(r.url.toString(), r.opts); }
              p.then(function(res){ if (res.pdf) pdfs++; }).catch(function(){}).then(function(){ setTimeout(siguiente, 400); });
            }
            siguiente();
          };

          if (!c) {
            EpenApp.registrarPagina(document.body ? document.body.innerText : '');
            EpenApp.candidatos(candidatos().length);
          }

          document.addEventListener('submit', function(ev){
            var form = ev.target;
            if (!form || form.querySelector('[name=security_code]')) return; // los formularios con captcha van normal
            var r = pedidoDeFormulario(form, ev.submitter);
            if (r.url.host !== location.host) return;
            ev.preventDefault();
            EpenApp.estado('Consultando…');
            traer(r.url.toString(), r.opts).then(function(res){
              if (!res.pdf) { document.open(); document.write(res.html); document.close(); }
            }).catch(function(e){ EpenApp.error(String(e)); });
          }, true);
        })();
    """.trimIndent()

    // ---------- PDF ----------
    private fun descargar(url: String, ua: String?, disp: String?, mime: String?) {
        Snackbar.make(web, "Descargando factura…", Snackbar.LENGTH_SHORT).show()
        val ref = web.url
        cola.execute {
            try {
                val g = Descargas.bajar(this, url, ua, disp, mime, clave, seccion.menu, ref)
                runOnUiThread { listo(g) }
            } catch (e: Exception) {
                runOnUiThread { fallo(e.message ?: "Error desconocido") }
            }
        }
    }

    private fun resumen(g: Guardado): String {
        val d = g.datos
        val partes = listOfNotNull(
            d.numero?.let { "N° $it" },
            d.total?.let { "$ " + "%,.2f".format(it).replace(",", "#").replace(".", ",").replace("#", ".") },
            d.vencimiento.takeIf { it.isNotBlank() }?.let { "vence $it" }
        )
        return if (partes.isEmpty()) "Factura guardada" else partes.joinToString(" · ")
    }

    private fun listo(g: Guardado) {
        val msg = if (g.repetido) "Ya la tenías: ${resumen(g)}" else "Guardada: ${resumen(g)}"
        Snackbar.make(web, msg, Snackbar.LENGTH_LONG)
            .setAction("Abrir") { Descargas.abrir(this, g.archivo) }.show()
    }

    private fun fallo(msg: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("No se pudo guardar el PDF")
            .setMessage("$msg\n\nPodés probar \"Guardar página como PDF\" desde el menú, o abrir la consulta en el navegador.")
            .setPositiveButton("Entendido", null).show()
    }

    private inner class Puente {
        @JavascriptInterface
        fun guardarPdf(base64: String, disposicion: String, url: String) {
            cola.execute {
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val nombre = URLUtil.guessFileName(url, disposicion, "application/pdf")
                    val g = Descargas.guardar(this@PortalActivity, bytes, clave, seccion.menu, nombre)
                    if (enLote) { if (g.repetido) repetidos.incrementAndGet() else nuevos.incrementAndGet() }
                    else runOnUiThread { listo(g) }
                } catch (e: Exception) {
                    if (enLote) errores.incrementAndGet() else runOnUiThread { fallo(e.message ?: "Error") }
                }
            }
        }

        /** Registra los comprobantes que muestra una página de resultados. */
        @JavascriptInterface
        fun registrarPagina(texto: String) {
            cola.execute {
                val leidos = Lector.leerPagina(texto)
                for (d in leidos) {
                    val n = d.numero ?: continue
                    Suministros.registrar(this@PortalActivity, Comprobante(
                        clave = clave, numero = n, vencimiento = d.vencimiento, total = d.total, origen = "portal"
                    ))
                }
                if (leidos.isNotEmpty()) runOnUiThread {
                    Snackbar.make(web, "${leidos.size} comprobante(s) anotados en la conexión", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        fun candidatos(n: Int) = runOnUiThread {
            if (n > 0 && !enLote) {
                fab.text = if (n == 1) "Bajar comprobante" else "Bajar todo ($n)"
                fab.isEnabled = true
                fab.visibility = View.VISIBLE
            }
        }

        /** Llega cuando JS terminó de pedir todo; se espera a que la cola guarde lo último. */
        @JavascriptInterface
        fun finLote(pdfs: Int) {
            cola.execute {
                runOnUiThread {
                    enLote = false
                    fab.isEnabled = true
                    fab.visibility = View.GONE
                    val msg = when {
                        pdfs == 0 && nuevos.get() == 0 ->
                            "Esta página no entregó PDF. Abrí un comprobante a mano o usá \"Guardar página como PDF\"."
                        else -> buildString {
                            append("${nuevos.get()} factura(s) nuevas")
                            if (repetidos.get() > 0) append(", ${repetidos.get()} que ya tenías")
                            if (errores.get() > 0) append(", ${errores.get()} con error")
                            append(".")
                        }
                    }
                    Snackbar.make(web, msg, Snackbar.LENGTH_LONG)
                        .setAction("Ver") { startActivity(Intent(this@PortalActivity, ArchivoActivity::class.java)) }
                        .show()
                }
            }
        }

        @JavascriptInterface
        fun estado(t: String) = runOnUiThread { Snackbar.make(web, t, Snackbar.LENGTH_SHORT).show() }

        @JavascriptInterface
        fun error(t: String) = runOnUiThread { fallo(t) }
    }

    override fun onDestroy() {
        web.destroy()
        cola.shutdown()
        super.onDestroy()
    }
}
