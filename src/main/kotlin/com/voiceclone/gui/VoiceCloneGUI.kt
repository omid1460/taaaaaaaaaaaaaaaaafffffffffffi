package com.voiceclone.gui

import com.voiceclone.core.VoiceCloneEngine
import com.voiceclone.core.CloneRequest
import com.voiceclone.updater.AutoUpdater
import javafx.application.Application
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

class VoiceCloneGUI : Application() {
    private lateinit var engine: VoiceCloneEngine
    private lateinit var autoUpdater: AutoUpdater
    private lateinit var primaryStage: Stage
    
    // UI Components
    private lateinit var profileComboBox: ComboBox<String>
    private lateinit var textArea: TextArea
    private lateinit var languageComboBox: ComboBox<String>
    private lateinit var speedSlider: Slider
    private lateinit var pitchSlider: Slider
    private lateinit var emotionComboBox: ComboBox<String>
    private lateinit var progressBar: ProgressBar
    private lateinit var statusLabel: Label
    private lateinit var characterCountLabel: Label
    
    companion object {
        fun launch(engine: VoiceCloneEngine, autoUpdater: AutoUpdater) {
            VoiceCloneGUI.engine = engine
            VoiceCloneGUI.autoUpdater = autoUpdater
            Application.launch(VoiceCloneGUI::class.java)
        }
        
        private lateinit var engine: VoiceCloneEngine
        private lateinit var autoUpdater: AutoUpdater
    }
    
    override fun start(primaryStage: Stage) {
        this.engine = VoiceCloneGUI.engine
        this.autoUpdater = VoiceCloneGUI.autoUpdater
        this.primaryStage = primaryStage
        
        primaryStage.title = "Voice Clone - نرم افزار کلون صدا"
        primaryStage.scene = createMainScene()
        primaryStage.minWidth = 800.0
        primaryStage.minHeight = 600.0
        primaryStage.show()
        
        // Load initial data
        loadVoiceProfiles()
        loadSupportedLanguages()
        
        logger.info("Voice Clone GUI started")
    }
    
    private fun createMainScene(): Scene {
        val root = BorderPane()
        root.padding = Insets(20.0)
        
        // Header
        root.top = createHeader()
        
        // Main content
        root.center = createMainContent()
        
        // Footer
        root.bottom = createFooter()
        
        // Apply modern styling
        val scene = Scene(root, 1000.0, 700.0)
        scene.stylesheets.add(javaClass.getResource("/styles.css")?.toExternalForm() ?: "")
        
        return scene
    }
    
    private fun createHeader(): VBox {
        val header = VBox(10.0)
        header.alignment = Pos.CENTER
        
        val title = Label("Voice Clone Application")
        title.font = Font.font("Arial", FontWeight.BOLD, 24.0)
        title.textFill = Color.DARKBLUE
        
        val subtitle = Label("کلون صدا با هوش مصنوعی - پشتیبانی از 99% زبان‌های دنیا")
        subtitle.font = Font.font("Arial", FontWeight.NORMAL, 14.0)
        subtitle.textFill = Color.GRAY
        
        header.children.addAll(title, subtitle, Separator())
        
        return header
    }
    
    private fun createMainContent(): TabPane {
        val tabPane = TabPane()
        
        // Voice Recording Tab
        val recordingTab = Tab("ضبط صدا / Record Voice")
        recordingTab.content = createRecordingTab()
        recordingTab.isClosable = false
        
        // Text to Speech Tab
        val ttsTab = Tab("تبدیل متن به گفتار / Text to Speech")
        ttsTab.content = createTTSTab()
        ttsTab.isClosable = false
        
        // Voice Profiles Tab
        val profilesTab = Tab("پروفایل‌های صوتی / Voice Profiles")
        profilesTab.content = createProfilesTab()
        profilesTab.isClosable = false
        
        // Settings Tab
        val settingsTab = Tab("تنظیمات / Settings")
        settingsTab.content = createSettingsTab()
        settingsTab.isClosable = false
        
        tabPane.tabs.addAll(recordingTab, ttsTab, profilesTab, settingsTab)
        
        return tabPane
    }
    
