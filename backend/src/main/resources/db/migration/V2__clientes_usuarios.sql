-- Sessão fica nesta versão: a fase 1 precisa dela antes de anexo e idempotência.

CREATE TABLE cliente (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(120) NOT NULL,
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT NULL,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    falhas_login INT NOT NULL DEFAULT 0,
    bloqueado_ate TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_email (email),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE TABLE refresh_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    hash CHAR(64) NOT NULL,
    expira_em TIMESTAMP(6) NOT NULL,
    revogado TINYINT(1) NOT NULL DEFAULT 0,
    jti_access CHAR(36) NOT NULL,
    access_expira_em TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_hash (hash),
    CONSTRAINT fk_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE access_token_revogado (
    jti CHAR(36) NOT NULL,
    expira_em TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (jti)
);
