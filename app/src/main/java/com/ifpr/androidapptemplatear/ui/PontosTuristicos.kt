package com.ifpr.androidapptemplatear.ui

data class PontoTuristico(val id: String, val nome: String, val latitude: Double, val longitude: Double)

/** Circuito urbano de patrimônio de Ponta Grossa. Fontes e alterações no README. */
object PontosTuristicos {
    val lugares = listOf(
        PontoTuristico("catedral", "Praça Marechal Floriano Peixoto — Catedral", -25.097639, -50.158917),
        PontoTuristico("polacos", "Praça Barão de Guaraúna — Igreja dos Polacos", -25.094701, -50.162793),
        PontoTuristico("ponto_azul", "Praça Barão do Rio Branco — Ponto Azul", -25.093357, -50.159092),
        PontoTuristico("estacao_saudade", "Estação Saudade", -25.093380, -50.153260),
        PontoTuristico("parque_ambiental", "Complexo Ambiental Governador Manoel Ribas", -25.095191, -50.154734)
    )
}
