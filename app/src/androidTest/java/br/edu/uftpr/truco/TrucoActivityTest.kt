package br.edu.uftpr.truco

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrucoActivityTest {

    private fun abrir(
        v1: Int = 0, v2: Int = 0, s1: Int = 0, s2: Int = 0
    ): ActivityScenario<TrucoActivity> {
        // launchActivityForResult: assim dá para ler o setResult() em scenario.result
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(contexto, TrucoActivity::class.java)
            .putExtra(TrucoActivity.EXTRA_NOME_J1, "Ana")
            .putExtra(TrucoActivity.EXTRA_NOME_J2, "Beto")
            .putExtra(TrucoActivity.EXTRA_VITORIAS_J1, v1)
            .putExtra(TrucoActivity.EXTRA_VITORIAS_J2, v2)
            .putExtra(TrucoActivity.EXTRA_SERIE_J1, s1)
            .putExtra(TrucoActivity.EXTRA_SERIE_J2, s2)
        return ActivityScenario.launchActivityForResult(intent)
    }

    private fun texto(id: Int) = withText(
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
    )

    private fun vitorias(n: Int) = withText(
        InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.label_vitorias, n)
    )

    private fun fogoVisivel(scenario: ActivityScenario<TrucoActivity>, id: Int): Boolean {
        var visivel = false
        scenario.onActivity { visivel = it.findViewById<View>(id).visibility == View.VISIBLE }
        return visivel
    }

    @Test
    fun mostraNomesEPlacarInicialDoIntent() {
        abrir(v1 = 2, v2 = 5).use {
            view(R.id.tvNomeJ1).check(matches(withText("Ana")))
            view(R.id.tvNomeJ2).check(matches(withText("Beto")))
            view(R.id.tvPtsJ1).check(matches(withText("0")))
            view(R.id.tvPtsJ2).check(matches(withText("0")))
            view(R.id.tvVitoriasJ1).check(matches(vitorias(2)))
            view(R.id.tvVitoriasJ2).check(matches(vitorias(5)))
        }
    }

    @Test
    fun cartasSomamPontosAoJogadorCerto() {
        abrir().use {
            view(R.id.btnJ1Tres).perform(click())
            view(R.id.btnJ1Um).perform(click())
            view(R.id.btnJ2Seis).perform(click())
            esperar {
                view(R.id.tvPtsJ1).check(matches(withText("4")))
                view(R.id.tvPtsJ2).check(matches(withText("6")))
            }
        }
    }

    @Test
    fun chegarEmDoze_somaVitoria_eZeraOsDoisPlacares() {
        abrir().use {
            view(R.id.btnJ2Seis).perform(click())
            view(R.id.btnJ1Doze).perform(click())
            esperar {
                view(R.id.tvVitoriasJ1).check(matches(vitorias(1)))
                view(R.id.tvPtsJ1).check(matches(withText("0")))
                view(R.id.tvPtsJ2).check(matches(withText("0")))
            }
            view(R.id.tvVitoriasJ2).check(matches(vitorias(0)))
        }
    }

    @Test
    fun passarDeDoze_tambemVence() {
        abrir().use {
            view(R.id.btnJ2Nove).perform(click())
            view(R.id.btnJ2Seis).perform(click()) // 15
            esperar { view(R.id.tvVitoriasJ2).check(matches(vitorias(1))) }
        }
    }

    @Test
    fun vitoriaDevolveVitoriasESeriesParaOSetup() {
        val cenario = abrir(v1 = 1, s1 = 1)
        view(R.id.btnJ1Doze).perform(click())
        esperar { view(R.id.tvVitoriasJ1).check(matches(vitorias(2))) }

        cenario.onActivity { it.onBackPressedDispatcher.onBackPressed() } // sem pontos na mão: fecha
        val resultado = cenario.result
        assertEquals(Activity.RESULT_OK, resultado.resultCode)
        assertEquals(2, resultado.resultData.getIntExtra(TrucoActivity.EXTRA_VITORIAS_J1, -1))
        assertEquals(0, resultado.resultData.getIntExtra(TrucoActivity.EXTRA_VITORIAS_J2, -1))
        assertEquals(2, resultado.resultData.getIntExtra(TrucoActivity.EXTRA_SERIE_J1, -1))
        assertEquals(0, resultado.resultData.getIntExtra(TrucoActivity.EXTRA_SERIE_J2, -1))
    }

    @Test
    fun terceiraVitoriaSeguida_acendeOFogoDoVencedor() {
        abrir(v1 = 2, s1 = 2).use { cenario ->
            assertEquals(false, fogoVisivel(cenario, R.id.ivFogoEsqJ1))
            view(R.id.btnJ1Doze).perform(click())
            esperar {
                assertEquals(true, fogoVisivel(cenario, R.id.ivFogoEsqJ1))
                assertEquals(true, fogoVisivel(cenario, R.id.ivFogoDirJ1))
            }
            assertEquals(false, fogoVisivel(cenario, R.id.ivFogoEsqJ2))
        }
    }

    @Test
    fun serieJaExistenteNoIntent_abreComFogo() {
        abrir(v1 = 4, s1 = 4).use { cenario ->
            esperar { assertEquals(true, fogoVisivel(cenario, R.id.ivFogoEsqJ1)) }
        }
    }

    @Test
    fun adversarioVencer_quebraASequenciaEApagaOFogo() {
        abrir(v1 = 3, s1 = 3).use { cenario ->
            esperar { assertEquals(true, fogoVisivel(cenario, R.id.ivFogoEsqJ1)) }
            view(R.id.btnJ2Doze).perform(click())
            esperar(5000) { assertEquals(false, fogoVisivel(cenario, R.id.ivFogoEsqJ1)) }
        }
    }

    @Test
    fun zerar_limpaPontosVitoriasESerie() {
        abrir(v1 = 3, v2 = 1, s1 = 3).use { cenario ->
            view(R.id.btnJ1Tres).perform(click())
            view(R.id.btnZerar).perform(click())
            esperar {
                view(R.id.tvPtsJ1).check(matches(withText("0")))
                view(R.id.tvVitoriasJ1).check(matches(vitorias(0)))
                view(R.id.tvVitoriasJ2).check(matches(vitorias(0)))
                assertEquals(false, fogoVisivel(cenario, R.id.ivFogoEsqJ1))
            }
        }
    }

    @Test
    fun girarATela_preservaPontosEVitorias() {
        abrir(v1 = 1).use { cenario ->
            view(R.id.btnJ1Seis).perform(click())
            esperar { view(R.id.tvPtsJ1).check(matches(withText("6"))) }
            cenario.recreate()
            view(R.id.tvPtsJ1).check(matches(withText("6")))
            view(R.id.tvVitoriasJ1).check(matches(vitorias(1)))
            view(R.id.tvNomeJ2).check(matches(withText("Beto")))
        }
    }

    @Test
    fun voltarComPontosNaMao_pedeConfirmacao() {
        abrir().use { cenario ->
            view(R.id.btnJ1Tres).perform(click())
            cenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            esperar { view(R.id.tvDialogoTitulo).check(matches(texto(R.string.titulo_descarte_dialog))) }
            view(R.id.btnDialogoCancelar).perform(click())
            // continua na mesa, com os pontos
            view(R.id.tvPtsJ1).check(matches(isDisplayed()))
        }
    }

    @Test
    fun abandonarNoDialogo_fechaATela() {
        val cenario = abrir()
        view(R.id.btnJ1Tres).perform(click())
        cenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        esperar { view(R.id.btnDialogoConfirmar).check(matches(isDisplayed())) }
        view(R.id.btnDialogoConfirmar).perform(click())
        assertEquals(Activity.RESULT_CANCELED, cenario.result.resultCode)
    }

    @Test
    fun botaoHistorico_abreSoOConfrontoAtual() {
        abrir(v1 = 3, v2 = 1).use {
            view(R.id.btnHistorico).perform(click())
            view(R.id.tvTitulo).check(matches(withText(R.string.titulo_historico)))
            androidx.test.espresso.Espresso.onData(org.hamcrest.Matchers.equalTo("Ana 3 x 1 Beto"))
                .check(matches(isDisplayed()))
        }
    }
}
