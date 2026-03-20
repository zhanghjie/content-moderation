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
BACKEND_DIR="/Users/zhanghaojie/IdeaProjects/content-moderation/content-moderation-server"
FRONTEND_DIR="/Users/zhanghaojie/IdeaProjects/content-moderation/content-moderation-web"
BACKEND_SCRIPT="$BACKEND_DIR/start.sh"
FRONTEND_SCRIPT="$FRONTEND_DIR/start.sh"

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
    
    # 启动后端
    print_info "正在启动后端服务..."
    if [ -f "$BACKEND_SCRIPT" ]; then
        bash "$BACKEND_SCRIPT" start
    else
        print_error "后端启动脚本不存在：$BACKEND_SCRIPT"
        exit 1
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
    
    echo ""
    print_title "启动完成"
    echo ""
    echo "后端服务："
    echo "  - API 地址：http://localhost:8080"
    echo "  - Swagger UI: http://localhost:8080/swagger-ui.html"
    echo ""
    echo "前端服务："
    echo "  - 访问地址：http://localhost:3000"
    echo ""
    echo "查看日志:"
    echo "  - 后端日志：bash $0 logs-backend"
    echo "  - 前端日志：bash $0 logs-frontend"
    echo ""
}

# 函数：停止所有服务
stop_all() {
    print_title "停止内容风控系统"
    
    # 停止前端
    print_info "正在停止前端服务..."
    if [ -f "$FRONTEND_SCRIPT" ]; then
        bash "$FRONTEND_SCRIPT" stop
    fi
    
    # 停止后端
    print_info "正在停止后端服务..."
    if [ -f "$BACKEND_SCRIPT" ]; then
        bash "$BACKEND_SCRIPT" stop
    fi
    
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
