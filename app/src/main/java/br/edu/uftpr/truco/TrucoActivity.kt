package br.edu.uftpr.truco

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TrucoActivity : AppCompatActivity() {
    private lateinit var tvNomeJ1: TextView
    private lateinit var tvPtsJ1: TextView
    private lateinit var tvNomeJ2: TextView
    private lateinit var tvPtsJ2: TextView
    private lateinit var tvVitoriasJ1: TextView
    private lateinit var tvVitoriasJ2: TextView
    private lateinit var tvGanhoJ1: TextView
    private lateinit var tvGanhoJ2: TextView
    private lateinit var boxPlacarJ1: View
    private lateinit var boxPlacarJ2: View
    private lateinit var ivFogoEsqJ1: ImageView
    private lateinit var ivFogoDirJ1: ImageView
    private lateinit var ivFogoEsqJ2: ImageView
    private lateinit var ivFogoDirJ2: ImageView

    private var nomeJ1 = ""
    private var pontosJ1 = 0
    private var vitoriasJ1 = 0
    private var nomeJ2 = ""
    private var pontosJ2 = 0
    private var vitoriasJ2 = 0

    // Vitórias seguidas (só uma das duas é maior que zero) e nível do fogo que está na tela
    private var serieJ1 = 0
    private var serieJ2 = 0
    private var nivelFogoJ1 = 0
    private var nivelFogoJ2 = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdgeEscuro()
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
        tvVitoriasJ1 = findViewById(R.id.tvVitoriasJ1)
        tvVitoriasJ2 = findViewById(R.id.tvVitoriasJ2)
        tvGanhoJ1 = findViewById(R.id.tvGanhoJ1)
        tvGanhoJ2 = findViewById(R.id.tvGanhoJ2)
        boxPlacarJ1 = findViewById(R.id.boxPlacarJ1)
        boxPlacarJ2 = findViewById(R.id.boxPlacarJ2)
        ivFogoEsqJ1 = findViewById(R.id.ivFogoEsqJ1)
        ivFogoDirJ1 = findViewById(R.id.ivFogoDirJ1)
        ivFogoEsqJ2 = findViewById(R.id.ivFogoEsqJ2)
        ivFogoDirJ2 = findViewById(R.id.ivFogoDirJ2)

        // Efeito de "afundar" em todos os botões da mesa
        Efeitos.aplicarToque(
            findViewById(R.id.btnJ1Um), findViewById(R.id.btnJ1Tres), findViewById(R.id.btnJ1Seis),
            findViewById(R.id.btnJ1Nove), findViewById(R.id.btnJ1Doze),
            findViewById(R.id.btnJ2Um), findViewById(R.id.btnJ2Tres), findViewById(R.id.btnJ2Seis),
            findViewById(R.id.btnJ2Nove), findViewById(R.id.btnJ2Doze),
            findViewById(R.id.btnZerar), findViewById(R.id.btnHistorico)
        )

        nomeJ1 = intent.getStringExtra(EXTRA_NOME_J1) ?: getString(R.string.nome_jogador_1)
        nomeJ2 = intent.getStringExtra(EXTRA_NOME_J2) ?: getString(R.string.nome_jogador_2)
        vitoriasJ1 = intent.getIntExtra(EXTRA_VITORIAS_J1, 0)
        vitoriasJ2 = intent.getIntExtra(EXTRA_VITORIAS_J2, 0)
        serieJ1 = intent.getIntExtra(EXTRA_SERIE_J1, 0)
        serieJ2 = intent.getIntExtra(EXTRA_SERIE_J2, 0)

        if (savedInstanceState != null) {
            pontosJ1 = savedInstanceState.getInt(STATE_PONTOS_J1)
            pontosJ2 = savedInstanceState.getInt(STATE_PONTOS_J2)
            vitoriasJ1 = savedInstanceState.getInt(STATE_VITORIAS_J1)
            vitoriasJ2 = savedInstanceState.getInt(STATE_VITORIAS_J2)
            serieJ1 = savedInstanceState.getInt(STATE_SERIE_J1)
            serieJ2 = savedInstanceState.getInt(STATE_SERIE_J2)
            devolverResultado()
        }

        atualizarTela()
        atualizarFogo(animar = false)

        onBackPressedDispatcher.addCallback(this) {
            if (pontosJ1 == 0 && pontosJ2 == 0) {
                finish()
            } else {
                Dialogos.confirmar(
                    this@TrucoActivity,
                    getString(R.string.titulo_descarte_dialog),
                    getString(R.string.mensagem_descarte_dialog),
                    getString(R.string.btn_continuar_dialog),
                    getString(R.string.btn_abandonar_dialog)
                ) { finish() }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PONTOS_J1, pontosJ1)
        outState.putInt(STATE_VITORIAS_J1, vitoriasJ1)
        outState.putInt(STATE_PONTOS_J2, pontosJ2)
        outState.putInt(STATE_VITORIAS_J2, vitoriasJ2)
        outState.putInt(STATE_SERIE_J1, serieJ1)
        outState.putInt(STATE_SERIE_J2, serieJ2)
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
        val antesJ1 = pontosJ1
        val antesJ2 = pontosJ2

        var venceu = false
        if (jogador == 1) {
            pontosJ1 += pontos
            if (pontosJ1 >= 12) venceu = true
        } else {
            pontosJ2 += pontos
            if (pontosJ2 >= 12) venceu = true
        }
        val depois = if (jogador == 1) pontosJ1 else pontosJ2

        // O estado (pontos e vitórias) é sempre resolvido primeiro...
        if (venceu) registrarVitoria(jogador)
        atualizarTela()

        // ...e a animação vem depois, só para enfeitar o que já aconteceu.
        animarPontuacao(jogador, pontos, antesJ1, antesJ2, depois, venceu)
    }

    private fun animarPontuacao(
        jogador: Int, pontos: Int, antesJ1: Int, antesJ2: Int, depois: Int, venceu: Boolean
    ) {
        val tvPts = if (jogador == 1) tvPtsJ1 else tvPtsJ2
        val tvGanho = if (jogador == 1) tvGanhoJ1 else tvGanhoJ2
        val caixa = if (jogador == 1) boxPlacarJ1 else boxPlacarJ2
        val antes = if (jogador == 1) antesJ1 else antesJ2

        Efeitos.flutuarGanho(tvGanho, pontos)

        if (!venceu) {
            Efeitos.contar(tvPts, antes, depois)
            Efeitos.pulo(caixa, 1.1f)
            return
        }

        // Vitória: sobe até 12, comemora em dourado e só então mostra o zero.
        val tvOutro = if (jogador == 1) tvPtsJ2 else tvPtsJ1
        val antesOutro = if (jogador == 1) antesJ2 else antesJ1
        Efeitos.contar(tvOutro, antesOutro, 0)
        Efeitos.contar(tvPts, antes, 12) {
            Efeitos.piscarOuro(tvPts)
            Efeitos.tremer(caixa)
            atualizarFogo(animar = true)
            tvPts.postDelayed({
                // Se o jogador já pontuou de novo nesse meio-tempo, não derruba o número novo para 0
                if (pontosJ1 != 0 || pontosJ2 != 0) return@postDelayed
                Efeitos.contar(tvPts, 12, 0, 300)
                Efeitos.pulo(caixa, 1.12f)
            }, 750)
        }
    }

    private fun registrarVitoria(jogador: Int) {
        if (jogador == 1) {
            vitoriasJ1++
            serieJ1++
            serieJ2 = 0
        } else {
            vitoriasJ2++
            serieJ2++
            serieJ1 = 0
        }
        pontosJ1 = 0
        pontosJ2 = 0

        val vencedor = if (jogador == 1) nomeJ1 else nomeJ2
        devolverResultado()
        Efeitos.toast(this, getString(R.string.toast_vitoria, vencedor, vitoriasJ1, vitoriasJ2))
    }

    private fun devolverResultado() {
        val intent = Intent()
        intent.putExtra(EXTRA_VITORIAS_J1, vitoriasJ1)
        intent.putExtra(EXTRA_VITORIAS_J2, vitoriasJ2)
        intent.putExtra(EXTRA_SERIE_J1, serieJ1)
        intent.putExtra(EXTRA_SERIE_J2, serieJ2)
        setResult(RESULT_OK, intent)
    }

    private fun atualizarTela() {
        tvNomeJ1.text = nomeJ1
        tvPtsJ1.text = pontosJ1.toString()
        tvNomeJ2.text = nomeJ2
        tvPtsJ2.text = pontosJ2.toString()
        tvVitoriasJ1.text = getString(R.string.label_vitorias, vitoriasJ1)
        tvVitoriasJ2.text = getString(R.string.label_vitorias, vitoriasJ2)
    }

    /** O fogo reflete a sequência de vitórias: nasce com 3 seguidas e some quando a sequência quebra. */
    private fun atualizarFogo(animar: Boolean) {
        val novoJ1 = Efeitos.nivelFogo(serieJ1)
        val novoJ2 = Efeitos.nivelFogo(serieJ2)
        Efeitos.fogo(ivFogoEsqJ1, ivFogoDirJ1, nivelFogoJ1, novoJ1, animar)
        Efeitos.fogo(ivFogoEsqJ2, ivFogoDirJ2, nivelFogoJ2, novoJ2, animar)
        nivelFogoJ1 = novoJ1
        nivelFogoJ2 = novoJ2
    }

    fun btnZerarOnClick(view: View) {
        Efeitos.tremer(boxPlacarJ1)
        Efeitos.tremer(boxPlacarJ2)
        // Uma contagem ainda em andamento sobrescreveria o "0" que atualizarTela() vai escrever
        Efeitos.pararContagem(tvPtsJ1)
        Efeitos.pararContagem(tvPtsJ2)
        pontosJ1 = 0
        pontosJ2 = 0
        vitoriasJ1 = 0
        vitoriasJ2 = 0
        serieJ1 = 0
        serieJ2 = 0

        devolverResultado()
        atualizarTela()
        atualizarFogo(animar = true)
        Efeitos.toast(this, getString(R.string.toast_zerou, vitoriasJ1, vitoriasJ2))
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
        const val EXTRA_SERIE_J1 = "serieJ1"
        const val EXTRA_SERIE_J2 = "serieJ2"

        private const val STATE_PONTOS_J1 = "pontosJ1"
        private const val STATE_VITORIAS_J1 = "vitoriasJ1"
        private const val STATE_PONTOS_J2 = "pontosJ2"
        private const val STATE_VITORIAS_J2 = "vitoriasJ2"
        private const val STATE_SERIE_J1 = "serieJ1"
        private const val STATE_SERIE_J2 = "serieJ2"
    }
}