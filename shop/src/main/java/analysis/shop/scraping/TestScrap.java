package analysis.shop.scraping;

/*import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import analysis.shop.model.Oferta;

//import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
*/
public class TestScrap {

	/*
	 * public List<Map<String, String>> scrape() { Oferta oferta = new Oferta();
	 * List<Map<String, String>> cards = new ArrayList<>(); ChromeOptions options =
	 * new ChromeOptions(); options.addArguments("--headless"); WebDriver driver =
	 * new ChromeDriver(options);
	 * 
	 * try { driver.get("https://redesupermarket.com.br/ofertas/");
	 * 
	 * Thread.sleep(3000);
	 * 
	 * String periodoValidade = "N/A";
	 * 
	 * try { WebElement validadeTag =
	 * driver.findElement(By.xpath("//p[contains(text(),'Ofertas válidas de')]"));
	 * String textoValidade = validadeTag.getText(); Pattern pattern =
	 * Pattern.compile("Ofertas válidas de (.+?),"); Matcher matcher =
	 * pattern.matcher(textoValidade); if (matcher.find()) { periodoValidade =
	 * matcher.group(1); } } catch (Exception e) {
	 * System.out.println("Não foi possível extrair o período de validade."); }
	 * 
	 * 
	 * List<WebElement> anuncios =
	 * driver.findElements(By.cssSelector("article.card-product"));
	 * 
	 * for (WebElement anuncio : anuncios) { Map<String, String> card = new
	 * HashMap<>();
	 * 
	 * WebElement imageTag = safeFind(anuncio,
	 * By.cssSelector("figure.card-product__image img")); card.put("image", imageTag
	 * != null ? imageTag.getAttribute("src") : "N/A");
	 * oferta.setImagemUrl(imageTag.getAttribute("src"));
	 * 
	 * WebElement titleTag = safeFind(anuncio,
	 * By.cssSelector("h3.card-product__title")); card.put("name", titleTag != null
	 * ? titleTag.getText().trim() : "N/A");
	 * oferta.setNome(titleTag.getText().trim());
	 * 
	 * WebElement priceTag = safeFind(anuncio,
	 * By.cssSelector("span.card-product__price")); card.put("price", priceTag !=
	 * null ? priceTag.getText().replace("\n", "").replace(" ", "") : "N/A");
	 * oferta.setPreco(priceTag.getText().replace("\n", "").replace(" ", "") );
	 * 
	 * System.out.println(titleTag.getText().trim()); //
	 * System.out.println(sealTag.getText().trim() : "N/A");
	 * System.out.println(priceTag.getText().replace("\n", "").replace(" ", ""));
	 * 
	 * oferta.setDataValidade(periodoValidade);
	 * 
	 * cards.add(card); }
	 * 
	 * // Extração do período de validade da oferta WebElement validadeTag =
	 * driver.findElement(By.xpath("//p[contains(text(),'Ofertas válidas de')]"));
	 * String textoValidade = validadeTag.getText();
	 * 
	 * Pattern pattern = Pattern.compile("Ofertas válidas de (.+?),"); Matcher
	 * matcher = pattern.matcher(textoValidade); if (matcher.find()) { String
	 * periodoValidade = matcher.group(1);
	 * System.out.println("Período de validade: " + periodoValidade); } else {
	 * System.out.println("Período de validade não encontrado."); }
	 * 
	 * // saveToCsv(cards); // saveToXls(cards); // você pode implementar com Apache
	 * POI, se quiser o XLS
	 * 
	 * } catch (Exception e) { e.printStackTrace(); } finally { driver.quit(); }
	 * return cards;
	 * 
	 * }
	 * 
	 * private static WebElement safeFind(WebElement root, By selector) { try {
	 * return root.findElement(selector); } catch (Exception e) { return null; } }
	 * 
	 * public static void main(String[] args) throws InterruptedException {
	 * TestScrap scraper = new TestScrap(); scraper.scrape();
	 * 
	 * driver.get("https://www.supermercadosguanabara.com.br/produtos/82");System.
	 * out.println(driver.getTitle());
	 * 
	 * WebElement search = driver .findElement(By.xpath(
	 * "/html/body/section/div[4]/div/div[3]/div/div/div[1]/div/div[2]/div/div"));
	 * 
	 * search.sendKeys("Teste");search.submit();Thread.sleep(2000);
	 * 
	 * assertTrue(driver.getTitle().contains("Teste - Pesquisa Google"));
	 * System.out.println(driver.getTitle());
	 * 
	 * driver.quit(); }
	 * 
	 * 
	 * }
	 */

}