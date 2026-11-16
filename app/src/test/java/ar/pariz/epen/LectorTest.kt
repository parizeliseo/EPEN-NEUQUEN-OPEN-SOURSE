package ar.pariz.epen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LectorTest {

    private val factura = """
        Liquidación de Servicios Públicos - Clase B   Hoja 1 / 1
        N° 00007-03125034   -Vencimiento: 21/10/2026
        SUMINISTRO / CONTRATO N°: 00118310/01
        FACTURA N° 0001-00003217 C.Interno
        Período Facturación: 01/08/2026 al 30/09/2026
        Consumo del período 412 kWh
        Subtotal energía $ 31.250,40
        TOTAL A PAGAR $ 45.873,12
    """.trimIndent()

    @Test fun leeNumeros() {
        val d = Lector.leerFactura(factura)
        assertEquals("0001-00003217", d.factura)
        assertEquals("00007-03125034", d.liquidacion)
        assertEquals("0001-00003217", d.numero)
    }

    @Test fun leeVencimientoTotalYConsumo() {
        val d = Lector.leerFactura(factura)
        assertEquals("21/10/2026", d.vencimiento)
        assertEquals(45873.12, d.total!!, 0.001)
        assertEquals(412, d.consumoKwh)
        assertEquals("01/08/2026 al 30/09/2026", d.periodo)
    }

    @Test fun consumoConSeparadorDeMiles() {
        assertEquals(1250, Lector.leerFactura("Consumo 1.250 kWh").consumoKwh)
    }

    @Test fun noInventaDatos() {
        val d = Lector.leerFactura("Texto sin datos de factura")
        assertNull(d.numero); assertNull(d.total); assertNull(d.consumoKwh)
        assertEquals("", d.vencimiento)
    }

    @Test fun leePaginaConVariosComprobantes() {
        val pagina = """
            Comprobante  Vencimiento  Importe
            00007-03125034  21/10/2026  $ 45.873,12
            00007-02988110  21/08/2026  $ 39.100,00
            Volver
        """.trimIndent()
        val l = Lector.leerPagina(pagina)
        assertEquals(2, l.size)
        assertEquals("00007-03125034", l[0].numero)
        assertEquals("21/10/2026", l[0].vencimiento)
        assertEquals(39100.0, l[1].total!!, 0.001)
    }

    @Test fun claveIgnoraCerosDeRelleno() {
        assertEquals(Suministros.clave("118310/1"), Suministros.clave("00118310/01"))
        assertEquals("118310/1", Suministros.normalizar(" 118310-1 "))
        assertNull(Suministros.normalizar("118310"))
    }
}
