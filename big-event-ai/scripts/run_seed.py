# -*- coding: utf-8 -*-
import pymysql

conn = pymysql.connect(
    host='localhost', port=3305, user='root', password='root',
    database='big_event', charset='utf8mb4'
)
cursor = conn.cursor()

with open('big-event-ai/scripts/seed_knowledge.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# Split by INSERT INTO keyword
parts = content.split('INSERT INTO')
for part in parts[1:]:
    stmt = ('INSERT INTO' + part).strip()
    # Find the last );  — the statement terminator
    idx = stmt.rfind(');')
    if idx > 0:
        stmt = stmt[:idx+2]
    try:
        cursor.execute(stmt)
    except Exception as e:
        print(f'Error: {e}')
        print(f'Stmt preview: {stmt[:200]}...')

conn.commit()
cursor.execute("SELECT id, title, word_count, quality FROM knowledge_article ORDER BY id")
for row in cursor.fetchall():
    print(f"  id={row[0]}  {row[1]}  ({row[2]} chars, quality={row[3]})")

conn.close()
print("Done")
