# Phase 1 代码结构与模块实现说明（中文）

本文基于 `README.md` 与源码结构，按模块说明 Phase 1 的实现方式与关键机制，重点覆盖入口、UI、数据层、模型层、安全、样式与测试。

---

## 1. 入口与启动流程

**相关文件**：
- `src/main/java/org/example/Main.java`
- `src/main/java/org/example/App.java`

**实现要点**：
- `Main.main()` 仅负责调用 `App.launchApp(args)`，将启动职责交给 JavaFX 应用类。
- `App.start(Stage)` 是 UI 的真正入口：
  - `dataStore.load()` ��取本地数据并初始化默认书籍。
  - 构建 `BorderPane root`，顶部为统一 Header，中心为 Landing 页。
  - 加载 `styles.css` 形成统一视觉风格。

**整体流程**：
1. 入口调用 `App.launchApp`。
2. `start` 初始化数据与 UI 根容器。
3. 根据用户登录状态切换 Portal 或 Dashboard。

---

## 2. UI 主模块（App.java）

**相关文件**：
- `src/main/java/org/example/App.java`

`App.java` 负责所有页面与交互逻辑，按角色拆分为三大门户：学生/教职员、作者、馆员。核心方法按“页面构建器”的方式组织。

### 2.1 顶部导航与 Landing 页面
- `buildHeader()`：统一的顶部栏，包含 Logo、标题、Home/Logout 按钮。
- `buildLanding()`：三张角色卡片（Student/Staff、Author、Librarian），点击进入对应 Portal。
- `buildPortalCard()`：封装卡片布局与按钮行为。

### 2.2 认证模块（Login / Register）
- `buildAuthView(Role)`：构建登录/注册 TabPane。
- `buildLoginTab(Role)`：
  - 收集用户名与密码。
  - 调用 `dataStore.authenticate(...)` 做角色匹配与密码验证。
  - 成功后调用 `showDashboard(user)`。
- `buildRegisterTab(Role)`：
  - 学生允许选择 Student/Staff 角色；作者填写 Bio；馆员填写 Employee ID。
  - 调用 `dataStore.registerUser(...)` 完成校验与持久化。

### 2.3 学生/教职员 Dashboard
- `buildStudentDashboard()` 由三部分构成：
  1. **统计卡**：可借书数量、我的借阅数、访问角色。
  2. **Catalog 区**：
     - `buildAvailableBooksTable()` 展示书籍。
     - `buildCatalogFilters()` 提供检索/筛选（标题、作者、类别、日期、可借状态）。
     - `buildAvailableActions()` 提供预览、摘要、借阅功能。
  3. **右侧卡片**：
     - 已借书列表 `buildBorrowedBooksTable()`。
     - 推荐列表 `buildRecommendationList()`（可点击定位到表格）。

**关键交互**：
- `showBorrowConfirmation()` 与 `showReturnConfirmation()` 通过 `Alert` 确认操作。
- 借阅成功后触发推荐刷新与统计更新。

### 2.4 作者 Dashboard
- `buildAuthorDashboard()`：
  - 顶部统计（总提交/已批准/被拒）。
  - 左侧为发布表单，右侧为提交列表。
  - 整体使用 `ScrollPane` 支持长页面滚动。

**发布表单与自动保存**：
- `buildAuthorPublishForm()`
  - `FlowPane` 实现多选 Genre。
  - `PauseTransition` 实现 2 秒自动保存草稿。
  - `dataStore.saveDraft()` 保存草稿；提交成功后 `clearDraft()`。

### 2.5 馆员 Dashboard
- `buildLibrarianDashboard()`：
  - 统计卡：待��批数、可借书数、注册用户数。
  - 待审批书籍列表 `buildPendingTable()`。
  - 审批按钮区 `buildApprovalActions()`：预览、打开文件、批准、拒绝。
  - 用户目录 `buildUsersTable()`。

**审批流程**：
- `handleApproval()` 调用 `dataStore.approveBook` 或 `rejectBook`，并展示确认弹窗。

### 2.6 书籍预览与文件打开
- `showQuickPreviewDialog()`：PDF 使用 PDFBox 渲染前 3 页；其他格式展示前 30 行文本。
- `showPdfPreviewDialog()` + `renderPdfPreview()`：
  - PDFBox `PDDocument` + `PDFRenderer` 渲染。
  - `Pagination` 展示分页缩略。
- `openBookFile()`：使用 `Desktop` 直接打开本地文件。

---

## 3. 数据层（DataStore）

**相关文件**：
- `src/main/java/org/example/data/DataStore.java`

DataStore 是所有数据的中心入口，负责：
1. 数据持久化（序列化文件）
2. 业务逻辑（注册、登录、借阅、审批）
3. 视图数据刷新（ObservableList）

