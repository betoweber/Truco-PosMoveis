package br.edu.uftpr.truco

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_NOME_J1
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_NOME_J2
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_SERIE_J1
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_SERIE_J2
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_VITORIAS_J1
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_VITORIAS_J2
import com.google.android.material.textfield.TextInputLayout

class SetupActivity : AppCompatActivity() {
    private lateinit var tiJ1: TextInputLayout
    private lateinit var tiJ2: TextInputLayout
    private lateinit var tvErroNomes: TextView
    private var chaves = ArrayList<String>()
    private var vitoriasA = ArrayList<Int>()
    private var vitoriasB = ArrayList<Int>()

    // Vitórias seguidas atuais de cada nome da chave (listas paralelas às de vitórias)
    private var seriesA = ArrayList<Int>()
    private var seriesB = ArrayList<Int>()

    private var nomeAtualJ1 = ""
    private var nomeAtualJ2 = ""

    /**
     * Barra só o "|", que é o separador da chave do confronto ("NomeA|NomeB"). Todo o resto
     * (acentos, hífen, apóstrofo, ponto...) continua valendo, porque faz parte de nomes de verdade.
     * Roda a cada digitação e colagem, então o caractere nunca chega ao campo.
     */
    private val filtroNome = InputFilter { origem, inicio, fim, _, _, _ ->
        val limpo = StringBuilder()
        for (i in inicio until fim) {
            val c = origem[i]
            if (c != SEPARADOR_CHAVE) limpo.append(c)
        }
        if (limpo.length == fim - inicio) null else limpo.toString()
    }

    private fun montarChave(n1: String, n2: String): Pair<String, Boolean> {
        val j1Primeiro = n1.compareTo(n2, ignoreCase = true) <= 0
        val chave = if (j1Primeiro) "$n1$SEPARADOR_CHAVE$n2" else "$n2$SEPARADOR_CHAVE$n1"
        return Pair(chave, !j1Primeiro)
    }

