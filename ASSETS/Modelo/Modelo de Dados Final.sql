

/* ==========================================================
   ESQUEMAS
   ========================================================== */

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'ref')     EXEC('CREATE SCHEMA ref');
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'core')    EXEC('CREATE SCHEMA core');
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'ingest')  EXEC('CREATE SCHEMA ingest');
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'analytics') EXEC('CREATE SCHEMA analytics');
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'sec')     EXEC('CREATE SCHEMA sec');
GO




/* ==========================================================
   DIMENSÕES / REFERÊNCIAS (ref)
   ========================================================== */

IF OBJECT_ID('ref.Pais') IS NOT NULL DROP TABLE ref.Pais;
CREATE TABLE ref.Pais (
    id_pais       INT IDENTITY(1,1)  PRIMARY KEY,
    nome          NVARCHAR(120)      NOT NULL,
    sigla_iso2    CHAR(2)            NULL,
    sigla_iso3    CHAR(3)            NULL,
    dt_criacao    DATETIME2(0)       NOT NULL CONSTRAINT DF_refPais_dt_criacao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT UQ_refPais_nome UNIQUE (nome)
);

IF OBJECT_ID('ref.Estado') IS NOT NULL DROP TABLE ref.Estado;
CREATE TABLE ref.Estado (
    id_estado     INT IDENTITY(1,1)  PRIMARY KEY,
    id_pais       INT                NOT NULL,
    nome          NVARCHAR(120)      NOT NULL,
    sigla_uf      CHAR(2)            NOT NULL,
    dt_criacao    DATETIME2(0)       NOT NULL CONSTRAINT DF_refEstado_dt_criacao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_refEstado_Pais FOREIGN KEY (id_pais) REFERENCES ref.Pais(id_pais),
    CONSTRAINT UQ_refEstado_pais_uf UNIQUE (id_pais, sigla_uf)
);

IF OBJECT_ID('ref.Municipio') IS NOT NULL DROP TABLE ref.Municipio;
CREATE TABLE ref.Municipio (
    id_municipio  INT IDENTITY(1,1)  PRIMARY KEY,
    id_estado     INT                NOT NULL,
    nome          NVARCHAR(200)      NOT NULL,
    codigo_ibge   INT                NULL,
    dt_criacao    DATETIME2(0)       NOT NULL CONSTRAINT DF_refMunicipio_dt_criacao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_refMunicipio_Estado FOREIGN KEY (id_estado) REFERENCES ref.Estado(id_estado),
    CONSTRAINT UQ_refMunicipio_estado_nome UNIQUE (id_estado, nome)
);

IF OBJECT_ID('ref.UnidadeMedida') IS NOT NULL DROP TABLE ref.UnidadeMedida;
CREATE TABLE ref.UnidadeMedida (
    id_unidade    INT IDENTITY(1,1)  PRIMARY KEY,
    sigla         NVARCHAR(20)       NOT NULL,   -- ex: kg, g, L, ml, un
    descricao     NVARCHAR(100)      NOT NULL,
    fator_base    DECIMAL(18,6)      NULL,       -- conversão para unidade base do tipo (opcional)
    grupo         NVARCHAR(50)       NULL,       -- ex: MASSA, VOLUME, UNIDADE
    CONSTRAINT UQ_refUnid_sigla UNIQUE (sigla)
);

IF OBJECT_ID('ref.Marca') IS NOT NULL DROP TABLE ref.Marca;
CREATE TABLE ref.Marca (
    id_marca      INT IDENTITY(1,1)  PRIMARY KEY,
    nome          NVARCHAR(120)      NOT NULL,
    CONSTRAINT UQ_refMarca_nome UNIQUE (nome)
);

IF OBJECT_ID('ref.Categoria') IS NOT NULL DROP TABLE ref.Categoria;
CREATE TABLE ref.Categoria (
    id_categoria  INT IDENTITY(1,1)  PRIMARY KEY,
    nome          NVARCHAR(120)      NOT NULL,
    caminho       NVARCHAR(400)      NULL,  -- ex: "Alimentos > Grãos > Arroz"
    CONSTRAINT UQ_refCategoria_nome UNIQUE (nome)
);

/* Perfis/ranges para filtro de público */
IF OBJECT_ID('ref.FaixaEtaria') IS NOT NULL DROP TABLE ref.FaixaEtaria;
CREATE TABLE ref.FaixaEtaria (
    id_faixa      INT IDENTITY(1,1)  PRIMARY KEY,
    descricao     NVARCHAR(80)       NOT NULL,  -- ex: 18-24, 25-34
    idade_min     INT                NULL,
    idade_max     INT                NULL,
    CONSTRAINT UQ_refFaixaEtaria UNIQUE (descricao)
);

