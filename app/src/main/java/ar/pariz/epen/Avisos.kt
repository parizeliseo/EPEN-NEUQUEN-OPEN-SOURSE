package ar.pariz.epen

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Avisos de la app:
 *  - vencimiento de facturas: el día anterior y el mismo día, con sonido;
 *  - nueva versión publicada en GitHub.
 * Un trabajo periódico (cada ~6 h) los revisa y además relee los PDF nuevos.
 */
object Avisos {
    const val CANAL_VENC = "vencimientos"
    const val CANAL_APP = "actualizaciones"
    private const val TRABAJO = "revision_diaria"
    private const val PREFS = "avisos"
    const val REPO = "parizeliseo/EPEN-NEUQUEN-OPEN-SOURSE"

    fun activos(ctx: Context) = ctx.getSharedPreferences(PREFS, 0).getBoolean("activos", true)

    fun activar(ctx: Context, si: Boolean) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean("activos", si).apply()
        programar(ctx)
    }

    fun crearCanales(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        nm.createNotificationChannel(NotificationChannel(CANAL_VENC, "Vencimientos de facturas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso con sonido el día anterior y el día del vencimiento"
            setSound(sonido, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
            enableVibration(true)
            enableLights(true)
        })
        nm.createNotificationChannel(NotificationChannel(CANAL_APP, "Nuevas versiones", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Aviso cuando hay una versión nueva de EPEN al día"
        })
    }

    fun programar(ctx: Context) {
        runCatching { crearCanales(ctx) }
        runCatching {
            val pedido = PeriodicWorkRequestBuilder<RevisionWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }
    }

    fun puedeNotificar(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    /** Facturas que vencen hoy o mañana y todavía no se avisaron. Devuelve cuántos avisos mostró. */
    fun revisarVencimientos(ctx: Context): Int {
        if (!activos(ctx) || !puedeNotificar(ctx)) return 0
        val prefs = ctx.getSharedPreferences(PREFS, 0)
        val avisados = prefs.getStringSet("avisados", emptySet())!!.toMutableSet()
        var n = 0
        for (s in Suministros.listar(ctx)) {
            // la factura más reciente de cada conexión es la que puede estar por vencer
            val c = Suministros.comprobantes(ctx, s.clave).firstOrNull { it.vencimiento.isNotBlank() } ?: continue
            val dias = diasHasta(c.vencimiento) ?: continue
            if (dias != 0L && dias != 1L) continue
            val clave = "${s.clave}|${c.numero}|$dias"
            if (clave in avisados) continue
            mostrarVencimiento(ctx, s, c, dias)
            avisados.add(clave)
            n++
        }
        // conserva solo los avisos de los últimos meses
        prefs.edit().putStringSet("avisados", avisados.toList().takeLast(200).toSet()).apply()
        return n
    }

    private fun mostrarVencimiento(ctx: Context, s: Suministro, c: Comprobante, dias: Long) {
        val abrir = PendingIntent.getActivity(ctx, s.clave.hashCode(),
            Intent(ctx, ConexionActivity::class.java).putExtra(ConexionActivity.EXTRA_CLAVE, s.clave)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pagar = PendingIntent.getActivity(ctx, s.clave.hashCode() + 1,
            Intent(ctx, PortalActivity::class.java)
                .putExtra(PortalActivity.EXTRA_SECCION, Seccion.FACTURA.name)
                .putExtra(PortalActivity.EXTRA_SUMINISTRO, s.numero)
                .putExtra(PortalActivity.EXTRA_ALIAS, s.alias)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cuando = if (dias == 0L) "Hoy vence" else "Mañana vence"
        val importe = c.total?.let { " por ${pesos(it)}" } ?: ""
        val n = NotificationCompat.Builder(ctx, CANAL_VENC)
            .setSmallIcon(R.drawable.ic_bolt)
            .setColor(AZUL)
            .setContentTitle("$cuando la factura de ${s.nombre}")
            .setContentText("Conexión ${s.numero}$importe · ${c.vencimiento}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Conexión ${s.numero}$importe.\nVencimiento: ${c.vencimiento}. Si ya la pagaste, ignorá este aviso."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // sonido y vibración en Android 7 o anterior
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .addAction(0, "Pagar / descargar", pagar)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(("v" + s.clave).hashCode(), n) } catch (_: SecurityException) {}
    }

    // ---------- nueva versión ----------

    data class Version(val nombre: String, val url: String)

    /** Consulta la última Release publicada en GitHub. Devuelve null si no hay una más nueva. */
    fun versionNueva(ctx: Context): Version? = try {
        val con = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000; readTimeout = 10000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        if (con.responseCode != 200) null else {
            val o = JSONObject(con.inputStream.bufferedReader().readText())
            val tag = o.optString("tag_name").removePrefix("v")
            val actual = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
            val apk = o.optJSONArray("assets")?.let { a ->
                (0 until a.length()).map { a.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk") }?.optString("browser_download_url")
            } ?: o.optString("html_url")
            if (esMasNueva(tag, actual)) Version(tag, apk) else null
        }
    } catch (_: Exception) { null }

    fun esMasNueva(a: String, b: String): Boolean {
        val x = a.split(".").map { it.toIntOrNull() ?: 0 }
        val y = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    fun guardarVersion(ctx: Context, v: Version?) = ctx.getSharedPreferences(PREFS, 0).edit()
        .putString("nueva_nombre", v?.nombre).putString("nueva_url", v?.url)
        .putLong("nueva_revisada", System.currentTimeMillis()).apply()

    fun versionGuardada(ctx: Context): Version? {
        val p = ctx.getSharedPreferences(PREFS, 0)
        val n = p.getString("nueva_nombre", null) ?: return null
        val actual = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
        return if (esMasNueva(n, actual)) Version(n, p.getString("nueva_url", "")!!) else null
    }

    fun ultimaRevisionVersion(ctx: Context) = ctx.getSharedPreferences(PREFS, 0).getLong("nueva_revisada", 0)

    fun notificarVersion(ctx: Context, v: Version) {
        val p = ctx.getSharedPreferences(PREFS, 0)
        if (p.getString("version_avisada", "") == v.nombre || !puedeNotificar(ctx)) return
        val abrir = PendingIntent.getActivity(ctx, 77, Intent(Intent.ACTION_VIEW, Uri.parse(v.url)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CANAL_APP)
            .setSmallIcon(R.drawable.ic_bolt)
            .setColor(AZUL)
            .setContentTitle("EPEN al día ${v.nombre} disponible")
            .setContentText("Tocá para descargar la nueva versión.")
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(77, n) } catch (_: SecurityException) {}
        p.edit().putString("version_avisada", v.nombre).apply()
    }
}

/** Trabajo en segundo plano: relee PDF nuevos, avisa vencimientos y busca versión nueva una vez por día. */
class RevisionWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        runCatching { Registro.actualizarSiHaceFalta(ctx) }
        runCatching { Avisos.revisarVencimientos(ctx) }
        if (System.currentTimeMillis() - Avisos.ultimaRevisionVersion(ctx) > TimeUnit.HOURS.toMillis(20)) {
            val v = Avisos.versionNueva(ctx)
            Avisos.guardarVersion(ctx, v)
            v?.let { Avisos.notificarVersion(ctx, it) }
        }
        return Result.success()
    }
}

/** Mantiene el registro al día sin intervención del usuario. */
object Registro {
    /** Subir este número cuando cambien las reglas de lectura obliga a releer todos los PDF. */
    const val VERSION_LECTOR = 4

    /**
     * Si cambió el lector, relee todos los PDF; si no, solo registra los nuevos y une duplicados.
     * Devuelve true si algo cambió. Se llama fuera del hilo principal.
     */
    @Synchronized
    fun actualizarSiHaceFalta(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences("epen", 0)
        return if (p.getInt("registro_version", 0) < VERSION_LECTOR) {
            Descargas.actualizarRegistro(ctx)
            p.edit().putInt("registro_version", VERSION_LECTOR).apply()
            true
        } else {
            val nuevos = Descargas.reindexar(ctx)
            val unidos = Suministros.depurar(ctx)
            nuevos + unidos > 0
        }
    }

}