    private val launcherTruco = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val v1 = it.data?.getIntExtra(EXTRA_VITORIAS_J1, 0) ?: 0
            val v2 = it.data?.getIntExtra(EXTRA_VITORIAS_J2, 0) ?: 0
            val s1 = it.data?.getIntExtra(EXTRA_SERIE_J1, 0) ?: 0
            val s2 = it.data?.getIntExtra(EXTRA_SERIE_J2, 0) ?: 0
            registrarMatchup(v1, v2, s1, s2)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdgeEscuro()
        setContentView(R.layout.activity_setup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tiJ1 = findViewById(R.id.tiJ1)
        tiJ2 = findViewById(R.id.tiJ2)
        tvErroNomes = findViewById(R.id.tvErroNomes)

        // Nosso filtro vem antes do limite de 16 caracteres (que já está no estilo do campo),
        // assim os caracteres recusados não gastam o limite.
        for (campo in listOf(tiJ1, tiJ2)) {
            campo.editText?.let { it.filters = arrayOf(filtroNome) + it.filters }
        }

        Efeitos.aplicarToque(findViewById(R.id.btnComecar), findViewById(R.id.btnHistoricoGeral))

        if (savedInstanceState != null) {
            chaves = savedInstanceState.getStringArrayList(STATE_MATCHUPS) ?: ArrayList()
            vitoriasA = savedInstanceState.getIntegerArrayList(STATE_VITORIAS_A) ?: ArrayList()
            vitoriasB = savedInstanceState.getIntegerArrayList(STATE_VITORIAS_B) ?: ArrayList()
            seriesA = savedInstanceState.getIntegerArrayList(STATE_SERIES_A) ?: ArrayList()
            seriesB = savedInstanceState.getIntegerArrayList(STATE_SERIES_B) ?: ArrayList()
            nomeAtualJ1 = savedInstanceState.getString(STATE_NOME_J1) ?: ""
            nomeAtualJ2 = savedInstanceState.getString(STATE_NOME_J2) ?: ""

            // As listas são paralelas: garante o mesmo tamanho mesmo se faltar algo no estado salvo
            while (seriesA.size < chaves.size) seriesA.add(0)
            while (seriesB.size < chaves.size) seriesB.add(0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_MATCHUPS, chaves)
        outState.putIntegerArrayList(STATE_VITORIAS_A, vitoriasA)
        outState.putIntegerArrayList(STATE_VITORIAS_B, vitoriasB)
        outState.putIntegerArrayList(STATE_SERIES_A, seriesA)
        outState.putIntegerArrayList(STATE_SERIES_B, seriesB)
        outState.putString(STATE_NOME_J1, nomeAtualJ1)
        outState.putString(STATE_NOME_J2, nomeAtualJ2)
    }

    fun btnComecar(view: View) {
        tvErroNomes.visibility = View.GONE
        tiJ2.editText?.isActivated = false

        val jogadorUm = lerNome(tiJ1, getString(R.string.nome_jogador_1))
        val jogadorDois = lerNome(tiJ2, getString(R.string.nome_jogador_2))
        if (!nomesValidos(jogadorUm, jogadorDois)) return

        nomeAtualJ1 = jogadorUm
        nomeAtualJ2 = jogadorDois

        val (v1, v2) = vitoriasDoMatchup(jogadorUm, jogadorDois)
        val (s1, s2) = seriesDoMatchup(jogadorUm, jogadorDois)

        val intent = Intent(this, TrucoActivity::class.java)
        intent.putExtra(EXTRA_NOME_J1, jogadorUm)
        intent.putExtra(EXTRA_NOME_J2, jogadorDois)
        intent.putExtra(EXTRA_VITORIAS_J1, v1)
        intent.putExtra(EXTRA_VITORIAS_J2, v2)
        intent.putExtra(EXTRA_SERIE_J1, s1)
        intent.putExtra(EXTRA_SERIE_J2, s2)
        launcherTruco.launch(intent)
    }

    fun btnHistoricoGeral(view: View) {
        val linhas = ArrayList<String>()
        for (i in chaves.indices) {
            val (nomeA, nomeB) = chaves[i].split(SEPARADOR_CHAVE)
            linhas.add("$nomeA ${vitoriasA[i]} x ${vitoriasB[i]} $nomeB")
        }
        val intent = Intent(this, HistoricoActivity::class.java)
        intent.putStringArrayListExtra(HistoricoActivity.EXTRA_LINHAS, linhas)
        startActivity(intent)
    }

    private fun registrarMatchup(v1: Int, v2: Int, s1: Int, s2: Int) {
        val (chave, invertido) = montarChave(nomeAtualJ1, nomeAtualJ2)
        val a = if (invertido) v2 else v1
        val b = if (invertido) v1 else v2
        val sa = if (invertido) s2 else s1
        val sb = if (invertido) s1 else s2

        val i = chaves.indexOfFirst { it.equals(chave, ignoreCase = true) }
        if (a == 0 && b == 0) {
            if (i != -1) {
                chaves.removeAt(i)
                vitoriasA.removeAt(i)
                vitoriasB.removeAt(i)
                seriesA.removeAt(i)
                seriesB.removeAt(i)
            }
            return
        }

        if (i != -1) {
            vitoriasA[i] = a
            vitoriasB[i] = b
            seriesA[i] = sa
            seriesB[i] = sb
        } else {
            chaves.add(chave)
            vitoriasA.add(a)
            vitoriasB.add(b)
            seriesA.add(sa)
            seriesB.add(sb)
        }
    }

    private fun nomesValidos(n1: String, n2: String): Boolean {
        if (n1.equals(n2, ignoreCase = true)) {
            tvErroNomes.text = getString(R.string.mensagem_erro_nomes_iguais)
            tvErroNomes.visibility = View.VISIBLE
            tiJ2.editText?.isActivated = true   // borda do campo fica vermelha (bg_input_j2)
            return false
        }
        return true
    }

    private fun lerNome(campo: TextInputLayout, padrao: String): String =
        (campo.editText?.text?.toString() ?: "").trim().ifBlank { padrao }

    private fun vitoriasDoMatchup(n1: String, n2: String): Pair<Int, Int> {
        val (chave, invertido) = montarChave(n1, n2)
        val i = chaves.indexOfFirst { it.equals(chave, ignoreCase = true) }

        if (i == -1) return Pair(0, 0)

        val v1 = if (invertido) vitoriasB[i] else vitoriasA[i]
        val v2 = if (invertido) vitoriasA[i] else vitoriasB[i]
        return Pair(v1, v2)
    }

    private fun seriesDoMatchup(n1: String, n2: String): Pair<Int, Int> {
        val (chave, invertido) = montarChave(n1, n2)
        val i = chaves.indexOfFirst { it.equals(chave, ignoreCase = true) }

        if (i == -1) return Pair(0, 0)

        val s1 = if (invertido) seriesB[i] else seriesA[i]
        val s2 = if (invertido) seriesA[i] else seriesB[i]
        return Pair(s1, s2)
    }

    companion object {
        // Separa os dois nomes na chave do confronto; por isso o filtro dos campos barra este caractere
        private const val SEPARADOR_CHAVE = '|'

        private const val STATE_MATCHUPS = "matchups"
        private const val STATE_VITORIAS_A = "vitoriasA"
        private const val STATE_VITORIAS_B = "vitoriasB"
        private const val STATE_SERIES_A = "seriesA"
        private const val STATE_SERIES_B = "seriesB"
        private const val STATE_NOME_J1 = "nomeAtualJ1"
        private const val STATE_NOME_J2 = "nomeAtualJ2"
    }
}