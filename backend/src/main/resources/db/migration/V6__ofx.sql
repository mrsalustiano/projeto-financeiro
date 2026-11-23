-- Fase 5: importação de OFX e regras de classificação por cliente.

ALTER TABLE lancamento
    ADD COLUMN fitid VARCHAR(255) NULL,
    MODIFY COLUMN categoria_id BIGINT NULL,
    ADD UNIQUE KEY uk_lancamento_fitid (cliente_id, conta_id, fitid);

CREATE TABLE regra_classificacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    termo VARCHAR(80) NOT NULL,
    dia_inicio TINYINT NOT NULL,
    dia_fim TINYINT NOT NULL,
    valor_minimo DECIMAL(15, 2) NOT NULL,
    valor_maximo DECIMAL(15, 2) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_regra_cliente_termo (cliente_id, termo, dia_inicio, dia_fim),
    CONSTRAINT fk_regra_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);
