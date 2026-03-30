CREATE TABLE IF NOT EXISTS contas (
    id bigserial PRIMARY KEY,
    numero int NOT NULL UNIQUE,
    titular varchar(255) NOT NULL,
    saldo double precision NOT NULL DEFAULT 0,
    tipo varchar(30) NOT NULL,
    criado_em timestamptz NOT NULL DEFAULT now(),
    atualizado_em timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_status CHECK ( tipo IN ('POUPANCA', 'CORRENTE') )
);

CREATE INDEX idx_contas_numero_titular_tipo
    ON contas(numero, titular, tipo);

CREATE OR REPLACE FUNCTION set_atualizado_em()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_atualizado_em
BEFORE UPDATE ON contas
FOR EACH ROW EXECUTE FUNCTION set_atualizado_em();

CREATE TABLE IF NOT EXISTS historico_operacoes (
    id bigserial PRIMARY KEY,
    conta_id bigint NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
    descricao text NOT NULL,
    data_hora timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS funcionarios (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL UNIQUE,
    senha varchar(255) NOT NULL,
    cargo varchar(100) NOT NULL,
    agencia varchar(50),
    criado_em timestamptz NOT NULL DEFAULT now(),
    atualizado_em timestamptz NOT NULL DEFAULT now()
);

CREATE SEQUENCE IF NOT EXISTS conta_numero_seq
    START WITH 10001
    INCREMENT BY 1
    NO CYCLE;