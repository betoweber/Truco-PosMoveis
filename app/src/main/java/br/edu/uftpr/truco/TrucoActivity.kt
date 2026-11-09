package br.edu.uftpr.truco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TrucoActivity : AppCompatActivity() {
    private lateinit var tvNomeJ1: TextView
    private lateinit var tvPtsJ1: TextView
    private lateinit var tvNomeJ2: TextView
    private lateinit var tvPtsJ2: TextView

    private var nomeJ1 = ""
    private var pontosJ1 = 0
    private var vitoriasJ1 = 0
    private var nomeJ2 = ""
    private var pontosJ2 = 0
    private var vitoriasJ2 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_truco)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvNomeJ1 = findViewById(R.id.tvNomeJ1)
        tvPtsJ1 = findViewById(R.id.tvPtsJ1)
        tvNomeJ2 = findViewById(R.id.tvNomeJ2)
        tvPtsJ2 = findViewById(R.id.tvPtsJ2)

        nomeJ1 = intent.getStringExtra(EXTRA_NOME_J1) ?: getString(R.string.nome_jogador_1)
        nomeJ2 = intent.getStringExtra(EXTRA_NOME_J2) ?: getString(R.string.nome_jogador_2)
        vitoriasJ1 = intent.getIntExtra(EXTRA_VITORIAS_J1, 0)
        vitoriasJ2 = intent.getIntExtra(EXTRA_VITORIAS_J2, 0)


        if (savedInstanceState != null) {
            pontosJ1 = savedInstanceState.getInt(STATE_PONTOS_J1)
            pontosJ2 = savedInstanceState.getInt(STATE_PONTOS_J2)
            vitoriasJ1 = savedInstanceState.getInt(STATE_VITORIAS_J1)
            vitoriasJ2 = savedInstanceState.getInt(STATE_VITORIAS_J2)
            devolverResultado()
        }

        atualizarTela()

        onBackPressedDispatcher.addCallback(this) {
            if (pontosJ1 == 0 && pontosJ2 == 0) {
                finish()
            } else {
                AlertDialog.Builder(this@TrucoActivity)
                    .setTitle(getString(R.string.titulo_descarte_dialog))
                    .setMessage(getString(R.string.mensagem_descarte_dialog))
                    .setPositiveButton(getString(R.string.btn_abandonar_dialog)) { _, _ -> finish() }
                    .setNegativeButton(getString(R.string.btn_continuar_dialog), null).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PONTOS_J1, pontosJ1)
        outState.putInt(STATE_VITORIAS_J1, vitoriasJ1)
        outState.putInt(STATE_PONTOS_J2, pontosJ2)
        outState.putInt(STATE_VITORIAS_J2, vitoriasJ2)
    }

    fun btnPontuarOnClick(view: View) {
        when (view.id) {
            R.id.btnJ1Um -> pontuar(1, 1)
            R.id.btnJ1Tres -> pontuar(1, 3)
            R.id.btnJ1Seis -> pontuar(1, 6)
            R.id.btnJ1Nove -> pontuar(1, 9)
            R.id.btnJ1Doze -> pontuar(1, 12)
            R.id.btnJ2Um -> pontuar(2, 1)
            R.id.btnJ2Tres -> pontuar(2, 3)
            R.id.btnJ2Seis -> pontuar(2, 6)
            R.id.btnJ2Nove -> pontuar(2, 9)
            R.id.btnJ2Doze -> pontuar(2, 12)
        }
    }

    private fun pontuar(jogador: Int, pontos: Int) {
        if (jogador == 1) {
            pontosJ1 += pontos
            if (pontosJ1 >= 12) registrarVitoria(1)
        } else {
            pontosJ2 += pontos
            if (pontosJ2 >= 12) registrarVitoria(2)
        }
        atualizarTela()
    }

    private fun registrarVitoria(jogador: Int) {
        if (jogador == 1) vitoriasJ1++ else vitoriasJ2++
        pontosJ1 = 0
        pontosJ2 = 0

        val vencedor = if (jogador == 1) nomeJ1 else nomeJ2
        devolverResultado()
        Toast.makeText(
            this,
            getString(R.string.toast_vitoria, vencedor, vitoriasJ1, vitoriasJ2),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun devolverResultado() {
        val intent = Intent()
        intent.putExtra(EXTRA_VITORIAS_J1, vitoriasJ1)
        intent.putExtra(EXTRA_VITORIAS_J2, vitoriasJ2)
        setResult(RESULT_OK, intent)
    }

    private fun atualizarTela() {
        tvNomeJ1.text = nomeJ1
        tvPtsJ1.text = pontosJ1.toString()
        tvNomeJ2.text = nomeJ2
        tvPtsJ2.text = pontosJ2.toString()
    }

    fun btnZerarOnClick(view: View) {
        pontosJ1 = 0
        pontosJ2 = 0
        vitoriasJ1 = 0
        vitoriasJ2 = 0

        devolverResultado()
        atualizarTela()
        Toast.makeText(
            this, getString(R.string.toast_zerou, vitoriasJ1, vitoriasJ2), Toast.LENGTH_SHORT
        ).show()
    }

    fun btnHistoricoOnClick(view: View) {
        val linha = "$nomeJ1 $vitoriasJ1 x $vitoriasJ2 $nomeJ2"

        val intent = Intent(this, HistoricoActivity::class.java)
        intent.putStringArrayListExtra(HistoricoActivity.EXTRA_LINHAS, arrayListOf(linha))
        startActivity(intent)
    }

    companion object {
        const val EXTRA_NOME_J1 = "nomeJ1"
        const val EXTRA_NOME_J2 = "nomeJ2"
        const val EXTRA_VITORIAS_J1 = "vitoriasJ1"
        const val EXTRA_VITORIAS_J2 = "vitoriasJ2"

        private const val STATE_PONTOS_J1 = "pontosJ1"
        private const val STATE_VITORIAS_J1 = "vitoriasJ1"
        private const val STATE_PONTOS_J2 = "pontosJ2"
        private const val STATE_VITORIAS_J2 = "vitoriasJ2"
    }
}