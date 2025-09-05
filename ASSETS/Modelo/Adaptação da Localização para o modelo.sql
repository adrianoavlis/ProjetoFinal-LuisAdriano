/* ===================================================================
   País → Estado → Município → Localização + Backfill Supermercado
   Banco: SQL Server (T-SQL)
   Autor: ChatGPT (GPT-5 Thinking)
   Objetivo:
     1) Adicionar camada ESTADO (UF) ao modelo geográfico.
     2) Ajustar Município para referenciar Estado (em vez de Cidade).
     3) Ajustar Localização para usar id_estado (em vez de id_cidade).
     4) Popular base com BR + UFs (opcional, recomendado).
     5) Fazer backfill do campo textual Supermercado.localizacao → Localizacao + id_localizacao.
   Observações:
     - O parsing do endereço é melhor-esforço; revise linhas com parse_status <> 'OK'.
     - O script é idempotente (pode ser executado mais de uma vez).
   =================================================================== */

SET NOCOUNT ON;
------------------------------------------------------------
-- 0) PRÉ-REQUISITOS MÍNIMOS
------------------------------------------------------------
IF DB_ID(N'cesta_basica') IS NOT NULL
BEGIN
  USE cesta_basica;
END
GO

------------------------------------------------------------
-- 1) TABELA PAÍS (caso não exista)
------------------------------------------------------------
IF OBJECT_ID('dbo.Pais','U') IS NULL
BEGIN
  CREATE TABLE dbo.Pais (
    id_pais     INT IDENTITY(1,1) PRIMARY KEY,
    nome        NVARCHAR(120) NOT NULL,
    iso2        CHAR(2)       NULL,
    iso3        CHAR(3)       NULL,
    sigla       NVARCHAR(10)  NULL,
    dt_criacao  DATETIME2(0)  NOT NULL CONSTRAINT DF_Pais_dt_criacao DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_Pais_nome UNIQUE (nome),
    CONSTRAINT UQ_Pais_iso2 UNIQUE (iso2),
    CONSTRAINT UQ_Pais_iso3 UNIQUE (iso3)
  );
  CREATE INDEX IX_Pais_nome ON dbo.Pais(nome);
END
GO

-- Seed BR
IF NOT EXISTS (SELECT 1 FROM dbo.Pais WHERE iso2='BR')
BEGIN
  INSERT INTO dbo.Pais(nome, iso2, iso3, sigla) VALUES (N'Brasil','BR','BRA','BR');
END
GO

------------------------------------------------------------
-- 2) TABELA ESTADO (UF) – NOVA
------------------------------------------------------------
IF OBJECT_ID('dbo.Estado','U') IS NULL
BEGIN
  CREATE TABLE dbo.Estado (
    id_estado   INT IDENTITY(1,1) PRIMARY KEY,
    id_pais     INT          NOT NULL,
    nome        NVARCHAR(120) NOT NULL,
    uf          CHAR(2)      NOT NULL,
    codigo_ibge INT          NULL,
    dt_criacao  DATETIME2(0) NOT NULL CONSTRAINT DF_Estado_dt_criacao DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Estado_Pais FOREIGN KEY (id_pais) REFERENCES dbo.Pais(id_pais),
    CONSTRAINT UQ_Estado_uf_pais UNIQUE (id_pais, uf)
  );
  CREATE INDEX IX_Estado_nome ON dbo.Estado(nome);
END
GO

