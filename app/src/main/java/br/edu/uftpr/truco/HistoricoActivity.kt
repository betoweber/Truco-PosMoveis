package br.edu.uftpr.truco

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HistoricoActivity : AppCompatActivity() {
    private lateinit var lvHistorico: ListView
    private lateinit var tvVazio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historico)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lvHistorico = findViewById(R.id.lvHistorico)
        tvVazio = findViewById(R.id.tvVazio)
        val linhas = intent.getStringArrayListExtra(EXTRA_LINHAS) ?: ArrayList()
        lvHistorico.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, linhas)

        if (linhas.isEmpty()) tvVazio.visibility = View.VISIBLE
    }

    companion object {
        const val EXTRA_LINHAS = "linhas"
    }
}