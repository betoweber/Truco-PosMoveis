package br.edu.uftpr.truco

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes de unidade (rodam na JVM, sem emulador) da regra pura do fogo da sequência. */
class EfeitosTest {

    @Test
    fun nivelFogo_semSequencia_naoAcende() {
        assertEquals(0, Efeitos.nivelFogo(0))
        assertEquals(0, Efeitos.nivelFogo(1))
        assertEquals(0, Efeitos.nivelFogo(2))
    }

    @Test
    fun nivelFogo_tresSeguidas_acendeNivel1() {
        assertEquals(1, Efeitos.nivelFogo(3))
        assertEquals(1, Efeitos.nivelFogo(4))
    }

    @Test
    fun nivelFogo_cincoSeguidas_sobeParaNivel2() {
        assertEquals(2, Efeitos.nivelFogo(5))
        assertEquals(2, Efeitos.nivelFogo(6))
    }

    @Test
    fun nivelFogo_seteOuMais_ficaNoNivel3() {
        assertEquals(3, Efeitos.nivelFogo(7))
        assertEquals(3, Efeitos.nivelFogo(50))
    }

    @Test
    fun nivelFogo_valorNegativoNaoAcende() {
        assertEquals(0, Efeitos.nivelFogo(-1))
    }

    @Test
    fun nivelFogo_nuncaDiminuiQuandoASerieCresce() {
        var anterior = 0
        for (serie in 0..20) {
            val nivel = Efeitos.nivelFogo(serie)
            assertTrue("nível caiu na série $serie", nivel >= anterior)
            anterior = nivel
        }
    }
}