IF OBJECT_ID('ref.FaixaRenda') IS NOT NULL DROP TABLE ref.FaixaRenda;
CREATE TABLE ref.FaixaRenda (
    id_faixa      INT IDENTITY(1,1)  PRIMARY KEY,
    descricao     NVARCHAR(120)      NOT NULL,  -- ex: até 2 SM, 2-5 SM
    renda_min     DECIMAL(18,2)      NULL,
    renda_max     DECIMAL(18,2)      NULL,
    CONSTRAINT UQ_refFaixaRenda UNIQUE (descricao)
);

/* ==========================================================
   NÚCLEO DO NEGÓCIO (core)
   ========================================================== */
IF OBJECT_ID('core.Localizacao') IS NOT NULL DROP TABLE core.Localizacao;
CREATE TABLE core.Localizacao (
    id_localizacao INT IDENTITY(1,1) PRIMARY KEY,
    id_municipio   INT               NOT NULL,
    logradouro     NVARCHAR(200)     NULL,
    numero         NVARCHAR(20)      NULL,
    cep            NVARCHAR(20)      NULL,
    complemento    NVARCHAR(120)     NULL,
    bairro         NVARCHAR(120)     NULL,
    dt_criacao     DATETIME2(0)      NOT NULL CONSTRAINT DF_coreLoc_dt_criacao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_coreLoc_Municipio FOREIGN KEY (id_municipio) REFERENCES ref.Municipio(id_municipio)
);
CREATE INDEX IX_coreLoc_municipio ON core.Localizacao(id_municipio);

IF OBJECT_ID('core.RedeVarejo') IS NOT NULL DROP TABLE core.RedeVarejo;
CREATE TABLE core.RedeVarejo (
    id_rede        INT IDENTITY(1,1) PRIMARY KEY,
    nome           NVARCHAR(200)     NOT NULL,   -- ex: Supermarket, Guanabara
    url_site       NVARCHAR(400)     NULL,
    cnpj           VARCHAR(18)       NULL,
    dt_criacao     DATETIME2(0)      NOT NULL CONSTRAINT DF_coreRede_dt_criacao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT UQ_coreRede_nome UNIQUE (nome)
);

IF OBJECT_ID('core.Loja') IS NOT NULL DROP TABLE core.Loja;
CREATE TABLE core.Loja (
    id_loja        INT IDENTITY(1,1) PRIMARY KEY,
    id_rede        INT               NOT NULL,
    id_localizacao INT               NOT NULL,
    nome_fantasia  NVARCHAR(200)     NULL,       -- opcional: identificar a filial
 --   codigo_interno NVARCHAR(50)      NULL,       -- código da rede
    dt_criacao     DATETIME2(0)      NOT NULL CONSTRAINT DF_coreLoja_dt_criacao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_coreLoja_Rede FOREIGN KEY (id_rede) REFERENCES core.RedeVarejo(id_rede),
    CONSTRAINT FK_coreLoja_Loc  FOREIGN KEY (id_localizacao) REFERENCES core.Localizacao(id_localizacao)
 --   CONSTRAINT UQ_coreLoja_rede_codigo UNIQUE (id_rede, ISNULL(codigo_interno,''))  -- evita duplicidade
);
CREATE INDEX IX_coreLoja_rede ON core.Loja(id_rede);


******************************************************************


IF OBJECT_ID('core.Produto') IS NOT NULL DROP TABLE core.Produto;
CREATE TABLE core.Produto (
    id_produto          INT IDENTITY(1,1) PRIMARY KEY,
    nome                NVARCHAR(240)     NOT NULL,
    id_marca            INT               NULL,
    id_categoria        INT               NULL,
    id_unidade_base     INT               NOT NULL,         -- unidade base para comparação (ex: g, ml, un)
    qtde_por_unidade    DECIMAL(18,6)     NOT NULL DEFAULT (1),  -- ex: 5 (kg) ou 1000 (ml)
    id_unidade_embalagem INT              NULL,             -- ex: unidade da embalagem exibida
    gtin                NVARCHAR(32)      NULL,             -- EAN/UPC
    ncm                 NVARCHAR(16)      NULL,
    ativo               BIT               NOT NULL DEFAULT (1),
    dt_criacao          DATETIME2(0)      NOT NULL CONSTRAINT DF_coreProd_dt_criacao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT FK_coreProd_Marca     FOREIGN KEY (id_marca)     REFERENCES ref.Marca(id_marca),
    CONSTRAINT FK_coreProd_Categoria FOREIGN KEY (id_categoria) REFERENCES ref.Categoria(id_categoria),
    CONSTRAINT FK_coreProd_UnidBase  FOREIGN KEY (id_unidade_base) REFERENCES ref.UnidadeMedida(id_unidade),
    CONSTRAINT FK_coreProd_UnidEmb   FOREIGN KEY (id_unidade_embalagem) REFERENCES ref.UnidadeMedida(id_unidade)
);
CREATE UNIQUE INDEX UQ_coreProduto_gtin ON core.Produto(gtin) WHERE gtin IS NOT NULL;
CREATE INDEX IX_coreProduto_cat ON core.Produto(id_categoria);

