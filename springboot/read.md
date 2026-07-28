# 宠物社区后端接口文档

## 统一返回结构

所有接口返回如下结构：

```json
{
  "code": 200,
  "msg": "请求成功",
  "data": {}
}
```
- code：状态码（200成功，其他为错误）
- msg：提示信息
- data：返回数据

---

## 认证与用户相关

### 发送验证码
- **接口**：`POST http://47.113.113.212:8080/auth/send-code`
- **参数**：
  - email（string，必填）：邮箱
  - type（int，选填）：验证码类型（0注册，1重置密码等）
  - 在接口后面拼接字符串
- **返回**：无

### 注册
- **接口**：`POST http://47.113.113.212:8080/auth/register`

- **参数**：JSON
  
  - username（string）
  - password（string）
  - email（string）
  - code（string，验证码）
  - 
  
- **返回**：用户信息

- 示例：

  ```json
  {
    "username": "11",
    "password": "yg1433223",
    "email": "2072712628@qq.com",
    "code": "976859",
    "securityQuestionId": 1,
    "securityAnswer": "你的答案"
  }
  ```

  

### 登录
- **接口**：`POST http://47.113.113.212:8080/auth/login`
- **参数**：JSON
  - username（string）
  - password（string）
- **返回**：用户信息（含token在响应头Authorization）

### 获取当前用户
- **接口**：`GET http://47.113.113.212:8080/auth/me`

- **参数**：Authorization  (格式：Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3c2JsemciLCJpYXQiOjE3NTMzNTc0NjUsImV4cCI6MTc1Mzk2MjI2NX0.Kuh5muXzYrgwtaAPuaXqoN6iCYtxdaLkDd7UyhceHIQ)

  ##### 从登录信息的协议头获取

- **返回**：用户信息

---

## 文章相关

### 获取所有文章
- **接口**：`GET http://47.113.113.212:8080/article/list`
- **参数**：Authorization  (格式：Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3c2JsemciLCJpYXQiOjE3NTMzNTc0NjUsImV4cCI6MTc1Mzk2MjI2NX0.Kuh5muXzYrgwtaAPuaXqoN6iCYtxdaLkDd7UyhceHIQ)

  ##### 从登录信息的协议头获取
- **返回**：文章列表

### 获取单篇文章
- **接口**：`GET http://47.113.113.212:8080/article/{id}`
- **参数**：Authorization  + id（路径参数）
- **返回**：文章详情

### 删除文章
- **接口**：`DELETE http://47.113.113.212:8080/article/{id}`
- **参数**：Authorization  + id（路径参数）
- **返回**：无

### 上传文章（含图片）
- **接口**：`POST http://47.113.113.212:8080/article/upload`
- **参数**：
  - title（string）
  - content（string）
  - images（文件数组，选填）
  - userId（long）
- **返回**：无

### 点赞
- **接口**：`POST http://47.113.113.212:8080/article/{id}/like`
- **参数**：
  - id（路径参数）
  - userId（long）
- **返回**：无

### 按点赞数降序查询文章
- **接口**：`GET http://47.113.113.212:8080/article/listByLikes`
- **参数**：
  - page（int）
  - size（int）
- **返回**：文章列表

---

## 评论相关

### 添加评论
- **接口**：`POST http://47.113.113.212:8080/article/{id}/comment`
- **参数**：
  - id（路径参数，文章ID）
  - userId（long）
  - content（string）
- **返回**：无

### 删除评论
- **接口**：`DELETE http://47.113.113.212:8080/article/comment/{commentId}`
- **参数**：
  - commentId（路径参数）
  - userId（long）
- **返回**：无

### 获取评论列表
- **接口**：`GET http://47.113.113.212:8080/article/{id}/comments`
- **参数**：id（路径参数，文章ID）
- **返回**：评论列表

---

## 收藏相关

### 添加收藏
- **接口**：`POST http://47.113.113.212:8080/article/{id}/favorite`
- **参数**：
  - id（路径参数，文章ID）
  - userId（long）
- **返回**：无

### 取消收藏
- **接口**：`DELETE http://47.113.113.212:8080/article/{id}/favorite`
- **参数**：
  - id（路径参数，文章ID）
  - userId（long）
- **返回**：无

