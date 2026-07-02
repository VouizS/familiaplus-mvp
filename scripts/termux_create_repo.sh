#!/data/data/com.termux/files/usr/bin/bash
set -e

REPO_DIR="$HOME/vidalink-mvp"
ZIP_NAME="FamiliaPlus_MVP.zip"

echo "==> Preparando pasta $REPO_DIR"
rm -rf "$REPO_DIR"
mkdir -p "$REPO_DIR"
cp -r . "$REPO_DIR"
cd "$REPO_DIR"

git init
git add .
git commit -m "Família+ MVP: admin and companion apps" || true

echo "==> Repositório local criado em: $REPO_DIR"
echo "Próximo passo: crie um repo vazio no GitHub e conecte com:"
echo "git remote add origin https://github.com/SEU_USUARIO/vidalink-mvp.git"
echo "git branch -M main"
echo "git push -u origin main"
