package co.edu.unal.directorioempresas.ui

import android.content.Context
import co.edu.unal.directorioempresas.R
import co.edu.unal.directorioempresas.data.Empresa
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Muestra un diálogo de confirmación antes de eliminar una empresa.
 */
fun mostrarConfirmacionEliminar(
    context: Context,
    empresa: Empresa,
    onConfirmar: () -> Unit
) {
    MaterialAlertDialogBuilder(context)
        .setTitle(R.string.eliminar_empresa_titulo)
        .setMessage(context.getString(R.string.eliminar_empresa_mensaje, empresa.nombre))
        .setPositiveButton(R.string.btn_eliminar) { dialog, _ ->
            onConfirmar()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.btn_cancelar) { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
