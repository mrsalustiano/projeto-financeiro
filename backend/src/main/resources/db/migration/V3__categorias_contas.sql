CREATE TABLE categoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    nome VARCHAR(80) NOT NULL,
    natureza VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categoria_cliente_nome (cliente_id, nome),
    CONSTRAINT fk_categoria_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE TABLE conta (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    nome VARCHAR(80) NOT NULL,
    banco VARCHAR(80) NOT NULL,
    codigo_banco VARCHAR(10) NULL,
    identificador_conta VARCHAR(64) NULL,
    tipo VARCHAR(20) NOT NULL,
    saldo_inicial DECIMAL(15, 2) NOT NULL,
    data_saldo DATE NOT NULL,
    limite DECIMAL(15, 2) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_conta_cliente_nome (cliente_id, nome),
    CONSTRAINT fk_conta_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);
