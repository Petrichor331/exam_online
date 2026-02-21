# AI智能评分服务

## 项目说明

本服务基于BERT语义分析和TF-IDF算法，为在线考试系统提供主观题自动评分功能。

## 功能特点

- **多维度评分**：结合BERT语义相似度（70%）、TF-IDF文本相似度（20%）、关键词匹配度（10%）
- **智能长度惩罚**：防止学生答案过短或过长作弊
- **S型曲线评分**：低相似度给保底分，高相似度区分度大
- **批量评分支持**：支持同时评分多道题目
- **高性能**：使用中文BERT模型，针对中文文本优化

## 技术栈

- **框架**：Flask + Flask-CORS
- **NLP模型**：sentence-transformers (text2vec-base-chinese)
- **分词**：jieba + jieba.analyse
- **相似度计算**：scikit-learn (TF-IDF, cosine_similarity)
- **其他**：numpy, pandas, scipy

## 安装步骤

### 1. 安装Python依赖

```bash
cd springboot/src/main/resources/python

pip install flask flask-cors
pip install sentence-transformers
pip install jieba
pip install scikit-learn
pip install scipy
pip install pandas
pip install numpy
```

或者一键安装：

```bash
pip install flask flask-cors sentence-transformers jieba scikit-learn scipy pandas numpy
```

### 2. 启动服务

#### Windows:
双击运行 `start_ai_service.bat`

#### Linux/Mac:
```bash
cd springboot/src/main/resources/python
python ai_grading_service.py
```

### 3. 验证服务

服务启动后会显示：
```
==================================================
启动AI评分API服务
==================================================
正在加载中文BERT模型，请稍候...
模型加载完成！用时 XX.X 秒

服务启动成功！
接口地址: http://localhost:5000
健康检查: GET http://localhost:5000/health
单题评分: POST http://localhost:5000/grade
批量评分: POST http://localhost:5000/grade/batch
==================================================
```

## API接口说明

### 1. 健康检查

**请求：**
```
GET http://localhost:5000/health
```

**响应：**
```json
{
  "status": "ok",
  "message": "AI评分服务正常运行"
}
```

### 2. 单题评分

**请求：**
```
POST http://localhost:5000/grade
Content-Type: application/json

{
  "standardAnswer": "Java面向对象的三大特性是封装、继承、多态。封装是指将数据和操作数据的方法绑定在一起；继承是指子类继承父类的特性；多态是指同一操作作用于不同的对象可以有不同的解释。",
  "studentAnswer": "三大特性：封装、继承、多态。封装就是把数据和方法包在一起，继承是子类继承父类，多态是不同的对象对同一消息有不同的响应。",
  "maxScore": 10
}
```

**响应：**
```json
{
  "success": true,
  "data": {
    "score": 8.5,
    "maxScore": 10,
    "similarity": 0.92,
    "features": {
      "tfidf": 0.85,
      "bert": 0.92,
      "keyword": 0.88,
      "ensemble": 0.90,
      "length_factor": 1.0
    }
  }
}
```

### 3. 批量评分

**请求：**
```
POST http://localhost:5000/grade/batch
Content-Type: application/json

{
  "answers": [
    {
      "answerId": 1,
      "questionId": 101,
      "standardAnswer": "标准答案1",
      "studentAnswer": "学生答案1",
      "maxScore": 10
    },
    {
      "answerId": 2,
      "questionId": 102,
      "standardAnswer": "标准答案2",
      "studentAnswer": "学生答案2",
      "maxScore": 15
    }
  ]
}
```

**响应：**
```json
{
  "success": true,
  "count": 2,
  "data": [
    {
      "answerId": 1,
      "questionId": 101,
      "score": 8.5,
      "maxScore": 10,
      "similarity": 0.92
    },
    {
      "answerId": 2,
      "questionId": 102,
      "score": 12.0,
      "maxScore": 15,
      "similarity": 0.88
    }
  ]
}
```

## 评分算法说明

### 1. BERT语义相似度 (权重70%)

使用shibing624/text2vec-base-chinese中文BERT模型，计算标准答案和学生答案的语义向量余弦相似度。

### 2. TF-IDF相似度 (权重20%)

- 使用jieba进行中文分词
- 去除停用词
- 使用TF-IDF向量计算余弦相似度

### 3. 关键词匹配度 (权重10%)

- 使用jieba.analyse提取关键词
- 计算Jaccard相似度

### 4. 长度惩罚因子

- 答案长度 < 标准答案30%：得分 × 0.7（过短）
- 答案长度 > 标准答案2倍：得分 × 0.9（过长）
- 其他情况：不惩罚

### 5. 分数映射规则 (S型曲线)

| 相似度 | 得分区间 | 说明 |
|--------|----------|------|
| > 0.90 | 95%-100% | 非常准确 |
| 0.80-0.90 | 85%-95% | 比较准确 |
| 0.60-0.80 | 70%-85% | 基本准确 |
| 0.40-0.60 | 50%-70% | 部分准确 |
| < 0.40 | 20%-50% | 不太准确 |

## 与Java后端集成

Java后端通过 `PythonGradingService` 调用本服务：

1. 学生交卷后，系统自动对客观题评分
2. 异步调用Python服务对主观题（填空、简答）进行AI评分
3. 评分结果保存到数据库，状态标记为"ai_graded"
4. 教师可以查看AI评分结果，必要时进行人工修正

## 配置说明

在 `application.yml` 中配置Python服务地址：

```yaml
python:
  service:
    url: http://localhost:5000
```

如果Python服务部署在其他机器或端口，请修改此配置。

## 常见问题

### Q1: 启动时提示模型下载失败？

**A:** 检查网络连接，模型会自动从HuggingFace下载。如果下载慢，可以设置镜像：
```python
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'
```

### Q2: 首次启动很慢？

**A:** 首次启动需要下载BERT模型（约400MB），请耐心等待。下载完成后会缓存在本地，下次启动很快。

### Q3: 评分不准确？

**A:** 
- 检查标准答案是否准确、完整
- 可以调整 `score_thresholds` 参数改变评分严格程度
- 对于重要考试，建议AI评分后由教师复核

### Q4: 内存占用大？

**A:** BERT模型需要约1-2GB内存，建议服务器至少4GB内存。

## 优化建议

1. **批处理**：尽量使用批量评分接口，比单题评分效率高
2. **缓存**：标准答案可以缓存BERT向量，避免重复编码
3. **异步**：评分操作应异步执行，避免阻塞主线程
4. **超时设置**：Java端设置合理的超时时间（建议60秒）

## 更新日志

### v1.0.0 (2024-XX-XX)
- 初始版本发布
- 支持BERT+TF-IDF+关键词多维度评分
- 支持单题和批量评分
- 实现S型曲线分数映射

## 联系方式

如有问题，请联系开发团队。
