@echo off
chcp 65001
echo ==========================================
echo 启动AI评分Python服务
echo ==========================================

REM 检查Python是否安装
python --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Python，请先安装Python 3.8+
    pause
    exit /b 1
)

echo [1/4] 检查Python版本...
python --version

echo [2/4] 检查并安装依赖...
REM 检查关键依赖
python -c "import flask" >nul 2>&1
if errorlevel 1 (
    echo 正在安装Flask...
    pip install flask flask-cors
)

python -c "import sentence_transformers" >nul 2>&1
if errorlevel 1 (
    echo 正在安装sentence-transformers...
    pip install sentence-transformers
)

python -c "import jieba" >nul 2>&1
if errorlevel 1 (
    echo 正在安装jieba...
    pip install jieba
)

python -c "import sklearn" >nul 2>&1
if errorlevel 1 (
    echo 正在安装scikit-learn...
    pip install scikit-learn
)

python -c "import scipy" >nul 2>&1
if errorlevel 1 (
    echo 正在安装scipy...
    pip install scipy
)

python -c "import pandas" >nul 2>&1
if errorlevel 1 (
    echo 正在安装pandas...
    pip install pandas
)

python -c "import numpy" >nul 2>&1
if errorlevel 1 (
    echo 正在安装numpy...
    pip install numpy
)

echo [3/4] 依赖检查完成
echo.
echo [4/4] 启动AI评分服务...
echo 首次启动需要下载模型，请耐心等待（约需2-5分钟）
echo.

python ai_grading_service.py

pause
