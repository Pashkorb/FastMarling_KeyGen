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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Hello extends JFrame {
    private static final SoftwareVersion[] VERSION_ORDER = {
            SoftwareVersion.V1_01,
            SoftwareVersion.V1_11,
            SoftwareVersion.V2_01,
            SoftwareVersion.V2_11
    };
    private static final String LICENSE_FILE_NAME = "FastMarking.lic";

    private final EnumMap<SoftwareVersion, LegacyView> legacyViews = new EnumMap<>(SoftwareVersion.class);
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

        view.readmeArea = createReadmeArea();
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
        view.licenseArea.setRows(8);
        attachCopyToClipboard(view.licenseArea);
        gbc.gridy = 5;
        gbc.weightx = 1;
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

        view.readmeArea = createReadmeArea();
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
        panel.add(new JLabel("Company:"), gbc);

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
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(featurePanel, gbc);

        view.selectAllButton = new JButton("Выбрать все");
        view.selectAllButton.addActionListener(e -> toggleSelectAll(view.checkBoxes));
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(view.selectAllButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Лицензионный ключ:"), gbc);

        view.licenseArea = createReadOnlyArea();
        view.licenseArea.setRows(8);
        attachCopyToClipboard(view.licenseArea);
        gbc.gridy = 9;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(view.licenseArea), gbc);

        view.generateButton = new JButton("Сгенерировать");
        view.generateButton.addActionListener(e -> generateLicenseForFeatureVersion(version));
        gbc.gridy = 10;
        gbc.weighty = 0;
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

        view.readmeArea = createReadmeArea();
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
        panel.add(new JLabel("Company:"), gbc);

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
        gbc.gridy = 6;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(featurePanel, gbc);

        view.selectAllButton = new JButton("Выбрать все");
        view.selectAllButton.addActionListener(e -> toggleSelectAll(view.checkBoxes));
        gbc.gridy = 7;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(view.selectAllButton, gbc);

        gbc.gridy = 8;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Флеш-накопитель:"), gbc);

        view.driveComboBox = new JComboBox<>();
        view.driveComboBox.setPrototypeDisplayValue(new DriveItem(null, "Съемный диск (X:)"));
        gbc.gridy = 9;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(view.driveComboBox, gbc);

        view.writeButton = new JButton("Записать лицензию");
        view.writeButton.addActionListener((ActionEvent e) -> generateAndWriteLicense(version));
        view.writeButton.setEnabled(false);
        gbc.gridy = 10;
        gbc.fill = GridBagConstraints.HORIZONTAL;
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
            String licenseKey = LicenseManager.generateLicenseKey(new LicenseRequest(expirationDate, version, List.of(), null));
            view.licenseNumberField.setText(Objects.requireNonNullElse(LicenseManager.getLicenseCode(licenseKey), ""));
            view.licenseArea.setText(licenseKey);
            view.licenseArea.setCaretPosition(0);
            performPostGenerationActions(version, null);
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
            String licenseKey = LicenseManager.generateLicenseKey(new LicenseRequest(expirationDate, version, features, company));
            v201View.licenseNumberField.setText(Objects.requireNonNullElse(LicenseManager.getLicenseCode(licenseKey), ""));
            v201View.licenseArea.setText(licenseKey);
            v201View.licenseArea.setCaretPosition(0);
            performPostGenerationActions(version, null);
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
            String licenseKey = LicenseManager.generateLicenseKey(new LicenseRequest(expirationDate, version, features, company));
            v211View.licenseNumberField.setText(Objects.requireNonNullElse(LicenseManager.getLicenseCode(licenseKey), ""));

            DriveItem selectedItem = (DriveItem) v211View.driveComboBox.getSelectedItem();
            if (selectedItem == null || selectedItem.root == null) {
                JOptionPane.showMessageDialog(this, "Выберите флешку для записи.", "Нет носителя", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Path targetPath = writeLicenseToDrive(selectedItem.root, licenseKey);
            performPostGenerationActions(version, targetPath);
            updateDriveList();
        } catch (DateTimeParseException ex) {
            showInvalidDateDialog();
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
        Files.writeString(target, licenseKey, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
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
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
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
        JTextArea area = switch (version) {
            case V1_01, V1_11 -> {
                LegacyView legacyView = legacyViews.get(version);
                yield legacyView != null ? legacyView.readmeArea : null;
            }
            case V2_01 -> v201View != null ? v201View.readmeArea : null;
            case V2_11 -> v211View != null ? v211View.readmeArea : null;
        };

        if (area == null) {
            return;
        }

        Path readmePath = Paths.get("versions", version.getFolderName(), "readme.txt");
        try {
            String text = Files.readString(readmePath, StandardCharsets.UTF_8);
            area.setText(text);
        } catch (MalformedInputException malformedInputException) {
            try {
                String text = Files.readString(readmePath, Charset.defaultCharset());
                area.setText(text);
            } catch (IOException ioException) {
                area.setText(buildReadmeWarning(readmePath, ioException.getMessage()));
            }
            area.setCaretPosition(0);
            return;
        } catch (IOException e) {
            area.setText(buildReadmeWarning(readmePath, e.getMessage()));
            area.setCaretPosition(0);
            return;
        }
        area.setCaretPosition(0);
    }

    private String buildReadmeWarning(Path readmePath, String details) {
        StringBuilder builder = new StringBuilder("Readme не доступно (")
                .append(readmePath.toAbsolutePath())
                .append(")");
        if (details != null && !details.isBlank()) {
            builder.append("\nПричина: ").append(details);
        }
        return builder.toString();
    }

    private void performPostGenerationActions(SoftwareVersion version, Path licenseOutput) {
        Path exePath = Paths.get("versions", version.getFolderName(), "FastMarking.exe");
        if (!Files.exists(exePath) || !Files.isRegularFile(exePath)) {
            JOptionPane.showMessageDialog(this,
                    "Не найден FastMarking.exe по пути: " + exePath.toAbsolutePath(),
                    "Файл не найден",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Files.isReadable(exePath)) {
            JOptionPane.showMessageDialog(this,
                    "Нет доступа для чтения файла: " + exePath.toAbsolutePath(),
                    "Нет доступа",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Path targetDir = createUniqueTargetDirectory();
            long exeSize = Files.size(exePath);
            FileStore store = Files.getFileStore(targetDir);
            if (store.getUsableSpace() < exeSize) {
                throw new IOException("Недостаточно свободного места для копирования в: " + targetDir.toAbsolutePath());
            }
            Path copiedFile = Files.copy(exePath, targetDir.resolve(exePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            StringBuilder message = new StringBuilder()
                    .append("Версия ")
                    .append(version.getDisplayName())
                    .append(" обработана.")
                    .append("\nПапка: ")
                    .append(targetDir.toAbsolutePath())
                    .append("\nFastMarking.exe скопирован: ")
                    .append(copiedFile.toAbsolutePath());
            if (licenseOutput != null) {
                message.append("\nЛицензия записана: ")
                        .append(licenseOutput.toAbsolutePath());
            }
            JOptionPane.showMessageDialog(this, message.toString(), "Операция выполнена", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SecurityException e) {
            StringBuilder error = new StringBuilder("Не удалось выполнить пост-действия: ")
                    .append(e.getMessage());
            if (licenseOutput != null) {
                error.append("\nЛицензия записана: ")
                        .append(licenseOutput.toAbsolutePath());
            }
            JOptionPane.showMessageDialog(this, error.toString(), "Ошибка файловой операции", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path createUniqueTargetDirectory() throws IOException {
        Path root = Paths.get("").toAbsolutePath();
        String datePart = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String baseName = "FastMarking_" + datePart;
        Path candidate = root.resolve(baseName);
        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = root.resolve(baseName + "_(" + suffix + ")");
            suffix++;
        }
        return Files.createDirectory(candidate);
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

    private JTextArea createReadmeArea() {
        JTextArea area = createReadOnlyArea();
        area.setRows(10);
        area.setText("Загрузка...");
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
        JTextField companyField;
        JTextArea licenseArea;
        JButton generateButton;
        JButton selectAllButton;
        Map<FeatureFlag, JCheckBox> checkBoxes;
        JTextArea readmeArea;
    }

    private static class ModernVersionView {
        JPanel panel;
        JFormattedTextField expirationField;
        JTextField licenseNumberField;
        JTextField companyField;
        JButton selectAllButton;
        Map<FeatureFlag, JCheckBox> checkBoxes;
        JComboBox<DriveItem> driveComboBox;
        JButton writeButton;
        JTextArea readmeArea;
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
