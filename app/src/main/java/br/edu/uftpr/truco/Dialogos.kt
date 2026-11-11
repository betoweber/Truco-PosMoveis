package br.edu.uftpr.truco

import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

object Dialogos {

    fun confirmar(
        activity: Activity,
        titulo: String,
        mensagem: String,
        textoCancelar: String,
        textoConfirmar: String,
        aoConfirmar: () -> Unit
    ) {
        val dialog = Dialog(activity, R.style.Truco_Dialogo)
        dialog.setContentView(R.layout.dialog_pixel)
        dialog.findViewById<TextView>(R.id.tvDialogoTitulo).text = titulo
        dialog.findViewById<TextView>(R.id.tvDialogoMensagem).text = mensagem

        val btnCancelar = dialog.findViewById<Button>(R.id.btnDialogoCancelar)
        val btnConfirmar = dialog.findViewById<Button>(R.id.btnDialogoConfirmar)
        btnCancelar.text = textoCancelar
        btnConfirmar.text = textoConfirmar
        btnCancelar.setOnClickListener { dialog.dismiss() }
        btnConfirmar.setOnClickListener {
            dialog.dismiss()
            aoConfirmar()
        }
        Efeitos.aplicarToque(btnCancelar, btnConfirmar)

        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}