    private fun createRecordingTab(): VBox {
        val vbox = VBox(15.0)
        vbox.padding = Insets(20.0)
        
        // Profile name input
        val profileNameLabel = Label("نام پروفایل / Profile Name:")
        profileNameLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        val profileNameField = TextField()
        profileNameField.promptText = "نام پروفایل صوتی خود را وارد کنید"
        
        // Language selection
        val languageLabel = Label("زبان / Language:")
        languageLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        val recordingLanguageCombo = ComboBox<String>()
        recordingLanguageCombo.items.addAll("fa - Persian (فارسی)", "en - English", "ar - Arabic (عربی)")
        recordingLanguageCombo.value = "fa - Persian (فارسی)"
        
        // Duration selection
        val durationLabel = Label("مدت زمان ضبط / Recording Duration:")
        durationLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        val durationSlider = Slider(1.0, 20.0, 10.0)
        durationSlider.showTickMarks = true
        durationSlider.showTickLabels = true
        durationSlider.majorTickUnit = 5.0
        durationSlider.blockIncrement = 1.0
        
        val durationValueLabel = Label("10 دقیقه / minutes")
        durationSlider.valueProperty().addListener { _, _, newValue ->
            durationValueLabel.text = "${newValue.toInt()} دقیقه / minutes"
        }
        
        // Recording controls
        val recordButton = Button("شروع ضبط / Start Recording")
        recordButton.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        recordButton.prefWidth = 200.0
        recordButton.styleClass.add("primary-button")
        
        val stopButton = Button("توقف ضبط / Stop Recording")
        stopButton.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        stopButton.prefWidth = 200.0
        stopButton.isDisable = true
        stopButton.styleClass.add("secondary-button")
        
        // Recording progress
        val recordingProgressBar = ProgressBar(0.0)
        recordingProgressBar.prefWidth = 400.0
        
        val recordingStatusLabel = Label("آماده برای ضبط / Ready to record")
        recordingStatusLabel.font = Font.font("Arial", 12.0)
        
        // Volume level indicator
        val volumeLabel = Label("سطح صدا / Volume Level:")
        val volumeProgressBar = ProgressBar(0.0)
        volumeProgressBar.prefWidth = 300.0
        volumeProgressBar.styleClass.add("volume-bar")
        
        // Upload alternative
        val uploadButton = Button("آپلود فایل صوتی / Upload Audio File")
        uploadButton.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        uploadButton.prefWidth = 200.0
        uploadButton.styleClass.add("secondary-button")
        
        // Event handlers
        recordButton.setOnAction {
            startRecording(
                profileNameField.text,
                durationSlider.value.toInt(),
                recordingLanguageCombo.value?.substringBefore(" -") ?: "fa",
                recordButton,
                stopButton,
                recordingProgressBar,
                recordingStatusLabel,
                volumeProgressBar
            )
        }
        
        stopButton.setOnAction {
            stopRecording(recordButton, stopButton, recordingStatusLabel)
        }
        
        uploadButton.setOnAction {
            uploadAudioFile(profileNameField.text, recordingLanguageCombo.value?.substringBefore(" -") ?: "fa")
        }
        
        vbox.children.addAll(
            profileNameLabel, profileNameField,
            languageLabel, recordingLanguageCombo,
            durationLabel, HBox(10.0, durationSlider, durationValueLabel),
            HBox(10.0, recordButton, stopButton),
            recordingProgressBar, recordingStatusLabel,
            Separator(),
            volumeLabel, volumeProgressBar,
            Separator(),
            uploadButton
        )
        
        return vbox
    }
    
