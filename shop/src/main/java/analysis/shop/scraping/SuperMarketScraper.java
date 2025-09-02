package analysis.shop.scraping;

// import io.github.bonigarcia.wdm.WebDriverManager;

/*import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
*/
public class SuperMarketScraper {
	/*
    public List<Map<String, String>> scrape() {
        List<Map<String, String>> cards = new ArrayList<>();

            System.setProperty("webdriver.chrome.driver", "C:\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // opcional: roda em modo invisível
        WebDriver driver = new ChromeDriver(options);

        try {
            driver.get("https://redesupermarket.com.br/ofertas/");

            List<WebElement> anuncios = driver.findElements(By.cssSelector("article.card-product"));

            for (WebElement anuncio : anuncios) {
                Map<String, String> card = new HashMap<>();

                // Selo
                WebElement sealTag = safeFind(anuncio, By.cssSelector("span.card-product__seal-inner"));
                card.put("seal", sealTag != null ? sealTag.getText().trim() : "N/A");

                // Imagem
                WebElement imageTag = safeFind(anuncio, By.cssSelector("figure.card-product__image img"));
                card.put("image", imageTag != null ? imageTag.getAttribute("src") : "N/A");

                // Título
                WebElement titleTag = safeFind(anuncio, By.cssSelector("h3.card-product__title"));
                card.put("name", titleTag != null ? titleTag.getText().trim() : "N/A");

                // Preço
                WebElement priceTag = safeFind(anuncio, By.cssSelector("span.card-product__price"));
                card.put("price", priceTag != null ? priceTag.getText().replace("\n", "").replace(" ", "") : "N/A");

                cards.add(card);
            }

          //  saveToCsv(cards);
           // saveToXls(cards); // você pode implementar com Apache POI, se quiser o XLS

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return cards;
    }

    private WebElement safeFind(WebElement root, By selector) {
        try {
            return root.findElement(selector);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveToCsv(List<Map<String, String>> cards) {
        String csvPath = "C:\\Users\\PC\\Projetos\\ProjetoFinal-LuisAdriano\\ASSETS\\data\\WebData\\supermarket\\datasetSupermarket.csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvPath, false))) {
            writer.println("seal;image;name;price");
            for (Map<String, String> card : cards) {
                writer.printf("%s;%s;%s;%s%n",
                        card.get("seal"),
                        card.get("image"),
                        card.get("name"),
                        card.get("price"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveToXls(List<Map<String, String>> cards) {
        // Opcional: implementar com Apache POI, se desejar exportar como XLS
    }
    
    public static void main(String[] args) {
    	SuperMarketScraper scraper = new SuperMarketScraper();
        scraper.scrape();
    }
    
    public static void main(String[] args) {
        WebDriverManager.chSromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get("https://example.com");
        driver.quit();
    }*/
}


