package com.ifpr.androidapptemplatear.ui

import org.junit.Assert.*
import org.junit.Test

/** Exercita o mesmo evento usado pela câmera, com distâncias sintéticas em vez de GPS. */
class EncontroGeograficoTest {
    private val modelos = listOf("escudo.glb", "espada.glb")

    private fun localizar(game: GameViewModel, distancias: Map<String, Float>, arquivo: String) =
        game.processarLocalizacao(distancias, 0L, 10f, modelos) { arquivo }

    @Test fun primeiraEntradaEmiteModeloJaComDezPontosEAtualizacoesNoRaioNaoRepetem() {
        val game = GameViewModel()
        val encontro = localizar(game, mapOf("catedral" to 80f), "escudo.glb")!!
        assertEquals("escudo.glb", encontro.arquivo)
        assertEquals("catedral", encontro.pontoId)
        assertEquals(10, encontro.pontosGanhos)
        assertEquals(10, game.pontuacao)
        repeat(10) {
            assertNull(localizar(game, mapOf("catedral" to 90f), "espada.glb"))
        }
        assertEquals(10, game.pontuacao)
    }

    @Test fun reencontrarMesmoArquivoEmOutroPontoNaoSomaMasArquivoNovoSoma() {
        val game = GameViewModel()
        localizar(game, mapOf("catedral" to 80f, "polacos" to 500f), "escudo.glb")
        val repetido = localizar(game, mapOf("catedral" to 500f, "polacos" to 80f), "escudo.glb")!!
        assertEquals(0, repetido.pontosGanhos)
        assertEquals(10, game.pontuacao)
        val novo = localizar(game, mapOf("catedral" to 80f, "polacos" to 500f), "espada.glb")!!
        assertEquals(10, novo.pontosGanhos)
        assertEquals(20, game.pontuacao)
    }

    @Test fun foraDoRaioOuComSinalInvalidoNaoEmiteEncontroNemSoma() {
        val game = GameViewModel()
        assertNull(localizar(game, mapOf("catedral" to 101f), "escudo.glb"))
        assertNull(game.processarLocalizacao(mapOf("catedral" to 80f), 30_001, 10f, modelos))
        assertNull(game.processarLocalizacao(mapOf("catedral" to 80f), 0, 100f, modelos))
        assertEquals(0, game.pontuacao)
        assertNull(game.visitasGeograficas.lugarAtualId)
    }

    @Test fun novaCacaReavaliaFixRecenteMesmoParadoSemDuplicarNaAtualizacaoSeguinte() {
        val game = GameViewModel()
        val ultimaPosicao = mapOf("catedral" to 80f)
        localizar(game, ultimaPosicao, "escudo.glb")
        assertEquals(10, game.pontuacao)
        game.iniciarPartida()
        assertEquals(0, game.pontuacao)
        // É a reavaliação chamada pela câmera após Iniciar partida, sem deslocamento novo.
        assertEquals(10, localizar(game, ultimaPosicao, "escudo.glb")!!.pontosGanhos)
        assertNull(localizar(game, ultimaPosicao, "escudo.glb"))
        assertEquals(10, game.pontuacao)
    }

    @Test fun iniciarAlternarEColetarEscudoNaoSaoDescobertasGeograficas() {
        val game = GameViewModel()
        assertEquals(0, game.pontuacao) // O modelo inicial da câmera pode estar visível.
        game.iniciarPartida()
        game.tocarObjeto(game.rodada, 0L)
        game.avancarTempo(2_000L)
        assertEquals(GameViewModel.Objeto.ESPADA, game.objetoAtual)
        assertEquals(1, game.escudos)
        assertEquals(0, game.pontuacao)
    }

    @Test fun encontroDuranteCacaPreservaAlvoVidasEscudosERelogio() {
        val game = GameViewModel()
        game.iniciarPartida()
        game.avancarTempo(2_000L)
        val encontro = localizar(game, mapOf("catedral" to 80f), "escudo.glb")!!
        assertEquals("espada.glb", encontro.arquivo)
        assertEquals(10, game.pontuacao)
        assertEquals(3, game.vidas)
        assertEquals(0, game.escudos)
        assertEquals(28_000L, game.tempoRestanteMs)
    }

    @Test fun gameOverBloqueiaTodoEventoDeLocalizacao() {
        val game = GameViewModel()
        game.iniciarPartida()
        game.avancarTempo(30_000L)
        assertNull(localizar(game, mapOf("catedral" to 80f), "escudo.glb"))
        assertNull(game.visitasGeograficas.lugarAtualId)
        assertEquals(0, game.pontuacao)
    }

    @Test fun reavaliarPosicaoAntigaOuSemArquivoValidoNaoDaPontos() {
        val game = GameViewModel()
        game.iniciarPartida()
        assertNull(game.processarLocalizacao(mapOf("catedral" to 80f), 30_001, 10f, modelos))
        assertNull(game.processarLocalizacao(mapOf("catedral" to 80f), 0, 10f, emptyList()))
        assertNull(game.processarLocalizacao(mapOf("catedral" to 80f), 0, 10f, listOf("ausente.glb")))
        assertEquals(0, game.pontuacao)
        assertNull(game.visitasGeograficas.lugarAtualId)
    }
}
