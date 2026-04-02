#!/bin/bash

# Content Moderation Server 后端启动脚本
# 项目路径：脚本所在目录，支持相对调用

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="content-moderation-server"
PROJECT_DIR="$SCRIPT_DIR"
PID_FILE="$PROJECT_DIR/.server.pid"
LOG_FILE="$PROJECT_DIR/logs/server.log"

# JDK 配置
detect_java_home() {
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        echo "$JAVA_HOME"
        return 0
    fi

    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
        local mac_java_home
        mac_java_home=$(/usr/libexec/java_home 2>/dev/null || true)
        if [ -n "$mac_java_home" ] && [ -x "$mac_java_home/bin/java" ]; then
            echo "$mac_java_home"
            return 0
        fi
    fi

    for candidate in \
        /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
        /usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/*.jdk/Contents/Home; do
        if [ -d "$candidate" ] && [ -x "$candidate/bin/java" ]; then
            echo "$candidate"
            return 0
        fi
    done

    if command -v java >/dev/null 2>&1; then
        local java_bin java_dir
        java_bin="$(command -v java)"
        java_dir="$(cd "$(dirname "$java_bin")/.." && pwd)"
        if [ -x "$java_dir/bin/java" ]; then
            echo "$java_dir"
            return 0
        fi
    fi

    return 1
}

if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
    JAVA_HOME="$(detect_java_home || true)"
    export JAVA_HOME
fi
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
else
    print_warning "未能自动检测到可用的 JAVA_HOME，后端可能无法启动"
fi

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

# 函数：逐个终止 PID
terminate_pids() {
    local pid_list="$1"
    local signal="${2:-TERM}"

    for pid in $pid_list; do
        if [ -n "$pid" ] && ps -p "$pid" > /dev/null 2>&1; then
            kill -"$signal" "$pid" 2>/dev/null || true
        fi
    done
}

# 函数：检查是否已运行
check_running() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            print_warning "服务已在运行中 (PID: $PID)"
            return 0
        else
            print_warning "发现残留的 PID 文件，清理中..."
            rm -f "$PID_FILE"
        fi
    fi
    return 1
}

# 函数：启动服务
start() {
    local profile="${1:-prod}"
    print_info "正在启动 $PROJECT_NAME..."
    
    cd "$PROJECT_DIR"
    
    # 创建日志目录
    mkdir -p "$(dirname "$LOG_FILE")"

    if [ -f "$PROJECT_DIR/.env.local" ]; then
        set -a
        source "$PROJECT_DIR/.env.local"
        set +a
    fi
    
    # 后台运行 (使用 nohup 确保进程独立运行)
    cd "$PROJECT_DIR"
    nohup mvn spring-boot:run -Dspring-boot.run.profiles="$profile" > "$LOG_FILE" 2>&1 &
    PID=$!
    
    # 保存 PID
    echo "$PID" > "$PID_FILE"
    
    # 等待服务启动 (最多等待 120 秒)
    print_info "等待服务启动 (最多 120 秒)..."
    local wait_count=0
    while [ $wait_count -lt 120 ]; do
        # 检查健康端点
        if curl -s http://localhost:9891/actuator/health 2>/dev/null | grep -q "UP"; then
            print_info "服务启动成功"
            break
        fi
        # 检查进程是否还在运行
        if ! ps -p "$PID" > /dev/null 2>&1; then
            print_error "服务进程已退出，请查看日志：$LOG_FILE"
            tail -30 "$LOG_FILE" 2>/dev/null
            break
        fi
        sleep 2
        wait_count=$((wait_count + 1))
        # 每 20 秒打印一次进度
        if [ $((wait_count % 10)) -eq 0 ]; then
            echo -n "."
        fi
    done
    echo ""
    
    # 最终检查
    if curl -s http://localhost:9891/actuator/health 2>/dev/null | grep -q "UP"; then
        print_info "✓ 服务已就绪"
    else
        print_warning "服务可能还在启动中，请查看日志"
        print_info "使用以下命令查看日志：tail -f $LOG_FILE"
    fi
    
    print_info "服务已启动 (PID: $PID)"
    print_info "日志文件：$LOG_FILE"
    print_info "访问地址：http://localhost:9891"
    print_info "Swagger UI: http://localhost:9891/swagger-ui.html"
}

# 函数：停止服务
stop() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if ps -p "$PID" > /dev/null 2>&1; then
            print_info "正在停止服务 (PID: $PID)..."
            terminate_pids "$PID" TERM
            sleep 2
            if ps -p "$PID" > /dev/null 2>&1; then
                print_warning "服务未响应，强制终止..."
                terminate_pids "$PID" KILL
            fi
            rm -f "$PID_FILE"
            print_info "服务已停止"
        else
            print_warning "服务未运行"
            rm -f "$PID_FILE"
        fi
    else
        # 尝试通过进程名查找
        PID_LIST=$(ps aux | grep "$PROJECT_NAME" | grep -v grep | awk '{print $2}')
        if [ -n "$PID_LIST" ]; then
            print_info "正在停止服务 (PID: $(echo "$PID_LIST" | tr '\n' ' '))..."
            terminate_pids "$PID_LIST" TERM
            sleep 2
            if [ -n "$(for pid in $PID_LIST; do if ps -p "$pid" > /dev/null 2>&1; then echo "$pid"; fi; done)" ]; then
                terminate_pids "$PID_LIST" KILL
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
    local profile="${1:-prod}"
    stop
    sleep 2
    start "$profile"
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

# 函数：清理
clean() {
    print_info "清理构建文件..."
    cd "$PROJECT_DIR"
    mvn clean
    rm -rf "$PROJECT_DIR/target"
    print_info "清理完成"
}

# 函数：编译
build() {
    print_info "编译项目..."
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests
    print_info "编译完成"
}

# 主函数
case "${1:-start}" in
    start)
        if check_running; then
            exit 0
        fi
        start "${2:-prod}"
        ;;
    stop)
        stop
        ;;
    restart)
        restart "${2:-prod}"
        ;;
    status)
        status
        ;;
    logs)
        logs
        ;;
    clean)
        clean
        ;;
    build)
        build
        ;;
    *)
        echo "用法：$0 {start|stop|restart|status|logs|clean|build}"
        echo ""
        echo "命令说明:"
        echo "  start   - 启动服务"
        echo "  stop    - 停止服务"
        echo "  restart - 重启服务"
        echo "  status  - 查看状态"
        echo "  logs    - 查看日志（实时）"
        echo "  clean   - 清理构建文件"
        echo "  build   - 编译项目"
        exit 1
        ;;
esac
