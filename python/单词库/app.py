#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
艾宾浩斯背单词 Web 后端
提供 API 供前端调用
"""

import sqlite3
import datetime
import random
import os
import json
from flask import Flask, request, jsonify, render_template, send_from_directory
import webbrowser
import threading
import time

app = Flask(__name__)

# ---------- 配置 ----------
DB_FILE = "words.db"
WORD_FILE = "单词汇总.txt"
NEW_WORDS_PER_DAY = 100
REVIEW_WORDS_PER_DAY = 200
REVIEW_INTERVALS = [1, 2, 4, 7, 15]   # 天
TEST_COUNT = 150

# ---------- 数据库初始化 ----------
def init_db():
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute('''
        CREATE TABLE IF NOT EXISTS words (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            word TEXT UNIQUE,
            meaning TEXT,
            level INTEGER DEFAULT 0,
            next_review_date TEXT,
            last_review_date TEXT,
            created_date TEXT
        )
    ''')
    c.execute('''
        CREATE TABLE IF NOT EXISTS daily_progress (
            date TEXT PRIMARY KEY,
            new_learned INTEGER DEFAULT 0,
            review_learned INTEGER DEFAULT 0
        )
    ''')
    conn.commit()
    conn.close()

def import_words():
    if not os.path.exists(WORD_FILE):
        print(f"⚠️ 单词文件 {WORD_FILE} 不存在，请放入该目录。")
        return
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute("SELECT COUNT(*) FROM words")
    if c.fetchone()[0] > 0:
        return  # 已有单词，跳过
    today = datetime.date.today().isoformat()
    with open(WORD_FILE, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            if '\t' in line:
                word, meaning = line.split('\t', 1)
            else:
                parts = line.split(' ', 1)
                if len(parts) == 2:
                    word, meaning = parts
                else:
                    continue
            c.execute('''
                INSERT OR IGNORE INTO words (word, meaning, level, next_review_date, created_date)
                VALUES (?, ?, ?, ?, ?)
            ''', (word.strip(), meaning.strip(), 0, today, today))
    conn.commit()
    conn.close()
    print("✅ 单词导入完成。")

# ---------- 数据库操作函数 ----------
def get_today_progress():
    today = datetime.date.today().isoformat()
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute("SELECT new_learned, review_learned FROM daily_progress WHERE date = ?", (today,))
    row = c.fetchone()
    conn.close()
    if row:
        return row[0], row[1]
    return 0, 0

def update_daily_progress(date, new_learned, review_learned):
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute('''
        INSERT OR REPLACE INTO daily_progress (date, new_learned, review_learned)
        VALUES (?, ?, ?)
    ''', (date, new_learned, review_learned))
    conn.commit()
    conn.close()

def get_review_words(limit):
    today = datetime.date.today().isoformat()
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute('''
        SELECT id, word, meaning FROM words
        WHERE level > 0 AND next_review_date <= ?
        ORDER BY next_review_date
        LIMIT ?
    ''', (today, limit))
    rows = [{"id": r[0], "word": r[1], "meaning": r[2]} for r in c.fetchall()]
    conn.close()
    return rows

def get_unlearned_words(limit):
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute('''
        SELECT id, word, meaning FROM words
        WHERE level = 0
        ORDER BY created_date
        LIMIT ?
    ''', (limit,))
    rows = [{"id": r[0], "word": r[1], "meaning": r[2]} for r in c.fetchall()]
    conn.close()
    return rows

def update_word_result(word_id, recognize):
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute("SELECT level FROM words WHERE id = ?", (word_id,))
    row = c.fetchone()
    if not row:
        conn.close()
        return
    current_level = row[0]
    if recognize:
        new_level = min(current_level + 1, len(REVIEW_INTERVALS))
        if new_level == 0:
            next_date = datetime.date.today()
        else:
            interval = REVIEW_INTERVALS[new_level - 1]
            next_date = datetime.date.today() + datetime.timedelta(days=interval)
    else:
        new_level = 0
        next_date = datetime.date.today() + datetime.timedelta(days=1)
    today = datetime.date.today().isoformat()
    c.execute('''
        UPDATE words
        SET level = ?, next_review_date = ?, last_review_date = ?
        WHERE id = ?
    ''', (new_level, next_date.isoformat(), today, word_id))
    conn.commit()
    conn.close()

def get_stats():
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute("SELECT COUNT(*) FROM words")
    total = c.fetchone()[0]
    c.execute("SELECT COUNT(*) FROM words WHERE level = 0")
    unlearned = c.fetchone()[0]
    c.execute("SELECT COUNT(*) FROM words WHERE level > 0")
    learned = c.fetchone()[0]
    today = datetime.date.today().isoformat()
    c.execute("SELECT COUNT(*) FROM words WHERE level > 0 AND next_review_date <= ?", (today,))
    due_review = c.fetchone()[0]
    conn.close()
    return {
        "total": total,
        "unlearned": unlearned,
        "learned": learned,
        "due_review": due_review
    }

def get_today_tasks():
    """返回今日剩余任务（复习词+新词），以及已学数量"""
    new_learned, review_learned = get_today_progress()
    review_limit = REVIEW_WORDS_PER_DAY - review_learned
    new_limit = NEW_WORDS_PER_DAY - new_learned
    review_words = get_review_words(max(review_limit, 0))
    new_words = get_unlearned_words(max(new_limit, 0))
    return {
        "review_words": review_words,
        "new_words": new_words,
        "review_count": len(review_words),
        "new_count": len(new_words),
        "total": len(review_words) + len(new_words),
        "today_review_learned": review_learned,
        "today_new_learned": new_learned
    }

# ---------- 路由 ----------
@app.route('/')
def index():
    return render_template('index.html')

@app.route('/api/today_tasks')
def api_today_tasks():
    data = get_today_tasks()
    return jsonify(data)

@app.route('/api/memory_check', methods=['POST'])
def api_memory_check():
    """检查一个单词，返回是否正确"""
    data = request.get_json()
    word_id = data.get('word_id')
    user_answer = data.get('user_answer', '').strip()
    conn = sqlite3.connect(DB_FILE)
    c = conn.cursor()
    c.execute("SELECT meaning FROM words WHERE id = ?", (word_id,))
    row = c.fetchone()
    conn.close()
    if not row:
        return jsonify({"error": "单词不存在"}), 404
    correct_meaning = row[0]
    correct = (user_answer == correct_meaning)
    return jsonify({"correct": correct, "meaning": correct_meaning})

@app.route('/api/learn', methods=['POST'])
def api_learn():
    data = request.get_json()
    word_id = data.get('word_id')
    recognize = data.get('recognize', True)
    # 更新单词
    update_word_result(word_id, recognize)
    # 更新 daily_progress
    today = datetime.date.today().isoformat()
    new_learned, review_learned = get_today_progress()
    # 判断该词是复习还是新词（通过查询其原有level判断，但我们无法在此获取，可以在调用时传入）
    # 可以从数据库获取当前level，但更新后level可能变了，所以我们用传入的is_review标记
    # 因为前端知道，我们让前端传递 is_review 参数
    is_review = data.get('is_review', False)
    if is_review:
        review_learned += 1
    else:
        new_learned += 1
    update_daily_progress(today, new_learned, review_learned)
    # 返回更新后的进度
    return jsonify({"success": True, "new_learned": new_learned, "review_learned": review_learned})

@app.route('/api/stats')
def api_stats():
    return jsonify(get_stats())

# ---------- 启动服务器 ----------
if __name__ == '__main__':
    # 初始化数据库和导入单词
    if not os.path.exists(DB_FILE):
        init_db()
        import_words()
    else:
        init_db()  # 确保表存在
    # 自动打开浏览器
    def open_browser():
        time.sleep(1)
        webbrowser.open('http://127.0.0.1:5000')
    threading.Thread(target=open_browser).start()
    app.run(debug=True, host='0.0.0.0', port=5000)