### 3.1 持久化机制
- `load()` 从 `data/lms-data.dat` 反序列化读取：用户、书籍、草稿。
- 若文件不存在，调用 `seedDefaultBooks()` 写入 50 本默认书。
- `save()` 将用户/书籍/草稿写回序列化文件。

### 3.2 用户与认证
- `registerUser(...)`：
  - 校验用户名与密码强度。
  - 使用 `PasswordUtil` 生成 Salt + Hash。
  - 写入本地列表后 `save()`。
- `authenticate(...)`：
  - 校验角色匹配与密码正确性。

### 3.3 书籍业务流程
- `submitBook(...)`：作者提交 -> 状态 `PENDING_APPROVAL`。
- `approveBook(...)` / `rejectBook(...)`：馆员审批改变状态。
- `borrowBook(...)` / `returnBook(...)`：借阅/归还并更新日期与状态。

### 3.4 推荐算法（Phase 1）
- `getRecommendations(username, limit)`：
  - 统计用户借阅历史的 genre 频次。
  - 若无借阅记录，按 `borrowCount` 热度排序。
  - 有历史则对候选书根据“匹配 genre 频次”评分。

### 3.5 ObservableList 视图缓存
- `refreshViews()` 每次调用将 List -> ObservableList，保证 TableView/ListView 数据同步。

---

## 4. 模型层（Model）

**相关文件**：
- `src/main/java/org/example/model/Book.java`
- `src/main/java/org/example/model/User.java`
- `src/main/java/org/example/model/Role.java`
- `src/main/java/org/example/model/BookStatus.java`
- `src/main/java/org/example/model/AuthorDraft.java`

### 4.1 Book
- 表示书籍的核心实体，字段包括：标题、作者、类别、描述、文件路径、日期、状态、借阅记录。
- `borrow()` 自动设��� `borrowedDate` 与 `dueDate=+14天`。
- `approve()` 设置 `APPROVED_AVAILABLE`。
- `returnBook()` 重置借阅信息。

### 4.2 User
- 存储用户名、姓名、角色、密码 Hash/Salt，以及可选 Bio、Employee ID。

### 4.3 Role 与 BookStatus
- `Role`：学生/教职员/作者/馆员。
- `BookStatus`：待审批、已批准可借、已借出、被拒绝。
- 枚举中包含 `displayName`，直接用于 UI 展示。

### 4.4 AuthorDraft
- 用于作者的草稿自动保存。
- 保存标题、类别、描述、文件路径与 `lastSaved` 时间。

---

## 5. 安全模块（PasswordUtil）

**相关文件**：
- `src/main/java/org/example/security/PasswordUtil.java`

**实现方式**：
- 使用 PBKDF2WithHmacSHA256 进行密码哈希。
- `generateSalt()` 生成随机 16 字节盐。
- `hashPassword()` 根据 salt 与迭代次数生成 Hash。
- `verifyPassword()` 做 Hash 比对。

**强度校验**在 `DataStore.validatePassword()`：
- 长度 8-64
- 至少包含大写、小写、数字、符号

---

## 6. 样式与 UI 设计（styles.css）

**相关文件**：
- `src/main/resources/styles.css`

**关键点**：
- 统一主色调：深蓝 + 青色，保持 HKUST 风格。
- 重要组件样式：
  - `app-header` 渐变导航栏
  - `portal-card` 与 `card` 统一卡片式布局
  - 表格行高、选中态与 hover 样式
  - `page-scroll` 支持长页面滚动条优化

**可维护性**：
- 通过 `getStyleClass().add(...)` 绑定到 UI 控件，使得样式与布局分离。

---

## 7. 测试模块

**相关文件**：
- `src/test/java/org/example/security/PasswordUtilTest.java`

**覆盖点**：
- 生成 salt 并 hash。
- 验证正确密码为 true，错误密码为 false。

---

## 8. 模块间协作关系（简要）

- `App` 作为 UI 层调用 `DataStore` 完成所有业务操作。
- `DataStore` 调用 `PasswordUtil` 进行安全校验。
- `App` 通过 `Model` 对象读取/展示信息。
- `styles.css` 为 UI 统一视觉标准。

---

## 9. 可扩展点（Phase 2 预留方向）

- 推荐系统可替换为更复杂的相似度/协同过滤算法。
- `DataStore` 可替换为数据库（MySQL/SQLite）。
- UI 可拆分为独立 Controller，降低 `App.java` 体积。

---

> 本报告侧重“代码如何实现需求”的技术说明，如需细分到每个方法的输入/输出契约，我可以补充更详细的 API 级别文档。
