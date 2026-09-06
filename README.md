# Guarda-Roupa — App Android (Kotlin + Jetpack Compose + Room)

## Como compilar

1. Instale o **Android Studio** (versão Koala/2024.1 ou mais recente).
2. Abra a pasta `RoupasApp` como projeto ("Open" → selecione esta pasta).
3. Deixe o Gradle sincronizar (ele vai baixar as dependências: Compose, Room, Coil, uCrop).
4. Rode em um emulador ou celular com **Android 8.0 (API 26)** ou superior.

> Este projeto foi escrito neste ambiente sem acesso à internet, então o Gradle **não foi
> executado/compilado aqui** — não há como baixar dependências ou rodar o Android SDK neste
> chat. O código foi revisado com cuidado, mas pequenos ajustes de sintaxe podem aparecer no
> primeiro build; é normal em projetos gerados dessa forma.

## O que está implementado de verdade

### Base (primeira versão)
- Banco de dados local (Room), 100% offline, dados persistem entre aberturas.
- Regra de **código único por tipo** (T01/G01/C01…), com mensagem de erro amigável.
- Regra de **combinação só existe com terno+gravata+camisa**, sem duplicar peças.
- **Exclusão em cascata** (peça → combinações → registros de calendário) sem referências quebradas.
- **Calendário minimizado de 3 meses** com números de dia pequenos e blur real (API 31+) nos
  meses anterior/seguinte, mês atual em destaque nítido.
- Formulário de **adicionar/editar peça** com foto (câmera/galeria + recorte/rotação via uCrop).
- **Calendário completo** com navegação de mês/ano, botão "Hoje", múltiplos registros por dia.
- Tema visual escuro "alfaiataria" com cartões translúcidos (glass).

### Completado nesta rodada
- **Navegação Terno → Gravata → Camisa → Calendário** (seção 7): telas próprias
  (`GravatasDoTernoScreen`, `CamisasDaCombinacaoScreen`); ao tocar numa camisa, abre um
  `DatePicker` para registrar o uso daquela combinação no dia escolhido.
- **Importar combinações**: abre o seletor de arquivos do sistema (SAF), valida a estrutura
  do ZIP, mostra o resumo com as contagens e só grava no banco após confirmação — com
  renumeração automática de códigos em conflito.
- **Exportar combinações**: diálogo para escolher o nome do arquivo e salva o `.zip` em
  Downloads (via MediaStore no Android 10+, escrita direta com permissão em versões anteriores).
- **Modo Grade com densidade ajustável**: 4 pontos selecionáveis (2/3/4 colunas + uma variante
  compacta de 4 colunas), sem depender de `LazyVerticalGrid` aninhado.
- **Pesquisa e filtros** (seção 11): tela dedicada com busca por nome/código, filtro por tipo,
  com/sem foto, combinações paradas (sem uso recente) e combinações menos usadas.
- **Editar peças** (seção 6): tela com abas Terno/Gravata/Camisa, tocar para editar, excluir
  com aviso e confirmação.
- **Prazo dos indicadores configurável** (ajuste 1): botão de relógio na barra do calendário
  completo, abre um diálogo para definir o prazo em dias (padrão 30), salvo em
  `SharedPreferences` e aplicado a todas as combinações sem apagar o histórico.
- **Cartão "+ Adicionar" no topo de cada sessão** (ajuste 2): Ternos/Gravatas/Camisas abrem o
  formulário já com o tipo fixo; Conjuntos abre um fluxo dedicado que obriga a ordem
  Terno → Gravata → Camisa antes de criar a combinação.
- **Persistência do modo de visualização** (ajuste 3): Detalhado/Compacto/Grade/Peças é um
  único estado salvo em `SharedPreferences`, usado em Ternos, Gravatas, Camisas e (parcialmente)
  Conjuntos, e não volta ao padrão ao navegar ou reabrir o app.
- Tela inicial reorganizada em **4 sessões com abas**: Ternos, Gravatas, Camisas e Conjuntos —
  cada uma com o cartão "+Adicionar" e a lista no modo de visualização atual.

## O que ainda ficou simplificado

- O **modo "Peças"** na sessão Conjuntos sempre usa o cartão de combinação empilhada (não há
  hoje uma variante Detalhado/Compacto/Grade específica para conjuntos — a spec diz "quando
  aplicável", e a leitura aqui foi a de manter Conjuntos com uma única apresentação).
- Ao criar um conjunto pelo fluxo "+Adicionar conjunto", a lista de gravatas/camisas mostradas
  é a lista completa cadastrada (não filtrada pelas associações já existentes do terno
  escolhido) — simplificação intencional para não travar o fluxo quando ainda não há associação.
- O aviso "antes de substituir uma combinação já registrada" (seção 9.5) não bloqueia nem
  pergunta explicitamente — como o app permite múltiplos usos no mesmo dia (seção 9.6), registrar
  de novo apenas adiciona um novo registro, sem sobrescrever o anterior.
- A leitura/validação do ZIP na importação roda de forma síncrona no momento do clique
  (não em background); para arquivos muito grandes isso pode travar a UI por um instante.
- Responsividade fina para tablets/orientação paisagem e testes de acessibilidade (seção 23)
  não foram auditados linha a linha.

Se quiser, posso continuar ajustando qualquer um desses pontos.
