


CREATE LOGIN shop2
WITH PASSWORD = 'rootshop2',
     CHECK_POLICY = ON,     -- aplica políticas de complexidade do Windows
     CHECK_EXPIRATION = ON; -- expira a senha conforme política


USE cesta_basica;
CREATE USER shopsys
FOR LOGIN shop2;


ALTER ROLE db_datareader ADD MEMBER shopsys;  -- Permite SELECT
ALTER ROLE db_datawriter ADD MEMBER shopsys;  -- Permite INSERT/UPDATE/DELETE
ALTER ROLE db_owner ADD MEMBER shopsys;       -- Permissões completas

SELECT name, type_desc 
FROM sys.database_principals
WHERE type IN ('S', 'U'); -- S = SQL user, U = Windows user


--Perfeito, Adriano! Abaixo está um script T-SQL completo para Microsoft SQL Server que implementa o modelo do seu PDF (tabelas, chaves primárias/estrangeiras, índices úteis e algumas regras de integridade). Adaptei tipos e restrições para boas práticas em SQL Server (ex.: `DATETIME2`, `DECIMAL(12,2)`, `NVARCHAR`), além de chaves únicas para evitar duplicidade de preços por data. Baseado no modelo descrito: Usuário, Produto, Supermercado, Preço, Acesso, Histórico de Preços e Configurações do Sistema.&#x20;