### 判断是否已收藏
- **接口**：`GET http://47.113.113.212:8080/article/{id}/favorite`
- **参数**：
  - id（路径参数，文章ID）
  - userId（long）
- **返回**：true/false

### 获取用户收藏列表
- **接口**：`GET http://47.113.113.212:8080/user/{userId}/favorites`
- **参数**：userId（路径参数）
- **返回**：收藏列表

---

## 成就系统

### 查询全部成就
- **接口**：`GET http://47.113.113.212:8080/achievement/all`
- **参数**：无
- **返回**：成就列表

### 查询用户成就
- **接口**：`GET http://47.113.113.212:8080/achievement/user/{userId}`
- **参数**：userId（路径参数）
- **返回**：用户成就列表

### 用户获得成就
- **接口**：`POST http://47.113.113.212:8080/achievement/user/{userId}/gain/{achievementId}`
- **参数**：userId、achievementId（路径参数）
- **返回**：无

### 添加成就
- **接口**：`POST http://47.113.113.212:8080/achievement/add`
- **参数**：JSON（成就对象）
- **返回**：无

---

## 宠物币流水

### 查询用户宠物币流水
- **接口**：`GET http://47.113.113.212:8080/pet-coin-log/user/{userId}`
- **参数**：userId（路径参数）
- **返回**：流水列表

---

## 举报相关

### 举报文章
- **接口**：`POST http://47.113.113.212:8080/report/article`
- **参数**：
  - articleId（long）
  - reporterId（long）
  - content（string）
- **返回**：无

---

## 后台管理接口（以管理员身份访问）

### 用户管理
- **接口**：`GET http://47.113.113.212:8080/admin/user/list`
- **参数**：pageNum、pageSize、username（可选）
- **返回**：用户分页列表
- **接口**：`POST http://47.113.113.212:8080/admin/user/disable/{userId}`
- **接口**：`POST http://47.113.113.212:8080/admin/user/enable/{userId}`

### 文章管理
- **接口**：`GET http://47.113.113.212:8080/admin/article/list`
- **接口**：`POST http://47.113.113.212:8080/admin/article/delete/{articleId}`

#### 文章审核（强制设置状态）
- **接口**：`POST http://47.113.113.212:8080/admin/article/audit`
- **参数**：
  - id（long，必填）：文章ID
  - status（int，必填）：审核状态（0=待审核，1=已通过，2=未通过）
- **用途**：管理员对文章进行审核，直接将文章的审核状态设置为指定值，不判断原状态。
- **返回**：操作成功时返回 `{ code: 200, msg: "请求成功", data: null }`
- **使用说明**：适用于批量或强制审核场景，不会校验当前状态，直接覆盖。
- **示例**：
  ```bash
   POST "http://47.113.113.212:8080/admin/article/audit?id=123&status=1" -H "Authorization: Bearer {token}"
  ```

- **接口**：`POST http://47.113.113.212:8080/admin/article/set-essence`
- **接口**：`POST http://47.113.113.212:8080/admin/article/set-recommend`

### 评论管理
- **接口**：`GET http://47.113.113.212:8080/admin/comment/list`

#### 评论审核（强制设置状态）
- **接口**：`POST http://47.113.113.212:8080/admin/comment/audit`
- **参数**：
  - id（long，必填）：评论ID
  - status（int，必填）：审核状态（0=待审核，1=已通过，2=未通过）
- **用途**：管理员对评论进行审核，直接将评论的审核状态设置为指定值，不判断原状态。
- **返回**：操作成功时返回 `{ code: 200, msg: "请求成功", data: null }`
- **使用说明**：适用于批量或强制审核场景，不会校验当前状态，直接覆盖。
- **示例**：
  ```bash
  curl -X POST "http://47.113.113.212:8080/admin/comment/audit?id=456&status=1" -H "Authorization: Bearer {token}"
  ```

#### 评论智能审核（推荐业务接口）
- **接口**：`POST http://47.113.113.212:8080/admin/comment/audit2`
- **参数**：
  - id（long，必填）：评论ID
- **用途**：将评论审核状态从0（待审核）改为1（已通过），如果本来就是1则不再更改。
- **返回**：
  - 更改成功：`{ code: 200, msg: "请求成功", data: { message: "更改成功", comment: {...} } }`
  - 已审核：`{ code: 200, msg: "请求成功", data: { message: "评论已经审核", comment: {...} } }`
