# Família+ MVP

## Nomes desta versão

Esta versão usa nomes menos explícitos, mas ainda transparentes:

- **Família+**: app do responsável.
- **Bem-estar**: app acompanhado.

O projeto evita nomes enganosos como “Configurações”, “Serviços Android” ou “Atualização do Sistema”, porque isso poderia caracterizar disfarce indevido.


MVP Android em Kotlin para proteção familiar consentida.

Este repositório gera dois APKs:

- `Família+`: app do responsável. Lê a última localização, bateria e SOS do acompanhado.
- `Bem-estar`: app do acompanhado. Envia localização com permissão explícita e notificação fixa de serviço ativo.

## O que esta versão faz

- Pareamento simples por `código familiar`.
- Envio de localização para Firebase Realtime Database via REST.
- Painel Admin com atualização manual ou automática a cada 5 segundos.
- Botão SOS no Companion.
- Status de bateria.
- Build automático no GitHub Actions.
- Artifact com dois APKs debug.

## O que esta versão NÃO faz

- Não é app escondido.
- Não grava microfone escondido.
- Não burla permissão do Android.
- Não mede batimento real diretamente pelo celular.

Batimentos cardíacos entram na fase 2 com Wear OS/Health Connect ou relógio/smartband compatível.

## Backend rápido para teste com Firebase Realtime Database

1. Crie um projeto no Firebase.
2. Crie um Realtime Database.
3. Copie a URL do banco, exemplo:

```txt
https://seu-projeto-default-rtdb.firebaseio.com
```

4. Para teste inicial, use regras temporárias de desenvolvimento:

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

Atenção: essas regras são abertas e servem apenas para teste. Antes de usar com clientes reais, troque por login, autenticação e regras por família.

## Como usar nos celulares

1. Instale `FamiliaPlus-Admin-debug.apk` no celular do responsável.
2. Instale `BemEstar-Companion-debug.apk` no celular acompanhado.
3. Nos dois apps, coloque a mesma Firebase DB URL.
4. Nos dois apps, coloque o mesmo código familiar, exemplo:

```txt
familia-teste
```

5. No Companion, toque em `Permitir` e depois `Iniciar`.
6. No Admin, toque em `Salvar e atualizar` ou `Ativar ao vivo`.

## GitHub Actions

Ao enviar este projeto para o GitHub, o workflow `.github/workflows/android-debug.yml` gera os APKs automaticamente.

O artifact final se chama:

```txt
FamiliaPlus-debug-apks
```

Dentro dele estarão:

```txt
FamiliaPlus-Admin-debug.apk
BemEstar-Companion-debug.apk
```

## Comandos Termux sugeridos

```bash
pkg update -y
pkg install git unzip -y

cd ~
unzip /sdcard/Download/FamiliaPlus_MVP.zip -d FamiliaPlus_MVP
cd FamiliaPlus_MVP

git init
git add .
git commit -m "Família+ MVP"

git remote add origin https://github.com/SEU_USUARIO/vidalink-mvp.git
git branch -M main
git push -u origin main
```

Depois disso, abra o GitHub > Actions > Build Família+ Debug APKs > Run workflow.

## Próximas fases

### Fase 2

- Histórico de rotas completo no Admin.
- Tela de mapa embutida.
- Geofence: casa, escola, trabalho, hospital.
- Alertas de chegada/saída.
- Health Connect/Wear OS para batimentos.

### Fase 3

- Login com Firebase Auth.
- Regras seguras por família.
- Convite por QR Code.
- Múltiplos acompanhados.
- Plano premium/mensalidade.

### Fase 4

- Botão de emergência com áudio curto voluntário, visível e acionado pelo usuário.
- Relatórios semanais.
- Painel web.