-- Seed 27 UFs do Brasil (se não existirem)
DECLARE @id_pais_br INT = (SELECT TOP(1) id_pais FROM dbo.Pais WHERE iso2='BR');
IF NOT EXISTS (SELECT 1 FROM dbo.Estado WHERE id_pais = @id_pais_br)
BEGIN
  INSERT INTO dbo.Estado(id_pais, nome, uf) VALUES
  (@id_pais_br,N'Acre','AC'),(@id_pais_br,N'Alagoas','AL'),(@id_pais_br,N'Amapá','AP'),
  (@id_pais_br,N'Amazonas','AM'),(@id_pais_br,N'Bahia','BA'),(@id_pais_br,N'Ceará','CE'),
  (@id_pais_br,N'Distrito Federal','DF'),(@id_pais_br,N'Espírito Santo','ES'),
  (@id_pais_br,N'Goiás','GO'),(@id_pais_br,N'Maranhão','MA'),(@id_pais_br,N'Mato Grosso','MT'),
  (@id_pais_br,N'Mato Grosso do Sul','MS'),(@id_pais_br,N'Minas Gerais','MG'),
  (@id_pais_br,N'Pará','PA'),(@id_pais_br,N'Paraíba','PB'),(@id_pais_br,N'Paraná','PR'),
  (@id_pais_br,N'Pernambuco','PE'),(@id_pais_br,N'Piauí','PI'),(@id_pais_br,N'Rio de Janeiro','RJ'),
  (@id_pais_br,N'Rio Grande do Norte','RN'),(@id_pais_br,N'Rio Grande do Sul','RS'),
  (@id_pais_br,N'Rondônia','RO'),(@id_pais_br,N'Roraima','RR'),(@id_pais_br,N'Santa Catarina','SC'),
  (@id_pais_br,N'São Paulo','SP'),(@id_pais_br,N'Sergipe','SE'),(@id_pais_br,N'Tocantins','TO');
END
GO

------------------------------------------------------------
-- 3) TABELAS CIDADE/MUNICÍPIO/LOCALIZAÇÃO (situação atual)
--    Vamos padronizar para País → Estado → Município → Localização
------------------------------------------------------------

-- Se ainda existir 'Cidade', manteremos apenas para migração.
-- Se não existir, seguimos adiante (modelo já atualizado).
IF OBJECT_ID('dbo.Cidade','U') IS NOT NULL
BEGIN
  -- Garante colunas esperadas (nome, uf, id_pais)
  IF COL_LENGTH('dbo.Cidade','nome') IS NULL OR COL_LENGTH('dbo.Cidade','id_pais') IS NULL
    RAISERROR('Tabela Cidade não possui colunas esperadas (nome, id_pais, uf). Ajuste ou remova antes da migração.',16,1);
END
GO

-- MUNICÍPIO: criar/ajustar estrutura para referenciar ESTADO
IF OBJECT_ID('dbo.Municipio','U') IS NULL
BEGIN
  CREATE TABLE dbo.Municipio (
    id_municipio  INT IDENTITY(1,1) PRIMARY KEY,
    nome          NVARCHAR(140) NOT NULL,
    id_estado     INT           NOT NULL,
    codigo_ibge   INT           NULL,
    dt_criacao    DATETIME2(0)  NOT NULL CONSTRAINT DF_Municipio_dt_criacao DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Municipio_Estado FOREIGN KEY (id_estado) REFERENCES dbo.Estado(id_estado),
    CONSTRAINT UQ_Municipio_nome_estado UNIQUE (nome, id_estado)
  );
  CREATE INDEX IX_Municipio_nome   ON dbo.Municipio(nome);
  CREATE INDEX IX_Municipio_estado ON dbo.Municipio(id_estado);
