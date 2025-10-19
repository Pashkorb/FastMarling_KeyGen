package org.example;

import org.example.Service.LicenseManager;
import org.example.Service.LicenseRequest;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Hello extends JFrame {
    private static final SoftwareVersion[] VERSION_ORDER = {
            SoftwareVersion.V1_01,
            SoftwareVersion.V1_11,
            SoftwareVersion.V2_01,
            SoftwareVersion.V2_11
    };
    private static final String LICENSE_FILE_NAME = "fastmarking_license.jwt";
    private JPanel mainPanel;

    private final EnumMap<SoftwareVersion, LegacyView> legacyViews = new EnumMap<>(SoftwareVersion.class);
    private final EnumSet<SoftwareVersion> readmeWarningShown = EnumSet.noneOf(SoftwareVersion.class);
    private FeatureVersionView v201View;
    private ModernVersionView v211View;
    private final JTabbedPane tabbedPane;

    public Hello() throws HeadlessException {
        super("FastMarling KeyGen");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        for (SoftwareVersion version : VERSION_ORDER) {
            switch (version) {
                case V1_01, V1_11 -> {
                    LegacyView view = createLegacyView(version);
                    legacyViews.put(version, view);
                    tabbedPane.addTab(version.getDisplayName(), view.panel);
                }
                case V2_01 -> {
                    v201View = createFeatureVersionView(version);
                    tabbedPane.addTab(version.getDisplayName(), v201View.panel);
                }
                case V2_11 -> {
                    v211View = createModernVersionView(version);
                    tabbedPane.addTab(version.getDisplayName(), v211View.panel);
                }
            }
        }

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int index = tabbedPane.getSelectedIndex();
                if (index >= 0 && index < VERSION_ORDER.length) {
                    onVersionSelected(VERSION_ORDER[index]);
                }
            }
        });

        add(tabbedPane, BorderLayout.CENTER);
        onVersionSelected(VERSION_ORDER[0]);
        setVisible(true);
    }

    private LegacyView createLegacyView(SoftwareVersion version) {
        LegacyView view = new LegacyView();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = createDefaultConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("О версии:"), gbc);

        view.readmeArea = createReadOnlyArea();
        view.readmeArea.setRows(10);
        view.readmeArea.setText("Загрузка...");
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(view.readmeArea), gbc);

        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Дата окончания (ГГГГ-ММ-ДД):"), gbc);

        view.expirationField = createMaskedDateField();
        gbc.gridx = 1;
        panel.add(view.expirationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Номер лицензии:"), gbc);

        view.licenseNumberField = createReadOnlyField();
        gbc.gridx = 1;
        panel.add(view.licenseNumberField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Лицензионный ключ:"), gbc);

        view.licenseArea = createReadOnlyArea();
        attachCopyToClipboard(view.licenseArea);
        gbc.gridy = 5;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(view.licenseArea), gbc);

        view.generateButton = new JButton("Сгенерировать");
        view.generateButton.addActionListener(e -> generateLicenseForLegacy(version));
        gbc.gridy = 6;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(view.generateButton, gbc);

        view.panel = panel;
        return view;
    }

    private FeatureVersionView createFeatureVersionView(SoftwareVersion version) {
        FeatureVersionView view = new FeatureVersionView();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = createDefaultConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("О версии:"), gbc);

        view.readmeArea = createReadOnlyArea();
        view.readmeArea.setRows(10);
        view.readmeArea.setText("Загрузка...");
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(view.readmeArea), gbc);

        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 2;
        panel.add(new JLabel("Дата окончания (ГГГГ-ММ-ДД):"), gbc);

        view.expirationField = createMaskedDateField();
        gbc.gridx = 1;
        panel.add(view.expirationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Номер лицензии:"), gbc);

        view.licenseNumberField = createReadOnlyField();
        gbc.gridx = 1;
        panel.add(view.licenseNumberField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Компания:"), gbc);

        view.companyField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(view.companyField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Лицензионный ключ:"), gbc);

        view.licenseArea = createReadOnlyArea();
        attachCopyToClipboard(view.licenseArea);
        gbc.gridy = 6;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(view.licenseArea), gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Функции лицензии:"), gbc);

        view.checkBoxes = createFeatureCheckboxes();
        JPanel featurePanel = new JPanel(new GridLayout(0, 2, 5, 5));
        view.checkBoxes.forEach((flag, box) -> featurePanel.add(box));
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(featurePanel, gbc);

        view.selectAllButton = new JButton("Выбрать все");
        view.selectAllButton.addActionListener(e -> toggleSelectAll(view.checkBoxes));
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(view.selectAllButton, gbc);

        view.generateButton = new JButton("Сгенерировать");
        view.generateButton.addActionListener(e -> generateLicenseForFeatureVersion(version));
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(view.generateButton, gbc);

        view.panel = panel;
        return view;
    }

    private ModernVersionView createModernVersionView(SoftwareVersion version) {
        ModernVersionView view = new ModernVersionView();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = createDefaultConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(new JLabel("О версии:"), gbc);

        view.readmeArea = createReadOnlyArea();
        view.readmeArea.setRows(10);
        view.readmeArea.setText("Загрузка...");
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(view.readmeArea), gbc);

        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Дата окончания (ГГГГ-ММ-ДД):"), gbc);

        view.expirationField = createMaskedDateField();
        gbc.gridx = 1;
        panel.add(view.expirationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Номер лицензии:"), gbc);

        view.licenseNumberField = createReadOnlyField();
        gbc.gridx = 1;
        panel.add(view.licenseNumberField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Компания:"), gbc);

        view.companyField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(view.companyField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Функции лицензии:"), gbc);

        view.checkBoxes = createFeatureCheckboxes();
        JPanel featurePanel = new JPanel(new GridLayout(0, 2, 5, 5));
        view.checkBoxes.forEach((flag, box) -> featurePanel.add(box));
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(featurePanel, gbc);

        view.selectAllButton = new JButton("Выбрать все");
        view.selectAllButton.addActionListener(e -> toggleSelectAll(view.checkBoxes));
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(view.selectAllButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Флеш-накопитель:"), gbc);

        view.driveComboBox = new JComboBox<>();
        view.driveComboBox.setPrototypeDisplayValue(new DriveItem(null, "Съемный диск (X:)"));
        gbc.gridx = 0;
        gbc.gridy = 9;
        panel.add(view.driveComboBox, gbc);

        view.writeButton = new JButton("Записать лицензию");
        view.writeButton.addActionListener((ActionEvent e) -> generateAndWriteLicense(version));
        view.writeButton.setEnabled(false);
        gbc.gridx = 0;
        gbc.gridy = 10;
        panel.add(view.writeButton, gbc);

        view.panel = panel;
        return view;
    }

    private void onVersionSelected(SoftwareVersion version) {
        loadReadme(version);
        if (version == SoftwareVersion.V2_11) {
            updateDriveList();
        }
    }

    private void generateLicenseForLegacy(SoftwareVersion version) {
        LegacyView view = legacyViews.get(version);
        if (view == null) {
            return;
        }
        try {
            LocalDate expirationDate = extractDate(view.expirationField);
            String licenseKey = LicenseManager.generateLicenseKey(new LicenseRequest(expirationDate, version, List.of(), null, null));
            view.licenseNumberField.setText(Objects.requireNonNullElse(LicenseManager.getLicenseCode(licenseKey), ""));
            view.licenseArea.setText(licenseKey);
            view.licenseArea.setCaretPosition(0);
            Path targetDir = executePostGenerationActions(version);
            if (targetDir != null) {
                showPostGenerationSuccess(version, targetDir, null);
            }
        } catch (DateTimeParseException ex) {
            showInvalidDateDialog();
        }
    }

    private void generateLicenseForFeatureVersion(SoftwareVersion version) {
        if (v201View == null) {
            return;
        }
        try {
            LocalDate expirationDate = extractDate(v201View.expirationField);
            List<FeatureFlag> features = collectSelectedFeatures(v201View.checkBoxes);
            String company = v201View.companyField.getText();
            String licenseKey = LicenseManager.generateLicenseKey(new LicenseRequest(expirationDate, version, features, company, null));
            v201View.licenseNumberField.setText(Objects.requireNonNullElse(LicenseManager.getLicenseCode(licenseKey), ""));
            v201View.licenseArea.setText(licenseKey);
            v201View.licenseArea.setCaretPosition(0);
            Path targetDir = executePostGenerationActions(version);
            if (targetDir != null) {
                showPostGenerationSuccess(version, targetDir, null);
            }
        } catch (DateTimeParseException ex) {
            showInvalidDateDialog();
        }
    }

    private void generateAndWriteLicense(SoftwareVersion version) {
        if (v211View == null) {
            return;
        }
        try {
            LocalDate expirationDate = extractDate(v211View.expirationField);
            List<FeatureFlag> features = collectSelectedFeatures(v211View.checkBoxes);
            String company = v211View.companyField.getText();
            DriveItem selectedItem = (DriveItem) v211View.driveComboBox.getSelectedItem();
            if (selectedItem == null || selectedItem.root == null) {
                JOptionPane.showMessageDialog(this, "Выберите флешку для записи.", "Нет носителя", JOptionPane.WARNING_MESSAGE);
                return;
            }
            UsbDeviceInfo deviceInfo = readUsbDeviceInfo(selectedItem.root);
            if (deviceInfo.removable != null && !deviceInfo.removable) {
                JOptionPane.showMessageDialog(this,
                        "Выбрано не съёмное устройство. Нужна именно USB-флешка.",
                        "Неверный носитель",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String deviceFingerprint = computeDeviceFingerprint(deviceInfo);
            String licenseKey = LicenseManager.generateLicenseKey(
                    new LicenseRequest(expirationDate, version, features, company, deviceFingerprint));
            v211View.licenseNumberField.setText(Objects.requireNonNullElse(LicenseManager.getLicenseCode(licenseKey), ""));

            Path targetPath = writeLicenseToDrive(selectedItem.root, licenseKey);
            if (targetPath == null) {
                return;
            }

            String licenseInfo = "Файл fastmarking_license.jwt успешно записан на флешку.";
            Path targetDir = executePostGenerationActions(version);
            if (targetDir != null) {
                showPostGenerationSuccess(version, targetDir, licenseInfo + "\n" + targetPath);
            } else {
                JOptionPane.showMessageDialog(this,
                        licenseInfo + "\n" + targetPath,
                        "Лицензия записана",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            updateDriveList();
        } catch (DateTimeParseException ex) {
            showInvalidDateDialog();
        } catch (DeviceInfoException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка устройства", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Ошибка записи", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path writeLicenseToDrive(File drive, String licenseKey) throws IOException {
        if (drive == null) {
            throw new IOException("Носитель не выбран");
        }
        if (!drive.exists()) {
            throw new IOException("Носитель недоступен: " + drive.getAbsolutePath());
        }
        if (!drive.canWrite()) {
            throw new IOException("Нет доступа для записи на: " + drive.getAbsolutePath());
        }
        long requiredSpace = licenseKey.getBytes(StandardCharsets.UTF_8).length;
        if (drive.getUsableSpace() < requiredSpace) {
            throw new IOException("Недостаточно свободного места на носителе");
        }
        Path target = drive.toPath().resolve(LICENSE_FILE_NAME);
        if (Files.exists(target)) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Файл " + LICENSE_FILE_NAME + " уже существует на выбранной флешке. Перезаписать?",
                    "Подтверждение перезаписи",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        Files.writeString(target, licenseKey, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    private UsbDeviceInfo readUsbDeviceInfo(File drive) throws DeviceInfoException {
        if (drive == null || !drive.exists()) {
            throw new DeviceInfoException("Невозможно создать лицензию: устройство не подходит.");
        }
        boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (!isWindows) {
            throw new DeviceInfoException("Невозможно создать лицензию: устройство не имеет уникальных аппаратных данных (флешка не подходит).");
        }

        String rootPath = drive.getAbsolutePath();
        if (rootPath == null || rootPath.length() < 2) {
            throw new DeviceInfoException("Невозможно создать лицензию: устройство не подходит.");
        }
        String driveId = rootPath.substring(0, 2).toUpperCase(Locale.ROOT);

        CommandResult result;
        try {
            result = runPowerShell(buildDeviceInfoScript(driveId));
        } catch (IOException e) {
            throw new DeviceInfoException("Невозможно создать лицензию: устройство не подходит.", e);
        }

        if (result.exitCode != 0) {
            throw new DeviceInfoException("Невозможно создать лицензию: устройство не подходит.");
        }

        Map<String, String> values = parseKeyValueLines(result.lines);
        String serial = values.get("SerialNumber");
        if (serial != null) {
            serial = serial.trim();
        }
        if (serial == null || serial.isBlank()) {
            throw new DeviceInfoException("Невозможно определить аппаратный серийный номер флешки.");
        }

        String sizeValue = values.get("Size");
        if (sizeValue != null) {
            sizeValue = sizeValue.trim();
        }
        if (sizeValue == null || sizeValue.isBlank()) {
            throw new DeviceInfoException("Невозможно прочитать размер или количество секторов устройства.");
        }

        long capacity;
        try {
            capacity = Long.parseLong(sizeValue);
        } catch (NumberFormatException e) {
            throw new DeviceInfoException("Невозможно прочитать размер или количество секторов устройства.");
        }

        if (capacity <= 0) {
            throw new DeviceInfoException("Невозможно создать лицензию: устройство не имеет уникальных аппаратных данных (флешка не подходит).");
        }

        long sectors = capacity / 512L;
        if (sectors <= 0) {
            throw new DeviceInfoException("Невозможно прочитать размер или количество секторов устройства.");
        }

        Boolean removable = null;
        String driveTypeValue = values.get("DriveType");
        if (driveTypeValue != null && !driveTypeValue.isBlank()) {
            try {
                int driveType = Integer.parseInt(driveTypeValue.trim());
                removable = driveType == 2;
            } catch (NumberFormatException ignored) {
                // Игнорируем некорректное значение типа диска.
            }
        }

        return new UsbDeviceInfo(serial, capacity, sectors, removable);
    }

    private String buildDeviceInfoScript(String driveId) {
        return "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; " +
                "$drive = Get-CimInstance Win32_LogicalDisk -Filter \"DeviceID='" + driveId + "'\"; " +
                "if ($drive -eq $null) { exit 1 }; " +
                "$partition = $drive | Get-CimAssociatedInstance -ResultClassName Win32_DiskPartition; " +
                "if ($partition -eq $null) { exit 2 }; " +
                "$disk = $partition | Get-CimAssociatedInstance -ResultClassName Win32_DiskDrive; " +
                "if ($disk -eq $null) { exit 3 }; " +
                "Write-Output ('SerialNumber=' + ($disk.SerialNumber -as [string])); " +
                "Write-Output ('Size=' + ($disk.Size -as [string])); " +
                "Write-Output ('DeviceID=' + ($disk.DeviceID -as [string])); " +
                "Write-Output ('DriveType=' + ($drive.DriveType -as [string]));";
    }

    private CommandResult runPowerShell(String script) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        }
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PowerShell command interrupted", e);
        }
        return new CommandResult(exitCode, lines);
    }

    private Map<String, String> parseKeyValueLines(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String line : lines) {
            int delimiterIndex = line.indexOf('=');
            if (delimiterIndex <= 0) {
                continue;
            }
            String key = line.substring(0, delimiterIndex).trim();
            String value = line.substring(delimiterIndex + 1).trim();
            if (!key.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private String computeDeviceFingerprint(UsbDeviceInfo info) {
        String raw = info.serial + "|" + info.capacity + "|" + info.sectors;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

    private void updateDriveList() {
        if (v211View == null) {
            return;
        }
        List<DriveItem> drives = detectRemovableDrives();
        DefaultComboBoxModel<DriveItem> model = new DefaultComboBoxModel<>();
        for (DriveItem drive : drives) {
            model.addElement(drive);
        }
        v211View.driveComboBox.setModel(model);
        boolean hasDrives = !drives.isEmpty();
        v211View.driveComboBox.setEnabled(hasDrives);
        v211View.writeButton.setEnabled(hasDrives);
        if (!hasDrives) {
            v211View.driveComboBox.addItem(new DriveItem(null, "Носители не найдены"));
            v211View.driveComboBox.setEnabled(false);
        }
    }

    private List<DriveItem> detectRemovableDrives() {
        List<DriveItem> result = new ArrayList<>();
        File[] roots = File.listRoots();
        if (roots == null) {
            return result;
        }
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        boolean isWindows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        for (File root : roots) {
            if (!fileSystemView.isDrive(root)) {
                continue;
            }
            String description = fileSystemView.getSystemTypeDescription(root);
            boolean removable = description != null && (description.toLowerCase().contains("removable")
                    || description.contains("Съемный")
                    || description.contains("Съёмный"));
            if (!removable && isWindows && root.getPath().equalsIgnoreCase("C:\\")) {
                continue;
            }
            if (!removable && !isWindows) {
                continue;
            }
            String displayName = fileSystemView.getSystemDisplayName(root);
            if (displayName == null || displayName.isBlank()) {
                displayName = root.getPath();
            }
            result.add(new DriveItem(root, displayName.trim()));
        }
        return result;
    }

    private void loadReadme(SoftwareVersion version) {
        JTextArea readmeArea = findReadmeArea(version);
        if (readmeArea == null) {
            return;
        }
        Path readmePath = Paths.get("versions", version.getFolderName(), "readme.txt");
        if (!Files.exists(readmePath)) {
            readmeArea.setText("Файл readme не найден: " + readmePath.toAbsolutePath());
            readmeArea.setCaretPosition(0);
            showReadmeWarning(version, "Файл readme не найден по пути:\n" + readmePath.toAbsolutePath());
            return;
        }
        try {
            String text = Files.readString(readmePath, StandardCharsets.UTF_8);
            readmeArea.setText(text);
        } catch (MalformedInputException malformedInputException) {
            try {
                String text = Files.readString(readmePath, Charset.defaultCharset());
                readmeArea.setText(text);
            } catch (IOException ioException) {
                readmeArea.setText("Не удалось загрузить readme: " + ioException.getMessage());
                readmeArea.setCaretPosition(0);
                showReadmeError(version, "Не удалось загрузить readme: " + ioException.getMessage());
                return;
            }
        } catch (IOException e) {
            readmeArea.setText("Не удалось загрузить readme: " + e.getMessage());
            readmeArea.setCaretPosition(0);
            showReadmeError(version, "Не удалось загрузить readme: " + e.getMessage());
            return;
        }
        readmeArea.setCaretPosition(0);
    }

    private JTextArea findReadmeArea(SoftwareVersion version) {
        if (legacyViews.containsKey(version)) {
            LegacyView view = legacyViews.get(version);
            return view != null ? view.readmeArea : null;
        }
        if (version == SoftwareVersion.V2_01 && v201View != null) {
            return v201View.readmeArea;
        }
        if (version == SoftwareVersion.V2_11 && v211View != null) {
            return v211View.readmeArea;
        }
        return null;
    }

    private void showReadmeWarning(SoftwareVersion version, String message) {
        if (readmeWarningShown.add(version)) {
            JOptionPane.showMessageDialog(this, message, "Readme недоступно", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void showReadmeError(SoftwareVersion version, String message) {
        if (readmeWarningShown.add(version)) {
            JOptionPane.showMessageDialog(this, message, "Ошибка чтения readme", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path executePostGenerationActions(SoftwareVersion version) {
        Path root = Paths.get("").toAbsolutePath().normalize();
        String dateSuffix = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String baseName = "FastMarking_" + dateSuffix;
        Path targetDir = resolveUniqueDirectory(root, baseName);
        try {
            Files.createDirectories(targetDir);
        } catch (AccessDeniedException e) {
            showFileErrorDialog("Нет доступа к созданию папки:\n" + targetDir.toAbsolutePath());
            return null;
        } catch (IOException e) {
            showFileErrorDialog("Не удалось создать папку:\n" + targetDir.toAbsolutePath() + "\n" + e.getMessage());
            return null;
        }

        Path sourceExe = Paths.get("versions", version.getFolderName(), "FastMarking.exe");
        if (!Files.exists(sourceExe)) {
            showFileErrorDialog("Файл FastMarking.exe не найден:\n" + sourceExe.toAbsolutePath());
            return null;
        }

        long requiredSpace;
        try {
            requiredSpace = Files.size(sourceExe);
        } catch (IOException e) {
            showFileErrorDialog("Не удалось получить информацию о FastMarking.exe:\n" + sourceExe.toAbsolutePath() + "\n" + e.getMessage());
            return null;
        }

        try {
            FileStore store = Files.getFileStore(targetDir);
            if (store.getUsableSpace() < requiredSpace) {
                showFileErrorDialog("Недостаточно места по пути:\n" + targetDir.toAbsolutePath());
                return null;
            }
        } catch (IOException ignored) {
            // Если не удалось определить доступное место, продолжим попытку копирования.
        }

        Path destination = targetDir.resolve("FastMarking.exe");
        try {
            Files.copy(sourceExe, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (AccessDeniedException e) {
            Path location = destination.getParent() != null ? destination.getParent() : destination;
            showFileErrorDialog("Нет доступа для записи файла в:\n" + location.toAbsolutePath());
            return null;
        } catch (FileSystemException e) {
            String reason = e.getReason();
            Path location = destination.getParent() != null ? destination.getParent() : destination;
            if (reason != null && reason.toLowerCase().contains("space")) {
                showFileErrorDialog("Недостаточно места по пути:\n" + location.toAbsolutePath());
            } else {
                String detail = e.getMessage() != null ? e.getMessage() : "Ошибка файловой системы";
                showFileErrorDialog("Ошибка файловой операции:\n" + destination.toAbsolutePath() + "\n" + detail);
            }
            return null;
        } catch (IOException e) {
            Path location = destination.getParent() != null ? destination.getParent() : destination;
            showFileErrorDialog("Не удалось скопировать FastMarking.exe в:\n" + location.toAbsolutePath() + "\n" + e.getMessage());
            return null;
        }

        return targetDir;
    }

    private Path resolveUniqueDirectory(Path root, String baseName) {
        Path candidate = root.resolve(baseName);
        int counter = 2;
        while (Files.exists(candidate)) {
            candidate = root.resolve(baseName + "_(" + counter + ")");
            counter++;
        }
        return candidate;
    }

    private void showPostGenerationSuccess(SoftwareVersion version, Path targetDir, String additionalInfo) {
        StringBuilder message = new StringBuilder();
        if (additionalInfo != null && !additionalInfo.isBlank()) {
            message.append(additionalInfo).append("\n\n");
        }
        message.append("Версия ").append(version.getFolderName()).append(" обработана.")
                .append("\n")
                .append("Создана папка: ").append(targetDir.toAbsolutePath())
                .append("\n")
                .append("FastMarking.exe скопирован.");
        JOptionPane.showMessageDialog(this, message.toString(), "Успех", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFileErrorDialog(String message) {
        JOptionPane.showMessageDialog(this,
                message + "\nРекомендуется повторить операцию или проверить путь.",
                "Ошибка файловой операции",
                JOptionPane.ERROR_MESSAGE);
    }

    private GridBagConstraints createDefaultConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        return gbc;
    }

    private JFormattedTextField createMaskedDateField() {
        try {
            MaskFormatter dateMask = new MaskFormatter("####-##-##");
            dateMask.setPlaceholderCharacter('_');
            JFormattedTextField field = new JFormattedTextField(dateMask);
            field.setColumns(10);
            return field;
        } catch (ParseException e) {
            throw new IllegalStateException("Ошибка создания маски даты", e);
        }
    }

    private JTextField createReadOnlyField() {
        JTextField textField = new JTextField(20);
        textField.setEditable(false);
        return textField;
    }

    private JTextArea createReadOnlyArea() {
        JTextArea area = new JTextArea(5, 30);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private void attachCopyToClipboard(JTextArea area) {
        area.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!area.getText().isEmpty()) {
                    Toolkit.getDefaultToolkit()
                            .getSystemClipboard()
                            .setContents(new StringSelection(area.getText()), null);
                    JOptionPane.showMessageDialog(
                            Hello.this,
                            "Содержимое скопировано!",
                            "Копирование",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
    }

    private Map<FeatureFlag, JCheckBox> createFeatureCheckboxes() {
        Map<FeatureFlag, JCheckBox> map = new LinkedHashMap<>();
        for (FeatureFlag flag : FeatureFlag.values()) {
            JCheckBox checkBox = new JCheckBox(flag.getDisplayName());
            map.put(flag, checkBox);
        }
        return map;
    }

    private void toggleSelectAll(Map<FeatureFlag, JCheckBox> checkBoxes) {
        boolean allSelected = checkBoxes.values().stream().allMatch(AbstractButton::isSelected);
        checkBoxes.values().forEach(box -> box.setSelected(!allSelected));
    }

    private List<FeatureFlag> collectSelectedFeatures(Map<FeatureFlag, JCheckBox> checkBoxes) {
        List<FeatureFlag> features = new ArrayList<>();
        checkBoxes.forEach((flag, box) -> {
            if (box.isSelected()) {
                features.add(flag);
            }
        });
        return features;
    }

    private LocalDate extractDate(JFormattedTextField field) {
        String raw = field.getText();
        String sanitized = raw.replace("_", "").replace(" ", "");
        if (sanitized.length() != 10) {
            throw new DateTimeParseException("Неверный формат даты", raw, 0);
        }
        return LocalDate.parse(field.getText(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private void showInvalidDateDialog() {
        JOptionPane.showMessageDialog(this,
                "Неверный формат даты! Используйте ГГГГ-ММ-ДД",
                "Ошибка",
                JOptionPane.ERROR_MESSAGE);
    }

    private static class LegacyView {
        JPanel panel;
        JFormattedTextField expirationField;
        JTextField licenseNumberField;
        JTextArea licenseArea;
        JButton generateButton;
        JTextArea readmeArea;
    }

    private static class FeatureVersionView {
        JPanel panel;
        JFormattedTextField expirationField;
        JTextField licenseNumberField;
        JTextArea licenseArea;
        JTextArea readmeArea;
        JTextField companyField;
        JButton generateButton;
        JButton selectAllButton;
        Map<FeatureFlag, JCheckBox> checkBoxes;
    }

    private static class ModernVersionView {
        JPanel panel;
        JFormattedTextField expirationField;
        JTextField licenseNumberField;
        JTextArea readmeArea;
        JTextField companyField;
        JButton selectAllButton;
        Map<FeatureFlag, JCheckBox> checkBoxes;
        JComboBox<DriveItem> driveComboBox;
        JButton writeButton;
    }

    private static class UsbDeviceInfo {
        final String serial;
        final long capacity;
        final long sectors;
        final Boolean removable;

        UsbDeviceInfo(String serial, long capacity, long sectors, Boolean removable) {
            this.serial = serial;
            this.capacity = capacity;
            this.sectors = sectors;
            this.removable = removable;
        }
    }

    private static class CommandResult {
        final int exitCode;
        final List<String> lines;

        CommandResult(int exitCode, List<String> lines) {
            this.exitCode = exitCode;
            this.lines = List.copyOf(lines);
        }
    }

    private static class DeviceInfoException extends Exception {
        DeviceInfoException(String message) {
            super(message);
        }

        DeviceInfoException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class DriveItem {
        final File root;
        final String displayName;

        DriveItem(File root, String displayName) {
            this.root = root;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
