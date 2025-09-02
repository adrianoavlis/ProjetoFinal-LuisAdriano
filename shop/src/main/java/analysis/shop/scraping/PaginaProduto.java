package analysis.shop.scraping;

import analysis.shop.model.Produto;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PaginaProduto {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Random random = new Random();

    public PaginaProduto(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public List<Produto> extrairProdutos() {
        List<Produto> produtos = new ArrayList<>();

        try {
            // Espera até que ao menos um produto esteja presente
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/section/div[4]/div/div[3]/div/div/div")));
        } catch (TimeoutException e) {
            System.out.println("Timeout: Nenhum produto encontrado na página.");
            return produtos;
        }

        List<WebElement> elementos = driver.findElements(By.xpath("/html/body/section/div[4]/div/div[3]/div/div/div"));

        for (WebElement el : elementos) {
            try {
                Produto p = new Produto();

                p.setId(UUID.randomUUID().toString());
                p.setNome(getText(el, "./div/div[2]/div/div"));
                p.setPreco(parsePreco(getText(el, "./div/div[1]/div/div/span")));

                String imageUrl = getImage(el, "./div/div[1]//img");
                String localImagePath = baixarImagem(imageUrl, p.getId());
                p.setImagemUrl(localImagePath);

                produtos.add(p);

                // Pausa humanizada entre extrações (500ms a 1500ms)
                Thread.sleep(500 + random.nextInt(1000));

            } catch (Exception e) {
                System.err.println("Erro ao extrair produto: " + e.getMessage());
            }
        }

        return produtos;
    }

    private String getText(WebElement el, String selector) {
        try {
            return el.findElement(By.xpath(selector)).getText().trim();
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    private String getImage(WebElement el, String selector) {
        try {
            return el.findElement(By.xpath(selector)).getAttribute("src");
        } catch (NoSuchElementException e) {
            return "";
        }
    }

    private double parsePreco(String text) {
        try {
            return Double.parseDouble(text.replaceAll("[^0-9,\\.]", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String baixarImagem(String imageUrl, String idProduto) {
        if (imageUrl == null || imageUrl.isEmpty()) return "";

        try (InputStream in = new URL(imageUrl).openStream()) {
            String nomeArquivo = "target/" + idProduto + ".jpg";
            File file = new File(nomeArquivo);
            file.getParentFile().mkdirs();

            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return nomeArquivo;
        } catch (Exception e) {
            System.err.println("Erro ao baixar imagem: " + imageUrl);
            return "";
        } finally {
		}
    }
}
