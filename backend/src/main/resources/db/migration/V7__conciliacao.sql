-- Fase 6: conciliação entre lançamento e item de extrato, e comprovante fora do banco.

CREATE TABLE conciliacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    conta_id BIGINT NOT NULL,
    lancamento_id BIGINT NOT NULL,
    extrato_id BIGINT NOT NULL,
    conciliado_em TIMESTAMP(6) NOT NULL,
    conciliado_por VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conciliacao_lancamento (cliente_id, lancamento_id),
    UNIQUE KEY uk_conciliacao_extrato (cliente_id, extrato_id),
    CONSTRAINT fk_conciliacao_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_conciliacao_conta FOREIGN KEY (conta_id) REFERENCES conta (id),
    CONSTRAINT fk_conciliacao_lancamento FOREIGN KEY (lancamento_id) REFERENCES lancamento (id) ON DELETE CASCADE,
    CONSTRAINT fk_conciliacao_extrato FOREIGN KEY (extrato_id) REFERENCES lancamento (id) ON DELETE CASCADE
);

CREATE TABLE comprovante (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    lancamento_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    tamanho BIGINT NOT NULL,
    storage_key VARCHAR(120) NOT NULL,
    criado_em TIMESTAMP(6) NOT NULL,
    criado_por VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_comprovante_storage (storage_key),
    KEY ix_comprovante_lancamento (cliente_id, lancamento_id),
    CONSTRAINT fk_comprovante_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_comprovante_lancamento FOREIGN KEY (lancamento_id) REFERENCES lancamento (id) ON DELETE CASCADE
);
