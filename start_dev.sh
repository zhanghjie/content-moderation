#!/bin/bash

# Content Moderation System 统一启动脚本
# 用于同时启动前后端服务

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目配置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/content-moderation-server"
FRONTEND_DIR="$SCRIPT_DIR/content-moderation-web"
BACKEND_SCRIPT="$BACKEND_DIR/start.sh"
FRONTEND_SCRIPT="$FRONTEND_DIR/start.sh"

# 函数：是否存在命令
has_cmd() {
    command -v "$1" > /dev/null 2>&1
}

# 函数：检查 PID 是否存活
is_pid_alive() {
    local pid="$1"
    if [ -z "$pid" ]; then
        return 1
    fi
    ps -p "$pid" > /dev/null 2>&1
}

# 函数：收集子进程（递归）
collect_descendants() {
    local pid="$1"
    if ! has_cmd pgrep; then
        return 0
    fi
    local children
    children=$(pgrep -P "$pid" 2>/dev/null || true)
    for c in $children; do
        collect_descendants "$c"
        echo "$c"
    done
}

# 函数：优雅终止进程树
kill_tree() {
    local pid="$1"
    local label="$2"
    local timeout_sec="${3:-8}"

    if ! is_pid_alive "$pid"; then
        return 0
    fi

    print_warning "准备终止进程树：$label (PID: $pid)"

    local descendants
    descendants=$(collect_descendants "$pid" || true)

    for d in $descendants; do
        if is_pid_alive "$d"; then
            kill -TERM "$d" 2>/dev/null || true
        fi
    done
    kill -TERM "$pid" 2>/dev/null || true

    local i=0
    while [ $i -lt "$timeout_sec" ]; do
        local alive=0
        if is_pid_alive "$pid"; then
            alive=1
        fi
        for d in $descendants; do
            if is_pid_alive "$d"; then
                alive=1
                break
            fi
        done
        if [ "$alive" -eq 0 ]; then
            print_info "进程树已退出：$label"
            return 0
        fi
        sleep 1
        i=$((i + 1))
    done

    print_warning "超时未退出，强制终止：$label"
    for d in $descendants; do
        if is_pid_alive "$d"; then
            kill -KILL "$d" 2>/dev/null || true
        fi
    done
    if is_pid_alive "$pid"; then
        kill -KILL "$pid" 2>/dev/null || true
    fi
}

# 函数：按端口杀掉监听进程（兜底）
kill_by_port() {
    local port="$1"
    local label="$2"
    if ! has_cmd lsof; then
        print_warning "lsof 不存在，跳过端口兜底清理：$label :$port"
        return 0
    fi
    local pids
    pids=$(lsof -ti "tcp:$port" 2>/dev/null || true)
    if [ -z "$pids" ]; then
        return 0
    fi
    for pid in $pids; do
        kill_tree "$pid" "$label(:$port)" 8
    done
}

# 函数：等待端口释放
wait_port_free() {
    local port="$1"
    local label="$2"
    local timeout_sec="${3:-10}"
    if ! has_cmd lsof; then
        return 0
    fi
    local i=0
    while [ $i -lt "$timeout_sec" ]; do
        local pids
        pids=$(lsof -ti "tcp:$port" 2>/dev/null || true)
        if [ -z "$pids" ]; then
            return 0
        fi
        sleep 1
        i=$((i + 1))
    done
    print_warning "端口仍被占用：$label :$port"
}

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

print_title() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 函数：启动所有服务
start_all() {
    print_title "启动内容风控系统"

    # 启动前强制清理旧进程，避免 PID 文件残留或端口占用导致重复启动失败
    print_info "启动前清理旧进程..."
    stop_all
    sleep 1
    
    # 启动后端（使用 dev 环境）
    print_info "正在启动后端服务..."
    if [ -f "$BACKEND_SCRIPT" ]; then
        bash "$BACKEND_SCRIPT" start dev
    else
        print_error "后端启动脚本不存在：$BACKEND_SCRIPT"
        exit 1
    fi
    
    # 后端已经在 start.sh 中等待启动完成，这里只需检查
    if curl -s http://localhost:9891/actuator/health > /dev/null 2>&1; then
        print_info "后端服务已就绪"
    else
        print_warning "后端服务可能还在启动中，请查看日志"
    fi
    
    echo ""
    
    # 启动前端
    print_info "正在启动前端服务..."
    if [ -f "$FRONTEND_SCRIPT" ]; then
        bash "$FRONTEND_SCRIPT" start
    else
        print_error "前端启动脚本不存在：$FRONTEND_SCRIPT"
        exit 1
    fi
    
    # 等待前端启动完成（最多等待 15 秒）
    print_info "等待前端服务就绪..."
    wait_count=0
    while [ $wait_count -lt 15 ]; do
        if curl -s http://localhost:3000 > /dev/null 2>&1; then
            print_info "前端服务已就绪"
            break
        fi
        sleep 1
        wait_count=$((wait_count + 1))
    done
    if [ $wait_count -eq 15 ]; then
        print_warning "前端服务启动超时"
    fi
    
    echo ""
    print_title "启动完成"
    echo ""
    echo "后端服务："
    echo "  - API 地址：http://localhost:9891"
    echo "  - Swagger UI: http://localhost:9891/swagger-ui.html"
    echo ""
    echo "前端服务："
    echo "  - 访问地址：http://localhost:3000"
    echo ""
    echo "查看日志:"
    echo "  - 后端日志：bash $0 logs-backend"
    echo "  - 前端日志：bash $0 logs-frontend"
    echo ""
    echo "停止服务：bash $0 stop"

    tail -f "$BACKEND_DIR/logs/server.log"
}

