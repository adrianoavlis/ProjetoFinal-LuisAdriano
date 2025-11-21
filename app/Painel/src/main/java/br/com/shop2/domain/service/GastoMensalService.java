package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.GastoMensalRepository;
import br.com.shop2.model.dados.GastoMensal;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GastoMensalService {

    private static final DateTimeFormatter FORMATO_MMAAAA = DateTimeFormatter.ofPattern("MMyyyy");
    private static final DateTimeFormatter FORMATO_EXIBICAO = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final Map<String, String> COLUNAS_CANONICAS = Map.ofEntries(
        Map.entry("total da cesta", "total"),
        Map.entry("custo da cesta", "total"),
        Map.entry("total", "total"),
        Map.entry("carne", "carne"),
        Map.entry("leite", "leite"),
        Map.entry("feijao", "feijao"),
        Map.entry("feijao carioquinha", "feijao"),
        Map.entry("feijao preto", "feijao"),
        Map.entry("arroz", "arroz"),
        Map.entry("arroz agulhinha", "arroz"),
        Map.entry("farinha", "farinha"),
        Map.entry("farinha de trigo", "farinha"),
        Map.entry("batata", "batata"),
        Map.entry("batata inglesa", "batata"),
        Map.entry("tomate", "tomate"),
        Map.entry("pao", "pao"),
        Map.entry("pao frances", "pao"),
        Map.entry("cafe", "cafe"),
        Map.entry("cafe em po", "cafe"),
        Map.entry("banana", "banana"),
        Map.entry("banana prata", "banana"),
        Map.entry("acucar", "acucar"),
        Map.entry("acucar cristal", "acucar"),
        Map.entry("oleo", "oleo"),
        Map.entry("oleo de soja", "oleo"),
        Map.entry("manteiga", "manteiga")
    );

    private static final Map<String, BiConsumer<GastoMensal, BigDecimal>> SETTERS = Map.ofEntries(
        Map.entry("total", GastoMensal::setTotalCesta),
        Map.entry("carne", GastoMensal::setCarne),
        Map.entry("leite", GastoMensal::setLeite),
        Map.entry("feijao", GastoMensal::setFeijao),
        Map.entry("arroz", GastoMensal::setArroz),
        Map.entry("farinha", GastoMensal::setFarinha),
        Map.entry("batata", GastoMensal::setBatata),
        Map.entry("tomate", GastoMensal::setTomate),
        Map.entry("pao", GastoMensal::setPao),
        Map.entry("cafe", GastoMensal::setCafe),
        Map.entry("banana", GastoMensal::setBanana),
        Map.entry("acucar", GastoMensal::setAcucar),
        Map.entry("oleo", GastoMensal::setOleo),
        Map.entry("manteiga", GastoMensal::setManteiga)
    );

    private static final List<String> COOKIE_SELECTORS = List.of(
        "button[aria-label*='aceit' i]",
        "button.cookie-accept",
        "button.lgpd-accept",
        ".cc-allow",
        "button[id*='cookie']",
        "button[class*='cookie']"
    );

    private static final List<String> CABECALHO_ESPERADO_NORMALIZADO = List.of(
        "",
        "total da cesta",
        "carne",
        "leite",
        "feijao",
        "arroz",
        "farinha",
        "batata",
        "tomate",
        "pao",
        "cafe",
        "banana",
        "acucar",
        "oleo",
        "manteiga"
    );

    private final GastoMensalRepository repository;

    public ImportacaoResultado importar(String dataInicial, String dataFinal, boolean sequencial) {
        YearMonth inicio = parseEntrada(dataInicial);
        YearMonth fim = parseEntrada(dataFinal);

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Datas devem estar no formato mmaaaa.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Data final deve ser maior ou igual à inicial.");
        }

        List<YearMonth> periodos = gerarPeriodos(inicio, fim);
        if (periodos.isEmpty()) {
            return ImportacaoResultado.vazio();
        }

        List<ImportacaoMes> resultados = sequencial
            ? importarSequencial(periodos)
            : importarParalelo(periodos);

        int processados = 0;
        int novos = 0;
        int atualizados = 0;
        List<YearMonth> sucesso = new ArrayList<>();
        Map<YearMonth, String> erros = new LinkedHashMap<>();

        for (ImportacaoMes mes : resultados) {
            if (mes.sucesso()) {
                processados += mes.processados();
                novos += mes.inseridos();
                atualizados += mes.atualizados();
                sucesso.add(mes.periodo());
            } else {
                if (mes.periodo() != null) {
                    erros.put(mes.periodo(), mes.erro());
                }
            }
        }

        sucesso.sort(Comparator.naturalOrder());

        return new ImportacaoResultado(processados, novos, atualizados, sucesso, erros);
    }

    public Page<GastoMensal> listar(String municipio, YearMonth mesAno, Pageable pageable) {
        Specification<GastoMensal> spec = null;
        if (municipio != null && !municipio.isBlank()) {
            String filtro = normalizarMunicipioFiltro(municipio);
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("municipio")), "%" + filtro + "%"));
        }
        if (mesAno != null) {
            YearMonth filtro = mesAno;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mesAno"), filtro));
        }
        return repository.findAll(spec, pageable);
    }

    public List<YearMonth> listarPeriodosImportados() {
        return repository.listarPeriodosImportados();
    }

    public YearMonth parseEntrada(String valor) {
        if (valor == null) {
            return null;
        }
        String apenasDigitos = valor.replaceAll("[^0-9]", "");
        if (apenasDigitos.length() != 6) {
            return null;
        }
        try {
            return YearMonth.parse(apenasDigitos, FORMATO_MMAAAA);
        } catch (Exception e) {
            return null;
        }
    }

    public String formatar(YearMonth mes) {
        return mes == null ? null : FORMATO_EXIBICAO.format(mes);
    }

    private List<YearMonth> gerarPeriodos(YearMonth inicio, YearMonth fim) {
        List<YearMonth> periodos = new ArrayList<>();
        YearMonth atual = inicio;
        while (!atual.isAfter(fim)) {
            periodos.add(atual);
            atual = atual.plusMonths(1);
        }
        return periodos;
    }

    private List<ImportacaoMes> importarSequencial(List<YearMonth> periodos) {
        WebDriver driver = criarDriver();
        try {
            List<ImportacaoMes> resultados = new ArrayList<>();
            for (YearMonth mes : periodos) {
                resultados.add(importarMes(driver, mes));
            }
            return resultados;
        } finally {
            driver.quit();
        }
    }

    private List<ImportacaoMes> importarParalelo(List<YearMonth> periodos) {
        if (periodos.size() == 1) {
            return importarSequencial(periodos);
        }
        int poolSize = Math.min(4, periodos.size());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<Map.Entry<YearMonth, Future<ImportacaoMes>>> tarefas = new ArrayList<>();
            for (YearMonth mes : periodos) {
                Future<ImportacaoMes> futuro = executor.submit(() -> importarMesComNovoDriver(mes));
                tarefas.add(new AbstractMap.SimpleEntry<>(mes, futuro));
            }
            List<ImportacaoMes> resultados = new ArrayList<>();
            for (Map.Entry<YearMonth, Future<ImportacaoMes>> tarefa : tarefas) {
                try {
                    resultados.add(tarefa.getValue().get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    resultados.add(ImportacaoMes.falha(tarefa.getKey(), "Importação interrompida."));
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    String mensagem = causa == null ? e.getMessage() : causa.getMessage();
                    resultados.add(ImportacaoMes.falha(tarefa.getKey(), mensagem));
                }
            }
            return resultados;
        } finally {
            executor.shutdown();
        }
    }

    private ImportacaoMes importarMesComNovoDriver(YearMonth mes) {
        WebDriver driver = criarDriver();
        try {
            return importarMes(driver, mes);
        } finally {
            driver.quit();
        }
    }

    private ImportacaoMes importarMes(WebDriver driver, YearMonth mes) {
        try {
            navegar(driver, mes);
            List<GastoMensal> dados = extrair(driver, mes);
            int inseridos = 0;
            int atualizados = 0;
            for (GastoMensal dado : dados) {
                Optional<GastoMensal> existente = repository.findByMunicipioIgnoreCaseAndMesAno(dado.getMunicipio(), mes);
                if (existente.isPresent()) {
                    GastoMensal alvo = existente.get();
                    aplicar(alvo, dado);
                    repository.save(alvo);
                    atualizados++;
                } else {
                    repository.save(dado);
                    inseridos++;
                }
            }
            return ImportacaoMes.sucesso(mes, dados.size(), inseridos, atualizados);
        } catch (Exception e) {
            return ImportacaoMes.falha(mes, e.getMessage());
        }
    }

    private void aplicar(GastoMensal alvo, GastoMensal origem) {
        alvo.setTotalCesta(origem.getTotalCesta());
        alvo.setCarne(origem.getCarne());
        alvo.setLeite(origem.getLeite());
        alvo.setFeijao(origem.getFeijao());
        alvo.setArroz(origem.getArroz());
        alvo.setFarinha(origem.getFarinha());
        alvo.setBatata(origem.getBatata());
        alvo.setTomate(origem.getTomate());
        alvo.setPao(origem.getPao());
        alvo.setCafe(origem.getCafe());
        alvo.setBanana(origem.getBanana());
        alvo.setAcucar(origem.getAcucar());
        alvo.setOleo(origem.getOleo());
        alvo.setManteiga(origem.getManteiga());
    }

    private void navegar(WebDriver driver, YearMonth mes) {
        driver.get("https://www.dieese.org.br/cesta/");
        esperarDom(driver);
        fecharCookies(driver);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebElement aba = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='interface']/ul/li[3]/a")));
        aba.click();

        WebElement radioGastoMensal = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[3]/div/div[3]/form/div[1]/p[3]/label[3]/input"))
        );
        radioGastoMensal.click();

        String data = mes.format(FORMATO_MMAAAA);
        WebElement campoData = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id='dataData']")));
        campoData.clear();
        campoData.sendKeys(data);

        WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='dataForm']/div[2]/div/input[1]")));
        botao.click();

        wait.until(drv -> drv.getCurrentUrl().contains("/cesta/data")
            || existeElemento(drv, By.cssSelector("#dados"))
            || existeElemento(drv, By.cssSelector("table")));
    }

    private List<GastoMensal> extrair(WebDriver driver, YearMonth mes) {
        WebElement tabela = localizarTabela(driver);

        List<String> headers = tabela.findElements(By.cssSelector("thead tr"))
            .stream()
            .flatMap(tr -> tr.findElements(By.cssSelector("th")).stream())
            .map(WebElement::getText)
            .map(String::trim)
            .collect(Collectors.toList());

        Map<Integer, BiConsumer<GastoMensal, BigDecimal>> mapeamento = mapearColunas(headers);

        List<WebElement> linhas = tabela.findElements(By.cssSelector("tbody tr"));
        List<GastoMensal> resultado = new ArrayList<>();
        for (WebElement linha : linhas) {
            List<WebElement> celulas = linha.findElements(By.cssSelector("td"));
            if (celulas.isEmpty()) {
                continue;
            }
            String municipio = celulas.get(0).getText().trim();
            if (municipio.isEmpty()) {
                continue;
            }
            GastoMensal gasto = new GastoMensal();
            gasto.setMunicipio(municipio);
            gasto.setMesAno(mes);

            for (int i = 1; i < celulas.size(); i++) {
                BiConsumer<GastoMensal, BigDecimal> setter = mapeamento.get(i);
                if (setter == null) {
                    continue;
                }
                BigDecimal valor = lerValor(celulas.get(i).getText());
                if (valor != null) {
                    setter.accept(gasto, valor);
                }
            }
            if (gasto.getTotalCesta() == null) {
                gasto.setTotalCesta(BigDecimal.ZERO);
            }
            resultado.add(gasto);
        }
        return resultado;
    }

    private WebElement localizarTabela(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        List<By> candidatos = List.of(
            By.cssSelector("table#dados"),
            By.cssSelector("#dados"),
            By.cssSelector("#dados table"),
            By.cssSelector("table.display"),
            By.cssSelector("table")
        );
        List<String> tentativas = new ArrayList<>();
        for (By candidato : candidatos) {
            tentativas.add(candidato.toString());
            try {
                List<WebElement> encontrados = wait.until(drv -> {
                    List<WebElement> elementos = drv.findElements(candidato);
                    return elementos.isEmpty() ? null : elementos;
                });
                for (WebElement tabela : encontrados) {
                    if (ehTabelaGastoMensal(tabela)) {
                        return tabela;
                    }
                }
            } catch (org.openqa.selenium.TimeoutException ignored) {
            }
        }
        throw new org.openqa.selenium.NoSuchElementException("Tabela de dados não encontrada ou não corresponde ao formato esperado. Seletores testados: " + tentativas);
    }

    private boolean ehTabelaGastoMensal(WebElement tabela) {
        boolean captionValida = tabela.findElements(By.cssSelector("caption")).stream()
            .map(WebElement::getText)
            .map(this::normalizar)
            .anyMatch(texto -> texto.contains("gasto mensal")
                || texto.replace(" ", "").contains("gastomensal"));
        if (!captionValida) {
            return false;
        }

        List<String> cabecalhosNormalizados = tabela.findElements(By.cssSelector("thead tr"))
            .stream()
            .flatMap(tr -> tr.findElements(By.cssSelector("th")).stream())
            .map(WebElement::getText)
            .map(String::trim)
            .map(this::normalizar)
            .collect(Collectors.toList());

        if (cabecalhosNormalizados.size() < CABECALHO_ESPERADO_NORMALIZADO.size()) {
            return false;
        }

        for (int i = 0; i < CABECALHO_ESPERADO_NORMALIZADO.size(); i++) {
            if (!Objects.equals(cabecalhosNormalizados.get(i), CABECALHO_ESPERADO_NORMALIZADO.get(i))) {
                return false;
            }
        }

        return true;
    }

    private Map<Integer, BiConsumer<GastoMensal, BigDecimal>> mapearColunas(List<String> headers) {
        Map<Integer, BiConsumer<GastoMensal, BigDecimal>> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String normalizado = normalizar(headers.get(i));
            if (normalizado.isEmpty()) {
                continue;
            }
            String chave = COLUNAS_CANONICAS.entrySet().stream()
                .filter(entry -> normalizado.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
            if (chave != null) {
                BiConsumer<GastoMensal, BigDecimal> setter = SETTERS.get(chave);
                if (setter != null) {
                    map.put(i, setter);
                }
            }
        }
        return map;
    }

    private BigDecimal lerValor(String bruto) {
        if (bruto == null) {
            return null;
        }
        String texto = bruto.trim();
        if (texto.isEmpty() || "-".equals(texto)) {
            return null;
        }
        try {
            String normalizado = texto.replace(".", "").replace(",", ".");
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private String normalizarMunicipioFiltro(String valor) {
        return valor.toLowerCase(Locale.ROOT).trim();
    }

    private void esperarDom(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(d ->
            Objects.equals(((JavascriptExecutor) d).executeScript("return document.readyState"), "complete"));
    }

    private void fecharCookies(WebDriver driver) {
        for (String seletor : COOKIE_SELECTORS) {
            try {
                WebElement botao = driver.findElement(By.cssSelector(seletor));
                botao.click();
                break;
            } catch (org.openqa.selenium.NoSuchElementException ignored) {
            }
        }
    }

    private boolean existeElemento(WebDriver driver, By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }

    private WebDriver criarDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
        return new ChromeDriver(options);
    }

    @Value
    @Builder
    public static class ImportacaoResultado {
        int totalProcessados;
        int totalInseridos;
        int totalAtualizados;
        List<YearMonth> periodosSucesso;
        Map<YearMonth, String> errosPorPeriodo;

        public static ImportacaoResultado vazio() {
            return new ImportacaoResultado(0, 0, 0, List.of(), Map.of());
        }
    }

    private static class ImportacaoMes {
        private final YearMonth periodo;
        private final int processados;
        private final int inseridos;
        private final int atualizados;
        private final boolean sucesso;
        private final String erro;

        private ImportacaoMes(YearMonth periodo, int processados, int inseridos, int atualizados, boolean sucesso, String erro) {
            this.periodo = periodo;
            this.processados = processados;
            this.inseridos = inseridos;
            this.atualizados = atualizados;
            this.sucesso = sucesso;
            this.erro = erro;
        }

        static ImportacaoMes sucesso(YearMonth periodo, int processados, int inseridos, int atualizados) {
            return new ImportacaoMes(periodo, processados, inseridos, atualizados, true, null);
        }

        static ImportacaoMes falha(YearMonth periodo, String erro) {
            return new ImportacaoMes(periodo, 0, 0, 0, false, erro);
        }

        YearMonth periodo() {
            return periodo;
        }

        int processados() {
            return processados;
        }

        int inseridos() {
            return inseridos;
        }

        int atualizados() {
            return atualizados;
        }

        boolean sucesso() {
            return sucesso;
        }

        String erro() {
            return erro;
        }
    }
}
