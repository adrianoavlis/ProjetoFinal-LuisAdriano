package br.com.shop2.domain.service;

import br.com.shop2.domain.repository.CestaBasicaRepository;
import br.com.shop2.model.common.CestaBasicaPreviewDTO;
import br.com.shop2.model.mercado.CestaBasica;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CestaBasicaService {

    private final CestaBasicaRepository repository;
    private final WebDriver driver;

    /** 1) Acessa DIEESE, preenche mmaaaa inicial/final, consulta e prepara preview (sem salvar) */
    public List<CestaBasicaPreviewDTO> previewDieese(String iniMMAAAA, String fimMMAAAA) {
        navegarDieesePreencherConsultar(iniMMAAAA, fimMMAAAA);
        return lerTabelaComoPreview();
    }

    /** 2) Confirma & salva: MERGE por (municipio, mesAno) — atualiza valor se já existir */
    @Transactional
    public int salvarSelecao(List<CestaBasicaPreviewDTO> selecionados) {
        if (selecionados == null || selecionados.isEmpty()) return 0;
        int count = 0;
        for (CestaBasicaPreviewDTO dto : selecionados) {
            if (dto.getMunicipio() == null || dto.getMesAno() == null || dto.getValor() == null) continue;

            repository.findByMunicipioAndMesAno(dto.getMunicipio(), dto.getMesAno())
                .ifPresentOrElse(existing -> {
                    // Atualiza valor se mudou
                    if (!Objects.equals(existing.getValor(), dto.getValor())) {
                        existing.setValor(dto.getValor());
                        // atualiza UF se informada agora
                        if (dto.getMunicipio() != null && !dto.getMunicipio().isBlank()) {
                            existing.setMunicipio(dto.getMunicipio().toUpperCase(Locale.ROOT));
                        }
                        repository.save(existing);
                    }
                }, () -> {
                    CestaBasica novo = CestaBasica.builder()
                        .municipio(dto.getMunicipio())
                        .mesAno(dto.getMesAno())
                        .valor(dto.getValor())
                        .build();
                    repository.save(novo);
                });
            count++;
        }
        return count;
    }

    /* ============================ Auxiliares Selenium ============================ */

    private void navegarDieesePreencherConsultar(String iniMMAAAA, String fimMMAAAA) {
        driver.get("https://www.dieese.org.br/cesta");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement inputIni = acharPrimeiroQueExistir(
            By.cssSelector("input[name='dataInicial']"),
            By.cssSelector("input[name='periodoInicial']"),
            By.id("dataInicial"),
            By.id("periodoInicial"),
            By.cssSelector("input[type='text'][placeholder*='Inicial']")
        );
        WebElement inputFim = acharPrimeiroQueExistir(
            By.cssSelector("input[name='dataFinal']"),
            By.cssSelector("input[name='periodoFinal']"),
            By.id("dataFinal"),
            By.id("periodoFinal"),
            By.cssSelector("input[type='text'][placeholder*='Final']")
        );

        limparEnviar(inputIni, iniMMAAAA);
        limparEnviar(inputFim, fimMMAAAA);

        WebElement botaoConsultar = acharPrimeiroQueExistir(
            By.xpath("//button[contains(.,'Consultar')]"),
            By.cssSelector("button[type='submit']"),
            By.xpath("//input[@type='submit' and (contains(@value,'Consultar') or contains(@value,'Buscar'))]")
        );
        botaoConsultar.click();

        // Espera redirecionar para /cesta/produto ou aparecer a tabela
        wait.until(drv -> drv.getCurrentUrl().contains("/cesta/produto") || existeElemento(By.cssSelector("table")));
    }

    private List<CestaBasicaPreviewDTO> lerTabelaComoPreview() {
        WebElement table = localizarTabelaOuFalhar();
        // Verifica se há dados
        List<WebElement> linhas = table.findElements(By.cssSelector("tbody tr"));
        if (linhas.isEmpty()) return List.of();

        // Headers
        List<String> cabecalhos = table.findElements(By.cssSelector("thead tr th"))
            .stream().map(th -> th.getText().trim()).collect(Collectors.toList());
        if (cabecalhos.size() <= 1) return List.of(); // só data e nada mais

        // Coluna 0 = "MM-YYYY"; demais = municípios (ou UF)
        String[] municipios = new String[cabecalhos.size() - 1];
       // String[] ufs = new String[cabecalhos.size() - 1];
        for (int i = 1; i < cabecalhos.size(); i++) {
            String h = cabecalhos.get(i);
            municipios[i - 1] = h;        // manter exatamente como vem no site
            //ufs[i - 1] = extrairUf(h);    // tentar derivar UF (se o header já for "RJ", etc.)
        }

        @SuppressWarnings("deprecation")
		NumberFormat nf = NumberFormat.getInstance(new Locale("pt","BR"));
        List<CestaBasicaPreviewDTO> out = new ArrayList<>();

        for (WebElement row : linhas) {
            List<WebElement> tds = row.findElements(By.cssSelector("td"));
            if (tds.isEmpty()) continue;

            String mesAno = tds.get(0).getText().trim();
            if (mesAno.isEmpty()) continue;

            for (int c = 1; c < tds.size() && c - 1 < municipios.length; c++) {
                String raw = tds.get(c).getText().trim();
                if (raw.isEmpty() || "-".equals(raw)) continue;
                try {
                    Double valor = nf.parse(raw).doubleValue();
                    out.add(CestaBasicaPreviewDTO.builder()
                        .municipio(municipios[c - 1])
                        //.estado(ufs[c - 1])      pode ser null
                        .mesAno(mesAno)
                        .valor(valor)
                        .build());
                } catch (Exception ignore) {}
            }
        }
        return out;
    }

    private WebElement localizarTabelaOuFalhar() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#dados")));
        } catch (Exception e) {
            // fallback: primeira <table> visível
            return new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));
        }
    }

    private WebElement acharPrimeiroQueExistir(By... tentativas) {
        for (By by : tentativas) {
            try {
                return new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(by));
            } catch (Exception ignore) {}
        }
        throw new org.openqa.selenium.NoSuchElementException("Elemento não encontrado por nenhum seletor informado.");
    }

    private boolean existeElemento(By by) {
        try { driver.findElement(by); return true; }
        catch (org.openqa.selenium.NoSuchElementException e) { return false; }
    }

    private void limparEnviar(WebElement el, String valor) {
        el.click();
        el.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        el.sendKeys(Keys.DELETE);
        el.sendKeys(valor);
    }

    /** Heurística simples: se o header já vier "RJ" usamos; tenta mapear alguns nomes comuns 
    private String extrairUf(String header) {
        String t = header.trim();
        if (t.length() == 2) return t.toUpperCase(Locale.ROOT);
        Map<String,String> map = Map.ofEntries(
            Map.entry("Rio de Janeiro","RJ"), Map.entry("São Paulo","SP"), Map.entry("Belo Horizonte","MG"),
            Map.entry("Vitória","ES"), Map.entry("Curitiba","PR"), Map.entry("Florianópolis","SC"),
            Map.entry("Porto Alegre","RS"), Map.entry("Belém","PA"), Map.entry("Manaus","AM"),
            Map.entry("Brasília","DF")
        );
        return map.getOrDefault(t, null);
    }*/
}
