-- Agent domain tables required by the agent module

CREATE TABLE IF NOT EXISTS agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    agent_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(100) NOT NULL,
    configuration TEXT,
    max_retries INTEGER NOT NULL DEFAULT 3,
    execution_timeout_seconds BIGINT NOT NULL DEFAULT 300,
    tenant_id BIGINT NOT NULL,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_agent_tenant_id ON agents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_status ON agents(status);

CREATE TABLE IF NOT EXISTS workflows (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    execution_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    agent_id BIGINT NOT NULL,
    input_parameters TEXT,
    output_result TEXT,
    error_message TEXT,
    execution_start_time TIMESTAMP,
    execution_end_time TIMESTAMP,
    execution_duration_ms BIGINT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    retry_count INTEGER NOT NULL DEFAULT 0,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_workflows_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workflow_agent_id ON workflows(agent_id);
CREATE INDEX IF NOT EXISTS idx_workflow_status ON workflows(status);
CREATE INDEX IF NOT EXISTS idx_workflow_tenant_id ON workflows(tenant_id);

CREATE TABLE IF NOT EXISTS workflow_steps (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    step_order INTEGER NOT NULL,
    step_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    workflow_id BIGINT NOT NULL,
    action TEXT,
    input TEXT,
    output TEXT,
    error_message TEXT,
    execution_start_time TIMESTAMP,
    execution_end_time TIMESTAMP,
    execution_duration_ms BIGINT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    condition TEXT,
    next_step_on_success VARCHAR(100),
    next_step_on_failure VARCHAR(100),
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT fk_workflow_steps_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_step_workflow_id ON workflow_steps(workflow_id);
CREATE INDEX IF NOT EXISTS idx_step_status ON workflow_steps(status);

-- RAG feedback table required by search feedback domain model
CREATE TABLE IF NOT EXISTS search_feedback (
    id BIGSERIAL PRIMARY KEY,
    query TEXT NOT NULL,
    query_hash VARCHAR(64) NOT NULL,
    chunk_id BIGINT NOT NULL,
    rank_position INTEGER NOT NULL,
    tenant_id BIGINT NOT NULL,
    feedback_type VARCHAR(50) NOT NULL,
    rating INTEGER,
    comment TEXT,
    clicked BOOLEAN DEFAULT FALSE,
    dwell_time_ms BIGINT,
    selected BOOLEAN DEFAULT FALSE,
    experiment_variant VARCHAR(64),
    session_id VARCHAR(64),
    source VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_feedback_chunk ON search_feedback(chunk_id);
CREATE INDEX IF NOT EXISTS idx_feedback_query ON search_feedback(query_hash);
CREATE INDEX IF NOT EXISTS idx_feedback_tenant ON search_feedback(tenant_id);
CREATE INDEX IF NOT EXISTS idx_feedback_timestamp ON search_feedback(created_at);