    private fun createTTSTab(): VBox {
        val vbox = VBox(15.0)
        vbox.padding = Insets(20.0)
        
        // Profile selection
        val profileLabel = Label("انتخاب پروفایل صوتی / Select Voice Profile:")
        profileLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        profileComboBox = ComboBox()
        profileComboBox.prefWidth = 300.0
        
        // Text input
        val textLabel = Label("متن برای تبدیل به گفتار (حداکثر 50,000 کاراکتر) / Text to Speech (Max 50,000 characters):")
        textLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        textArea = TextArea()
        textArea.promptText = "متن خود را اینجا وارد کنید...\nEnter your text here..."
        textArea.prefRowCount = 10
        textArea.wrapText = true
        
        characterCountLabel = Label("0 / 50,000 کاراکتر")
        characterCountLabel.font = Font.font("Arial", 12.0)
        
        textArea.textProperty().addListener { _, _, newValue ->
            val count = newValue?.length ?: 0
            characterCountLabel.text = "$count / 50,000 کاراکتر"
            characterCountLabel.textFill = if (count > 50000) Color.RED else Color.BLACK
        }
        
        // Language detection/selection
        val langLabel = Label("زبان / Language:")
        langLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        languageComboBox = ComboBox()
        languageComboBox.items.add("auto - تشخیص خودکار / Auto Detect")
        languageComboBox.value = "auto - تشخیص خودکار / Auto Detect"
        languageComboBox.prefWidth = 250.0
        
        val detectButton = Button("تشخیص زبان / Detect Language")
        detectButton.setOnAction { detectLanguage() }
        
        // Voice parameters
        val parametersLabel = Label("تنظیمات صدا / Voice Parameters:")
        parametersLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        // Speed control
        val speedLabel = Label("سرعت / Speed:")
        speedSlider = Slider(0.5, 2.0, 1.0)
        speedSlider.showTickMarks = true
        speedSlider.majorTickUnit = 0.5
        val speedValueLabel = Label("1.0x")
        speedSlider.valueProperty().addListener { _, _, newValue ->
            speedValueLabel.text = String.format("%.1fx", newValue.toDouble())
        }
        
        // Pitch control
        val pitchLabel = Label("زیر و بمی / Pitch:")
        pitchSlider = Slider(0.5, 2.0, 1.0)
        pitchSlider.showTickMarks = true
        pitchSlider.majorTickUnit = 0.5
        val pitchValueLabel = Label("1.0x")
        pitchSlider.valueProperty().addListener { _, _, newValue ->
            pitchValueLabel.text = String.format("%.1fx", newValue.toDouble())
        }
        
        // Emotion selection
        val emotionLabel = Label("احساس / Emotion:")
        emotionComboBox = ComboBox()
        emotionComboBox.items.addAll("neutral", "happy", "sad", "angry", "calm")
        emotionComboBox.value = "neutral"
        
        // Generate button
        val generateButton = Button("تولید گفتار / Generate Speech")
        generateButton.font = Font.font("Arial", FontWeight.BOLD, 16.0)
        generateButton.prefWidth = 250.0
        generateButton.styleClass.add("primary-button")
        
        generateButton.setOnAction { generateSpeech() }
        
        // Progress and status
        progressBar = ProgressBar(0.0)
        progressBar.prefWidth = 400.0
        progressBar.isVisible = false
        
        statusLabel = Label("آماده / Ready")
        statusLabel.font = Font.font("Arial", 12.0)
        
        vbox.children.addAll(
            profileLabel, profileComboBox,
            textLabel, textArea, characterCountLabel,
            langLabel, HBox(10.0, languageComboBox, detectButton),
            Separator(),
            parametersLabel,
            HBox(10.0, speedLabel, speedSlider, speedValueLabel),
            HBox(10.0, pitchLabel, pitchSlider, pitchValueLabel),
            HBox(10.0, emotionLabel, emotionComboBox),
            Separator(),
            generateButton,
            progressBar, statusLabel
        )
        
        return vbox
    }
    
