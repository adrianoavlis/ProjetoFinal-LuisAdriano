package com.api.shop2.service;

import com.api.shop2.model.CestaBasica;
import com.api.shop2.repository.CestaBasicaRepository;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CestaBasicaService {

    private final CestaBasicaRepository repository;

    public void scrapeAndSave(String filePath) {
        WebDriver driver = new ChromeDriver();
        try {
            driver.get(filePath);

            WebElement table = driver.findElement(By.id("dados"));
            List<WebElement> rows = table.findElements(By.tagName("tr"));

            // Pega o cabeçalho (cidades)
            List<WebElement> headers = table.findElements(By.tagName("th"));
            String[] estados = headers.stream().skip(1).map(WebElement::getText).toArray(String[]::new);

            for (WebElement row : rows) {
                List<WebElement> cols = row.findElements(By.tagName("td"));
                if (cols.isEmpty()) continue;

                String mesAno = cols.get(0).getText();

                for (int i = 1; i < cols.size(); i++) {
                    String valorStr = cols.get(i).getText().replace(",", ".").trim();
                    if (!valorStr.equals("-") && !valorStr.isEmpty()) {
                        Double valor = Double.parseDouble(valorStr);
                        CestaBasica cb = CestaBasica.builder()
                                .estado(estados[i - 1])
                                .mesAno(mesAno)
                                .valor(valor)
                                .build();
                        repository.save(cb);
                    }
                }
            }
        } finally {
            driver.quit();
        }
    }
}