IF OBJECT_ID('core.LojaProduto') IS NOT NULL DROP TABLE core.LojaProduto;
CREATE TABLE core.LojaProduto (
    id_loja       INT  NOT NULL,
    id_produto    INT  NOT NULL,
    ativo         BIT  NOT NULL DEFAULT (1),
    dt_inicio     DATE NULL,
    dt_fim        DATE NULL,
    CONSTRAINT PK_coreLojaProduto PRIMARY KEY (id_loja, id_produto),
    CONSTRAINT FK_coreLojaProduto_Loja    FOREIGN KEY (id_loja)    REFERENCES core.Loja(id_loja),
    CONSTRAINT FK_coreLojaProduto_Produto FOREIGN KEY (id_produto) REFERENCES core.Produto(id_produto)
);

/* Histórico de preços (regular/promo) com fonte e granularidade diária/hora) */

IF OBJECT_ID('core.Preco') IS NOT NULL DROP TABLE core.Preco;
CREATE TABLE core.Preco (
    id_preco       BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_loja        INT             NOT NULL,
    id_produto     INT             NOT NULL,
    id_fonte       INT             NOT NULL,   -- ingest.Fonte
    tipo_preco     CHAR(1)         NOT NULL,   -- 'R' = regular, 'P' = promocional
    preco          DECIMAL(18,4)   NOT NULL,
    coletado_em    DATETIME2(0)    NOT NULL,   -- timestamp da coleta
    validade_ini   DATE            NULL,       -- se extraído de oferta/encarte
    validade_fim   DATE            NULL,
    unidade_preco  INT             NULL,       -- unidade do preço exibido na página
    qtde_base      DECIMAL(18,6)   NULL,       -- qtde usada na exibição (ex: 5kg)
    preco_unit_padrao AS
        CASE
            WHEN qtde_base IS NOT NULL AND qtde_base > 0
                 THEN TRY_CONVERT(DECIMAL(18,6), preco / qtde_base)
            ELSE NULL
        END PERSISTED,  -- preço “por 1 unidade-base” (quando possível)
    id_execucao    BIGINT          NULL,       -- ingest.ExecucaoColeta
    CONSTRAINT FK_corePreco_Loja      FOREIGN KEY (id_loja)    REFERENCES core.Loja(id_loja),
    CONSTRAINT FK_corePreco_Produto   FOREIGN KEY (id_produto) REFERENCES core.Produto(id_produto),
    CONSTRAINT FK_corePreco_Fonte     FOREIGN KEY (id_fonte)   REFERENCES ingest.Fonte(id_fonte),
    CONSTRAINT FK_corePreco_Execucao  FOREIGN KEY (id_execucao)REFERENCES ingest.ExecucaoColeta(id_execucao)
);
-- Evita duplicidade na mesma coleta/janela
CREATE UNIQUE INDEX UX_corePreco_unico
ON core.Preco (id_loja, id_produto, id_fonte, tipo_preco, CAST(coletado_em AS  DATETIME2(0)));

/* Avaliações/feedback do público para filtros e insights */

IF OBJECT_ID('core.AvaliacaoProduto') IS NOT NULL DROP TABLE core.AvaliacaoProduto;
CREATE TABLE core.AvaliacaoProduto (
    id_avaliacao  BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_usuario    INT             NOT NULL,
    id_produto    INT             NOT NULL,
    nota          TINYINT         NOT NULL CHECK (nota BETWEEN 1 AND 5),
    comentario    NVARCHAR(500)   NULL,
    criado_em     DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_coreAvaliacao_Usuario FOREIGN KEY (id_usuario) REFERENCES sec.Usuario(id_usuario),
    CONSTRAINT FK_coreAvaliacao_Produto FOREIGN KEY (id_produto) REFERENCES core.Produto(id_produto),
    CONSTRAINT UQ_coreAvaliacao_unicidade UNIQUE (id_usuario, id_produto)
);
CREATE INDEX IX_coreAvaliacao_produto ON core.AvaliacaoProduto(id_produto);

