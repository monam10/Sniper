#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# setup.sh — إعداد سيرفر SnapLoad محلياً للاختبار
# ─────────────────────────────────────────────────────────────────────────────
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}══════════════════════════════════════════${NC}"
echo -e "${GREEN}   SnapLoad API Server — Local Setup       ${NC}"
echo -e "${GREEN}══════════════════════════════════════════${NC}"
echo ""

# 1. التحقق من Python
echo -e "${YELLOW}[1/5] التحقق من Python...${NC}"
if ! command -v python3 &> /dev/null; then
    echo -e "${RED}❌ Python 3 غير مثبت. يرجى تثبيته من https://python.org${NC}"
    exit 1
fi
PYTHON_VERSION=$(python3 --version)
echo -e "${GREEN}✅ $PYTHON_VERSION${NC}"

# 2. إنشاء virtual environment
echo -e "${YELLOW}[2/5] إنشاء virtual environment...${NC}"
if [ ! -d "venv" ]; then
    python3 -m venv venv
    echo -e "${GREEN}✅ تم إنشاء venv${NC}"
else
    echo -e "${GREEN}✅ venv موجود مسبقاً${NC}"
fi

# 3. تفعيل venv وتثبيت المتطلبات
echo -e "${YELLOW}[3/5] تثبيت المتطلبات...${NC}"
source venv/bin/activate
pip install --upgrade pip --quiet
pip install -r requirements.txt --quiet
echo -e "${GREEN}✅ تم تثبيت المتطلبات${NC}"

# 4. تحديث yt-dlp
echo -e "${YELLOW}[4/5] تحديث yt-dlp...${NC}"
yt-dlp -U --quiet 2>/dev/null || true
YT_DLP_VERSION=$(yt-dlp --version 2>/dev/null || echo "غير متاح")
echo -e "${GREEN}✅ yt-dlp v$YT_DLP_VERSION${NC}"

# 5. تشغيل السيرفر
echo -e "${YELLOW}[5/5] تشغيل السيرفر على port 5000...${NC}"
echo ""
echo -e "${GREEN}══════════════════════════════════════════${NC}"
echo -e "${GREEN}   Server running at: http://localhost:5000 ${NC}"
echo -e "${GREEN}══════════════════════════════════════════${NC}"
echo ""
echo -e "Endpoints للاختبار:"
echo -e "  GET  http://localhost:5000/ping"
echo -e "  GET  http://localhost:5000/"
echo -e "  POST http://localhost:5000/info"
echo -e "  POST http://localhost:5000/download-url"
echo -e "  POST http://localhost:5000/formats"
echo -e "  POST http://localhost:5000/update"
echo ""
echo -e "مثال اختبار سريع:"
echo -e '  curl -X POST http://localhost:5000/info -H "Content-Type: application/json" -d '"'"'{"url":"https://youtu.be/dQw4w9WgXcQ"}'"'"
echo ""
echo -e "${YELLOW}اضغط Ctrl+C لإيقاف السيرفر${NC}"
echo ""

export PORT=5000
export FLASK_ENV=development

# تشغيل مباشر بـ Flask للتطوير
python3 main.py
