package org.example;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.NoSuchElementException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class InstagramPostCounter {

    // Инициализация логгера
    private static final Logger logger = Logger.getLogger(VKPostCounter.class.getName());

    public static void parse(String username, String password, String targetUser, LocalDate startDate, LocalDate endDate) throws InterruptedException {

        // Установка уровня логирования на INFO
        logger.setLevel(Level.INFO);

        // Создание файла обработчика и настройка формата логирования
        try {
            FileHandler fh = new FileHandler("instagram.log");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);

            // Пишем лог
            logger.info("Instagram parsing start");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "File logger not working", e);
        }


        // Установка пути к драйверу Chrome
        System.setProperty("webdriver.chrome.driver", "chrome/chromedriver.exe");

        // Настройка параметров Chrome
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");

        // Создание экземпляра WebDriver для Chrome
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        driver.manage().window().maximize();

        try {
            // Авторизация в Instagram (если необходимо)
            driver.get("https://www.instagram.com");
            Thread.sleep(2000); // Подождите загрузки страницы


            // Вход в аккаунт
            driver.findElement(By.name("username")).sendKeys(username);
            driver.findElement(By.name("password")).sendKeys(password);
            driver.findElement(By.xpath("//button[@type='submit']")).click();
            Thread.sleep(30000); // Подождите загрузки страницы

            // Переход на страницу пользователя
            driver.get("https://www.instagram.com/" + targetUser + "/");
            Thread.sleep(5000); // Подождите загрузки страницы

            // Инициализация переменных для даты и времени
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int postCount = 0;
            boolean hasMorePosts = true;
            Set<String> processedPosts = new HashSet<>();

            // Основной цикл для обхода постов
            while (hasMorePosts) {
                List<WebElement> posts = driver.findElements(By.xpath("//a[contains(@href, '/p/') or contains(@href, '/reel/')]"));

                for (WebElement post : posts) {
                    try {
                        String postUrl = post.getAttribute("href");

                        if (processedPosts.contains(postUrl)) {
                            logger.info("Пост уже отработан: " + postUrl);
                            continue; // Пропускаем уже обработанные посты
                        }

                        // Открываем пост в новой вкладке
                        ((JavascriptExecutor) driver).executeScript("window.open(arguments[0]);", postUrl);
                        String newTab = driver.getWindowHandles().stream().skip(1).findFirst().get();
                        driver.switchTo().window(newTab);

                        Thread.sleep(2000); // Подождите загрузки страницы

                        WebElement timeElement = driver.findElement(By.xpath("//time"));
                        String dateTimeString = timeElement.getAttribute("datetime").split("T")[0];
                        LocalDate postDate = LocalDate.parse(dateTimeString, formatter);
                        logger.info("Current post:" + postUrl + " Post date: " + postDate);

                        // Проверяем, что дата поста находится в заданном диапазоне
                        if (postDate.isBefore(startDate)) {
                            hasMorePosts = false;
                            driver.close();
                            driver.switchTo().window(driver.getWindowHandles().iterator().next());
                            break;
                        }
                        if (!postDate.isAfter(endDate)) {
                            postCount++;
                        }

                        processedPosts.add(postUrl); // Добавляем обработанный пост в набор

                        // Закрываем текущую вкладку и возвращаемся на основную страницу профиля
                        driver.close();
                        driver.switchTo().window(driver.getWindowHandles().iterator().next());

                        Thread.sleep(2000); // Подождите загрузки страницы
                    } catch (StaleElementReferenceException | TimeoutException | NoSuchElementException e) {
                        // Обработка ошибок и обновление списка постов
                        logger.log(Level.SEVERE, "Ошибка при обработке поста: " + e.getMessage());
                        System.out.println("Ошибка при обработке поста: " + e.getMessage());
                        posts = driver.findElements(By.xpath("//a[contains(@href, '/p/') or contains(@href, '/reel/')]"));
                    }
                }

                // Прокрутка вниз для загрузки дополнительных постов
                if (hasMorePosts) {
                    logger.info("Прокрутка");
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    Thread.sleep(3000);  // Подождите, пока новые посты загрузятся
                }
            }

            System.out.println("INSTAGRAM. Количество постов c " + startDate + " по " + endDate + ": " + postCount);
            logger.info("INSTAGRAM. Количество постов c " + startDate + " по " + endDate + ": " + postCount);
        } finally {
            // Закрытие WebDriver
            driver.quit();
        }
    }
}