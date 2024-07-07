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
import java.time.LocalDate;;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class VKPostCounter {

    // Инициализация логгера
    private static final Logger logger = Logger.getLogger(VKPostCounter.class.getName());

    public static void parse(String username, String password, String targetUser, LocalDate startDate, LocalDate endDate) throws InterruptedException {

        // Установка уровня логирования на INFO
        logger.setLevel(Level.INFO);

        // Создание файла обработчика и настройка формата логирования
        try {
            FileHandler fh = new FileHandler("vk.log");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);

            // Пишем лог
            logger.info("VK parsing start");
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
            // Авторизация в VK (если необходимо)
            //driver.get("https://vk.com");
            //Thread.sleep(2000); // Подождите загрузки страницы

            // Вход в аккаунт
            //driver.findElement(By.id("index_email")).sendKeys(username);
            //driver.findElement(By.id("index_pass")).sendKeys(password);
            //driver.findElement(By.cssSelector("#index_login > div > form > button")).click();
            //Thread.sleep(25000); // Подождите загрузки страницы

            // Переход на страницу пользователя
            driver.get("https://vk.com/" + targetUser);
            Thread.sleep(5000); // Подождите загрузки страницы

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy");

            int currentYear = LocalDate.now().getYear();

            int postCount = 0;
            boolean hasMorePosts = true;
            Set<String> processedPosts = new HashSet<>();

            // Основной цикл для обхода постов
            while (hasMorePosts) {
                List<WebElement> posts = driver.findElements(By.cssSelector(".post"));

                for (WebElement post : posts) {
                    try {
                        String postId = post.getAttribute("id");

                        if (processedPosts.contains(postId)) {
                            logger.info("Пост уже отработан " + postId);
                            continue; // Пропускаем уже обработанные посты
                        }

                        WebElement timeElement = post.findElement(By.cssSelector("time.PostHeaderSubtitle__item"));
                        String dateTimeString = timeElement.getText();
                        LocalDate postDate;

                        try {
                            // Проверяем, заканчивается ли строка на "г."
                            if (dateTimeString.contains(" в ")) {
                                String date = dateTimeString.split(" в ")[0].trim();
                                // Добавляем текущий год для дат без года
                                String dateTimeStringWithYear = date + " " + currentYear;
                                String formattedDate = formatToFullMonthName(dateTimeStringWithYear);
                                postDate = LocalDate.parse(formattedDate, formatter);
                            } else {
                                String formattedDate = formatToFullMonthName(dateTimeString);
                                postDate = LocalDate.parse(formattedDate, formatter);
                            }
                        } catch (DateTimeParseException e) {
                            logger.log(Level.SEVERE, "Ошибка парсинга даты: " + e.getMessage());
                            System.out.println("Ошибка парсинга даты: " + e.getMessage());
                            continue;
                        }
                        logger.info("Post date: " + dateTimeString + " Processed date: " + postDate + " id: " + postId);
                        // Проверяем, что дата поста находится в заданном диапазоне
                        if (postDate.isBefore(startDate)) {
                            hasMorePosts = false;
                            break;
                        }
                        if (!postDate.isAfter(endDate)) {
                            postCount++;
                        }

                        processedPosts.add(postId); // Добавляем обработанный пост в набор
                    } catch (StaleElementReferenceException | TimeoutException | NoSuchElementException e) {
                        // Обработка ошибок и обновление списка постов
                        logger.log(Level.SEVERE, "Ошибка при обработке поста: " + e.getMessage());
                        System.out.println("Ошибка при обработке поста: " + e.getMessage());
                        posts = driver.findElements(By.cssSelector(".post"));
                    }
                }

                // Прокрутка вниз для загрузки дополнительных постов
                if (hasMorePosts) {
                    logger.info("Прокрутка");
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    Thread.sleep(3000);  // Подождите, пока новые посты загрузятся
                }
            }

            System.out.println("VK. Количество постов c " + startDate + " по " + endDate + ": " + postCount);
            logger.info("VK. Количество постов c " + startDate + " по " + endDate + ": " + postCount);

        } finally {
            // Закрытие WebDriver
            driver.quit();
        }
    }

    // Метод для создания карты сопоставления трехбуквенных сокращений месяцев с полными названиями
    private static Map<String, String> createMonthMap() {
        Map<String, String> monthMap = new HashMap<>();
        monthMap.put("янв", "января");
        monthMap.put("фев", "февраля");
        monthMap.put("мар", "марта");
        monthMap.put("апр", "апреля");
        monthMap.put("мая", "мая");
        monthMap.put("июн", "июня");
        monthMap.put("июл", "июля");
        monthMap.put("авг", "августа");
        monthMap.put("сен", "сентября");
        monthMap.put("окт", "октября");
        monthMap.put("ноя", "ноября");
        monthMap.put("дек", "декабря");
        return monthMap;
    }

    public static String formatToFullMonthName(String inputDateStr) {
        // Создаем карту для сопоставления трехбуквенных сокращений месяцев с полными названиями
        Map<String, String> monthMap = createMonthMap();

        // Разбиваем строку на части: день, сокращение месяца и год
        String[] parts = inputDateStr.split(" ");
        if (parts.length != 3) {
            logger.log(Level.SEVERE, "Invalid date format: " + inputDateStr);
            throw new IllegalArgumentException("Invalid date format: " + inputDateStr);
        }

        // Получаем сокращение месяца
        String monthAbbreviation = parts[1];

        // Получаем полное название месяца из карты
        String fullMonthName = monthMap.get(monthAbbreviation);
        if (fullMonthName == null) {
            logger.log(Level.SEVERE, "Unknown month abbreviation: " + monthAbbreviation);
            throw new IllegalArgumentException("Unknown month abbreviation: " + monthAbbreviation);
        }

        // Форматируем и возвращаем дату с полным названием месяца
        return String.format("%s %s %s", parts[0], fullMonthName, parts[2]);
    }
}