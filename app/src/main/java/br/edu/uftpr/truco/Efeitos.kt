package br.edu.uftpr.truco

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible

/**
 * Animações "de jogo" do app. São só cosméticas: o estado (pontos, vitórias)
 * continua vivendo nas Activities
 */
object Efeitos {

    /** Botão afunda ao tocar e volta com um "quique" ao soltar. */
    @SuppressLint("ClickableViewAccessibility")
    fun aplicarToque(vararg views: View) {
        for (v in views) {
            v.setOnTouchListener { view, evento ->
                when (evento.actionMasked) {
                    MotionEvent.ACTION_DOWN -> view.animate()
                        .scaleX(0.88f).scaleY(0.88f)
                        .setDuration(70)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(260)
                        .setInterpolator(OvershootInterpolator(3.5f))
                        .start()
                }
                false // não consome: o onClick do XML continua funcionando
            }
        }
    }

    /** Cresce e volta, como o número do Balatro ao pontuar. */
    fun pulo(view: View, escala: Float = 1.2f) {
        view.animate().cancel()
        view.animate()
            .scaleX(escala).scaleY(escala)
            .setDuration(110)
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(OvershootInterpolator(3f))
                    .start()
            }
            .start()
    }

    /** Conta de [de] até [ate] no próprio TextView. [aoFim] só roda se a contagem não for cancelada. */
    fun contar(tv: TextView, de: Int, ate: Int, duracao: Long = 380L, aoFim: (() -> Unit)? = null) {
        (tv.getTag(R.id.tag_contador) as? ValueAnimator)?.cancel()
        tv.text = de.toString()
        if (de == ate) {
            aoFim?.invoke()
            return
        }
        val animador = ValueAnimator.ofInt(de, ate).apply {
            this.duration = duracao
            interpolator = DecelerateInterpolator()
            addUpdateListener { tv.text = (it.animatedValue as Int).toString() }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelado = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelado) aoFim?.invoke()
                }
            })
        }
        tv.setTag(R.id.tag_contador, animador)
        animador.start()
    }

    /** Interrompe uma contagem em andamento, para ela não sobrescrever um texto definido depois. */
    fun pararContagem(tv: TextView) {
        (tv.getTag(R.id.tag_contador) as? ValueAnimator)?.cancel()
    }

    /** "+N" sobe e some, mostrando quanto o jogador acabou de ganhar. */
    fun flutuarGanho(tv: TextView, pontos: Int) {
        tv.animate().cancel()
        tv.text = tv.context.getString(R.string.ganho_pontos, pontos)
        tv.alpha = 1f
        tv.translationY = 0f
        tv.visibility = View.VISIBLE
        tv.animate()
            .translationY(-dp(tv, 44))
            .alpha(0f)
            .setDuration(800)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                tv.visibility = View.INVISIBLE
                tv.translationY = 0f
            }
            .start()
    }

    /** Sacode a caixa de um lado para o outro (vitória / zerar). */
    fun tremer(view: View) {
        val d = dp(view, 10)
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -d, d, -d * 0.8f, d * 0.8f, -d * 0.4f, d * 0.4f, 0f)
            .apply { duration = 450 }
            .start()
    }

    /** Número pisca em dourado e volta à cor normal. */
    fun piscarOuro(tv: TextView) {
        val normal = ContextCompat.getColor(tv.context, R.color.cream)
        val ouro = ContextCompat.getColor(tv.context, R.color.gold)
        ValueAnimator.ofArgb(normal, ouro, normal, ouro, normal).apply {
            duration = 700
            addUpdateListener { tv.setTextColor(it.animatedValue as Int) }
            start()
        }
    }

    /**
     * Toast com o visual do app (caixa pixelada). Continua sendo um Toast do sistema,
     * só que com o layout próprio em vez do balão padrão.
     */
    @Suppress("DEPRECATION")
    fun toast(contexto: Context, texto: String) {
        val view = LayoutInflater.from(contexto).inflate(R.layout.toast_pixel, null) as TextView
        view.text = texto
        Toast(contexto).apply {
            duration = Toast.LENGTH_SHORT
            this.view = view
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, (96 * contexto.resources.displayMetrics.density).toInt())
        }.show()
    }

    private fun dp(view: View, valor: Int): Float = valor * view.resources.displayMetrics.density

    // ------------------------------------------------------------------ fogo da sequência

    private val FOGO_DRAWABLES = intArrayOf(R.drawable.ic_fogo_1, R.drawable.ic_fogo_2, R.drawable.ic_fogo_3)
    private val FOGO_ESCALAS = floatArrayOf(0.8f, 1.0f, 1.3f)

    /** 0 = sem fogo; 1 = a partir de 3 seguidas; 2 = a partir de 5; 3 = a partir de 7. */
    fun nivelFogo(serie: Int): Int = when {
        serie >= 7 -> 3
        serie >= 5 -> 2
        serie >= 3 -> 1
        else -> 0
    }

    /**
     * Mostra/atualiza o fogo dos dois lados do placar. [de] e [para] são níveis (0 = apagado).
     * Com [animar], o fogo "nasce" com um quique, cresce de nível com um pulso ou murcha ao apagar.
     */
    fun fogo(esq: ImageView, dir: ImageView, de: Int, para: Int, animar: Boolean) {
        for ((i, v) in listOf(esq, dir).withIndex()) {
            v.animate().cancel()
            v.setTag(R.id.tag_fogo_nivel, para)

            if (para == 0) {
                pararChama(v)
                if (v.visibility != View.VISIBLE) continue
                if (!animar) {
                    v.visibility = View.GONE
                    continue
                }
                v.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(320)
                    .withEndAction {
                        if (v.getTag(R.id.tag_fogo_nivel) == 0) v.visibility = View.GONE
                    }.start()
                continue
            }

            if (de == para && v.isVisible) continue

            val escala = FOGO_ESCALAS[para - 1]
            v.setImageResource(FOGO_DRAWABLES[para - 1])
            v.pivotX = v.layoutParams.width / 2f
            v.pivotY = v.layoutParams.height.toFloat()
            val aparecendo = v.visibility != View.VISIBLE
            v.visibility = View.VISIBLE
            v.alpha = 1f
            pararChama(v)

            if (!animar) {
                v.scaleX = escala
                v.scaleY = escala
                tremeluzir(v, escala, i)
                continue
            }
            if (aparecendo) {
                v.scaleX = 0f
                v.scaleY = 0f
            }
            v.animate().scaleX(escala).scaleY(escala)
                .setDuration(if (aparecendo) 500 else 320)
                .setInterpolator(OvershootInterpolator(2.6f))
                .withEndAction {
                    if (v.getTag(R.id.tag_fogo_nivel) == para) tremeluzir(v, escala, i)
                }.start()
        }
    }

    private fun tremeluzir(v: ImageView, escala: Float, lado: Int) {
        pararChama(v)
        val animador = ObjectAnimator.ofPropertyValuesHolder(
            v,
            PropertyValuesHolder.ofFloat(View.SCALE_Y, escala * 0.9f, escala * 1.07f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 1f)
        ).apply {
            duration = 380L + lado * 110L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        v.setTag(R.id.tag_fogo, animador)
        // Animação infinita precisa morrer com a tela, senão segura a Activity na memória
        v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) {
                animador.cancel()
                view.removeOnAttachStateChangeListener(this)
            }
        })
        animador.start()
    }

    private fun pararChama(v: View) {
        (v.getTag(R.id.tag_fogo) as? Animator)?.cancel()
        v.setTag(R.id.tag_fogo, null)
    }
}
