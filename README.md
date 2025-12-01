# Plataforma de Análise da Cesta Básica

Aplicação web desenvolvida como parte do Trabalho de Conclusão de Curso do Bacharelado em Sistemas de Informação da Universidade Federal Fluminense (UFF), Instituto de Computação (IC).
A plataforma analisa o custo da cesta básica no Brasil, integrando dados oficiais e técnicas analíticas para apoiar pesquisas, monitoramento socioeconômico e decisões em políticas públicas.

## 🎯 Objetivo da Aplicação e Base Científica

A Plataforma de Análise da Cesta Básica oferece um painel interativo para:

- Explorar a **evolução de preços** dos itens essenciais.
- **Comparar municípios** e capitais brasileiras.
- Identificar **padrões de variação, sazonalidade e assimetrias regionais**.
- Relacionar o custo da cesta com **inflação e eventos externos**.

As análises, indicadores e visualizações foram fundamentados nos dados do DIEESE, além dos resultados discutidos no artigo científico desenvolvido para o TCC.

## 📚 Eixos Analíticos da Pesquisa

- **Evolução dos preços**
- **Comparação regional**
- **Peso dos itens**
- **Influência de eventos externos**

## 📊 Funcionalidades Principais

1. 📈**Evolução dos preços**
   – Tendência, variação mensal, preço médio, máximo e mínimo.
3. 🌎**Comparação regional**
   – Análises lado a lado entre municípios; gráficos e tabelas comparativas.
5. 🥫**Peso dos itens**
   – Participação percentual dos alimentos na composição total da cesta.
7. ⚡**Influência de eventos externos**
   – CRUD de eventos e correlação com oscilações de preços.

## 💻 Arquitetura e Tecnologias Utilizadas

- **Back-end:** Spring Boot 3.5.5, Java 21  
- **Front-end:** JSP, HTML5, CSS3, JavaScript, Bootstrap 5  
- **Banco de Dados:** Microsoft SQL Server  
- **Versionamento:** Git e GitHub

##  Painel Analítico da Cesta Básica

![Evolução de Preços](Prototipos/Evolução.png)

![Evolução de Preços](Prototipos/Grafico_Evolucao.png)

![Comparativo Regional](Prototipos/Comparativo_Regional_.png)

![Comparativo Regional](Prototipos/Comparativo_Regional_2.png)

![Peso dos Itens](Prototipos/Peso_dos_itens.png)

![Influencia Eventos Externos](Prototipos/Influencia_Eventos_Externos.png)


## 🗂️ Estrutura do Repositório

```text
ProjetoFinal-LuisAdriano/app/Painel
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   ├── resources/
│   │   └── webapp/
│   └── test/
└── README.md
```

## ⚙️ Instalação e Execução Local

1. **Clonar o repositório**

```bash
git clone https://github.com/adrianoavlis/ProjetoFinal-LuisAdriano.git
cd ProjetoFinal-LuisAdriano/app/Painel
```

2. **Configurar o banco de dados SQL Server**

3. **Instalar dependências**

```bash
mvn clean install
```

4. **Executar a aplicação**

```bash
mvn spring-boot:run
```

5. **Acessar o painel**

Acesse:  
http://localhost:8081

## 👤 Autores e Créditos

- **Luis Adriano da Silva** – Desenvolvedor e autor do TCC  
- **Prof. João Felipe Pimentel** – Orientador, IC/UFF