--```sql
/* ==========================================================
   DATABASE & SCHEMA (opcional)
   ========================================================== */
-- CREATE DATABASE CestaBasicaDB;
-- GO
-- USE CestaBasicaDB;
-- GO

IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = 'cesta_basica')
    EXEC('CREATE SCHEMA cesta_basica');
GO

/* ==========================================================
   TABELA: Usuario
   (id_usuario, nome, email, senha, localizacao)
   ========================================================== */
IF OBJECT_ID('Usuario') IS NOT NULL DROP TABLE Usuario;
GO
CREATE TABLE Usuario (
    id_usuario       INT IDENTITY(1,1) PRIMARY KEY,
    nome             NVARCHAR(120)       NOT NULL,
    email            NVARCHAR(255)       NOT NULL,
    senha_hash       VARBINARY(256)      NULL,  -- substitui "senha" do modelo. Em produção, armazene HASH + SALT, não a senha em texto
    localizacao      INT			     NULL,
    dt_criacao       DATETIME2(0)        NOT NULL CONSTRAINT DF_Usuario_dt_criacao DEFAULT (SYSUTCDATETIME())
);
-- e-mail deve ser único
CREATE UNIQUE INDEX UX_Usuario_email ON Usuario(email);
CREATE INDEX IX_Usuario_nome ON Usuario(nome);





/* ==========================================================
   TABELA: Produto
   (id_produto, nome, categoria, descricao, unidade_medida)
   ========================================================== */
IF OBJECT_ID('Produto') IS NOT NULL DROP TABLE Produto;
GO
CREATE TABLE Produto (
    id_produto       INT IDENTITY(1,1) PRIMARY KEY,
    nome             NVARCHAR(200)       NOT NULL,
    categoria        NVARCHAR(100)       NOT NULL,
    descricao        NVARCHAR(MAX)       NULL,
    unidade_medida   NVARCHAR(30)        NOT NULL,   -- ex.: kg, L, un
    dt_criacao       DATETIME2(0)        NOT NULL CONSTRAINT DF_Produto_dt_criacao DEFAULT (SYSUTCDATETIME())
);
CREATE INDEX IX_Produto_nome ON Produto(nome);
CREATE INDEX IX_Produto_categoria ON Produto(categoria);



/* ==========================================================
   TABELA: Supermercado
   (id_supermercado, nome, localizacao, url_site)
   ========================================================== */
IF OBJECT_ID('Supermercado') IS NOT NULL DROP TABLE Supermercado;
GO
CREATE TABLE Supermercado (
    id_supermercado  INT IDENTITY(1,1) PRIMARY KEY,
    nome             NVARCHAR(200)       NOT NULL,
    localizacao      NVARCHAR(255)       NOT NULL,
    url_site         NVARCHAR(400)       NULL,
    dt_criacao       DATETIME2(0)        NOT NULL CONSTRAINT DF_Super_dt_criacao DEFAULT (SYSUTCDATETIME())
);
CREATE INDEX IX_Supermercado_nome ON Supermercado(nome);
CREATE INDEX IX_Supermercado_localizacao ON Supermercado(localizacao);



/* ==========================================================
   TABELA: Preco
   (id_preco, id_produto, id_supermercado, preco, data_coleta)
   ========================================================== */
IF OBJECT_ID('Preco') IS NOT NULL DROP TABLE Preco;
GO
CREATE TABLE Preco (
    id_preco         BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_produto       INT                 NOT NULL,
    id_supermercado  INT                 NOT NULL,
    preco            DECIMAL(12,2)       NOT NULL,
    data_coleta      DATE                NOT NULL,
    dt_insercao      DATETIME2(0)        NOT NULL CONSTRAINT DF_Preco_dt_insercao DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT CK_Preco_valor_pos CHECK (preco >= 0),
    CONSTRAINT FK_Preco_Produto
        FOREIGN KEY (id_produto) REFERENCES Produto(id_produto),
    CONSTRAINT FK_Preco_Supermercado
        FOREIGN KEY (id_supermercado) REFERENCES Supermercado(id_supermercado)
);
-- Evita duplicar o preço do MESMO produto no MESMO mercado na MESMA data
CREATE UNIQUE INDEX UX_Preco_Produto_Super_Data
    ON Preco(id_produto, id_supermercado, data_coleta);

CREATE INDEX IX_Preco_Produto ON Preco(id_produto);
CREATE INDEX IX_Preco_Supermercado ON Preco(id_supermercado);
CREATE INDEX IX_Preco_Data ON Preco(data_coleta);



/* ==========================================================
   TABELA: Acesso
   (id_acesso, id_usuario, id_produto, data_acesso, contador_acessos)
   ========================================================== */
IF OBJECT_ID('Acesso') IS NOT NULL DROP TABLE Acesso;
GO
CREATE TABLE Acesso (
    id_acesso        BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_usuario       INT                 NOT NULL,
    id_produto       INT                 NOT NULL,
    data_acesso      DATE                NOT NULL,
    contador_acessos INT                 NOT NULL CONSTRAINT DF_Acesso_cont DEFAULT (1),
    dt_registro      DATETIME2(0)        NOT NULL CONSTRAINT DF_Acesso_dt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT CK_Acesso_cont_pos CHECK (contador_acessos > 0),
    CONSTRAINT FK_Acesso_Usuario
        FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario),
    CONSTRAINT FK_Acesso_Produto
        FOREIGN KEY (id_produto) REFERENCES Produto(id_produto)
);
-- Um registro por usuário/produto/dia (facilita KPIs do dashboard)
CREATE UNIQUE INDEX UX_Acesso_User_Prod_Dia
    ON Acesso(id_usuario, id_produto, data_acesso);

CREATE INDEX IX_Acesso_Data ON Acesso(data_acesso);



/* ==========================================================
   TABELA: HistoricoPreco
   (id_historico, id_produto, id_supermercado, preco_anterior, preco_atual, data_atualizacao)
   ========================================================== */
IF OBJECT_ID('HistoricoPreco') IS NOT NULL DROP TABLE HistoricoPreco;
GO
CREATE TABLE HistoricoPreco (
    id_historico     BIGINT IDENTITY(1,1) PRIMARY KEY,
    id_produto       INT                 NOT NULL,
    id_supermercado  INT                 NOT NULL,
    preco_anterior   DECIMAL(12,2)       NOT NULL,
    preco_atual      DECIMAL(12,2)       NOT NULL,
    data_atualizacao DATE                NOT NULL,
    dt_registro      DATETIME2(0)        NOT NULL CONSTRAINT DF_Hist_dt DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT CK_Hist_Preco_Pos CHECK (preco_anterior >= 0 AND preco_atual >= 0),
    CONSTRAINT FK_Hist_Produto
        FOREIGN KEY (id_produto) REFERENCES Produto(id_produto),
    CONSTRAINT FK_Hist_Supermercado
        FOREIGN KEY (id_supermercado) REFERENCES Supermercado(id_supermercado)
);
-- Evita múltiplos históricos no mesmo dia para o mesmo par produto/mercado
CREATE UNIQUE INDEX UX_Hist_Prod_Super_Data
    ON HistoricoPreco(id_produto, id_supermercado, data_atualizacao);

CREATE INDEX IX_Hist_Prod ON HistoricoPreco(id_produto);
CREATE INDEX IX_Hist_Super ON HistoricoPreco(id_supermercado);
CREATE INDEX IX_Hist_Data ON HistoricoPreco(data_atualizacao);



/* ==========================================================
   TABELA: ConfiguracoesSistema
   (id_configuracao, parametro, valor)
   ========================================================== */
IF OBJECT_ID('ConfiguracoesSistema') IS NOT NULL DROP TABLE ConfiguracoesSistema;
GO
CREATE TABLE ConfiguracoesSistema (
    id_configuracao  INT IDENTITY(1,1) PRIMARY KEY,
    parametro        NVARCHAR(150)       NOT NULL,
    valor            NVARCHAR(MAX)       NOT NULL,
    dt_atualizacao   DATETIME2(0)        NOT NULL CONSTRAINT DF_Config_dt DEFAULT (SYSUTCDATETIME())
);
-- Um parâmetro por nome
CREATE UNIQUE INDEX UX_Config_parametro ON ConfiguracoesSistema(parametro);
GO
```

