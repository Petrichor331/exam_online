# -*- coding: utf-8 -*-
"""
AI评分API服务
提供HTTP接口供Java后端调用
"""
import jieba
import jieba.analyse
import numpy as np
import pandas as pd
from scipy.stats import pearsonr
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sentence_transformers import SentenceTransformer
from sklearn.linear_model import Ridge
import re
import os
import time
import json
from flask import Flask, request, jsonify
from flask_cors import CORS

os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'
os.environ['HF_HUB_DISABLE_SYMLINKS_WARNING'] = '1'

app = Flask(__name__)
CORS(app)  # 允许跨域

# 中文停用词表
STOP_WORDS = set(
    ['的', '了', '在', '是', '我', '有', '和', '就', '不', '人', '都', '一', '一个', '上', '也', '很', '到', '说', '要',
     '去', '你', '会', '着', '没有', '看', '好', '自己', '这'])


def chinese_tokenizer(text):
    """改进的中文分词，去除停用词"""
    words = jieba.cut(text)
    return [w for w in words if w not in STOP_WORDS and len(w.strip()) > 0]


def preprocess_text(text):
    """文本预处理：去除特殊字符、标准化"""
    if not text:
        return ""
    text = re.sub(r'\s+', '', text)
    text = re.sub(r'[^\w\s]', '', text)
    return text.strip()


class OptimizedSubjectiveScorer:
    def __init__(self, alpha=0.3, use_regression=False):
        self.alpha = alpha
        self.use_regression = use_regression
        self.regression_model = Ridge(alpha=1.0) if use_regression else None
        
        # S型曲线评分阈值配置
        self.score_thresholds = [
            (0.90, 0.95, 0.0),
            (0.80, 0.85, 1.0),
            (0.60, 0.70, 0.75),
            (0.40, 0.50, 1.0),
            (0.00, 0.20, 1.0),
        ]

        # 加载BERT模型
        print("正在加载中文BERT模型，请稍候...")
        start = time.time()
        try:
            self.bert = SentenceTransformer("shibing624/text2vec-base-chinese")
            print(f"模型加载完成！用时 {time.time() - start:.1f} 秒")
        except Exception as e:
            print(f"模型加载失败: {e}")
            raise

        self.tfidf = TfidfVectorizer(
            tokenizer=chinese_tokenizer,
            ngram_range=(1, 2),
            max_features=5000
        )

    def extract_keywords(self, text, top_k=5):
        """提取关键词"""
        try:
            keywords = jieba.analyse.extract_tags(text, topK=top_k, withWeight=True)
            return {word: weight for word, weight in keywords}
        except:
            return {}

    def keyword_similarity(self, std_text, stu_text):
        """基于关键词的Jaccard相似度"""
        std_keywords = set(self.extract_keywords(std_text).keys())
        stu_keywords = set(self.extract_keywords(stu_text).keys())

        if not std_keywords or not stu_keywords:
            return 0.0

        intersection = len(std_keywords & stu_keywords)
        union = len(std_keywords | stu_keywords)
        return intersection / union if union > 0 else 0.0

    def tfidf_similarity(self, standard, student):
        """TF-IDF相似度"""
        corpus = [preprocess_text(standard), preprocess_text(student)]
        try:
            tfidf_matrix = self.tfidf.fit_transform(corpus)
            return cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:2])[0][0]
        except:
            return 0.0

    def bert_similarity(self, standard, student):
        """BERT语义相似度"""
        texts = [preprocess_text(standard), preprocess_text(student)]
        
        try:
            embeddings = self.bert.encode(texts, normalize_embeddings=True)
            return cosine_similarity(
                embeddings[0].reshape(1, -1),
                embeddings[1].reshape(1, -1)
            )[0][0]
        except Exception as e:
            print(f"BERT计算错误: {e}")
            return 0.0

    def length_penalty(self, std_text, stu_text, max_ratio=2.0):
        """长度惩罚"""
        if not std_text or not stu_text:
            return 0.0
            
        std_len = len(std_text)
        stu_len = len(stu_text)

        if std_len == 0:
            return 0.0

        ratio = stu_len / std_len

        if ratio < 0.3:
            return 0.7
        elif ratio > max_ratio:
            return 0.9
        else:
            return 1.0

    def compute_features(self, standard, student):
        """计算多维度特征"""
        sim_tfidf = self.tfidf_similarity(standard, student)
        sim_bert = self.bert_similarity(standard, student)
        sim_keyword = self.keyword_similarity(standard, student)

        final_sim = (0.7 * sim_bert +
                     0.2 * sim_tfidf +
                     0.1 * sim_keyword)

        len_factor = self.length_penalty(standard, student)

        return {
            'tfidf': round(sim_tfidf, 4),
            'bert': round(sim_bert, 4),
            'keyword': round(sim_keyword, 4),
            'ensemble': round(final_sim * len_factor, 4),
            'length_factor': len_factor
        }

    def similarity_to_score(self, features, max_score):
        """相似度转换为分数"""
        sim = features['ensemble']
        score = None
        
        for i, (threshold, base_score, slope) in enumerate(self.score_thresholds):
            if sim >= threshold:
                if i == 0:
                    score = base_score * max_score
                else:
                    prev_threshold = self.score_thresholds[i-1][0]
                    score = (base_score + (sim - threshold) * slope) * max_score
                break
        
        if score is None:
            min_base = self.score_thresholds[-1][1]
            score = max(min_base * max_score, sim * max_score)

        return round(score, 2)

    def grade_single(self, standard, student, max_score):
        """评分单道题"""
        features = self.compute_features(standard, student)
        score = self.similarity_to_score(features, max_score)

        return {
            "score": score,
            "maxScore": max_score,
            "similarity": features['bert'],
            "features": features
        }

    def grade_batch(self, items):
        """批量评分"""
        results = []
        for item in items:
            result = self.grade_single(
                item.get('standardAnswer', ''),
                item.get('studentAnswer', ''),
                item.get('maxScore', 10)
            )
            result['answerId'] = item.get('answerId')
            result['questionId'] = item.get('questionId')
            results.append(result)
        return results


