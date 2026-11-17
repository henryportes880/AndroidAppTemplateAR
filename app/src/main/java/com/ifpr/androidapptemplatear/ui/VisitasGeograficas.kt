package com.ifpr.androidapptemplatear.ui

/** Histerese independente do Android: os testes usam distâncias, não GPS real. */
class VisitasGeograficas {
    companion object {
        const val RAIO_ENTRADA = 100f
        const val RAIO_SAIDA = 125f
        const val PRECISAO_MAXIMA = 50f
        const val IDADE_MAXIMA_MS = 30_000L

        fun recente(idadeMs: Long) = idadeMs in 0L..IDADE_MAXIMA_MS
    }

    var lugarAtualId: String? = null
        private set

    fun verificar(distancias: Map<String, Float>, idadeMs: Long, precisao: Float?): String? {
        if (!recente(idadeMs) || precisao == null || !precisao.isFinite() ||
            precisao !in 0f..PRECISAO_MAXIMA) return null

        lugarAtualId?.let { id ->
            val distancia = distancias[id]
            if (distancia != null && distancia in 0f..RAIO_SAIDA) return null
        }
        lugarAtualId = null
        val proximo = distancias.entries
            .filter { it.value in 0f..RAIO_ENTRADA }
            .minByOrNull { it.value } ?: return null
        lugarAtualId = proximo.key
        return proximo.key
    }

    fun reiniciar() {
        lugarAtualId = null
    }
}
