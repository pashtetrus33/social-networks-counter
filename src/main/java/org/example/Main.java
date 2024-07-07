package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream(Paths.get("config.txt").toFile())) {
            props.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String vkUsername = props.getProperty("vk.username");
        String vkPassword = props.getProperty("vk.password");
        String vkTargetUser = props.getProperty("vk.targetUser");

        String instUsername = props.getProperty("inst.username");
        String instPassword = props.getProperty("inst.password");
        String instTargetUser = props.getProperty("inst.targetUser");

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(props.getProperty("startDate"));
            endDate = LocalDate.parse(props.getProperty("endDate"));
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return;
        }

        String method = props.getProperty("method");

        switch (method.toLowerCase()) {
            case "vk":
                VKPostCounter.parse(vkUsername, vkPassword, vkTargetUser, startDate, endDate);
                break;
            case "instagram":
                InstagramPostCounter.parse(instUsername, instPassword, instTargetUser, startDate, endDate);
                break;
            case "both":
                VKPostCounter.parse(vkUsername, vkPassword, vkTargetUser, startDate, endDate);
                InstagramPostCounter.parse(instUsername, instPassword, instTargetUser, startDate, endDate);
                break;
            default:
                System.out.println("Invalid method specified in config.txt");
                break;
        }
    }
}