/* ==========================================================
   INGESTÃO (ingest) – fontes, execuções, encartes/ofertas
   ========================================================== */

IF OBJECT_ID('ingest.Fonte') IS NOT NULL DROP TABLE ingest.Fonte;
CREATE TABLE ingest.Fonte (
    id_fonte      INT IDENTITY(1,1)  PRIMARY KEY,
    nome          NVARCHAR(200)      NOT NULL,  -- ex: "Scraper Supermarket", "Import CSV"
    tipo          NVARCHAR(30)       NOT NULL,  -- SCRAPER | MANUAL | CSV | API
    base_url      NVARCHAR(400)      NULL,
    robots_ok     BIT                NULL,
    ativo         BIT                NOT NULL DEFAULT (1),
    dt_criacao    DATETIME2(0)       NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_ingestFonte_nome UNIQUE (nome)
);

IF OBJECT_ID('ingest.ExecucaoColeta') IS NOT NULL DROP TABLE ingest.ExecucaoColeta;
CREATE TABLE ingest.ExecucaoColeta (
    id_execucao     BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_fonte        INT             NOT NULL,
    iniciado_em     DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    finalizado_em   DATETIME2(0)    NULL,
    status          NVARCHAR(20)    NOT NULL DEFAULT 'RUNNING', -- RUNNING|OK|ERROR
    registros_lidos INT             NULL,
    registros_ok    INT             NULL,
    registros_erro  INT             NULL,
    log_execucao    NVARCHAR(MAX)   NULL,
    CONSTRAINT FK_ingestExec_Fonte FOREIGN KEY (id_fonte) REFERENCES ingest.Fonte(id_fonte)
);
CREATE INDEX IX_ingestExec_fonte_time ON ingest.ExecucaoColeta(id_fonte, iniciado_em DESC);

IF OBJECT_ID('ingest.Encarte') IS NOT NULL DROP TABLE ingest.Encarte;
CREATE TABLE ingest.Encarte (
    id_encarte    INT IDENTITY(1,1) PRIMARY KEY,
    id_fonte      INT             NOT NULL,
    id_loja       INT             NULL,        -- às vezes encarte é da rede inteira
    url_pdf       NVARCHAR(600)   NULL,
    periodo_ini   DATE            NOT NULL,
    periodo_fim   DATE            NOT NULL,
    criado_em     DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_ingestEnc_Fonte FOREIGN KEY (id_fonte) REFERENCES ingest.Fonte(id_fonte),
    CONSTRAINT FK_ingestEnc_Loja  FOREIGN KEY (id_loja)  REFERENCES core.Loja(id_loja)
);

IF OBJECT_ID('ingest.OfertaEncarte') IS NOT NULL DROP TABLE ingest.OfertaEncarte;
CREATE TABLE ingest.OfertaEncarte (
    id_oferta     BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_encarte    INT             NOT NULL,
    id_produto    INT             NOT NULL,
    preco_promoc  DECIMAL(18,4)   NOT NULL,
    unidade_preco INT             NULL,
    qtde_base     DECIMAL(18,6)   NULL,
    observacao    NVARCHAR(300)   NULL,
    CONSTRAINT FK_ingestOferta_Enc FOREIGN KEY (id_encarte) REFERENCES ingest.Encarte(id_encarte),
    CONSTRAINT FK_ingestOferta_Prod FOREIGN KEY (id_produto) REFERENCES core.Produto(id_produto)
);

/* ==========================================================
   SEGURANÇA / PERFIS (sec)
   ========================================================== */

IF OBJECT_ID('sec.Usuario') IS NOT NULL DROP TABLE sec.Usuario;
CREATE TABLE sec.Usuario (
    id_usuario      INT IDENTITY(1,1) PRIMARY KEY,
    nome            NVARCHAR(200)    NOT NULL,
    email           NVARCHAR(200)    NOT NULL,
    senha_hash      VARBINARY(256)   NULL,      -- se autenticar no app
    criado_em       DATETIME2(0)     NOT NULL DEFAULT SYSUTCDATETIME(),
    ativo           BIT              NOT NULL DEFAULT (1),
    CONSTRAINT UQ_secUsuario_email UNIQUE (email)
);