END
ELSE
BEGIN
  -- Já existe: garantir coluna id_estado e FK; remover dependência de Cidade se houver
  IF COL_LENGTH('dbo.Municipio','id_estado') IS NULL
    ALTER TABLE dbo.Municipio ADD id_estado INT NULL;

  -- Migrar id_estado a partir de Cidade (se houver relacionamento antigo)
  IF OBJECT_ID('dbo.Cidade','U') IS NOT NULL AND COL_LENGTH('dbo.Municipio','id_cidade') IS NOT NULL
  BEGIN
    -- Mapear UF -> Estado
    ;WITH C AS (
      SELECT c.id_cidade, c.uf, c.id_pais
      FROM dbo.Cidade c
    )
    UPDATE m
      SET id_estado = e.id_estado
    FROM dbo.Municipio m
    JOIN C ON C.id_cidade = m.id_cidade
    JOIN dbo.Estado e ON e.uf = C.uf AND e.id_pais = C.id_pais;
  END

  -- Se ainda houver nulos, defina RJ por padrão (ajuste manual depois)
  IF EXISTS (SELECT 1 FROM dbo.Municipio WHERE id_estado IS NULL)
  BEGIN
    DECLARE @id_estado_rj INT = (SELECT TOP(1) id_estado FROM dbo.Estado WHERE uf='RJ' AND id_pais=@id_pais_br);
    UPDATE dbo.Municipio SET id_estado = ISNULL(id_estado, @id_estado_rj);
  END

  -- Tornar NOT NULL e criar FK (se ainda não existir)
  IF COL_LENGTH('dbo.Municipio','id_estado') IS NOT NULL
  BEGIN
    ALTER TABLE dbo.Municipio WITH CHECK ADD CONSTRAINT FK_Municipio_Estado FOREIGN KEY (id_estado) REFERENCES dbo.Estado(id_estado);
    ALTER TABLE dbo.Municipio ALTER COLUMN id_estado INT NOT NULL;
  END

  -- Remover FK e coluna id_cidade se existirem
  DECLARE @fkMunicipioCidade SYSNAME;
  SELECT @fkMunicipioCidade = fk.name
  FROM sys.foreign_keys fk
  WHERE fk.parent_object_id = OBJECT_ID('dbo.Municipio')
    AND fk.referenced_object_id = OBJECT_ID('dbo.Cidade');
  IF @fkMunicipioCidade IS NOT NULL
    EXEC('ALTER TABLE dbo.Municipio DROP CONSTRAINT ' + QUOTENAME(@fkMunicipioCidade));

  IF COL_LENGTH('dbo.Municipio','id_cidade') IS NOT NULL
    ALTER TABLE dbo.Municipio DROP COLUMN id_cidade;
END
GO

-- LOCALIZAÇÃO: garantir colunas e FKs para País/Estado/Município
IF OBJECT_ID('dbo.Localizacao','U') IS NULL
BEGIN
  CREATE TABLE dbo.Localizacao (
    id_localizacao INT IDENTITY(1,1) PRIMARY KEY,
    logradouro     NVARCHAR(160) NOT NULL,
    numero         NVARCHAR(20)  NULL,
    complemento    NVARCHAR(120) NULL,
    bairro         NVARCHAR(120) NULL,
    cep            VARCHAR(9)    NOT NULL,
    id_municipio   INT           NOT NULL,
    id_estado      INT           NOT NULL,
    id_pais        INT           NOT NULL,
    dt_criacao     DATETIME2(0)  NOT NULL CONSTRAINT DF_Localizacao_dt_criacao DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Localizacao_Municipio FOREIGN KEY (id_municipio) REFERENCES dbo.Municipio(id_municipio),
    CONSTRAINT FK_Localizacao_Estado    FOREIGN KEY (id_estado)    REFERENCES dbo.Estado(id_estado),
    CONSTRAINT FK_Localizacao_Pais      FOREIGN KEY (id_pais)      REFERENCES dbo.Pais(id_pais),
    CONSTRAINT CK_Localizacao_CEP_format CHECK (
        cep LIKE '[0-9][0-9][0-9][0-9][0-9]-[0-9][0-9][0-9]'
        OR (cep NOT LIKE '%[^0-9]%' AND LEN(cep) = 8)
    )
  );
  CREATE INDEX IX_Localizacao_cep  ON dbo.Localizacao(cep);
  CREATE INDEX IX_Localizacao_refs ON dbo.Localizacao(id_pais, id_estado, id_municipio);
