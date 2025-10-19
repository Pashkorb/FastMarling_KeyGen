package org.example;

import org.example.Service.LicenseManager;

import javax.swing.*;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        LocalDate expirationDate = LocalDate.of(2025, 12, 31);
        System.setProperty("sun.java2d.uiScale", "1.0");
        Hello helloForm = new Hello();
        helloForm.setVisible(true);
    }
}


