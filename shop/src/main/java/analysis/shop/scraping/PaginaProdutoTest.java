package analysis.shop.scraping;

import analysis.shop.model.Produto;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaginaProdutoTest {

    private WebDriver driver;

    @BeforeAll
    public static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setupTest() {
        driver = new ChromeDriver();
        driver.get("https://www.supermercadosguanabara.com.br/produtos/82"); 
    }

    @Test
    public void testExtrairProdutosComImagens() {
        PaginaProduto pagina = new PaginaProduto(driver);
        List<Produto> produtos = pagina.extrairProdutos();

        assertFalse(produtos.isEmpty(), "A lista de produtos não deve estar vazia.");

        for (Produto produto : produtos) {
            assertNotNull(produto.getNome(), "Nome do produto não deve ser nulo.");
            assertTrue(produto.getPreco() > 0, "Preço deve ser maior que zero.");
            assertTrue(new java.io.File(produto.getImagemUrl()).exists(), "Arquivo de imagem deve existir: " + produto.getImagemUrl());
        }
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