END
ELSE
BEGIN
  -- Já existe: acrescentar id_estado, remover id_cidade, criar FK/índices
  IF COL_LENGTH('dbo.Localizacao','id_estado') IS NULL
    ALTER TABLE dbo.Localizacao ADD id_estado INT NULL;

  -- Backfill id_estado a partir do município
  UPDATE L
    SET id_estado = M.id_estado
  FROM dbo.Localizacao L
  JOIN dbo.Municipio M ON M.id_municipio = L.id_municipio
  WHERE L.id_estado IS NULL;

  -- FK e NOT NULL
  ALTER TABLE dbo.Localizacao WITH CHECK ADD CONSTRAINT FK_Localizacao_Estado FOREIGN KEY (id_estado) REFERENCES dbo.Estado(id_estado);
  ALTER TABLE dbo.Localizacao ALTER COLUMN id_estado INT NOT NULL;

  -- Remover FK/coluna id_cidade se existirem
  DECLARE @fkLocCidade SYSNAME;
  SELECT @fkLocCidade = fk.name
  FROM sys.foreign_keys fk
  WHERE fk.parent_object_id = OBJECT_ID('dbo.Localizacao')
    AND fk.referenced_object_id = OBJECT_ID('dbo.Cidade');
  IF @fkLocCidade IS NOT NULL
    EXEC('ALTER TABLE dbo.Localizacao DROP CONSTRAINT ' + QUOTENAME(@fkLocCidade));
  IF COL_LENGTH('dbo.Localizacao','id_cidade') IS NOT NULL
    ALTER TABLE dbo.Localizacao DROP COLUMN id_cidade;

  IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Localizacao_cep' AND object_id=OBJECT_ID('dbo.Localizacao'))
    CREATE INDEX IX_Localizacao_cep  ON dbo.Localizacao(cep);
  IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='IX_Localizacao_refs' AND object_id=OBJECT_ID('dbo.Localizacao'))
    CREATE INDEX IX_Localizacao_refs ON dbo.Localizacao(id_pais, id_estado, id_municipio);
END
GO

-- TRIGGER de consistência: Localizacao.id_municipio deve pertencer ao id_estado/id_pais
IF OBJECT_ID('dbo.trg_Localizacao_check','TR') IS NOT NULL
  DROP TRIGGER dbo.trg_Localizacao_check;
GO
CREATE TRIGGER dbo.trg_Localizacao_check
ON dbo.Localizacao
AFTER INSERT, UPDATE
AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (
    SELECT 1
    FROM inserted i
    JOIN dbo.Municipio m ON m.id_municipio = i.id_municipio
    JOIN dbo.Estado    e ON e.id_estado    = m.id_estado
    WHERE (i.id_estado <> e.id_estado) OR (i.id_pais <> e.id_pais)
  )
  BEGIN
    RAISERROR(N'Inconsistência geográfica: id_municipio não pertence ao id_estado/id_pais informados.',16,1);
    ROLLBACK TRANSACTION;
    RETURN;
  END
END
GO

-- Após migração, Cidade pode ser descartada (opcional)
-- DROP TABLE dbo.Cidade; -- descomente se não houver mais dependências
GO

------------------------------------------------------------
-- 4) BACKFILL Supermercado.localizacao → Localizacao
------------------------------------------------------------

-- 4.1) Segurança: garantir coluna id_localizacao em Supermercado
IF COL_LENGTH('dbo.Supermercado','id_localizacao') IS NULL
BEGIN
  ALTER TABLE dbo.Supermercado ADD id_localizacao INT NULL;
  ALTER TABLE dbo.Supermercado WITH CHECK ADD CONSTRAINT FK_Supermercado_Localizacao
    FOREIGN KEY (id_localizacao) REFERENCES dbo.Localizacao(id_localizacao);
  CREATE INDEX IX_Supermercado_id_localizacao ON dbo.Supermercado(id_localizacao);
END
GO

-- 4.2) Função utilitária: apenas dígitos
IF OBJECT_ID('dbo.fn_only_digits','FN') IS NOT NULL
  DROP FUNCTION dbo.fn_only_digits;
GO
CREATE FUNCTION dbo.fn_only_digits (@s NVARCHAR(MAX))
RETURNS VARCHAR(4000)
AS
BEGIN
  DECLARE @i INT = 1, @len INT = LEN(@s), @out VARCHAR(4000) = '';
  WHILE @i <= @len
  BEGIN
    DECLARE @ch NCHAR(1) = SUBSTRING(@s, @i, 1);
    IF @ch LIKE N'[0-9]' SET @out += CONVERT(VARCHAR(1), @ch);
    SET @i += 1;
  END
  RETURN @out;
END
GO