# 函数：停止所有服务
stop_all() {
    print_title "停止内容风控系统"
    
    # 停止前端
    print_info "正在停止前端服务..."
    if [ -f "$FRONTEND_SCRIPT" ]; then
        bash "$FRONTEND_SCRIPT" stop || true
    fi
    if [ -f "$FRONTEND_DIR/.frontend.pid" ]; then
        FRONT_PID=$(cat "$FRONTEND_DIR/.frontend.pid" 2>/dev/null || true)
        if [ -n "$FRONT_PID" ]; then
            kill_tree "$FRONT_PID" "frontend(pidfile)" 8 || true
        fi
        rm -f "$FRONTEND_DIR/.frontend.pid" 2>/dev/null || true
    fi
    kill_by_port 3000 "frontend" || true
    wait_port_free 3000 "frontend" 10 || true
    
    # 停止后端
    print_info "正在停止后端服务..."
    if [ -f "$BACKEND_SCRIPT" ]; then
        bash "$BACKEND_SCRIPT" stop || true
    fi
    if [ -f "$BACKEND_DIR/.server.pid" ]; then
        BACK_PID=$(cat "$BACKEND_DIR/.server.pid" 2>/dev/null || true)
        if [ -n "$BACK_PID" ]; then
            kill_tree "$BACK_PID" "backend(pidfile)" 8 || true
        fi
        rm -f "$BACKEND_DIR/.server.pid" 2>/dev/null || true
    fi
    kill_by_port 9891 "backend" || true
    wait_port_free 9891 "backend" 10 || true
    
    print_info "所有服务已停止"
}

# 函数：重启所有服务
restart_all() {
    print_title "重启内容风控系统"
    stop_all
    sleep 2
    start_all
}

# 函数：查看状态
status_all() {
    print_title "内容风控系统状态"
    
    echo "后端服务状态:"
    echo "-------------------"
    if [ -f "$BACKEND_SCRIPT" ]; then
        bash "$BACKEND_SCRIPT" status
    else
        print_error "后端启动脚本不存在"
    fi
    
    echo ""
    echo "前端服务状态:"
    echo "-------------------"
    if [ -f "$FRONTEND_SCRIPT" ]; then
        bash "$FRONTEND_SCRIPT" status
    else
        print_error "前端启动脚本不存在"
    fi
}

# 函数：查看后端日志
logs_backend() {
    if [ -f "$BACKEND_SCRIPT" ]; then
        bash "$BACKEND_SCRIPT" logs
    else
        print_error "后端启动脚本不存在"
    fi
}

# 函数：查看前端日志
logs_frontend() {
    if [ -f "$FRONTEND_SCRIPT" ]; then
        bash "$FRONTEND_SCRIPT" logs
    else
        print_error "前端启动脚本不存在"
    fi
}

# 函数：构建所有
build_all() {
    print_title "构建内容风控系统"
    
    # 构建后端
    print_info "正在构建后端..."
    if [ -f "$BACKEND_SCRIPT" ]; then
        bash "$BACKEND_SCRIPT" build
    fi
    
    # 构建前端
    print_info "正在构建前端..."
    if [ -f "$FRONTEND_SCRIPT" ]; then
        bash "$FRONTEND_SCRIPT" build
    fi
    
    print_info "构建完成"
}

# 主函数
case "${1:-start}" in
    start)
        start_all
        ;;
    stop)
        stop_all
        ;;
    restart)
        restart_all
        ;;
    status)
        status_all
        ;;
    logs-backend)
        logs_backend
        ;;
    logs-frontend)
        logs_frontend
        ;;
    logs)
        echo "用法：$0 logs-backend | logs-frontend"
        exit 1
        ;;
    build)
        build_all
        ;;
    *)
        echo "用法：$0 {start|stop|restart|status|logs-backend|logs-frontend|build}"
        echo ""
        echo "命令说明:"
        echo "  start          - 启动所有服务（前后端）"
        echo "  stop           - 停止所有服务"
        echo "  restart        - 重启所有服务"
        echo "  status         - 查看所有服务状态"
        echo "  logs-backend   - 查看后端日志（实时）"
        echo "  logs-frontend  - 查看前端日志（实时）"
        echo "  build          - 构建所有项目"
        echo ""
        echo "示例:"
        echo "  $0 start       # 启动所有服务"
        echo "  $0 status      # 查看状态"
        echo "  $0 logs-backend # 查看后端日志"
        echo ""
        exit 1
        ;;
esac
