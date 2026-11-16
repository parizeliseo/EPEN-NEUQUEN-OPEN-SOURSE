package ar.pariz.epen

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ResumenTest {

    private val factura = """
        Liquidación de Servicios Públicos   N° 00007-16504550
        Vencimiento: 10/11/2026
        Cargo fijo                          1.200,00
        Energía consumida   14 kWh   85,10  1.191,40
        Alumbrado Público                   3.450,20
        Tasa municipal                        512,30
        I.V.A. 21,00 %                      1.380,77
        Ingresos Brutos                       210,00
        Interés por pago fuera de término     150,45
        Subtotal                            8.095,22
        TOTAL A PAGAR $                     8.095,22
        Talón para el EPEN
        Alumbrado Público                   3.450,20
        TOTAL A PAGAR                       8.095,22
    """.trimIndent()

    @Test fun clasificaConceptosSinRepetirElTalon() {
        val c = Lector.conceptos(factura)
        val porCat = c.groupBy { it.categoria }.mapValues { e -> e.value.sumOf { it.importe } }
        assertEquals(3450.20, porCat[Categoria.ALUMBRADO]!!, 0.001)
        assertEquals(512.30 + 1380.77 + 210.00, porCat[Categoria.IMPUESTO]!!, 0.001)
        assertEquals(150.45, porCat[Categoria.INTERES]!!, 0.001)
        assertEquals(1200.00 + 1191.40, porCat[Categoria.ENERGIA]!!, 0.001)
        assertEquals("Energía consumida", c.first { it.categoria == Categoria.ENERGIA && it.importe == 1191.40 }.nombre)
    }

    @Test fun resumenDelUltimoAnioCuentaCadaFacturaUnaVez() {
        val conc = Lector.conceptos(factura)
        val comps = listOf(
            Comprobante("1/1", "00007-1", "10/11/2026", 8095.22, 14, conceptos = conc),
            Comprobante("1/1", "00007-2", "10/09/2026", 9000.0, 15, conceptos = conc),
            Comprobante("1/1", "00007-3", "10/09/2025", 7000.0, 36) // fuera del último año
        )
        val hoy = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 15) }
        val r = ResumenAnual.de(comps, hoy)
        assertEquals(2, r.facturas)
        assertEquals(29, r.kwh)
        assertEquals(17095.22, r.total, 0.001)
        assertEquals(2 * 3450.20, r.alumbrado, 0.001)
        assertEquals(2 * 150.45, r.intereses, 0.001)
        assertEquals(2, r.conDesglose)
    }
}
