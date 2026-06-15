"""
AI服务配置文件
负责加载环境变量和配置参数
"""
import os
from pathlib import Path

# 在加载任何其他模块之前清除代理环境变量
for proxy_key in ['HTTP_PROXY', 'HTTPS_PROXY', 'http_proxy', 'https_proxy', 'ALL_PROXY', 'all_proxy']:
    os.environ.pop(proxy_key, None)

from dotenv import load_dotenv

# 尝试加载.env文件
env_path = Path(__file__).parent / ".env"
load_dotenv(env_path)
print(f"[配置] 正在从 {env_path} 加载环境变量...")

class Config:
    """服务配置类"""

    # AI模型配置
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
    OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-3.5-turbo")
    OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")

    # UApiPro 配置
    UAPIPRO_API_KEY = os.getenv("UAPIPRO_API_KEY", "")
    UAPIPRO_BASE_URL = os.getenv("UAPIPRO_BASE_URL", "https://api.uapipro.com")

    # 服务配置
    HOST = os.getenv("HOST", "0.0.0.0")
    PORT = int(os.getenv("PORT", 8001))

    # 请求配置
    TIMEOUT = 60
    MAX_RETRIES = 3

    # MySQL 配置（知识库直读）
    MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
    MYSQL_PORT = int(os.getenv("MYSQL_PORT", 3305))
    MYSQL_USER = os.getenv("MYSQL_USER", "root")
    MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "root")
    MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "big_event")

    # 知识库检索配置
    KNOWLEDGE_TOP_K = int(os.getenv("KNOWLEDGE_TOP_K", 3))
    KNOWLEDGE_MIN_SIMILARITY = float(os.getenv("KNOWLEDGE_MIN_SIMILARITY", 0.05))

    @classmethod
    def print_config(cls):
        """打印当前配置（用于调试）"""
        print(f"[配置] OPENAI_API_KEY: {'已设置 (长度: ' + str(len(cls.OPENAI_API_KEY)) + ')' if cls.OPENAI_API_KEY else '未设置'}")
        print(f"[配置] OPENAI_MODEL: {cls.OPENAI_MODEL}")
        print(f"[配置] OPENAI_BASE_URL: {cls.OPENAI_BASE_URL}")
        print(f"[配置] UAPIPRO_API_KEY: {'已设置 (长度: ' + str(len(cls.UAPIPRO_API_KEY)) + ')' if cls.UAPIPRO_API_KEY else '未设置'}")
        print(f"[配置] UAPIPRO_BASE_URL: {cls.UAPIPRO_BASE_URL}")
        print(f"[配置] HOST: {cls.HOST}, PORT: {cls.PORT}")
        print(f"[配置] HTTP_PROXY: {os.environ.get('HTTP_PROXY', '未设置')}")
        print(f"[配置] HTTPS_PROXY: {os.environ.get('HTTPS_PROXY', '未设置')}")

config = Config()

# 启动时打印配置信息
config.print_config()