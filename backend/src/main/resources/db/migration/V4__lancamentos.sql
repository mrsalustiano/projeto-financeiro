-- Fase 3: lançamentos, parcelas, fechamento de mês e idempotência.
-- transferencia nasce aqui porque a fase 4 só acrescenta o caso de uso.

CREATE TABLE parcela (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    grupo CHAR(36) NOT NULL,
    total INT NOT NULL,
    valor_total DECIMAL(15, 2) NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_parcela_grupo (cliente_id, grupo),
    CONSTRAINT fk_parcela_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE TABLE lancamento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    conta_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    natureza VARCHAR(20) NOT NULL,
    tipo_despesa VARCHAR(20) NULL,
    valor DECIMAL(15, 2) NOT NULL,
    data DATE NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    parcela_id BIGINT NULL,
    numero_parcela INT NULL,
    total_parcelas INT NULL,
    estornado_em TIMESTAMP(6) NULL,
    estorno_motivo VARCHAR(255) NULL,
    estornado_por VARCHAR(120) NULL,
    criado_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_lancamento_parcela (cliente_id, parcela_id, numero_parcela),
    KEY ix_lancamento_cliente_data (cliente_id, data),
    KEY ix_lancamento_conta (cliente_id, conta_id),
    CONSTRAINT fk_lancamento_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_lancamento_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT fk_lancamento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id),
    CONSTRAINT fk_lancamento_parcela FOREIGN KEY (parcela_id) REFERENCES parcela (id)
);

CREATE TABLE transferencia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    conta_origem_id BIGINT NOT NULL,
    conta_destino_id BIGINT NOT NULL,
    valor DECIMAL(15, 2) NOT NULL,
    data DATE NOT NULL,
    descricao VARCHAR(255) NULL,
    criado_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_transferencia_cliente_data (cliente_id, data),
    CONSTRAINT fk_transferencia_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_transferencia_origem FOREIGN KEY (conta_origem_id) REFERENCES conta (id),
    CONSTRAINT fk_transferencia_destino FOREIGN KEY (conta_destino_id) REFERENCES conta (id)
);

CREATE TABLE fechamento_mes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    ano SMALLINT NOT NULL,
    mes TINYINT NOT NULL,
    fechado_em TIMESTAMP(6) NOT NULL,
    fechado_por VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fechamento_cliente_mes (cliente_id, ano, mes),
    CONSTRAINT fk_fechamento_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE TABLE idempotency_key (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    chave VARCHAR(120) NOT NULL,
    hash_corpo CHAR(64) NOT NULL,
    recurso_id BIGINT NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_cliente_chave (cliente_id, chave),
    CONSTRAINT fk_idempotency_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);
