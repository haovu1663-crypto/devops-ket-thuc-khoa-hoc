#!/bin/bash
# ==============================================================================
# SCRIPT TỰ ĐỘNG THIẾT LẬP HẠ TẦNG VPS (UBUNTU 22.04 LTS / DEBIAN)
# ==============================================================================

set -e

echo "🚀 [1/5] Cập nhật hệ điều hành..."
sudo apt-get update && sudo apt-get upgrade -y
sudo apt-get install -y curl wget git ufw apt-transport-https ca-certificates gnupg lsb-release

echo "🛡️ [2/5] Thiết lập UFW Firewall bảo mật..."
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp comment 'SSH Port'
sudo ufw allow 80/tcp comment 'HTTP Web'
sudo ufw allow 443/tcp comment 'HTTPS Web'
sudo ufw --force enable
sudo ufw status verbose

echo "🐳 [3/5] Cài đặt Docker Engine & Docker Compose Plugin..."
if ! command -v docker &> /dev/null; then
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
      sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    
    sudo usermod -aG docker $USER
    echo "✅ Docker đã được cài đặt thành công!"
else
    echo "ℹ️ Docker đã được cài đặt từ trước."
fi

echo "🔒 [4/5] Cài đặt Certbot..."
sudo apt-get install -y certbot

echo "📂 [5/5] Chuẩn bị thư mục triển khai dự án tại /opt/ecommerce..."
sudo mkdir -p /opt/ecommerce
sudo chown -R $USER:$USER /opt/ecommerce

echo "=============================================================================="
echo "🎉 HOÀN TẤT THIẾT LẬP VPS!"
echo "👉 Thư mục triển khai: /opt/ecommerce"
echo "=============================================================================="
