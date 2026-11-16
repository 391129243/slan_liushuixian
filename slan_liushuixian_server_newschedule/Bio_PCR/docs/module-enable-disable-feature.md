# 模组启用/禁用管理功能实现方案

## 功能概述

在模组管理页面（ModuleManagerFragment）的列表中添加一列"启用状态"开关，允许用户禁用特定模组。禁用的模组在系统中将被完全忽略，包括：
- UI层：自检页面、主界面的相关控件不可点击（添加半透明遮罩）
- 业务层：一键复位、一键还盘、机械臂调度、流程调度都不考虑该模组
- 通信层：警报不考虑该模组的上报信息

## 需求分析

### 1. 数据持久化
- **存储方式**：使用 SharedPreferences 存储模组启用状态
- **存储键值**：`module_enabled_{deviceId}` → boolean
- **生效时机**：设置后需要重启机器才能生效

### 2. 数据模型层
在 `WSSConnectionItem` 添加属性：
```java
private boolean isEnabled;  // 模组是否启用（从 SharedPreferences 加载）
```

### 3. UI层修改

#### 3.1 ModuleManagerFragment
- 在列表中添加"启用"列（使用 Switch 或 CheckBox）
- 点击开关时更新 SharedPreferences
- 提示用户需要重启才能生效

#### 3.2 自检页面（SelfcheckFragment）
- 检查各模组的启用状态
- 对禁用的模组UI添加半透明遮罩 + 不可点击
- 涉及控件：PCR模组、RNA模组、托盘、机械臂等

#### 3.3 主界面
- 检查各模组的启用状态
- 对禁用的模组UI添加半透明遮罩 + 不可点击

### 4. 业务逻辑层修改

#### 4.1 一键复位功能
- 跳过禁用的模组

#### 4.2 一键还盘功能
- 不考虑禁用模组的盖子、托盘等
- 跳过禁用的PCR/RNA模组

#### 4.3 机械臂调度（ArmManager）
- 调度任务分配时过滤禁用的模组
- 不向禁用的模组下发任务

#### 4.4 流程调度（APLManager、SchedulingEngine）
- 调度引擎在分配任务时过滤禁用的模组
- checkXxxWork 方法中跳过禁用的模组

#### 4.5 警报系统
- 不处理来自禁用模组的警报消息
- 不在警报列表中显示禁用模组的警报

## 实现步骤

### 阶段1：数据模型和持久化工具类（30分钟）
1. 创建 `ModuleEnableManager` 工具类
   - 封装 SharedPreferences 读写
   - 提供启用/禁用接口
   - 提供批量查询接口
2. 在 `WSSConnectionItem` 添加 `isEnabled` 属性和方法
3. 在应用启动时从 SharedPreferences 加载启用状态

### 阶段2：UI层实现（1小时）
1. 修改 `fragment_module_manager.xml`
   - 添加"启用"列标题
2. 修改 `item_list_module_manager.xml`
   - 添加 Switch 控件
3. 修改 `ModuleSettingsAdapter`
   - 绑定 Switch 状态
   - 处理 Switch 点击事件
4. 修改 `ModuleManagerFragment`
   - 实现开关点击逻辑
   - 显示"需要重启"提示

### 阶段3：UI遮罩实现（45分钟）
1. 创建通用遮罩布局 `layout_disabled_overlay.xml`
2. 修改自检页面
   - 为各模组控件添加遮罩容器
   - 根据启用状态显示/隐藏遮罩
3. 修改主界面
   - 为各模组控件添加遮罩容器
   - 根据启用状态显示/隐藏遮罩

### 阶段4：业务逻辑层修改（1.5小时）
1. 修改 `APLManager`
   - `resetOneKey` 方法：过滤禁用模组
   - `clearData` 方法：过滤禁用模组
2. 修改 `SelfcheckFragment`
   - `autoReset` 方法：过滤禁用模组
   - `autoReturnTraySession` 方法：过滤禁用模组
3. 修改 `ArmManager`
   - 调度任务分配时检查模组启用状态
4. 修改 `PCRManager` / `RNAManager`
   - `checkXxxWork` 方法：跳过禁用模组
5. 修改 `SchedulingEngine` (如果使用)
   - 状态机方法中过滤禁用模组

### 阶段5：警报系统修改（30分钟）
1. 修改警报处理逻辑
   - 过滤来自禁用模组的警报
2. 修改警报显示逻辑
   - 不显示禁用模组的警报

### 阶段6：测试和验证（1小时）
1. 单元测试
   - ModuleEnableManager 读写测试
   - WSSConnectionItem 状态加载测试
2. 集成测试
   - 禁用模组后UI遮罩显示
   - 禁用模组后调度逻辑过滤
   - 禁用模组后警报过滤
3. 端到端测试
   - 完整流程测试

## 关键技术点

