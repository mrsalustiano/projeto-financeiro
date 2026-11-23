CREATE TABLE auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NULL,
    usuario_id BIGINT NULL,
    usuario_email VARCHAR(255) NULL,
    usuario_perfil VARCHAR(20) NULL,
    acao VARCHAR(20) NOT NULL,
    recurso VARCHAR(60) NOT NULL,
    recurso_id BIGINT NULL,
    dados_antes JSON NULL,
    dados_depois JSON NULL,
    criado_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_auditoria_cliente (cliente_id, id),
    KEY ix_auditoria_recurso (recurso, id),
    KEY ix_auditoria_criado (criado_em)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
