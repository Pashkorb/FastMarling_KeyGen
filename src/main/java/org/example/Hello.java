package org.example;

import org.example.Service.LicenseManager;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Hello extends JFrame {
    private JPanel mainPanel;
    private JButton сгенерироватьButton;
    private JFormattedTextField textDataEnd;
    private JTextField textLicenseNum;
    private JTextArea textFieldLicense;
    private JScrollPane scrollPane;

    public Hello() throws HeadlessException {
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Дата окончания
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Дата окончания (ГГГГ-ММ-ДД):"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        addMaskedTextField();
        mainPanel.add(textDataEnd, gbc);

        // Номер лицензии
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("Номер лицензии:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        textLicenseNum = new JTextField(20);
        textLicenseNum.setEditable(false);
        mainPanel.add(textLicenseNum, gbc);

        // Лицензионный ключ
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel licenseLabel = new JLabel("Лицензионный ключ:");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(licenseLabel, gbc);

        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        textFieldLicense = new JTextArea(5, 20);
        textFieldLicense.setEditable(false);
        textFieldLicense.setLineWrap(true);
        textFieldLicense.setWrapStyleWord(true);

        // Добавляем обработчик клика
        textFieldLicense.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!textFieldLicense.getText().isEmpty()) {
                    Toolkit.getDefaultToolkit()
                            .getSystemClipboard()
                            .setContents(new StringSelection(textFieldLicense.getText()), null);
                    JOptionPane.showMessageDialog(
                            Hello.this,
                            "Содержимое скопировано!",
                            "Копирование",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });

        scrollPane = new JScrollPane(textFieldLicense);
        mainPanel.add(scrollPane, gbc);

        // Кнопка
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        сгенерироватьButton = new JButton("Сгенерировать");
        сгенерироватьButton.addActionListener(this::generateLicense);
        mainPanel.add(сгенерироватьButton, gbc);

        add(mainPanel);
        setVisible(true);
    }

    private void addMaskedTextField() {
        try {
            MaskFormatter dateMask = new MaskFormatter("####-##-##");
            dateMask.setPlaceholderCharacter('_');
            textDataEnd = new JFormattedTextField(dateMask);
            textDataEnd.setColumns(10);
        } catch (ParseException e) {
            throw new RuntimeException("Ошибка создания маски даты", e);
        }
    }

    private void generateLicense(ActionEvent e) {
        try {
            String dateStr = textDataEnd.getText().replace("_", "");
            if (dateStr.length() != 10) throw new DateTimeParseException("Неверный формат даты", dateStr, 0);

            LocalDate expirationDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            String licenseKey = LicenseManager.generateLicenseKey(expirationDate);

            textLicenseNum.setText(LicenseManager.getLicenseCode(licenseKey));
            textFieldLicense.setText(licenseKey);

        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Неверный формат даты! Используйте ГГГГ-ММ-ДД",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}