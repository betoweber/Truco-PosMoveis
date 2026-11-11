package br.edu.uftpr.truco

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Test
    fun mostraLogoEBotaoIniciar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            view(R.id.logo).check(matches(isDisplayed()))
            view(R.id.btnIniciar).check(matches(isDisplayed()))
        }
    }

    @Test
    fun iniciar_abreATelaDeSetup() {
        ActivityScenario.launch(MainActivity::class.java).use {
            view(R.id.btnIniciar).perform(click())
            view(R.id.tiJ1).check(matches(isDisplayed()))
            view(R.id.btnComecar).check(matches(isDisplayed()))
        }
    }
}
