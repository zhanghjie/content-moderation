#!/bin/bash

# Content Moderation Web 前端启动脚本
# 项目路径：/Users/zhanghaojie/IdeaProjects/content-moderation/content-moderation-web

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目配置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_NAME="content-moderation-web"
PROJECT_DIR="$SCRIPT_DIR"
PID_FILE="$PROJECT_DIR/.frontend.pid"
LOG_FILE="$PROJECT_DIR/logs/frontend.log"

# 函数：打印信息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 函数：检查 Node.js
check_node() {
    if ! command -v node &> /dev/null; then
        print_error "Node.js 未安装，请先安装 Node.js"
        exit 1
    fi
    
    NODE_VERSION=$(node -v)
    NPM_VERSION=$(npm -v)
    print_info "Node.js 版本：$NODE_VERSION"
    print_info "npm 版本：$NPM_VERSION"
}

# 函数：检查是否已运行
check_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            print_warning "前端服务已在运行中 (PID: $PID)"
            return 0
        else
            print_warning "发现残留的 PID 文件，清理中..."
            rm -f "$PID_FILE"
        fi
    fi
    return 1
}

# 函数：安装依赖
install() {
    print_info "安装依赖..."
    cd "$PROJECT_DIR"
    npm install
    print_info "依赖安装完成"
}

# 函数：启动开发服务器
start() {
    print_info "正在启动 $PROJECT_NAME 开发服务器..."
    
    cd "$PROJECT_DIR"
    
    # 检查依赖
    if [ ! -d "node_modules" ]; then
        print_warning "未找到 node_modules，正在安装依赖..."
        install
    fi
    
    # 创建日志目录
    mkdir -p "$(dirname "$LOG_FILE")"
    
    # 后台运行
    nohup npm run dev > "$LOG_FILE" 2>&1 &
    PID=$!
    
    # 保存 PID
    echo "$PID" > "$PID_FILE"
    
    # 等待服务启动
    sleep 3
    
    print_info "服务已启动 (PID: $PID)"
    print_info "日志文件：$LOG_FILE"
    print_info "访问地址：http://localhost:3000"
}

# 函数：停止服务
stop() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            print_info "正在停止服务 (PID: $PID)..."
            kill "$PID"
            sleep 2
            if ps -p "$PID" > /dev/null 2>&1; then
                print_warning "服务未响应，强制终止..."
                kill -9 "$PID"
            fi
            rm -f "$PID_FILE"
            print_info "服务已停止"
        else
            print_warning "服务未运行"
            rm -f "$PID_FILE"
        fi
    else
        # 尝试通过进程名查找
        PID=$(ps aux | grep "vite" | grep "$PROJECT_NAME" | grep -v grep | awk '{print $2}')
        if [ -n "$PID" ]; then
            print_info "正在停止服务 (PID: $PID)..."
            kill "$PID"
            sleep 2
            if ps -p "$PID" > /dev/null 2>&1; then
                kill -9 "$PID"
            fi
            print_info "服务已停止"
        else
            print_warning "未找到运行的服务"
        fi
    fi
}

# 函数：重启服务
restart() {
    print_info "正在重启服务..."
    stop
    sleep 2
    start
}

# 函数：查看状态
status() {
    if check_running; then
        PID=$(cat "$PID_FILE")
        print_info "服务运行中 (PID: $PID)"
        echo ""
        echo "日志最后 10 行:"
        echo "-------------------"
        tail -n 10 "$LOG_FILE" 2>/dev/null || echo "暂无日志"
        echo "-------------------"
    else
        print_info "服务未运行"
    fi
}

# 函数：查看日志
logs() {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        print_warning "日志文件不存在"
    fi
}

# 函数：构建生产版本
build() {
    print_info "构建生产版本..."
    cd "$PROJECT_DIR"
    npm run build
    print_info "构建完成，输出目录：dist/"
}

# 函数：预览生产版本
preview() {
    print_info "预览生产版本..."
    cd "$PROJECT_DIR"
    npm run preview
}

# 函数：清理
clean() {
    print_info "清理构建文件..."
    cd "$PROJECT_DIR"
    rm -rf "$PROJECT_DIR/node_modules"
    rm -rf "$PROJECT_DIR/dist"
    rm -f "$PROJECT_DIR/package-lock.json"
    print_info "清理完成"
}

# 主函数
case "${1:-start}" in
    start)
        if check_running; then
            exit 0
        fi
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    install)
        install
        ;;
    build)
        build
        ;;
    preview)
        preview
        ;;
    clean)
        clean
        ;;
    *)
        echo "用法：$0 {start|stop|restart|status|logs|install|build|preview|clean}"
        echo ""
        echo "命令说明:"
        echo "  start   - 启动开发服务器"
        echo "  stop    - 停止服务"
        echo "  restart - 重启服务"
        echo "  status  - 查看状态"
        echo "  logs    - 查看日志（实时）"
        echo "  install - 安装依赖"
        echo "  build   - 构建生产版本"
        echo "  preview - 预览生产版本"
        echo "  clean   - 清理项目"
        exit 1
        ;;
esac