-- 4.3) Stage para parsing (execute, revise, e depois finalize)
IF OBJECT_ID('dbo.Super_Localizacao_Stage','U') IS NOT NULL DROP TABLE dbo.Super_Localizacao_Stage;
GO
CREATE TABLE dbo.Super_Localizacao_Stage (
  id_supermercado INT PRIMARY KEY,
  raw_localizacao NVARCHAR(255) NOT NULL,
  logradouro      NVARCHAR(160) NULL,
  numero          NVARCHAR(20)  NULL,
  bairro          NVARCHAR(120) NULL,
  municipio_nome  NVARCHAR(140) NULL,
  uf              CHAR(2)       NULL,
  cep             VARCHAR(9)    NULL,
  parse_status    VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
);

INSERT INTO dbo.Super_Localizacao_Stage (id_supermercado, raw_localizacao)
SELECT s.id_supermercado, s.localizacao
FROM dbo.Supermercado s
WHERE s.localizacao IS NOT NULL AND (s.id_localizacao IS NULL OR s.id_localizacao = 0);
GO

-- 4.4) Heurística de parsing (melhor-esforço)
-- CEP: pega últimos 8 dígitos e formata XXXXX-XXX (se houver)
UPDATE st
SET cep = CASE
            WHEN LEN(digits) >= 8 THEN STUFF(STUFF(RIGHT(digits,8),6,0,'-'),1,0,'')
            ELSE NULL
          END
FROM dbo.Super_Localizacao_Stage st
CROSS APPLY (SELECT dbo.fn_only_digits(st.raw_localizacao) AS digits) d;

-- UF: tenta casar qualquer UF existente no texto (prioriza final da string)
UPDATE st
SET uf = e.uf
FROM dbo.Super_Localizacao_Stage st
CROSS APPLY (SELECT TOP 1 e.uf
             FROM dbo.Estado e
             WHERE st.raw_localizacao LIKE '%' + e.uf + '%'
             ORDER BY CASE WHEN RIGHT(RTRIM(st.raw_localizacao), 2) = e.uf THEN 0 ELSE 1 END) ca(uf)
WHERE st.uf IS NULL;

-- LOGRADOURO / NÚMERO / BAIRRO / MUNICÍPIO (padrão comum: "Rua X, 123 - Bairro Y, Cidade Z - UF, CEP")
UPDATE st
SET 
  logradouro = LTRIM(RTRIM(LEFT(st.raw_localizacao, NULLIF(NULLIF(NULLIF(CHARINDEX(',', st.raw_localizacao),0),0),0)-1))),
  numero = LTRIM(RTRIM(
              CASE 
                WHEN CHARINDEX(',', st.raw_localizacao) > 0 
                THEN LEFT(SUBSTRING(st.raw_localizacao, CHARINDEX(',', st.raw_localizacao)+1, 50),
                          NULLIF(CHARINDEX(' ', SUBSTRING(st.raw_localizacao, CHARINDEX(',', st.raw_localizacao)+1, 50)+' ') -1, -1))
                ELSE NULL
              END)),
  bairro = NULL, -- difícil inferir com segurança sem padrão; ajuste manual se desejar
  municipio_nome = 
    LTRIM(RTRIM(
      CASE 
        WHEN st.uf IS NOT NULL AND CHARINDEX('-', st.raw_localizacao) > 0 
          THEN 
            -- trecho entre última vírgula antes do " - UF" e " - UF"
            LEFT(
              RIGHT(st.raw_localizacao, LEN(st.raw_localizacao) - CHARINDEX(',', st.raw_localizacao)),
              NULLIF(CHARINDEX(' - ' + st.uf, RIGHT(st.raw_localizacao, LEN(st.raw_localizacao) - CHARINDEX(',', st.raw_localizacao))) -1, -1)
            )
        ELSE NULL
      END))
WHERE st.logradouro IS NULL;

-- Marcar linhas que conseguiram CEP e UF pelo menos
UPDATE st
SET parse_status = CASE WHEN cep IS NOT NULL AND uf IS NOT NULL THEN 'OK' ELSE 'REVIEW' END
FROM dbo.Super_Localizacao_Stage st;

------------------------------------------------------------
-- 4.5) Convergir Stage → Localizacao (criando Estado/Município se preciso)
------------------------------------------------------------