IF OBJECT_ID('sec.PerfilUsuario') IS NOT NULL DROP TABLE sec.PerfilUsuario;
CREATE TABLE sec.PerfilUsuario (
    id_usuario     INT           PRIMARY KEY,
    id_faixa_etaria INT          NULL,
    id_faixa_renda INT           NULL,
    tamanho_familia SMALLINT     NULL,
    genero         NVARCHAR(20)  NULL,
    bairro_texto   NVARCHAR(120) NULL, -- opcional: narrativa de bairro/comunidade
    CONSTRAINT FK_secPerfil_Usuario     FOREIGN KEY (id_usuario)     REFERENCES sec.Usuario(id_usuario),
    CONSTRAINT FK_secPerfil_FaixaEtaria FOREIGN KEY (id_faixa_etaria) REFERENCES ref.FaixaEtaria(id_faixa),
    CONSTRAINT FK_secPerfil_FaixaRenda  FOREIGN KEY (id_faixa_renda)  REFERENCES ref.FaixaRenda(id_faixa)
);

/* ==========================================================
   ANÁLISE (analytics) – cestas, itens, snapshots por loja
   ========================================================== */

IF OBJECT_ID('analytics.Cesta') IS NOT NULL DROP TABLE analytics.Cesta;
CREATE TABLE analytics.Cesta (
    id_cesta     INT IDENTITY(1,1) PRIMARY KEY,
    nome         NVARCHAR(200)   NOT NULL,  -- ex: “Cesta Básica Nacional”
    descricao    NVARCHAR(400)   NULL,
    ativa        BIT             NOT NULL DEFAULT (1),
    criada_em    DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_analyticsCesta_nome UNIQUE (nome)
);

IF OBJECT_ID('analytics.CestaItem') IS NOT NULL DROP TABLE analytics.CestaItem;
CREATE TABLE analytics.CestaItem (
    id_cesta     INT             NOT NULL,
    id_produto   INT             NOT NULL,
    quantidade   DECIMAL(18,6)   NOT NULL DEFAULT (1),
    CONSTRAINT PK_analyticsCestaItem PRIMARY KEY (id_cesta, id_produto),
    CONSTRAINT FK_analyticsCestaItem_Cesta   FOREIGN KEY (id_cesta)   REFERENCES analytics.Cesta(id_cesta),
    CONSTRAINT FK_analyticsCestaItem_Produto FOREIGN KEY (id_produto) REFERENCES core.Produto(id_produto)
);

IF OBJECT_ID('analytics.SnapshotCestaLoja') IS NOT NULL DROP TABLE analytics.SnapshotCestaLoja;
CREATE TABLE analytics.SnapshotCestaLoja (
    id_snapshot   BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_cesta      INT             NOT NULL,
    id_loja       INT             NOT NULL,
    snapshot_em   DATE            NOT NULL,
    total_preco   DECIMAL(18,4)   NOT NULL,
    qtd_itens     INT             NOT NULL,
    eh_mais_barata BIT            NOT NULL DEFAULT (0),
    gerado_por    NVARCHAR(40)    NULL,   -- JOB|API|MANUAL
    CONSTRAINT FK_snap_cesta FOREIGN KEY (id_cesta) REFERENCES analytics.Cesta(id_cesta),
    CONSTRAINT FK_snap_loja  FOREIGN KEY (id_loja)  REFERENCES core.Loja(id_loja),
    CONSTRAINT UQ_snap_unico UNIQUE (id_cesta, id_loja, snapshot_em)
);

/* ==========================================================
   ÍNDICES ÚTEIS
   ========================================================== */

CREATE INDEX IX_preco_busca_1 ON core.Preco (id_produto, CAST(coletado_em AS DATE), tipo_preco);
CREATE INDEX IX_preco_busca_2 ON core.Preco (id_loja, CAST(coletado_em AS DATE));
CREATE INDEX IX_oferta_encarte ON ingest.OfertaEncarte (id_encarte);

/* ==========================================================
   VIEWS DE APOIO (ex.: último preço)
   ========================================================== */

IF OBJECT_ID('core.vw_UltimoPreco') IS NOT NULL DROP VIEW core.vw_UltimoPreco;
GO
CREATE VIEW core.vw_UltimoPreco AS
WITH ult AS (
    SELECT
        p.id_loja,
        p.id_produto,
        p.tipo_preco,
        p.preco,
        p.preco_unit_padrao,
        p.coletado_em,
        ROW_NUMBER() OVER (PARTITION BY p.id_loja, p.id_produto, p.tipo_preco ORDER BY p.coletado_em DESC) AS rn
    FROM core.Preco p
)
SELECT id_loja, id_produto, tipo_preco, preco, preco_unit_padrao, coletado_em
FROM ult
WHERE rn = 1;
GO

/* ==========================================================
   EXEMPLOS DE CHECKS/REGRAS (opcional)
   ========================================================== */

ALTER TABLE core.Preco WITH CHECK ADD
    CONSTRAINT CK_corePreco_tipo CHECK (tipo_preco IN ('R','P'));



