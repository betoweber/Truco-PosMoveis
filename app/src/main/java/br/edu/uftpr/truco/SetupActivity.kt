package br.edu.uftpr.truco

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_NOME_J1
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_NOME_J2
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_VITORIAS_J1
import br.edu.uftpr.truco.TrucoActivity.Companion.EXTRA_VITORIAS_J2
import com.google.android.material.textfield.TextInputLayout

class SetupActivity : AppCompatActivity() {
    private lateinit var tiJ1: TextInputLayout
    private lateinit var tiJ2: TextInputLayout
    private var chaves = ArrayList<String>()
    private var vitoriasA = ArrayList<Int>()
    private var vitoriasB = ArrayList<Int>()

    private var nomeAtualJ1 = ""
    private var nomeAtualJ2 = ""

    private fun montarChave(n1: String, n2: String): Pair<String, Boolean> {
        val j1Primeiro = n1.compareTo(n2, ignoreCase = true) <= 0
        val chave = if (j1Primeiro) "$n1|$n2" else "$n2|$n1"
        return Pair(chave, !j1Primeiro)
    }

    private val launcherTruco = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val v1 = it.data?.getIntExtra(EXTRA_VITORIAS_J1, 0) ?: 0
            val v2 = it.data?.getIntExtra(EXTRA_VITORIAS_J2, 0) ?: 0
            registrarMatchup(v1, v2)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tiJ1 = findViewById(R.id.tiJ1)
        tiJ2 = findViewById(R.id.tiJ2)

        if (savedInstanceState != null) {
            chaves = savedInstanceState.getStringArrayList(STATE_MATCHUPS) ?: ArrayList()
            vitoriasA = savedInstanceState.getIntegerArrayList(STATE_VITORIAS_A) ?: ArrayList()
            vitoriasB = savedInstanceState.getIntegerArrayList(STATE_VITORIAS_B) ?: ArrayList()
            nomeAtualJ1 = savedInstanceState.getString(STATE_NOME_J1) ?: ""
            nomeAtualJ2 = savedInstanceState.getString(STATE_NOME_J2) ?: ""
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_MATCHUPS, chaves)
        outState.putIntegerArrayList(STATE_VITORIAS_A, vitoriasA)
        outState.putIntegerArrayList(STATE_VITORIAS_B, vitoriasB)
        outState.putString(STATE_NOME_J1, nomeAtualJ1)
        outState.putString(STATE_NOME_J2, nomeAtualJ2)
    }

    fun btnComecar(view: View) {
        tiJ2.error = null

        val jogadorUm = lerNome(tiJ1, getString(R.string.nome_jogador_1))
        val jogadorDois = lerNome(tiJ2, getString(R.string.nome_jogador_2))
        if (!nomesValidos(jogadorUm, jogadorDois)) return

        nomeAtualJ1 = jogadorUm
        nomeAtualJ2 = jogadorDois

        val (v1, v2) = vitoriasDoMatchup(jogadorUm, jogadorDois)

        val intent = Intent(this, TrucoActivity::class.java)
        intent.putExtra(EXTRA_NOME_J1, jogadorUm)
        intent.putExtra(EXTRA_NOME_J2, jogadorDois)
        intent.putExtra(EXTRA_VITORIAS_J1, v1)
        intent.putExtra(EXTRA_VITORIAS_J2, v2)
        launcherTruco.launch(intent)
    }

    fun btnHistoricoGeral(view: View) {
        val linhas = ArrayList<String>()
        for (i in chaves.indices) {
            val (nomeA, nomeB) = chaves[i].split("|")
            linhas.add("$nomeA ${vitoriasA[i]} x ${vitoriasB[i]} $nomeB")
        }
        val intent = Intent(this, HistoricoActivity::class.java)
        intent.putStringArrayListExtra(HistoricoActivity.EXTRA_LINHAS, linhas)
        startActivity(intent)
    }

    private fun registrarMatchup(v1: Int, v2: Int) {
        val (chave, invertido) = montarChave(nomeAtualJ1, nomeAtualJ2)
        val a = if (invertido) v2 else v1
        val b = if (invertido) v1 else v2

        val i = chaves.indexOfFirst { it.equals(chave, ignoreCase = true) }
        if (a == 0 && b == 0) {
            if (i != -1) {
                chaves.removeAt(i)
                vitoriasA.removeAt(i)
                vitoriasB.removeAt(i)
            }
            return
        }

        if (i != -1) {
            vitoriasA[i] = a
            vitoriasB[i] = b
        } else {
            chaves.add(chave)
            vitoriasA.add(a)
            vitoriasB.add(b)
        }
    }

    private fun nomesValidos(n1: String, n2: String): Boolean {
        if (n1.equals(n2, ignoreCase = true)) {
            tiJ2.error = getString(R.string.mensagem_erro_nomes_iguais)
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

    companion object {
        private const val STATE_MATCHUPS = "matchups"
        private const val STATE_VITORIAS_A = "vitoriasA"
        private const val STATE_VITORIAS_B = "vitoriasB"
        private const val STATE_NOME_J1 = "nomeAtualJ1"
        private const val STATE_NOME_J2 = "nomeAtualJ2"
    }
}