### Observações e decisões de projeto

* **Senha**: troquei `senha (VARCHAR)` por `senha_hash VARBINARY(256)` para sinalizar armazenamento de *hash* (ex.: `HASHBYTES('SHA2_256', ...)` + *salt*). Em dev, mantenha como está se preferir; em produção, evite texto puro.&#x20;
* **Tipos**: usei `NVARCHAR` para internacionalização e `DECIMAL(12,2)` para preços. Datas usam `DATE` para coleta/atualização e `DATETIME2` para *timestamps* de registro.&#x20;
* **Unicidade**:

  * `Preco`: `(id_produto, id_supermercado, data_coleta)` único para não repetir coleta do mesmo dia.
  * `HistoricoPreco`: `(id_produto, id_supermercado, data_atualizacao)` único.
  * `Acesso`: `(id_usuario, id_produto, data_acesso)` único para facilitar agregações diárias.
  * `Usuario.email` e `ConfiguracoesSistema.parametro` únicos.&#x20;
* **Índices**: adicionei índices em campos de junção e busca (`nome`, `categoria`, `data_*`) para dar performance aos relatórios citados (dashboard analítico).&#x20;
* **Checks**: garantem preços e contadores não negativos.&#x20;

### Próximos passos úteis (se quiser evoluir)

* **Geolocalização real**: trocar `localizacao NVARCHAR` por `GEOGRAPHY` com latitude/longitude para calcular custo de deslocamento (distância).&#x20;
* **View de “melhor mercado”**: criar *views* ou *stored procedures* que consolidem o menor preço por produto e ranqueiem supermercados por quantidade de itens vantajosos.&#x20;
* **ETL de histórico**: *trigger* ou job que, ao inserir um novo preço diferente do último, grave em `HistoricoPreco`.

Se quiser, já te entrego uma *view* de “menor preço por produto/supermercado no dia mais recente” ou uma *procedure* de ranking por cesta básica. Quer que eu inclua?