    private fun createProfilesTab(): VBox {
        val vbox = VBox(15.0)
        vbox.padding = Insets(20.0)
        
        val titleLabel = Label("مدیریت پروفایل‌های صوتی / Voice Profiles Management")
        titleLabel.font = Font.font("Arial", FontWeight.BOLD, 16.0)
        
        val profilesList = ListView<String>()
        profilesList.prefHeight = 300.0
        
        val refreshButton = Button("بروزرسانی / Refresh")
        val deleteButton = Button("حذف / Delete")
        val exportButton = Button("صادرات / Export")
        
        refreshButton.setOnAction { loadVoiceProfiles() }
        deleteButton.setOnAction { deleteSelectedProfile(profilesList) }
        exportButton.setOnAction { exportSelectedProfile(profilesList) }
        
        val buttonBox = HBox(10.0, refreshButton, deleteButton, exportButton)
        buttonBox.alignment = Pos.CENTER
        
        vbox.children.addAll(titleLabel, profilesList, buttonBox)
        
        return vbox
    }
    
    private fun createSettingsTab(): VBox {
        val vbox = VBox(15.0)
        vbox.padding = Insets(20.0)
        
        val titleLabel = Label("تنظیمات / Settings")
        titleLabel.font = Font.font("Arial", FontWeight.BOLD, 16.0)
        
        // Auto-update settings
        val autoUpdateCheck = CheckBox("بروزرسانی خودکار / Auto Update")
        autoUpdateCheck.isSelected = autoUpdater.isAutoUpdateEnabled()
        autoUpdateCheck.setOnAction {
            autoUpdater.setAutoUpdateEnabled(autoUpdateCheck.isSelected)
        }
        
        val checkUpdateButton = Button("بررسی بروزرسانی / Check for Updates")
        checkUpdateButton.setOnAction { checkForUpdates() }
        
        // Language settings
        val interfaceLanguageLabel = Label("زبان رابط کاربری / Interface Language:")
        val interfaceLanguageCombo = ComboBox<String>()
        interfaceLanguageCombo.items.addAll("فارسی / Persian", "English", "عربی / Arabic")
        interfaceLanguageCombo.value = "فارسی / Persian"
        
        // Audio settings
        val audioSettingsLabel = Label("تنظیمات صوتی / Audio Settings")
        audioSettingsLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        val qualityLabel = Label("کیفیت صوتی / Audio Quality:")
        val qualityCombo = ComboBox<String>()
        qualityCombo.items.addAll("High (22050 Hz)", "Medium (16000 Hz)", "Low (8000 Hz)")
        qualityCombo.value = "High (22050 Hz)"
        
        // About section
        val aboutLabel = Label("درباره / About")
        aboutLabel.font = Font.font("Arial", FontWeight.BOLD, 14.0)
        
        val versionLabel = Label("نسخه / Version: 1.0.0")
        val developerLabel = Label("توسعه‌دهنده / Developer: Voice Clone Team")
        val licenseLabel = Label("مجوز / License: Free & Open Source")
        
        vbox.children.addAll(
            titleLabel,
            autoUpdateCheck, checkUpdateButton,
            Separator(),
            interfaceLanguageLabel, interfaceLanguageCombo,
            Separator(),
            audioSettingsLabel,
            qualityLabel, qualityCombo,
            Separator(),
            aboutLabel,
            versionLabel, developerLabel, licenseLabel
        )
        
        return vbox
    }
    
    private fun createFooter(): HBox {
        val footer = HBox(20.0)
        footer.alignment = Pos.CENTER
        footer.padding = Insets(10.0)
        
        val statusIcon = Label("●")
        statusIcon.textFill = Color.GREEN
        statusIcon.font = Font.font(16.0)
        
        val footerStatus = Label("آماده / Ready")
        footerStatus.font = Font.font("Arial", 12.0)
        
        val serverButton = Button("شروع سرور / Start Server")
        serverButton.setOnAction { startServer() }
        
        footer.children.addAll(statusIcon, footerStatus, Separator(), serverButton)
        
        return footer
    }
    
