package br.edu.uftpr.truco

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoricoActivityTest {

    private fun abrir(vararg linhas: String): ActivityScenario<HistoricoActivity> {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(contexto, HistoricoActivity::class.java)
            .putStringArrayListExtra(HistoricoActivity.EXTRA_LINHAS, arrayListOf(*linhas))
        return ActivityScenario.launch(intent)
    }

    @Test
    fun listaSemLinhas_mostraAvisoDeVazio() {
        abrir().use {
            view(R.id.tvVazio).check(matches(isDisplayed()))
        }
    }

    @Test
    fun listaComLinhas_mostraCadaConfronto_eEscondeOAviso() {
        abrir("Ana 3 x 1 Beto", "Ana 0 x 2 Carlos").use {
            view(R.id.tvVazio).check(matches(withEffectiveVisibility(Visibility.GONE)))
            onData(equalTo("Ana 3 x 1 Beto")).check(matches(isDisplayed()))
            onData(equalTo("Ana 0 x 2 Carlos")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun mostraOTitulo() {
        abrir("Ana 1 x 0 Beto").use {
            view(R.id.tvTitulo).check(matches(withText(R.string.titulo_historico)))
        }
    }
}
