package br.edu.uftpr.truco

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Tela cheia com barras do sistema transparentes e ícones claros.
 * O app tem um único tema escuro (feltro), então não deve seguir o modo claro/escuro do sistema.
 */
fun ComponentActivity.edgeToEdgeEscuro() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
    )
}