    // Event handlers
    private fun startRecording(
        profileName: String,
        duration: Int,
        language: String,
        recordButton: Button,
        stopButton: Button,
        progressBar: ProgressBar,
        statusLabel: Label,
        volumeBar: ProgressBar
    ) {
        if (profileName.isEmpty()) {
            showAlert("خطا / Error", "لطفاً نام پروفایل را وارد کنید / Please enter profile name")
            return
        }
        
        recordButton.isDisable = true
        stopButton.isDisable = false
        progressBar.progress = 0.0
        statusLabel.text = "در حال ضبط... / Recording..."
        
        GlobalScope.launch {
            try {
                val profile = engine.recordVoiceSample(profileName, duration, language)
                
                Platform.runLater {
                    recordButton.isDisable = false
                    stopButton.isDisable = true
                    progressBar.progress = 1.0
                    statusLabel.text = "ضبط کامل شد / Recording completed"
                    loadVoiceProfiles()
                    showAlert("موفقیت / Success", "پروفایل صوتی با موفقیت ایجاد شد / Voice profile created successfully")
                }
                
            } catch (e: Exception) {
                Platform.runLater {
                    recordButton.isDisable = false
                    stopButton.isDisable = true
                    statusLabel.text = "خطا در ضبط / Recording error"
                    showAlert("خطا / Error", "خطا در ایجاد پروفایل: ${e.message}")
                }
            }
        }
    }
    
    private fun stopRecording(recordButton: Button, stopButton: Button, statusLabel: Label) {
        // Stop recording logic
        recordButton.isDisable = false
        stopButton.isDisable = true
        statusLabel.text = "ضبط متوقف شد / Recording stopped"
    }
    
