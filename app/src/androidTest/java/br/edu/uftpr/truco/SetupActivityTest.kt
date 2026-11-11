package br.edu.uftpr.truco

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupActivityTest {

    private fun nomes(a: String, b: String) {
        view(R.id.jogadorUmTxt).perform(replaceText(a))
        view(R.id.jogadorDoisTxt).perform(replaceText(b))
        closeSoftKeyboard()
    }

    @Test
    fun nomesIguais_mostramErro_eNaoAbremAMesa() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            nomes("Ana", "ana") // sem diferenciar maiúsculas
            view(R.id.btnComecar).perform(click())
            view(R.id.tvErroNomes).check(matches(isDisplayed()))
            view(R.id.tvErroNomes).check(matches(withText(R.string.mensagem_erro_nomes_iguais)))
            view(R.id.btnComecar).check(matches(isDisplayed())) // continua no Setup
        }
    }

    @Test
    fun corrigirOsNomes_eComecar_escondeOErroEAbreAMesa() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            nomes("Ana", "Ana")
            view(R.id.btnComecar).perform(click())
            view(R.id.tvErroNomes).check(matches(isDisplayed()))
            nomes("Ana", "Beto")
            view(R.id.btnComecar).perform(click())
            view(R.id.tvNomeJ1).check(matches(withText("Ana")))
            view(R.id.tvNomeJ2).check(matches(withText("Beto")))
        }
    }

    @Test
    fun camposVazios_viramJogador1eJogador2() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            view(R.id.btnComecar).perform(click())
            view(R.id.tvNomeJ1).check(matches(withText(R.string.nome_jogador_1)))
            view(R.id.tvNomeJ2).check(matches(withText(R.string.nome_jogador_2)))
        }
    }

    @Test
    fun espacosNasPontas_saoRemovidos() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            nomes("   Ana  ", "Beto ")
            view(R.id.btnComecar).perform(click())
            view(R.id.tvNomeJ1).check(matches(withText("Ana")))
            view(R.id.tvNomeJ2).check(matches(withText("Beto")))
        }
    }

    @Test
    fun filtro_barraSoOPipe_eMantemAcentosEPontuacao() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            view(R.id.jogadorUmTxt).perform(replaceText("Zé|Maria-Luiza"))
            view(R.id.jogadorDoisTxt).perform(replaceText("D'Ávila Jr."))
            closeSoftKeyboard()
            view(R.id.jogadorUmTxt).check(matches(withText("ZéMaria-Luiza")))
            view(R.id.jogadorDoisTxt).check(matches(withText("D'Ávila Jr.")))
        }
    }

    @Test
    fun nomeNaoPassaDe16Caracteres() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            view(R.id.jogadorUmTxt).perform(replaceText("ABCDEFGHIJKLMNOPQRST"))
            view(R.id.jogadorUmTxt).check(matches(withText("ABCDEFGHIJKLMNOP")))
        }
    }

    @Test
    fun historicoGeralVazio_mostraAviso() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            view(R.id.btnHistoricoGeral).perform(click())
            view(R.id.tvVazio).check(matches(isDisplayed()))
        }
    }

    @Test
    fun fluxoCompleto_vitoriaApareceNoHistoricoGeral_comQualquerOrdemDosNomes() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            // Ana x Beto: Ana vence
            nomes("Ana", "Beto")
            view(R.id.btnComecar).perform(click())
            view(R.id.btnJ1Doze).perform(click())
            esperar { view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 1"))) }
            pressBack() // sem pontos na mão: volta direto ao Setup

            // Mesmo confronto com a ordem trocada: Beto (J1) x Ana (J2); o placar acompanha cada nome
            nomes("Beto", "Ana")
            view(R.id.btnComecar).perform(click())
            view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 0")))
            view(R.id.tvVitoriasJ2).check(matches(withText("VITÓRIAS 1")))
            view(R.id.btnJ1Doze).perform(click()) // Beto vence
            esperar { view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 1"))) }
            pressBack()

            view(R.id.btnHistoricoGeral).perform(click())
            onData(equalTo("Ana 1 x 1 Beto")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun confrontosDiferentes_ficamSeparados() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            nomes("Ana", "Beto")
            view(R.id.btnComecar).perform(click())
            view(R.id.btnJ1Doze).perform(click())
            esperar { view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 1"))) }
            pressBack()

            nomes("Ana", "Carlos")
            view(R.id.btnComecar).perform(click())
            view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 0"))) // não herdou do Beto
            view(R.id.btnJ2Doze).perform(click())
            esperar { view(R.id.tvVitoriasJ2).check(matches(withText("VITÓRIAS 1"))) }
            pressBack()

            view(R.id.btnHistoricoGeral).perform(click())
            onData(equalTo("Ana 1 x 0 Beto")).check(matches(isDisplayed()))
            onData(equalTo("Ana 0 x 1 Carlos")).check(matches(isDisplayed()))
            view(R.id.tvVazio).check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    @Test
    fun zerarNaMesa_removeOConfrontoDoHistoricoGeral() {
        ActivityScenario.launch(SetupActivity::class.java).use {
            nomes("Ana", "Beto")
            view(R.id.btnComecar).perform(click())
            view(R.id.btnJ1Doze).perform(click())
            esperar { view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 1"))) }
            view(R.id.btnZerar).perform(click())
            esperar { view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 0"))) }
            pressBack()

            view(R.id.btnHistoricoGeral).perform(click())
            view(R.id.tvVazio).check(matches(isDisplayed()))
        }
    }

    @Test
    fun girarATelaDoSetup_mantemOsConfrontos() {
        ActivityScenario.launch(SetupActivity::class.java).use { cenario ->
            nomes("Ana", "Beto")
            view(R.id.btnComecar).perform(click())
            view(R.id.btnJ1Doze).perform(click())
            esperar { view(R.id.tvVitoriasJ1).check(matches(withText("VITÓRIAS 1"))) }
            pressBack()

            cenario.recreate()
            view(R.id.btnHistoricoGeral).perform(click())
            onData(equalTo("Ana 1 x 0 Beto")).check(matches(isDisplayed()))
        }
    }
}