- **使用说明**：适用于只审核未审核内容的场景，防止重复操作，返回最新评论对象。
- **示例**：
  ```bash
  curl -X POST "http://47.113.113.212:8080/admin/comment/audit2?id=456" -H "Authorization: Bearer {token}"
  ```

- **接口**：`POST http://47.113.113.212:8080/admin/comment/delete/{id}`

### 成就管理
- **接口**：`GET http://47.113.113.212:8080/admin/achievement/list`
- **接口**：`POST http://47.113.113.212:8080/admin/achievement/add`
- **接口**：`POST http://47.113.113.212:8080/admin/achievement/edit`
- **接口**：`POST http://47.113.113.212:8080/admin/achievement/delete/{id}`

### 宠物币流水管理
- **接口**：`GET http://47.113.113.212:8080/admin/pet-coin-log/list/{userId}`
- **接口**：`POST http://47.113.113.212:8080/admin/pet-coin-log/grant`
- **接口**：`POST http://47.113.113.212:8080/admin/pet-coin-log/deduct`

### 用户成就管理
- **接口**：`GET http://47.113.113.212:8080/admin/user-achievement/list/{userId}`
- **接口**：`POST http://47.113.113.212:8080/admin/user-achievement/revoke`

### 媒体管理
- **接口**：`POST http://47.113.113.212:8080/admin/media/image/audit`
- **接口**：`POST http://47.113.113.212:8080/admin/media/image/delete/{id}`
- **接口**：`GET http://47.113.113.212:8080/admin/media/image/list`

### 管理员登录
- **接口**：`POST http://47.113.113.212:8080/admin/login`
- **参数**：username、password
- **返回**：token

---

## API测试相关接口

### 管理员注册
- **接口**：`POST http://47.113.113.212:8080/auth/admin/register`
- **参数**：JSON
  - username（string，必填）：用户名
  - email（string，必填）：邮箱
  - password（string，必填）：密码
  - confirmPassword（string，必填）：确认密码
  - code（string，必填）：验证码
  - adminCode（string，必填）：管理员注册码
  - securityQuestionId（int，必填）：安全问题ID
  - securityAnswer（string，必填）：安全问题答案
- **功能**：注册管理员账户，需要提供管理员注册码验证身份
- **返回**：注册结果
- **示例**：
  ```json
  {
    "username": "admin",
    "email": "admin@example.com",
    "password": "123456",
    "confirmPassword": "123456",
    "code": "123456",
    "adminCode": "ADMIN123",
    "securityQuestionId": 1,
    "securityAnswer": "我的答案"
  }
  ```

### 管理员认证
- **接口**：`POST http://47.113.113.212:8080/admin/auth/login`
- **参数**：JSON
  - username（string，必填）：用户名
  - password（string，必填）：密码
- **功能**：管理员登录验证，返回JWT token用于后续接口认证
- **返回**：管理员信息（含token在响应头Authorization）

### 文章搜索
- **接口**：`GET http://47.113.113.212:8080/article/search/title`
- **参数**：
  - keyword（string，必填）：搜索关键词
- **功能**：根据标题关键词模糊搜索文章，返回匹配的文章列表（包含图片信息）
- **返回**：匹配的文章列表

### 文章推荐管理
- **接口**：`POST http://47.113.113.212:8080/admin/recommend/set/{articleId}`
- **参数**：articleId（路径参数，文章ID）
- **功能**：将指定文章设置为推荐状态（is_recommend = 1）
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/recommend/cancel/{articleId}`
- **参数**：articleId（路径参数，文章ID）
- **功能**：取消指定文章的推荐状态（is_recommend = 0）
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/recommend/toggle/{articleId}`
- **参数**：articleId（路径参数，文章ID）
- **功能**：切换文章的推荐状态（如果当前是推荐则取消，如果当前不是推荐则设置）
- **返回**：操作结果

- **接口**：`GET http://47.113.113.212:8080/admin/recommend/list`
- **参数**：无
- **功能**：获取所有推荐状态的文章列表（is_recommend = 1 且 status = 1）
- **返回**：推荐文章列表