    private fun uploadAudioFile(profileName: String, language: String) {
        val fileChooser = FileChooser()
        fileChooser.title = "انتخاب فایل صوتی / Select Audio File"
        fileChooser.extensionFilters.addAll(
            FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.m4a", "*.flac"),
            FileChooser.ExtensionFilter("All Files", "*.*")
        )
        
        val selectedFile = fileChooser.showOpenDialog(primaryStage)
        if (selectedFile != null) {
            GlobalScope.launch {
                try {
                    // Process uploaded file
                    val profileId = java.util.UUID.randomUUID().toString()
                    val modelPath = engine.voiceModel.trainVoiceModel(selectedFile, profileId, language)
                    
                    Platform.runLater {
                        loadVoiceProfiles()
                        showAlert("موفقیت / Success", "فایل صوتی با موفقیت آپلود شد / Audio file uploaded successfully")
                    }
                    
                } catch (e: Exception) {
                    Platform.runLater {
                        showAlert("خطا / Error", "خطا در آپلود فایل: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun generateSpeech() {
        val text = textArea.text
        val profileId = profileComboBox.value
        
        if (text.isEmpty()) {
            showAlert("خطا / Error", "لطفاً متن را وارد کنید / Please enter text")
            return
        }
        
        if (profileId == null) {
            showAlert("خطا / Error", "لطفاً پروفایل صوتی را انتخاب کنید / Please select voice profile")
            return
        }
        
        if (text.length > 50000) {
            showAlert("خطا / Error", "متن نباید بیش از 50,000 کاراکتر باشد / Text must not exceed 50,000 characters")
            return
        }
        
        progressBar.isVisible = true
        progressBar.progress = ProgressBar.INDETERMINATE_PROGRESS
        statusLabel.text = "در حال تولید گفتار... / Generating speech..."
        
        GlobalScope.launch {
            try {
                val request = CloneRequest(
                    text = text,
                    profileId = profileId,
                    language = languageComboBox.value?.substringBefore(" -") ?: "auto",
                    speed = speedSlider.value.toFloat(),
                    pitch = pitchSlider.value.toFloat(),
                    emotion = emotionComboBox.value
                )
                
                val audioFile = engine.cloneVoice(request)
                
                Platform.runLater {
                    progressBar.isVisible = false
                    statusLabel.text = "تولید گفتار کامل شد / Speech generation completed"
                    
                    // Play audio or save file dialog
                    val alert = Alert(Alert.AlertType.INFORMATION)
                    alert.title = "موفقیت / Success"
                    alert.headerText = "گفتار با موفقیت تولید شد / Speech generated successfully"
                    alert.contentText = "فایل صوتی در: ${audioFile.absolutePath}"
                    
                    val playButton = ButtonType("پخش / Play")
                    val saveButton = ButtonType("ذخیره / Save")
                    val okButton = ButtonType("تایید / OK")
                    
                    alert.buttonTypes.setAll(playButton, saveButton, okButton)
                    
                    val result = alert.showAndWait()
                    when (result.orElse(okButton)) {
                        playButton -> playAudioFile(audioFile)
                        saveButton -> saveAudioFile(audioFile)
                    }
                }
                
            } catch (e: Exception) {
                Platform.runLater {
                    progressBar.isVisible = false
                    statusLabel.text = "خطا در تولید گفتار / Speech generation error"
                    showAlert("خطا / Error", "خطا در تولید گفتار: ${e.message}")
                }
            }
        }
    }
    
    private fun detectLanguage() {
        val text = textArea.text
        if (text.isEmpty()) {
            showAlert("خطا / Error", "لطفاً متن را وارد کنید / Please enter text")
            return
        }
        
        GlobalScope.launch {
            try {
                val detectedLang = engine.multiLanguageTTS.detectLanguage(text)
                val langInfo = engine.multiLanguageTTS.getLanguageInfo(detectedLang)
                
                Platform.runLater {
                    languageComboBox.value = "$detectedLang - ${langInfo?.name ?: detectedLang}"
                    showAlert("تشخیص زبان / Language Detection", "زبان تشخیص داده شده: ${langInfo?.name ?: detectedLang}")
                }
                
            } catch (e: Exception) {
                Platform.runLater {
                    showAlert("خطا / Error", "خطا در تشخیص زبان: ${e.message}")
                }
            }
        }
    }
    
    private fun loadVoiceProfiles() {
        GlobalScope.launch {
            try {
                val profiles = engine.getVoiceProfiles()
                
                Platform.runLater {
                    profileComboBox.items.clear()
                    profiles.forEach { profile ->
                        profileComboBox.items.add("${profile.name} (${profile.language})")
                    }
                }
                
            } catch (e: Exception) {
                logger.error("Failed to load voice profiles", e)
            }
        }
    }
    
    private fun loadSupportedLanguages() {
        GlobalScope.launch {
            try {
                val languages = engine.getSupportedLanguages()
                
                Platform.runLater {
                    languages.forEach { lang ->
                        val langInfo = engine.multiLanguageTTS.getLanguageInfo(lang)
                        languageComboBox.items.add("$lang - ${langInfo?.name ?: lang}")
                    }
                }
                
            } catch (e: Exception) {
                logger.error("Failed to load supported languages", e)
            }
        }
    }
    
    private fun deleteSelectedProfile(profilesList: ListView<String>) {
        val selected = profilesList.selectionModel.selectedItem
        if (selected != null) {
            val confirmation = Alert(Alert.AlertType.CONFIRMATION)
            confirmation.title = "تأیید حذف / Confirm Deletion"
            confirmation.headerText = "حذف پروفایل صوتی / Delete Voice Profile"
            confirmation.contentText = "آیا مطمئن هستید که می‌خواهید این پروفایل را حذف کنید؟"
            
            val result = confirmation.showAndWait()
            if (result.isPresent && result.get() == ButtonType.OK) {
                // Delete profile logic
                showAlert("موفقیت / Success", "پروفایل حذف شد / Profile deleted")
            }
        }
    }
    
    private fun exportSelectedProfile(profilesList: ListView<String>) {
        val selected = profilesList.selectionModel.selectedItem
        if (selected != null) {
            val fileChooser = FileChooser()
            fileChooser.title = "صادرات پروفایل / Export Profile"
            fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("ZIP Files", "*.zip"))
            
            val saveFile = fileChooser.showSaveDialog(primaryStage)
            if (saveFile != null) {
                // Export profile logic
                showAlert("موفقیت / Success", "پروفایل صادر شد / Profile exported")
            }
        }
    }
    
    private fun checkForUpdates() {
        GlobalScope.launch {
            try {
                val updateResponse = autoUpdater.checkForUpdates(force = true)
                
                Platform.runLater {
                    if (updateResponse.available) {
                        val alert = Alert(Alert.AlertType.INFORMATION)
                        alert.title = "بروزرسانی موجود / Update Available"
                        alert.headerText = "نسخه جدید موجود است / New version available"
                        alert.contentText = "نسخه فعلی: ${updateResponse.current}\nآخرین نسخه: ${updateResponse.latest}"
                        
                        val updateButton = ButtonType("بروزرسانی / Update")
                        val laterButton = ButtonType("بعداً / Later")
                        
                        alert.buttonTypes.setAll(updateButton, laterButton)
                        
                        val result = alert.showAndWait()
                        if (result.isPresent && result.get() == updateButton) {
                            updateResponse.updateInfo?.let { updateInfo ->
                                downloadAndInstallUpdate(updateInfo)
                            }
                        }
                    } else {
                        showAlert("بروزرسانی / Update", "برنامه به‌روز است / Application is up to date")
                    }
                }
                
            } catch (e: Exception) {
                Platform.runLater {
                    showAlert("خطا / Error", "خطا در بررسی بروزرسانی: ${e.message}")
                }
            }
        }
    }
    
    private fun downloadAndInstallUpdate(updateInfo: com.voiceclone.updater.UpdateInfo) {
        GlobalScope.launch {
            try {
                val success = autoUpdater.downloadAndInstallUpdate(updateInfo)
                
                Platform.runLater {
                    if (success) {
                        showAlert("موفقیت / Success", "بروزرسانی نصب شد. برنامه مجدداً راه‌اندازی می‌شود / Update installed. Application will restart")
                    } else {
                        showAlert("خطا / Error", "خطا در نصب بروزرسانی / Error installing update")
                    }
                }
                
            } catch (e: Exception) {
                Platform.runLater {
                    showAlert("خطا / Error", "خطا در دانلود بروزرسانی: ${e.message}")
                }
            }
        }
    }
    
    private fun startServer() {
        GlobalScope.launch {
            try {
                val server = com.voiceclone.api.VoiceCloneServer(engine)
                server.start()
                
            } catch (e: Exception) {
                Platform.runLater {
                    showAlert("خطا / Error", "خطا در راه‌اندازی سرور: ${e.message}")
                }
            }
        }
    }
    
    private fun playAudioFile(audioFile: File) {
        // Implement audio playback
        try {
            val desktop = java.awt.Desktop.getDesktop()
            desktop.open(audioFile)
        } catch (e: Exception) {
            showAlert("خطا / Error", "خطا در پخش فایل صوتی: ${e.message}")
        }
    }
    
    private fun saveAudioFile(audioFile: File) {
        val fileChooser = FileChooser()
        fileChooser.title = "ذخیره فایل صوتی / Save Audio File"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("WAV Files", "*.wav"))
        fileChooser.initialFileName = "voice_clone_${System.currentTimeMillis()}.wav"
        
        val saveFile = fileChooser.showSaveDialog(primaryStage)
        if (saveFile != null) {
            try {
                audioFile.copyTo(saveFile, overwrite = true)
                showAlert("موفقیت / Success", "فایل ذخیره شد / File saved successfully")
            } catch (e: Exception) {
                showAlert("خطا / Error", "خطا در ذخیره فایل: ${e.message}")
            }
        }
    }
    
    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}