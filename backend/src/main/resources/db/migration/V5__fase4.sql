-- Fase 4: orçamento, meta, aporte e recorrência.
-- transferencia já nasceu na V4 porque a fase 3 precisava do saldo com transferências.

CREATE TABLE orcamento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    ano SMALLINT NOT NULL,
    mes TINYINT NOT NULL,
    limite DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orcamento_categoria_mes (cliente_id, categoria_id, ano, mes),
    CONSTRAINT fk_orcamento_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_orcamento_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);

CREATE TABLE meta (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    nome VARCHAR(80) NOT NULL,
    valor_alvo DECIMAL(15, 2) NOT NULL,
    data_alvo DATE NULL,
    criada_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_meta_cliente_nome (cliente_id, nome),
    CONSTRAINT fk_meta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE TABLE aporte (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    meta_id BIGINT NOT NULL,
    valor DECIMAL(15, 2) NOT NULL,
    data DATE NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_aporte_meta (cliente_id, meta_id),
    CONSTRAINT fk_aporte_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_aporte_meta FOREIGN KEY (meta_id) REFERENCES meta (id)
);

CREATE TABLE recorrencia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    conta_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    natureza VARCHAR(20) NOT NULL,
    tipo_despesa VARCHAR(20) NULL,
    valor DECIMAL(15, 2) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    dia_do_mes TINYINT NOT NULL,
    ativa TINYINT(1) NOT NULL DEFAULT 1,
    pausada_em TIMESTAMP(6) NULL,
    criada_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_recorrencia_cliente_descricao (cliente_id, descricao),
    KEY ix_recorrencia_ativa (ativa),
    CONSTRAINT fk_recorrencia_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_recorrencia_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT fk_recorrencia_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);

CREATE TABLE recorrencia_ocorrencia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    recorrencia_id BIGINT NOT NULL,
    ano SMALLINT NOT NULL,
    mes TINYINT NOT NULL,
    lancamento_id BIGINT NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ocorrencia_recorrencia_mes (cliente_id, recorrencia_id, ano, mes),
    CONSTRAINT fk_ocorrencia_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_ocorrencia_recorrencia FOREIGN KEY (recorrencia_id) REFERENCES recorrencia (id),
    CONSTRAINT fk_ocorrencia_lancamento FOREIGN KEY (lancamento_id) REFERENCES lancamento (id)
);