- **接口**：`POST http://47.113.113.212:8080/admin/recommend/batch-set`
- **参数**：JSON数组（文章ID列表）
- **功能**：批量设置多篇文章为推荐状态
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/recommend/batch-cancel`
- **参数**：JSON数组（文章ID列表）
- **功能**：批量取消多篇文章的推荐状态
- **返回**：操作结果

### 举报管理
- **接口**：`GET http://47.113.113.212:8080/admin/report/article/list`
- **参数**：无
- **功能**：获取所有文章举报记录列表
- **返回**：文章举报列表

- **接口**：`GET http://47.113.113.212:8080/admin/report/comment/list`
- **参数**：无
- **功能**：获取所有评论举报记录列表
- **返回**：评论举报列表

- **接口**：`POST http://47.113.113.212:8080/admin/report/article/audit`
- **参数**：
  - reportId（long，必填）：举报ID
  - status（int，必填）：审核状态（0=待处理，1=已处理，2=已驳回）
  - remark（string，选填）：审核备注
- **功能**：审核文章举报，如果审核通过（status=1）则删除被举报的文章
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/report/comment/audit`
- **参数**：
  - reportId（long，必填）：举报ID
  - status（int，必填）：审核状态（0=待处理，1=已处理，2=已驳回）
  - remark（string，选填）：审核备注
- **功能**：审核评论举报，如果审核通过（status=1）则删除被举报的评论
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/report/article/delete/{reportId}`
- **参数**：reportId（路径参数，举报ID）
- **功能**：删除指定的文章举报记录
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/report/comment/delete/{reportId}`
- **参数**：reportId（路径参数，举报ID）
- **功能**：删除指定的评论举报记录
- **返回**：操作结果

### 用户管理扩展
- **接口**：`POST http://47.113.113.212:8080/admin/user/update`
- **参数**：JSON（用户信息对象）
- **功能**：更新用户信息，包括用户名、邮箱、状态等
- **返回**：操作结果

### 文章管理扩展
- **接口**：`POST http://47.113.113.212:8080/admin/article/edit`
- **参数**：JSON（文章信息对象）
- **功能**：编辑文章信息，包括标题、内容、状态等
- **返回**：操作结果

### 宠物币管理扩展
- **接口**：`POST http://47.113.113.212:8080/admin/pet-coin-log/grant`
- **参数**：
  - userId（long，必填）：用户ID
  - amount（int，必填）：发放数量
  - description（string，必填）：发放说明
- **功能**：给指定用户发放宠物币，增加用户余额并记录流水
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/pet-coin-log/deduct`
- **参数**：
  - userId（long，必填）：用户ID
  - amount（int，必填）：扣除数量
  - description（string，必填）：扣除说明
- **功能**：扣除指定用户的宠物币，减少用户余额并记录流水
- **返回**：操作结果

### 成就管理扩展
- **接口**：`POST http://47.113.113.212:8080/admin/achievement/edit`
- **参数**：JSON（成就信息对象）
- **功能**：编辑成就信息，包括成就名称、描述、图标等
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/user-achievement/revoke`
- **参数**：
  - userId（long，必填）：用户ID
  - achievementId（long，必填）：成就ID
- **功能**：撤销用户已获得的成就，从用户成就列表中移除
- **返回**：操作结果

### 媒体管理扩展
- **接口**：`POST http://47.113.113.212:8080/admin/media/image/audit`
- **参数**：
  - id（long，必填）：媒体ID
  - status（int，必填）：审核状态（0=待审核，1=已通过，2=未通过）
- **功能**：审核媒体文件，控制媒体文件的可见性
- **返回**：操作结果

- **接口**：`POST http://47.113.113.212:8080/admin/media/image/delete/{id}`
- **参数**：id（路径参数，媒体ID）
- **功能**：删除指定的媒体文件
- **返回**：操作结果

---

## 说明

- 所有需要登录的接口需在请求头携带 `Authorization: Bearer {token}`。
- API测试相关接口主要用于前端测试系统，提供完整的管理功能。
- 推荐管理接口支持单个和批量操作，方便管理员管理文章推荐。
- 举报管理接口提供完整的举报处理流程，包括审核和删除功能。

