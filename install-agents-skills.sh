#!/usr/bin/bash

# Dê permissão de execução para o script
# sudo chmod u+x install-agents-skills.sh

# Instala os sub-agentes

mkdir -p .claude/agents
BASE=https://raw.githubusercontent.com/VoltAgent/awesome-claude-code-subagents/main/categories

curl -s $BASE/02-language-specialists/java-architect.md        -o .claude/agents/java-architect.md
curl -s $BASE/02-language-specialists/spring-boot-engineer.md  -o .claude/agents/spring-boot-engineer.md
curl -s $BASE/02-language-specialists/angular-architect.md     -o .claude/agents/angular-architect.md
curl -s $BASE/01-core-development/ui-designer.md                -o .claude/agents/ui-designer.md
curl -s $BASE/04-quality-security/code-reviewer.md             -o .claude/agents/code-reviewer.md
curl -s $BASE/04-quality-security/security-auditor.md          -o .claude/agents/security-auditor.md
curl -s $BASE/04-quality-security/qa-expert.md                 -o .claude/agents/qa-expert.md
curl -s $BASE/04-quality-security/test-automator.md            -o .claude/agents/test-automator.md
curl -s $BASE/04-quality-security/architect-reviewer.md        -o .claude/agents/architect-reviewer.md
curl -s $BASE/08-business-product/technical-writer.md          -o .claude/agents/technical-writer.md
curl -s $BASE/09-meta-orchestration/agent-organizer.md         -o .claude/agents/agent-organizer.md

# Instala as skills
#
# using-superpowers, using-git-worktrees, subagent-driven-development e
# finishing-a-development-branch não eram instaladas antes, mas writing-plans e
# executing-plans (abaixo) as citam como "REQUIRED SUB-SKILL"/skill raiz do pacote
# (ver .claude/skills/writing-plans/SKILL.md e executing-plans/SKILL.md) — sem elas,
# essas duas skills instruem o uso de sub-skills que não existem no projeto.

npx skills add obra/superpowers \
  -s brainstorming \
  -s writing-plans \
  -s executing-plans \
  -s test-driven-development \
  -s requesting-code-review \
  -s receiving-code-review \
  -s verification-before-completion \
  -s using-superpowers \
  -s using-git-worktrees \
  -s subagent-driven-development \
  -s finishing-a-development-branch \
  --agent claude-code \
  -y