package br.com.shop2.domain.service;


import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.WebDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeleniumConfig {
    @Bean(destroyMethod = "quit")
    public WebDriver webDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("--headless=new","--no-sandbox","--disable-dev-shm-usage","--window-size=1920,1080");
        return new ChromeDriver(opt);
    }
}