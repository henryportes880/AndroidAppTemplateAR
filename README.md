# Caçadores de Mito

Jogo Android de reflexo em realidade aumentada, construído sobre o
AndroidAppTemplateAR e a integração **feature-sceneview / SceneView 2.2.1**.

Branch da entrega de 10/11: [`10/11`](https://github.com/henryportes880/AndroidAppTemplateAR/tree/10/11).

## Fase 5 — localização e pontuação (17/11)

Branches da atividade: [`17-11`](https://github.com/henryportes880/AndroidAppTemplateAR/tree/17-11)
e [`feature-location-score`](https://github.com/henryportes880/AndroidAppTemplateAR/tree/feature-location-score).

### Base conferida e integração

- Base: `origin/10/11`, commit **`baa66d71033427ec929226547cf9234dee2c2753`**.
  A diferença para `origin/03-11.2` era somente a indicação da branch no README.
  A `main` estava atrasada: nela faltavam a caça, o Game Over e os modelos de escudo/espada.
- Foi criado um checkout separado, inicialmente limpo, e a branch `17-11` antes das edições.
  Trabalho existente apenas no computador do aluno não foi acessado ou substituído.
- A localização não estava em `10/11` nem em uma branch de origem. Foi comparada e integrada
  a `upstream/feature-location` do professor, commit `2496fb260c612e57917ce1523df7d22b44e201cb`.
  Merge **`89894ef`**: conflito da câmera resolvido mantendo as regras próprias do jogo.
  O merge ocorreu somente em `17-11`; `main` e `10/11` foram preservadas.
- A solução usa Kotlin, Fragments/XML, `LocationManager`, `Geocoder`, corrotinas e
  os ViewModels da Activity, além de manter SceneView/ARCore e os modos de câmera/3D.

### Pontos turísticos: circuito urbano de patrimônio

Os três cadastros do professor foram personalizados. Catedral e Igreja dos Polacos
passaram a usar referências das praças públicas próximas. Vila Velha foi substituída
pelo Ponto Azul para concentrar a exploração no centro histórico, sem deslocamento
até o parque estadual. Foram acrescentados **Estação Saudade** e **Parque Ambiental**:
**cinco pontos no total**, com IDs estáveis em `PontosTuristicos.kt`.

Coordenadas em graus decimais, na ordem latitude e longitude. O raio se refere à
coordenada indicada, não à área inteira do parque. Use as áreas públicas acessíveis
no entorno, respeite o trânsito e as condições locais; não é necessário entrar em edifícios.

| ID / alteração | Referência | Latitude | Longitude | Fonte dos nomes e coordenadas |
| --- | --- | --- | --- | --- |
| `catedral` — alterado | Praça Marechal Floriano Peixoto, junto à Catedral | -25.097639 | -50.158917 | [Turismo Campos Gerais — praça e entorno](https://www.camposgeraisdoparana.com.br/cidades/ponta-grossa/praca-marechal-floriano-peixoto-e-seu-entorno/) (DMS convertido e arredondado) |
| `polacos` — alterado | Praça Barão de Guaraúna, junto à Igreja dos Polacos | -25.094701 | -50.162793 | [Departamento de Patrimônio Cultural de PG — praça](https://patrimonioculturalpontagrossa.wordpress.com/2020/08/10/praca-barao-de-guarauna/) |
| `ponto_azul` — substitui Vila Velha | Praça Barão do Rio Branco / Ponto Azul | -25.093357 | -50.159092 | [Departamento de Patrimônio Cultural de PG — praça](https://patrimonioculturalpontagrossa.wordpress.com/2020/08/10/praca-barao-do-rio-branco/) |
| `estacao_saudade` — novo | Estação Saudade | -25.093380 | -50.153260 | [OpenStreetMap, nó 4287261390 via Mapcarta](https://mapcarta.com/pt/N4287261390); [identificação do Sesc](https://www.sescpr.com.br/2019/12/sesc-estacao-saudade-sera-inaugurada-dia-16-de-dezembro/) |
| `parque_ambiental` — novo | Complexo Ambiental Governador Manoel Ribas | -25.095191 | -50.154734 | [Departamento de Patrimônio Cultural de PG — parque](https://patrimonioculturalpontagrossa.wordpress.com/2020/08/10/praca-parque-ambiental/) |

Fontes consultadas em 17/11/2026. Elas confirmam referências geográficas, não a
precisão de um GPS nem as condições de acesso no dia da visita.

### Regra de pontuação e relação com os modelos

- **10 pontos por arquivo de modelo novo.** A chave é o nome exato do arquivo, conforme
  o PDF (páginas 15–18); não o ID do ponto, o toque, o frame ou a instância do objeto.
- `GameViewModel.registrarEncontro` usa um `Set<String>`: o mesmo arquivo em outro
  ponto ou visita não aumenta os pontos novamente. Somente o evento geográfico chama
  essa função. Escolher **Exibir** na aba Modelos ou coletar escudos não concede esses 10 pontos.
- Os cinco GLBs originais foram preservados: `escudo.glb`, `espada.glb`, `neutrofilo.glb`,
  `plane.glb` e `sofa.glb`. O controlador do professor não tinha associação fixa ponto/modelo:
  sorteava um arquivo da raiz de assets. Essa relação foi preservada na exploração.
- **Durante a caça cronometrada**, o encontro usa o arquivo do alvo da rodada já exibido
  (escudo ou espada). Não substitui um escudo por um sofá ou modifica o comportamento do
  toque. Isso adapta a localização às regras próprias existentes. O arquivo usado na
  pontuação corresponde ao modelo selecionado/alvo, sem criar ou renomear assets.
- A exploração geográfica começa ao abrir a tela, com zero pontos. A primeira confirmação
  de **Iniciar partida** começa uma caça nova, zera pontos e arquivos encontrados e libera
  a visita atual. Reexibir a tela ou chamar o início outra vez não zera uma caça em andamento.
- **Jogar novamente** cria uma Activity/partida nova: repõe pontos, visitas, vidas, escudos e
  tempo. Girar, trocar de aba ou minimizar mantém o estado no ViewModel da Activity.
  A partida não é persistida após o encerramento do processo.
- Vitória ou Game Over bloqueiam registros e param a localização. A pontuação de descobertas
  também aparece na tela final, separada da quantidade de escudos coletados.

### Localização, endereço e ciclo de vida

Entrada em até **100 m**; nova visita somente após sair de **125 m**. Posições com mais de
**30 s** ou precisão pior que **50 m** não geram encontros. Se mais de um raio abranger a
posição, considera-se o ponto mais próximo. `VisitasGeograficas` fica no ViewModel, evitando
novo sorteio só porque o fragmento foi recriado. O conjunto de arquivos protege a pontuação
mesmo quando outra visita legítima sorteia um modelo repetido.

O `LocalizacaoController` se conecta uma vez por view, pede localização precisa em primeiro
plano e remove as inscrições dos provedores no `onStop`/`onDestroy` e no encerramento.
Permissão negada, GPS desativado, ausência de posição recente, precisão ruim e falha de
endereço têm mensagens próprias e ação de tentativa/permissão. Não há monitoramento em
segundo plano. A caça de reflexo continua utilizável quando a localização não é autorizada.

O endereço usa `Geocoder` em `Dispatchers.IO`, sem bloquear a interface. A consulta é
limitada a uma por vez e renovada depois de 30 segundos ou 50 m de deslocamento. Se o
jogador se mover enquanto a consulta estiver em andamento, busca a posição mais recente
antes de exibir um endereço antigo. A falha de endereço não impede a detecção por coordenadas.

### Interface e botões

Mantidos nome, ícone e modelos de **Caçadores de Mito**, com roxo e verde-água da identidade
existente organizados em uma paleta escura. Botões Material com cantos discretos, contraste,
altura mínima e estados pressionado/desabilitado consistentes. **Posicionar** fica centralizado
na base, com largura limitada, ícone e espaço reservado. Os controles e informações ficam
em painel rolável acima dele para telas menores, paisagem e fontes maiores. Endereço atual
e **Pontos: N** aparecem juntos nesse painel. Foram tratados os insets das barras do sistema.

### Verificações da Fase 5

Verificação executada nesta branch com **JDK 17.0.21**, **Gradle 8.11.1** e **SDK 36**:

- `assembleDebug`: passou; APK de debug gerado.
- `testDebugUnitTest`: **24 testes passaram, zero falhas/erros** (16 do GameViewModel,
  7 de visitas geográficas e 1 preexistente). Cobrem +10 no primeiro arquivo, duplicidade,
  total por arquivos diferentes, reinício, vitória/derrota e limites de distância/precisão/idade.
- `lintDebug`: passou, **0 erros e 54 avisos**, registrados no relatório em
  `app/build/reports/lint-results-debug.html`. Não foi criado baseline nem desligado o lint.
  Os avisos incluem dependências, recursos legados sem uso e textos da visualização.
- `assembleDebugAndroidTest`: passou. A compilação inclui a verificação de retenção de
  pontos na recriação e a presença dos campos de endereço e pontuação. **Esses testes Android
  não foram executados em dispositivo.**
- `git diff --check`: passou; os cinco GLBs não têm alterações em relação a `baa66d7`.
- Contraste calculado da paleta: texto branco no botão roxo **7,10:1**; texto secundário
  no painel **9,26:1**; destaque verde-água no painel **9,96:1**. Isso não substitui a
  conferência visual de tamanho/posição/toque em um Android.

Os testes de visitas usam **distâncias sintéticas**, não GPS, câmera ou AR físicos.
Não há AVD configurado, `/dev/kvm` ou USB de celular exposto neste ambiente. O ADB da
imagem também não inicializou porque tentou gravar em `/home/agent/.android`, que é
somente leitura. Assim, **câmera, GPS, endereço real, permissões na interface, aparência em
aparelho e o print solicitado continuam pendentes do roteiro abaixo**. Nenhuma captura
foi fabricada como evidência de execução.

### Executar e obter o print exigido

1. No Android Studio, busque as referências e abra `17-11` ou `feature-location-score`.
   Antes de trocar a branch, confira `git status` e preserve alterações locais. Sincronize
   o Gradle com SDK 36 e JDK 17 ou superior compatível com Gradle 8.11.1.
2. Execute `app` em Android API 24 ou superior. No celular, ative depuração USB e use um
   aparelho compatível com ARCore para validar AR. Autorize câmera e **localização precisa**.
3. Na aba **Jogar**, ative o GPS e aguarde o endereço real. Aproxime-se de um ponto da tabela
   e confira **Pontos: 10** na primeira descoberta. Em AR, toque em **Posicionar** para ancorar
   o modelo; a pontuação geográfica não exige esse toque.
4. Capture com **Power + Volume para baixo** ou com o botão **Take Screenshot** do Android
   Studio/Running Devices. Enquadre o próprio aplicativo com endereço e pontuação legíveis
   ao mesmo tempo; se necessário, role o painel para mostrar ambos. Não use mockup ou imagem
   de preview XML como prova. Revise o endereço exposto antes de compartilhar a imagem.

Comandos de build/teste e instalação:

```sh
bash gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
# Com celular/emulador conectado:
bash gradlew installDebug connectedDebugAndroidTest
```

O APK de debug fica em `app/build/outputs/apk/debug/app-debug.apk` e não é versionado.

### Roteiro curto de validação no Android

- **Primeira descoberta / duplicidade:** entrar em um raio; conferir +10. Permanecer no
  local e girar a tela: não deve ocorrer outro encontro nem aumentar o total. Escolher um
  arquivo na aba Modelos também não deve pontuar.
- **Outra visita / arquivo novo:** sair mais de 125 m e voltar. Se o arquivo for o mesmo,
  manter o total; se for outro, somar 10. A unicidade é por arquivo em toda a sessão/partida,
  inclusive entre dois pontos diferentes.
- **Localização simulada:** no emulador use Extended Controls → Location, envie as
  coordenadas da tabela, depois uma posição fora de 125 m e retorne. Latitude/longitude
  no painel do emulador podem aparecer em ordem diferente: confira os rótulos. O projeto
  detecta emuladores e usa câmera/3D como fallback; isso não valida ARCore físico. Registre
  essa execução como **localização simulada**, mesmo que o Geocoder devolva um endereço real.
- **Permissões e falhas:** negar localização ou permitir somente aproximada deve apresentar
  a orientação sem encerrar o app; ativar a precisa nas Configurações deve permitir retomar.
  Desligar GPS deve mostrar aviso. Sem Internet/Geocoder, o endereço pode ficar indisponível,
  mas coordenadas precisas ainda podem gerar encontros.
- **Caça, fim e reinício:** iniciar partida zera a pontuação anterior e mantém a regra de
  3 vidas, 5 escudos e 30 s. Encontros durante a caça usam o alvo atual, sem alterar escudos,
  vidas ou o relógio. Depois de vencer/perder, callbacks não aumentam pontos. Jogar novamente
  começa em zero. Rotação, alternância de abas e retorno do segundo plano não repõem recursos.
- **Interface:** conferir retrato/paisagem e fonte grande, estados habilitado/desabilitado,
  acesso ao endereço/pontos e ausência de sobreposição com Posicionar e a barra de navegação.

### Explicação curta para revisar e entregar

> Cada modelo encontrado pela localização vale 10 pontos na primeira descoberta. O jogo
> guarda os nomes dos arquivos em um conjunto, então encontrar o mesmo arquivo em outro
> ponto ou visita não soma novamente. O ViewModel mantém os pontos ao girar a tela. Uma nova
> partida limpa os pontos e o conjunto, e o Game Over impede novas descobertas. O endereço
> atual vem da localização do Android, com a conversão para endereço feita fora da interface.

## Como jogar

1. Abra a aba **Jogar**. Em AR, aponte para uma mesa ou chão e toque em
   **Posicionar**. O aplicativo também preserva os modos de câmera/3D da base.
2. Toque em **Iniciar caça**, leia as regras e confirme **Iniciar partida**.
3. **Recupere 5 escudos em 30 segundos.** Um objeto aparece por vez, alternando
   entre escudo e espada a cada 2 segundos de jogo.
4. Toque no **escudo** para coletar. Evite a **espada**: tocá-la tira uma das suas
   três vidas. Cada aparição só conta uma vez; o objeto desaparece após o toque e
   o próximo aparece na troca programada. Tocar no fundo não produz efeito.
5. Ao coletar 5 escudos, você vence. Com zero vidas, ou tempo esgotado antes da
   meta, aparece **Game Over**, informando o motivo e o resultado da coleta.
6. **Jogar novamente** inicia uma partida nova: 3 vidas, 0 escudos, 30 segundos,
   escudo inicial e regras antes de começar.

O relógio só corre com a tela em primeiro plano, modelo carregado e cena pronta;
em AR, o objeto deve estar ancorado e a câmera rastreando. Trocar de aba, girar o
aparelho, minimizar ou reposicionar pausa o tempo durante a transição, sem repor
vidas, escudos ou tempo. É preciso reposicionar após recriar a cena AR.

A aba **Modelos** mantém os arquivos anteriores para exploração antes da partida.
Durante uma caça, ela não permite substituir o alvo e alterar a regra de coleta.
Os objetos do jogo mantêm a mesma âncora AR entre as rodadas. Escudo e espada são
reutilizados na mesma cena, evitando carregar os GLBs a cada aparição. Os gestos
de edição ficam disponíveis na exploração e são desativados nos alvos da caça.

## Atividade do professor e revisão inicial

Referência: **Desenvolvimento de Aplicativos — Jogo em Realidade Aumentada –
Game Over**, Jailton Coelho, 2026, Aula 2 (18 páginas).

O repositório foi clonado neste ambiente. Não havia alterações locais acessíveis.
A branch confirmada pelo aluno foi `03-11.2`, originalmente em `99709e1`.
Não foram encontrados arquivos `AGENTS.md`; o README original continha apenas
o nome do repositório. Arquivos eventualmente existentes apenas no computador do
aluno não puderam ser inspecionados. A integração da cena já estava presente;
as regras e a tela final estavam ausentes. Os modelos e a base de visualização
foram preservados. A atualização do jogo acrescenta uma meta de coleta e um
prazo, dando propósito ao uso dos escudos e espadas existentes.

| Requisito do PDF | Situação inicial em `99709e1` | Implementação atual |
| --- | --- | --- |
| Usar a integração feature-sceneview (p. 6) | Concluído no código | Preservado SceneView 2.2.1 e fallback de câmera/3D |
| Definir e explicar a derrota (p. 2–4) | Ainda não implementado | Zero vidas ou prazo de 30 s sem coletar 5 escudos; regras antes do início |
| Mostrar vidas e controlar o estado (p. 7) | Ainda não implementado | HUD com vidas, escudos e tempo; GameViewModel |
| Intervalo de 1 s entre danos, se usada a regra das vidas (p. 5, 8) | Ainda não implementado | Intervalo mantido, além de permitir um dano por aparição da espada |
| Interação real ligada à derrota (p. 9) | Ainda não implementado | Hit test/toque no nó 3D; espada tira vida, escudo pontua |
| Encerrar ações e tarefas (p. 10–11) | Ainda não implementado | Bloqueia ações e cancela relógio, carregamentos e watchdogs; libera cena e câmera; não há sons de partida |
| GameOverActivity com motivo e botão (p. 12–14) | Ainda não implementado | Mesma classe/pacote; interface por código; derrota e vitória mostram coleta e reinício |
| Activity não exportada no Manifest (p. 15) | Ainda não implementado | Registrada com exported=false |
| Reiniciar todo o estado e repor objetos (p. 16) | Ainda não implementado | Nova MainActivity, novos ViewModels e cena nova |
| Girar não recuperar vidas (p. 16) | Ainda não implementado | Estado no escopo da Activity: mantém vidas, coleta, tempo e rodada |
| Tela única e Voltar não recuperar partida (p. 17) | Ainda não implementado | Guarda contra abertura repetida e finalização da Activity anterior |
| Testar no celular (p. 17–18) | Pendente | Ainda requer o roteiro físico abaixo |
| Entregar branch, vídeo e regra (p. 18) | Incompleto: branch existia | Regra e branch documentadas; gravação e postagem no Classroom pendentes |

O PDF permite escolher uma condição coerente com o jogo. Três vidas são um
exemplo, e o toque em objeto perigoso é permitido na página 9. Não foi necessário
configurar Firebase, obter novos modelos ou trocar as tecnologias do projeto.

## Validação da aula anterior — registro histórico

Validação da base `10/11`, anterior à Fase 5, executada com JDK 17 e SDK 36:

- `assembleDebug`: passou; APK gerado.
- `testDebugUnitTest`: passou, 13 testes no total, sem falhas (12 da lógica do
  jogo e 1 preexistente). Cobrem vitória, as duas derrotas, coleta única, toques
  atrasados, alternância de objetos, prazo e bloqueio após o encerramento.
- `lintDebug`: passou, 0 erros e 53 avisos nas partes preexistentes do projeto
  (recursos, textos e dependências). Nenhum aviso foi ocultado.
- `assembleDebugAndroidTest`: passou. Isso comprova a compilação dos testes
  Android, não a execução deles em um dispositivo.

Análise de código, testes unitários e compilação não comprovam que o AR funciona
em um celular. Não há dispositivo conectado ao ADB, emulador instalado ou
`/dev/kvm` disponível neste ambiente. Os testes Android foram preparados para
recriação da Activity, mensagem final, reinício e vitória, mas precisam de um
Android para serem executados.

## Executar

Abra esta branch no Android Studio, use **JDK 17**, **SDK 36**, Build Tools 35.0.0
e execute o módulo `app` em Android 7.0/API 24 ou superior.
Para AR físico, use celular compatível com ARCore e Google Play Services for AR
instalado; autorize a câmera. Os modos de câmera/3D são alternativas de
visualização e não comprovam o rastreamento AR.

```sh
bash gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
# Com Android conectado e depuração USB ativada:
bash gradlew installDebug
bash gradlew connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (não versionado).
O APK Android não pode ser instalado no iPhone.

## Roteiro de teste e vídeo real

- **Preparação:** abrir, posicionar, iniciar e conferir 3 vidas, 0/5 escudos e 30 s.
  O prazo não deve diminuir durante carregamento ou posicionamento.
- **Coleta/vitória:** tocar somente nos escudos de aparições diferentes. Tocar
  repetidamente no mesmo objeto não deve duplicar a coleta. No quinto, conferir
  Vitória e 5/5; reiniciar e conferir o estado inicial.
- **Derrota por vidas:** iniciar outra partida e tocar nas três primeiras
  espadas que aparecerem. Conferir 2, 1 e 0 vidas, Game Over e o motivo.
- **Derrota por tempo:** reiniciar e não coletar escudos; após 30 segundos de
  jogo ativo, conferir Game Over por tempo, mesmo ainda tendo vidas.
- **Estado/câmera:** minimizar, girar, alternar abas e reposicionar. Conferir
  pausa nas transições, sem recuperar recursos. Em AR, conferir que a troca de
  escudo para espada mantém a posição ancorada.
- **Encerramento:** verificar que câmera e objetos param, só há uma tela final
  e Voltar não reabre a partida encerrada. Repetir o reinício.

Grave a execução real de uma derrota e do reinício para o Classroom. Uma
simulação ilustrativa não substitui esse teste ou a gravação do aplicativo.

Descrição pronta para entregar:

> O objetivo é recuperar 5 escudos em 30 segundos. Escudos e espadas alternam
> automaticamente a cada 2 segundos. Tocar no escudo aumenta a coleta; tocar na
> espada retira uma das 3 vidas. Cada aparição só conta uma vez. A partida termina
> quando as vidas acabam ou o tempo esgota antes da meta. O Game Over informa o
> motivo, interrompe as ações e permite reiniciar com o estado inicial. Ao atingir
> a meta, a tela final mostra vitória.

## Apoio na implementação

IA utilizada para integrar a localização, implementar pontuação e estilos, preparar testes e documentação. O aluno deve revisar a regra e executar o roteiro em Android antes da entrega.
