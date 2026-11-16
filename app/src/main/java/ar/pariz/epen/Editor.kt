package ar.pariz.epen

import android.app.Activity
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Diálogo para dar de alta, editar o borrar una conexión. */
object Editor {

    /** [listo] recibe la conexión guardada, o null si se borró. */
    fun abrir(act: Activity, actual: Suministro?, listo: (Suministro?) -> Unit) {
        val cont = act.vertical(20)
        fun campo(hint: String, valor: String, tipo: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS) =
            EditText(act).apply { this.hint = hint; setText(valor); inputType = tipo }

        val numero = campo("Suministro/Contrato (ej. 118310/1)", actual?.numero ?: "", InputType.TYPE_CLASS_TEXT)
        val alias = campo("Nombre (ej. Casa, Cabaña)", actual?.alias ?: "")
        val titular = campo("Titular (opcional)", actual?.titular ?: "")
        val direccion = campo("Dirección del suministro (opcional)", actual?.direccion ?: "")
        val notas = campo("Notas (opcional)", actual?.notas ?: "",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        val factura = campo("Comprobante de referencia (opcional, 0001-00003217)", actual?.ultimaFactura ?: "",
            InputType.TYPE_CLASS_TEXT)

        cont.addView(numero)
        listOf(alias, titular, direccion, notas, factura).forEach { cont.addView(it, cont.lp(6)) }
        cont.addView(act.texto(
            "El N° de conexión está en tu factura como \"Suministro/Contrato\". El comprobante de referencia solo " +
                "lo pide EPEN para reimprimir facturas anteriores y no debe ser la última emitida; la app lo completa sola " +
                "cuando ya bajaste alguna.",
            12f, color = GRIS_TEXTO
        ), cont.lp(8))

        val b = MaterialAlertDialogBuilder(act)
            .setTitle(if (actual == null) "Nueva conexión" else "Editar conexión")
            .setView(android.widget.ScrollView(act).apply { addView(cont) })
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
        if (actual != null) b.setNeutralButton("Borrar") { _, _ ->
            MaterialAlertDialogBuilder(act)
                .setMessage("¿Borrar ${actual.nombre} y sus comprobantes registrados? Los PDF descargados no se borran.")
                .setPositiveButton("Borrar") { _, _ -> Suministros.borrar(act, actual.clave); listo(null) }
                .setNegativeButton("Cancelar", null).show()
        }
        val d = b.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val num = Suministros.normalizar(numero.text.toString())
            val fac = factura.text.toString().trim()
            when {
                num == null -> numero.error = "Formato: números separados por \"/\" (ej. 118310/1)"
                !Suministros.facturaValida(fac) -> factura.error = "Formato: 0001-00003217"
                else -> {
                    val nueva = (actual ?: Suministro(num)).copy(
                        numero = num, alias = alias.text.toString().trim(), titular = titular.text.toString().trim(),
                        direccion = direccion.text.toString().trim(), notas = notas.text.toString().trim(),
                        ultimaFactura = fac
                    )
                    if (!Suministros.guardar(act, nueva, actual?.clave)) {
                        numero.error = "Esa conexión ya está registrada"
                    } else {
                        d.dismiss()
                        Toast.makeText(act, "Conexión guardada", Toast.LENGTH_SHORT).show()
                        listo(nueva)
                    }
                }
            }
        }
    }
}
