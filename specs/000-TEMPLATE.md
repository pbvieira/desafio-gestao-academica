<!--
COMO USAR ESTE TEMPLATE
1. Copie este arquivo para specs/NNN-nome-curto-da-tarefa.md (NNN = próximo número sequencial, 3 dígitos).
2. Preencha TODAS as seções antes de escrever código de produção. "N/A" é uma resposta válida, silêncio não é.
3. Se, durante a implementação, algo divergir do planejado aqui, atualize este arquivo — ele deve refletir
   a realidade ao final da tarefa, não só a intenção inicial.
4. Toda decisão marcada abaixo como "requer confirmação" vira uma entrada em docs/DECISIONS.md antes de você
   prosseguir com a implementação dessa parte específica.
-->

# NNN — <Nome curto da tarefa>

**Status:** `rascunho` | `aprovada` | `em andamento` | `concluída`
**Seção(ões) do PRD relacionadas:** <ex: PRD §02, §04>
**Módulo(s) Modulith afetado(s):** <ex: academico, notificacao>

## 1. Objetivo

<Uma ou duas frases: o que esta tarefa entrega e por quê. Evite descrever "como" aqui — isso vai na seção 4.>

## 2. Escopo

**Dentro do escopo:**
- <item>
- <item>

**Fora do escopo (não-objetivos):**
- <item — seja explícito sobre o que NÃO está sendo feito aqui, para não expandir a tarefa sem perceber>

## 3. Regras de negócio envolvidas

<Liste as regras do PRD §02 que esta tarefa implementa, altera ou depende. Copie a regra literal do PRD e
adicione qualquer refinamento necessário para implementá-la sem ambiguidade.>

- [ ] <regra 1>
- [ ] <regra 2>

## 4. Abordagem técnica

<Descreva a solução em nível de design: quais classes/componentes, qual camada, como se encaixa na
arquitetura-alvo do CLAUDE.md. Não precisa ser um diagrama formal, mas precisa ser específico o suficiente
para alguém (ou você, daqui a uma semana) implementar sem reinventar o desenho.>

## 5. Decisões que requerem confirmação antes de implementar

<Liste aqui qualquer ponto onde existe mais de uma alternativa razoável. NÃO decida sozinho — pare aqui,
registre a pergunta, e só prossiga depois que a decisão estiver em docs/DECISIONS.md (ver skill decision-log).
Se não houver nenhuma decisão não-trivial nesta tarefa, escreva "Nenhuma identificada".>

| # | Pergunta | Alternativas consideradas | Decisão (link para DECISIONS.md) |
|---|---|---|---|
| 1 | | | |

## 6. Critérios de aceite

<Lista objetiva e testável. Cada item aqui deveria, idealmente, virar um teste ou um cenário de teste.>

- [ ] <critério 1>
- [ ] <critério 2>

## 7. Plano de testes

| Tipo | O que será testado | Onde vive |
|---|---|---|
| Unitário | | `src/test/java/...` |
| Integração | | `src/test/java/...` (Testcontainers) |
| E2E | | `e2e/...` |

**Justificativa de cobertura:** <por que os testes acima são suficientes para as regras críticas desta
tarefa — não escreva "para bater 80%", escreva o que de fato está sendo validado e por quê importa.>

## 8. Impacto em observabilidade / segurança

- **Logs/observabilidade:** <novos logs estruturados, métricas, health checks afetados, se houver>
- **Segurança:** <endpoints novos/alterados, dados sensíveis envolvidos, validações de entrada necessárias>

## 9. Definition of Done desta tarefa

- [ ] Critérios de aceite (seção 6) atendidos
- [ ] Testes da seção 7 implementados e passando
- [ ] Cobertura ≥ 80% no(s) módulo(s) afetado(s), com sentido (ver CLAUDE.md item 5)
- [ ] `docs/DECISIONS.md` atualizado com todas as decisões da seção 5
- [ ] `code-reviewer` executado — achados endereçados ou justificadamente descartados
- [ ] `security-auditor` executado — achados endereçados ou justificadamente descartados
- [ ] Esta spec atualizada para refletir o que foi de fato implementado
- [ ] `./mvnw clean verify` passando
