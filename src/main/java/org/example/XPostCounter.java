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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;


public class XPostCounter {

    // Инициализация логгера
    private static final Logger logger = Logger.getLogger(XPostCounter.class.getName());

    public static void parse(String phone, String username, String password, String targetUser, LocalDate startDate, LocalDate endDate) throws InterruptedException {

        // Установка уровня логирования на INFO
        logger.setLevel(Level.INFO);

        // Создание файла обработчика и настройка формата логирования
        try {
            FileHandler fh = new FileHandler("x.log");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);

            // Пишем лог
            logger.info("X parsing start");
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
            // Авторизация в Twitter (если необходимо)
            driver.get("https://x.com/login");
            Thread.sleep(2000); // Подождите загрузки страницы

            // Вход в аккаунт
            driver.findElement(By.name("text")).sendKeys(username);
            // Находим кнопку по CSS-селектору
            // Находим кнопку по XPath
            WebElement button = driver.findElement(By.xpath("//button[@role='button' and @type='button' and .//span[text()='Далее']]"));

            // Кликаем на кнопку
            button.click();

            // Находим элемент по атрибуту data-testid
            WebElement inputField = driver.findElement(By.cssSelector("input[data-testid='ocfEnterTextTextInput']"));
            Thread.sleep(3000); // Подождите загрузки страницы
            // Вставляем текст в поле
            inputField.sendKeys(phone);

            // Находим кнопку по XPath
            button = driver.findElement(By.xpath("//button[@role='button' and @type='button' and .//span[text()='Далее']]"));

            // Кликаем на кнопку
            button.click();
            Thread.sleep(3000); // Подождите загрузки страницы
            // Находим элемент по атрибуту name
            WebElement passwordField = driver.findElement(By.cssSelector("input[name='password']"));

            // Вставляем текст в поле
            passwordField.sendKeys(password);

            // Находим кнопку по XPath
            button = driver.findElement(By.xpath("//button[@role='button' and @type='button' and .//span[text()='Войти']]"));

            // Кликаем на кнопку
            button.click();



            // driver.findElement(By.name("session[password]")).sendKeys(password);

                Thread.sleep(10000); // Подождите загрузки страницы

                // Переход на страницу пользователя
                driver.get("https://twitter.com/" + targetUser);
                Thread.sleep(5000); // Подождите загрузки страницы

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy");

                int postCount = 0;
                boolean hasMorePosts = true;
                Set<String> processedPosts = new HashSet<>();

                // Основной цикл для обхода твитов
                while (hasMorePosts) {
                    List<WebElement> tweets = driver.findElements(By.cssSelector("article"));

                    for (WebElement tweet : tweets) {
                        try {
                            // Пропуск закрепленных твитов
                            List<WebElement> elements = tweet.findElements(By.cssSelector("div[data-testid='socialContext']"));
                            if (elements.size() > 0 && elements.get(0).getText().contains("Закреплено")) {
                                continue;
                            }

                            WebElement userDiv = tweet.findElement(By.cssSelector("[data-testid='User-Name']"));
                            String tweetId = userDiv.getAttribute("id");
                            // Находим все ссылки внутри div
                            List<WebElement> anchors = userDiv.findElements(By.tagName("a"));
                            // Находим последнюю ссылку (последний элемент в списке)
                            WebElement lastAnchor = anchors.get(anchors.size() - 1);

                            // Получаем значение атрибута href
                            String hrefValue = lastAnchor.getAttribute("href");

                            if (processedPosts.contains(tweetId)) {
                                logger.info("Твит уже обработан " + tweetId + ": " + hrefValue);
                                continue; // Пропускаем уже обработанные твиты
                            }


                            WebElement timeElement = tweet.findElement(By.cssSelector("time"));
                            String dateTimeString = timeElement.getAttribute("datetime");
                            LocalDate postDate;

                            try {
                                // Преобразование даты из формата Twitter в LocalDate
                                postDate = LocalDate.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                            } catch (DateTimeParseException e) {
                                logger.log(Level.SEVERE, "Ошибка парсинга даты: " + e.getMessage());
                                System.out.println("Ошибка парсинга даты: " + e.getMessage());
                                continue;
                            }
                            logger.info("Post date: " + dateTimeString + " Processed date: " + postDate + " id:" + tweetId + ": " + hrefValue);
                            // Проверяем, что дата твита находится в заданном диапазоне
                            if (postDate.isBefore(startDate)) {
                                hasMorePosts = false;
                                break;
                            }
                            if (!postDate.isAfter(endDate)) {
                                postCount++;
                            }

                            processedPosts.add(tweetId); // Добавляем обработанный твит в набор
                        } catch (StaleElementReferenceException | TimeoutException | NoSuchElementException e) {
                            // Обработка ошибок и обновление списка твитов
                            logger.log(Level.SEVERE, "Ошибка при обработке твита: " + e.getMessage());
                            System.out.println("Ошибка при обработке твита: " + e.getMessage());
                            tweets = driver.findElements(By.cssSelector("article"));
                        }
                    }

                    // Прокрутка вниз для загрузки дополнительных твитов
                    if (hasMorePosts) {
                        logger.info("Прокрутка");
                        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                        Thread.sleep(3000);  // Подождите, пока новые твиты загрузятся
                    }
                }

                System.out.println("X. Количество твитов c " + startDate + " по " + endDate + ": " + postCount);
                logger.info("X. Количество твитов c " + startDate + " по " + endDate + ": " + postCount);

            } finally {
                // Закрытие WebDriver
                driver.quit();
            }
        }
    }
