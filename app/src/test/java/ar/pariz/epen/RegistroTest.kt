package ar.pariz.epen

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Registro de conexiones y comprobantes: persistencia, lotes y capacidad. */
@RunWith(RobolectricTestRunner::class)
class RegistroTest {

    private lateinit var ctx: Context
    private val archivo get() = File(ctx.filesDir, "registro.json")

    @Before fun limpiar() {
        ctx = ApplicationProvider.getApplicationContext()
        ctx.filesDir.listFiles()?.forEach { it.deleteRecursively() }
        Suministros.reiniciarCache()
    }

    @Test fun guardaYRecuperaDelDisco() {
        assertTrue(Suministros.guardar(ctx, Suministro("00147351/02", "Terreno", "Eliseo", "Tobiano")))
        Suministros.registrar(ctx, Comprobante("147351/2", "00007-1", "10/11/2026", 26259.76, 14))
        Suministros.reiniciarCache()  // simula reabrir la app
        val s = Suministros.buscar(ctx, "147351/2")!!
        assertEquals("Terreno", s.alias)
        assertEquals(26259.76, Suministros.comprobantes(ctx, "147351/2").single().total!!, 0.001)
    }

    @Test fun noDuplicaLaMismaConexionConCeros() {
        assertTrue(Suministros.guardar(ctx, Suministro("147351/2", "Terreno")))
        assertFalse(Suministros.guardar(ctx, Suministro("00147351/02", "Otro")))
        assertEquals(1, Suministros.listar(ctx).size)
    }

    @Test fun unaFacturaConDosNumerosEsUnSoloComprobante() {
        Suministros.guardar(ctx, Suministro("147351/2"))
        Suministros.registrar(ctx, Comprobante("147351/2", "0000-24252201", "10/11/2026", 26259.76, 14, otroNumero = "00007-16504550"))
        Suministros.registrar(ctx, Comprobante("147351/2", "00007-16504550", "10/11/2026", 26259.76))
        assertEquals(1, Suministros.comprobantes(ctx, "147351/2").size)
    }

    @Test fun elLoteEscribeUnaSolaVezAlFinal() {
        Suministros.guardar(ctx, Suministro("1/1"))
        val antes = archivo.lastModified()
        Thread.sleep(20)
        Suministros.lote(ctx) {
            repeat(50) { Suministros.registrar(ctx, Comprobante("1/1", "00007-$it", "10/01/2026", 100.0)) }
            // en memoria ya están, en disco todavía no
            assertEquals(50, Suministros.comprobantes(ctx, "1/1").size)
            assertEquals(antes, archivo.lastModified())
        }
        Suministros.reiniciarCache()
        assertEquals(50, Suministros.comprobantes(ctx, "1/1").size)
    }

    @Test fun ordenaDelMasNuevoAlMasViejo() {
        Suministros.guardar(ctx, Suministro("1/1"))
        listOf("10/03/2026", "10/11/2026", "10/07/2025").forEachIndexed { i, f ->
            Suministros.registrar(ctx, Comprobante("1/1", "00007-$i", f, 1.0))
        }
        assertEquals(listOf("10/11/2026", "10/03/2026", "10/07/2025"),
            Suministros.comprobantes(ctx, "1/1").map { it.vencimiento })
    }

    @Test fun respaldoIdaYVuelta() {
        Suministros.guardar(ctx, Suministro("147351/2", "Terreno", "Eliseo", "Tobiano"))
        Suministros.registrar(ctx, Comprobante("147351/2", "00007-1", "10/11/2026", 26259.76, 14,
            conceptos = listOf(Concepto("Alumbrado Público", 3450.2, Categoria.ALUMBRADO))))
        val json = Suministros.exportar(ctx)
        limpiar()
        val (nuevas, comps) = Suministros.importar(ctx, json)
        assertEquals(1, nuevas); assertEquals(1, comps)
        val c = Suministros.comprobantes(ctx, "147351/2").single()
        assertEquals(Categoria.ALUMBRADO, c.conceptos.single().categoria)
        // importar dos veces no duplica
        Suministros.importar(ctx, json)
        assertEquals(1, Suministros.comprobantes(ctx, "147351/2").size)
    }

    @Test fun archivoDanadoNoRompeLaApp() {
        archivo.writeText("{esto no es json")
        Suministros.reiniciarCache()
        assertEquals(0, Suministros.listar(ctx).size)
        assertTrue(Suministros.guardar(ctx, Suministro("1/1")))
    }

    /** Capacidad: 20 conexiones con 50 facturas cada una (1000 comprobantes). */
    @Test fun milComprobantesRapido() {
        val t0 = System.nanoTime()
        Suministros.lote(ctx) {
            for (k in 1..20) {
                Suministros.guardar(ctx, Suministro("$k/1", "Conexión $k"))
                for (i in 1..50) Suministros.registrar(ctx, Comprobante("$k/1", "00007-${k * 1000 + i}",
                    "%02d/%02d/20%02d".format(10, (i % 12) + 1, 20 + i / 12), 1000.0 + i, i,
                    conceptos = listOf(Concepto("IVA", 21.0, Categoria.IMPUESTO), Concepto("Alumbrado Público", 9.0, Categoria.ALUMBRADO))))
            }
        }
        val alta = (System.nanoTime() - t0) / 1_000_000
        Suministros.reiniciarCache()
        val t1 = System.nanoTime()
        // lo que hace la pantalla principal: listar y resumir cada conexión
        repeat(10) { Suministros.listar(ctx).forEach { ResumenAnual.de(Suministros.comprobantes(ctx, it.clave)) } }
        val lectura = (System.nanoTime() - t1) / 1_000_000
        println("1000 comprobantes: alta $alta ms, 10 dibujos de inicio $lectura ms")
        assertEquals(1000, Suministros.todosLosComprobantes(ctx).size)
        assertTrue("alta demasiado lenta: $alta ms", alta < 10_000)
        assertTrue("lectura demasiado lenta: $lectura ms", lectura < 3_000)
    }
}
