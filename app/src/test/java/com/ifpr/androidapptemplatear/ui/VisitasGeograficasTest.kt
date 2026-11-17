package com.ifpr.androidapptemplatear.ui

import org.junit.Assert.*
import org.junit.Test

class VisitasGeograficasTest {
    @Test fun entraEmCemMetrosEPermanecerNaoRepeteEncontro() {
        val visitas = VisitasGeograficas()
        assertNull(visitas.verificar(mapOf("a" to 101f), 0, 10f))
        assertEquals("a", visitas.verificar(mapOf("a" to 100f), 0, 10f))
        for (distancia in listOf(80f, 98f, 103f, 125f)) {
            assertNull(visitas.verificar(mapOf("a" to distancia), 0, 10f))
        }
    }

    @Test fun precisaSairDeCentoEVinteECincoParaNovaVisita() {
        val visitas = VisitasGeograficas()
        visitas.verificar(mapOf("a" to 80f), 0, 10f)
        assertNull(visitas.verificar(mapOf("a" to 126f), 0, 10f))
        assertNull(visitas.lugarAtualId)
        assertEquals("a", visitas.verificar(mapOf("a" to 80f), 0, 10f))
    }

    @Test fun escolheMaisProximoSemTrocarPorOscilacaoEntreDoisPontos() {
        val visitas = VisitasGeograficas()
        assertEquals("b", visitas.verificar(mapOf("a" to 90f, "b" to 50f), 0, 10f))
        assertNull(visitas.verificar(mapOf("a" to 10f, "b" to 120f), 0, 10f))
        assertEquals("a", visitas.verificar(mapOf("a" to 80f, "b" to 126f), 0, 10f))
    }

    @Test fun posicaoAntigaOuPrecisaoRuimNaoGeraEncontro() {
        val visitas = VisitasGeograficas()
        for (idade in listOf(-1L, 30_001L)) {
            assertNull(visitas.verificar(mapOf("a" to 0f), idade, 10f))
        }
        for (precisao in listOf(null, -1f, 51f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(visitas.verificar(mapOf("a" to 0f), 0, precisao))
        }
        assertEquals("a", visitas.verificar(mapOf("a" to 0f), 30_000L, 50f))
    }

    @Test fun sinalRuimNaoLiberaUmaVisitaJaRegistrada() {
        val visitas = VisitasGeograficas()
        visitas.verificar(mapOf("a" to 80f), 0, 10f)
        assertNull(visitas.verificar(mapOf("a" to 200f), 0, 100f))
        assertNull(visitas.verificar(mapOf("a" to 80f), 0, 10f))
    }

    @Test fun reinicioLiberaVisitaEConservaOsLimitesDaRegra() {
        val visitas = VisitasGeograficas()
        visitas.verificar(mapOf("a" to 80f), 0, 10f)
        visitas.reiniciar()
        assertEquals("a", visitas.verificar(mapOf("a" to 80f), 0, 10f))
    }

    @Test fun cadastroTemCincoPontosDistintosComIdentidadeECoordenadasValidas() {
        val lugares = PontosTuristicos.lugares
        assertTrue(lugares.size >= 5)
        assertEquals(lugares.size, lugares.map { it.id }.toSet().size)
        assertEquals(lugares.size, lugares.map { it.latitude to it.longitude }.toSet().size)
        lugares.forEach {
            assertTrue(it.nome.isNotBlank())
            assertTrue(it.latitude in -90.0..90.0)
            assertTrue(it.longitude in -180.0..180.0)
        }
    }
}
