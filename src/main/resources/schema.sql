-- 创建知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
                                              id BIGSERIAL PRIMARY KEY,
                                              name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- 创建文档表
CREATE TABLE IF NOT EXISTS document (
                                        id BIGSERIAL PRIMARY KEY,
                                        knowledge_base_id BIGINT NOT NULL,
                                        file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    status VARCHAR(20) DEFAULT 'PENDING',
    chunk_count INT DEFAULT 0,
    token_count INT DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id)
    );

-- 初始化默认知识库
INSERT INTO knowledge_base (name, code, description) VALUES
                                                         ('HR知识库', 'HR', '人力资源相关制度文档'),
                                                         ('财务知识库', 'FINANCE', '财务管理制度文档'),
                                                         ('研发知识库', 'R&D', '研发技术文档')
ON CONFLICT (code) DO NOTHING;