-- Garante que Estados do BR já existem (feito acima). Criar Municípios ausentes:
;WITH muni AS (
  SELECT DISTINCT
    mn = NULLIF(LTRIM(RTRIM(st.municipio_nome)),''), 
    uf = st.uf
  FROM dbo.Super_Localizacao_Stage st
  WHERE st.parse_status IN ('OK','REVIEW') AND st.uf IS NOT NULL
)
INSERT INTO dbo.Municipio (nome, id_estado)
SELECT m.mn, e.id_estado
FROM muni m
JOIN dbo.Estado e ON e.uf = m.uf AND e.id_pais = @id_pais_br
LEFT JOIN dbo.Municipio mx ON mx.nome = m.mn AND mx.id_estado = e.id_estado
WHERE m.mn IS NOT NULL AND mx.id_municipio IS NULL;

-- Inserir Localização (evitar duplicados por (logradouro, numero, cep, municipio))
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name='UQ_Localizacao_unique' AND object_id = OBJECT_ID('dbo.Localizacao'))
BEGIN
  CREATE UNIQUE INDEX UQ_Localizacao_unique ON dbo.Localizacao (logradouro, numero, cep, id_municipio);
END

;WITH ref AS (
  SELECT
    st.id_supermercado,
    st.logradouro,
    st.numero,
    st.cep,
    e.id_estado,
    e.id_pais,
    m.id_municipio
  FROM dbo.Super_Localizacao_Stage st
  JOIN dbo.Estado e ON e.uf = st.uf AND e.id_pais = @id_pais_br
  JOIN dbo.Municipio m ON m.nome = st.municipio_nome AND m.id_estado = e.id_estado
  WHERE st.parse_status IN ('OK','REVIEW') AND st.logradouro IS NOT NULL AND st.cep IS NOT NULL
)
-- insert faltantes
INSERT INTO dbo.Localizacao (logradouro, numero, cep, id_municipio, id_estado, id_pais)
SELECT r.logradouro, r.numero, r.cep, r.id_municipio, r.id_estado, r.id_pais
FROM ref r
LEFT JOIN dbo.Localizacao l ON l.logradouro = r.logradouro
                           AND ISNULL(l.numero,'') = ISNULL(r.numero,'')
                           AND l.cep = r.cep
                           AND l.id_municipio = r.id_municipio
WHERE l.id_localizacao IS NULL;

-- Atualizar Supermercado.id_localizacao
;WITH ref2 AS (
  SELECT
    st.id_supermercado,
    l.id_localizacao
  FROM dbo.Super_Localizacao_Stage st
  JOIN dbo.Estado e ON e.uf = st.uf AND e.id_pais = @id_pais_br
  JOIN dbo.Municipio m ON m.nome = st.municipio_nome AND m.id_estado = e.id_estado
  JOIN dbo.Localizacao l ON l.logradouro = st.logradouro
                        AND ISNULL(l.numero,'') = ISNULL(st.numero,'')
                        AND l.cep = st.cep
                        AND l.id_municipio = m.id_municipio
)
UPDATE s
SET s.id_localizacao = r.id_localizacao
FROM dbo.Supermercado s
JOIN ref2 r ON r.id_supermercado = s.id_supermercado
WHERE s.id_localizacao IS NULL OR s.id_localizacao = 0;

------------------------------------------------------------
-- 4.6) Relatório rápido do backfill
------------------------------------------------------------
SELECT parse_status, COUNT(*) AS qtde
FROM dbo.Super_Localizacao_Stage
GROUP BY parse_status;

-- Ver linhas que precisam de revisão
SELECT TOP (100) *
FROM dbo.Super_Localizacao_Stage
WHERE parse_status = 'REVIEW'
ORDER BY id_supermercado;

-- Após revisar e ajustar manualmente os campos na Stage, você pode reexecutar a partir da seção 4.5
-- para refletir as correções em Localizacao e no Supermercado.id_localizacao.

/* ==========================================================
   5) (Opcional) Após validação, desativar coluna textual antiga
   ==========================================================
-- ALTER TABLE dbo.Supermercado DROP COLUMN localizacao;
*/
