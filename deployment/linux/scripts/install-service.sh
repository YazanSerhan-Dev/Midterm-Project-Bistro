#!/bin/bash
set -e

APP_DIR="/opt/bistro"
SERVICE_NAME="bistro"
JAR_NAME="ServerHeadless.jar"
SERVICE_FILE_SRC="$(pwd)/deployment/linux/systemd/bistro.service"
SERVICE_FILE_DST="/etc/systemd/system/${SERVICE_NAME}.service"

echo "[1/6] Create user 'bistro' (if missing)"
id -u bistro >/dev/null 2>&1 || sudo useradd -r -s /usr/sbin/nologin bistro

echo "[2/6] Create app directory ${APP_DIR}"
sudo mkdir -p "${APP_DIR}"
sudo chown -R bistro:bistro "${APP_DIR}"

echo "[3/6] Copy jar into ${APP_DIR} (you must place ${JAR_NAME} there manually or edit script)"
# Example:
# sudo cp "${JAR_NAME}" "${APP_DIR}/${JAR_NAME}"
# sudo chown bistro:bistro "${APP_DIR}/${JAR_NAME}"

echo "[4/6] Install systemd service"
sudo cp "${SERVICE_FILE_SRC}" "${SERVICE_FILE_DST}"

echo "[5/6] Reload + enable service"
sudo systemctl daemon-reload
sudo systemctl enable --now "${SERVICE_NAME}"

echo "[6/6] Done. Check status:"
echo "sudo systemctl status ${SERVICE_NAME} --no-pager"
