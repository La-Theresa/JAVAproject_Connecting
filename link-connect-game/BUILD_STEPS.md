# Link Connect Game 构建步骤日志

## Step 1 - 创建独立工程骨架
- 新建目录：`link-connect-game/src` 与 `link-connect-game/data/saves`
- 分层结构：`app`、`model`、`logic`、`data`、`ui`
- 目标：从新文件开始实现，不修改demo原文件

## Step 2 - 实现基础模型层
- 新建 `Constants`：难度、分数、UI、文件路径常量
- 新建 `Position`、`Tile`、`Path`、`OperationLog`
- 新建 `GameBoard`：使用 `Set<Position>` 和 `Map<Integer, Set<Position>>` 缓存

## Step 3 - 实现核心算法层
- 新建 `PathFinder`：基于 `Deque` 的 BFS（0/1/2转角）
- 新建 `DeadEndDetector`：查找可消对
- 新建 `ComboManager`：连击递增得分
- 新建 `BoardGenerator`：随机棋盘 + 可解性验证 + 洗牌

## Step 4 - 实现会话与存档模型
- 新建 `GameSnapshot`：可序列化快照
- 新建 `GameSession`：消除、计时、胜负判定、提示、路径记录

## Step 5 - 实现用户/存档/排行
- 新建 `User` 与 `UserManager`：注册、登录、SHA-256、持久化
- 新建 `SaveManager`：按用户隔离存档，损坏文件识别
- 新建 `RecordManager`：Top N 排行

## Step 6 - 实现GUI与交互
- 新建 `GameFrame`（CardLayout）
- 新建 `MainMenuPanel`、`LoginPanel`、`RegisterPanel`、`LeaderboardPanel`
- 新建 `GamePanel`：棋盘绘制、选中高亮、路径显示、提示高亮
- 新建 `Main`：程序入口

## Step 7 - 编译与修复（已完成）
- 尝试执行 `javac` 全量编译，当前环境未安装JDK命令行（`javac`不可用）
- 使用编辑器问题检测工具检查 `src` 目录，未发现语法错误
- 修复用户数据加载统计偏差：新增 `User.restoreStats()` 并修正 `UserManager.load()`

## Step 8 - 交付与运行说明
- 项目源码位于 `link-connect-game/src`
- 入口类：`app.Main`
- 若本机安装JDK后可执行：`javac -encoding UTF-8 -d out (all java files)`，然后 `java -cp out app.Main`
