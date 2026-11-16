#!/bin/bash

# ============================================================
# Gradle 构建脚本（使用 D: 盘缓存）
# ============================================================
# 用途：设置正确的 Gradle 缓存路径并执行构建任务
# 使用方法：
#   ./build_with_d_cache.sh clean          # 清理项目
#   ./build_with_d_cache.sh assembleDebug  # 构建 Debug 版本
#   ./build_with_d_cache.sh assembleRelease # 构建 Release 版本
#   ./build_with_d_cache.sh --stop         # 停止 Gradle 守护进程
# ============================================================

# 设置 Gradle 缓存路径到 D: 盘
export GRADLE_USER_HOME=/d/gradle_cache

echo "=========================================="
echo "Gradle 构建脚本"
echo "=========================================="
echo "缓存路径: $GRADLE_USER_HOME"
echo "任务参数: $@"
echo "=========================================="

# 如果没有参数，显示帮助信息
if [ $# -eq 0 ]; then
    echo ""
    echo "用法示例："
    echo "  $0 clean                 # 清理项目"
    echo "  $0 assembleDebug         # 构建 Debug 版本"
    echo "  $0 assembleRelease       # 构建 Release 版本"
    echo "  $0 clean assembleDebug   # 清理并构建"
    echo "  $0 --stop                # 停止 Gradle 守护进程"
    echo ""
    exit 0
fi

# 执行 Gradle 任务
./gradlew "$@"

# 保存退出状态
EXIT_CODE=$?

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo "✅ 构建成功！"
else
    echo "❌ 构建失败（退出码: $EXIT_CODE）"
    echo ""
    echo "常见问题排查："
    echo "1. 文件锁定错误 → 运行: $0 --stop 后重试"
    echo "2. 内存不足错误 → 在 gradle.properties 中降低内存"
    echo "3. 依赖下载失败 → 检查网络连接"
fi
echo "=========================================="

exit $EXIT_CODE