# 全局评分器实例
scorer = None


def init_scorer():
    """初始化评分器"""
    global scorer
    if scorer is None:
        scorer = OptimizedSubjectiveScorer()
    return scorer


@app.route('/health', methods=['GET'])
def health_check():
    """健康检查接口"""
    return jsonify({
        "status": "ok",
        "message": "AI评分服务正常运行"
    })


@app.route('/grade', methods=['POST'])
def grade():
    """
    单题评分接口
    请求参数：
    {
        "standardAnswer": "标准答案",
        "studentAnswer": "学生答案",
        "maxScore": 10
    }
    """
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({"error": "请求参数为空"}), 400
            
        standard = data.get('standardAnswer', '')
        student = data.get('studentAnswer', '')
        max_score = data.get('maxScore', 10)
        
        if not standard or not student:
            return jsonify({
                "score": 0,
                "maxScore": max_score,
                "similarity": 0,
                "message": "答案不能为空"
            })
        
        # 确保评分器已初始化
        init_scorer()
        
        # 评分
        result = scorer.grade_single(standard, student, max_score)
        
        return jsonify({
            "success": True,
            "data": result
        })
        
    except Exception as e:
        print(f"评分错误: {e}")
        return jsonify({
            "error": str(e),
            "message": "评分失败"
        }), 500


@app.route('/grade/batch', methods=['POST'])
def grade_batch():
    """
    批量评分接口
    请求参数：
    {
        "answers": [
            {
                "answerId": 1,
                "questionId": 101,
                "standardAnswer": "标准答案1",
                "studentAnswer": "学生答案1",
                "maxScore": 10
            },
            ...
        ]
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'answers' not in data:
            return jsonify({"error": "缺少answers参数"}), 400
            
        answers = data.get('answers', [])
        
        if not answers:
            return jsonify({"error": "答案列表为空"}), 400
        
        # 确保评分器已初始化
        init_scorer()
        
        # 批量评分
        results = scorer.grade_batch(answers)
        
        return jsonify({
            "success": True,
            "data": results,
            "count": len(results)
        })
        
    except Exception as e:
        print(f"批量评分错误: {e}")
        return jsonify({
            "error": str(e),
            "message": "批量评分失败"
        }), 500


if __name__ == '__main__':
    print("=" * 50)
    print("启动AI评分API服务")
    print("=" * 50)
    
    # 预加载模型
    try:
        init_scorer()
        print("\n服务启动成功！")
        print("接口地址: http://localhost:5000")
        print("健康检查: GET http://localhost:5000/health")
        print("单题评分: POST http://localhost:5000/grade")
        print("批量评分: POST http://localhost:5000/grade/batch")
        print("=" * 50)
    except Exception as e:
        print(f"模型加载失败: {e}")
        exit(1)
    
    # 启动服务
    app.run(host='0.0.0.0', port=5000, debug=False)
