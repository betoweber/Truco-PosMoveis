package br.edu.uftpr.truco

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.ViewInteraction

/**
 * O app anima números e fogo, e o Espresso não espera animações. Por isso as conferências do
 * estado "final" são repetidas por até [tempoMs] antes de falhar.
 */
fun esperar(tempoMs: Long = 3000, conferencia: () -> Unit) {
    val limite = System.currentTimeMillis() + tempoMs
    var ultimoErro: Throwable? = null
    while (System.currentTimeMillis() < limite) {
        try {
            conferencia()
            return
        } catch (e: Throwable) {
            ultimoErro = e
            Thread.sleep(100)
        }
    }
    throw ultimoErro ?: AssertionError("tempo esgotado")
}

fun view(id: Int): ViewInteraction = onView(withId(id))