### 1. SharedPreferences 键值设计
```java
// 键格式
String KEY_PREFIX = "module_enabled_";
String key = KEY_PREFIX + deviceId;  // 例如：module_enabled_SLANPCR1111

// 默认值
boolean defaultEnabled = true;  // 默认启用所有模组
```

### 2. 遮罩实现
```xml
<!-- 半透明遮罩布局 -->
<FrameLayout>
    <!-- 原有控件 -->
    <Button ... />
    
    <!-- 禁用遮罩（初始隐藏）-->
    <View
        android:id="@+id/disabled_overlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#80000000"
        android:clickable="true"
        android:visibility="gone"/>
</FrameLayout>
```

### 3. 业务逻辑过滤模式
```java
// 在调度/管理方法中添加启用状态检查
if (!ModuleEnableManager.getInstance().isModuleEnabled(deviceId)) {
    Log.d(TAG, "模组已禁用，跳过: " + deviceId);
    return;  // 或 continue
}
```

## 文件清单

### 新增文件
1. `app/src/main/java/com/gidi/bio_pcr/util/ModuleEnableManager.java` - 模组启用管理工具类
2. `app/src/main/res/layout/layout_disabled_overlay.xml` - 通用禁用遮罩布局

### 修改文件

#### UI层（6个）
1. `app/src/main/res/layout/fragment_module_manager.xml` - 添加"启用"列标题
2. `app/src/main/res/layout/item_list_module_manager.xml` - 添加 Switch 控件
3. `app/src/main/java/com/gidi/bio_pcr/adapter/ModuleSettingsAdapter.java` - 绑定Switch
4. `app/src/main/java/com/gidi/bio_pcr/fragment/setting/ModuleManagerFragment.java` - 处理开关点击
5. `app/src/main/res/layout/fragment_selfcheck.xml` - 添加遮罩容器
6. `app/src/main/java/com/paul/pipeline/fragment/SelfcheckFragment.java` - 显示/隐藏遮罩

#### 数据模型层（1个）
7. `app/src/main/java/com/winters/boweilibrary/autopipeline/model/item/WSSConnectionItem.java` - 添加isEnabled属性

#### 业务逻辑层（5个）
8. `app/src/main/java/com/paul/pipeline/manager/APLManager.java` - 过滤禁用模组
9. `app/src/main/java/com/paul/pipeline/manager/ArmManager.java` - 调度时过滤
10. `app/src/main/java/com/paul/pipeline/manager/PCRManager.java` - checkWork过滤
11. `app/src/main/java/com/paul/pipeline/manager/RNAManager.java` - checkWork过滤
12. `app/src/main/java/com/paul/pipeline/manager/TrayManager.java` - 可能需要过滤

#### 警报系统（待确认）
13. 警报处理类（需要进一步确认具体文件）

## 注意事项

1. **重启生效**：设置启用/禁用后，需要重启应用才能生效，因为模组连接在启动时建立
2. **默认值**：新模组默认为启用状态
3. **UI反馈**：设置后立即在UI上显示"需要重启"的提示
4. **数据一致性**：SharedPreferences 和 WSSConnectionItem 中的状态必须同步
5. **兼容性**：确保旧版本数据升级时所有模组默认启用
6. **安全性**：至少保留一个PCR模组启用，避免完全无法使用

## 测试用例

### 单元测试
- [ ] ModuleEnableManager 保存和读取
- [ ] WSSConnectionItem 启用状态加载
- [ ] 默认值处理（新模组）

### 集成测试
- [ ] 禁用模组后UI遮罩显示正确
- [ ] 禁用模组后一键复位跳过该模组
- [ ] 禁用模组后一键还盘跳过该模组
- [ ] 禁用模组后机械臂调度跳过该模组
- [ ] 禁用模组后流程调度跳过该模组
- [ ] 禁用模组后警报过滤正确

### 端到端测试
- [ ] 完整流程：禁用→重启→验证UI→验证调度→启用→重启→验证恢复

## 开发时间估算

| 阶段 | 时间 |
|------|------|
| 阶段1：数据模型和持久化 | 30分钟 |
| 阶段2：UI层实现 | 1小时 |
| 阶段3：UI遮罩实现 | 45分钟 |
| 阶段4：业务逻辑层修改 | 1.5小时 |
| 阶段5：警报系统修改 | 30分钟 |
| 阶段6：测试和验证 | 1小时 |
| **总计** | **约5.5小时** |

## 风险评估

### 高风险
- 业务逻辑层修改点多，可能遗漏某些调度路径
- 重启生效机制可能导致用户困惑

### 中风险
- UI遮罩可能影响现有布局
- SharedPreferences 数据丢失风险

### 低风险
- 数据模型层修改简单
- UI层修改相对独立

## 后续优化建议

1. **动态生效**：研究是否可以不重启即可生效（需要重新建立WebSocket连接）
2. **批量操作**：支持批量启用/禁用多个模组
3. **权限管理**：限制普通用户禁用模组的权限
4. **日志记录**：记录模组启用/禁用的操作日志
5. **状态同步**：考虑将启用状态同步到服务器
