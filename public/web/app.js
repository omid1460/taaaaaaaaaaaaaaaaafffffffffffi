// Voice Clone Web App JavaScript
class VoiceCloneApp {
    constructor() {
        this.apiBase = '/api';
        this.isRecording = false;
        this.mediaRecorder = null;
        this.audioChunks = [];
        this.recordingStartTime = null;
        
        this.init();
    }
    
    async init() {
        await this.loadSupportedLanguages();
        await this.loadProfiles();
        this.setupEventListeners();
        
        console.log('Voice Clone App initialized');
    }
    
    setupEventListeners() {
        // Character count for text area
        const inputText = document.getElementById('inputText');
        if (inputText) {
            inputText.addEventListener('input', this.updateCharacterCount.bind(this));
        }
        
        // Auto-save settings
        const autoUpdate = document.getElementById('autoUpdate');
        if (autoUpdate) {
            autoUpdate.addEventListener('change', this.saveSettings.bind(this));
        }
        
        // Interface language change
        const interfaceLanguage = document.getElementById('interfaceLanguage');
        if (interfaceLanguage) {
            interfaceLanguage.addEventListener('change', this.changeLanguage.bind(this));
        }
    }
    
    // Tab Management
    showTab(tabName) {
        // Hide all tab contents
        const tabContents = document.querySelectorAll('.tab-content');
        tabContents.forEach(content => content.classList.remove('active'));
        
        // Remove active class from all tabs
        const tabs = document.querySelectorAll('.tab');
        tabs.forEach(tab => tab.classList.remove('active'));
        
        // Show selected tab content
        const selectedContent = document.getElementById(tabName);
        if (selectedContent) {
            selectedContent.classList.add('active');
        }
        
        // Add active class to selected tab
        const selectedTab = event.target.closest('.tab');
        if (selectedTab) {
            selectedTab.classList.add('active');
        }
        
        // Load data for specific tabs
        if (tabName === 'profiles') {
            this.loadProfiles();
        }
    }
    
    // Recording Functions
    async startRecording() {
        const profileName = document.getElementById('profileName').value.trim();
        const language = document.getElementById('recordLanguage').value;
        const duration = parseInt(document.getElementById('duration').value);
        
        if (!profileName) {
            this.showStatus('recordStatus', 'لطفاً نام پروفایل را وارد کنید', 'error');
            return;
        }
        
        try {
            // Request microphone access
            const stream = await navigator.mediaDevices.getUserMedia({ 
                audio: {
                    sampleRate: 22050,
                    channelCount: 1,
                    echoCancellation: true,
                    noiseSuppression: true
                }
            });
            
            this.mediaRecorder = new MediaRecorder(stream, {
                mimeType: 'audio/webm;codecs=opus'
            });
            
            this.audioChunks = [];
            this.recordingStartTime = Date.now();
            this.isRecording = true;
            
            // UI updates
            document.getElementById('startRecord').classList.add('hidden');
            document.getElementById('stopRecord').classList.remove('hidden');
            this.showStatus('recordStatus', 'در حال ضبط...', 'info');
            
            // Start recording
            this.mediaRecorder.start(1000); // Collect data every second
            
            // Handle data available
            this.mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    this.audioChunks.push(event.data);
                }
                this.updateRecordingProgress();
            };
            
            // Handle recording stop
            this.mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
                await this.uploadRecording(audioBlob, profileName, language);
                
                // Clean up
                stream.getTracks().forEach(track => track.stop());
                this.isRecording = false;
                
                // UI updates
                document.getElementById('startRecord').classList.remove('hidden');
                document.getElementById('stopRecord').classList.add('hidden');
            };
            
            // Auto-stop after duration
            setTimeout(() => {
                if (this.isRecording) {
                    this.stopRecording();
                }
            }, duration * 60 * 1000);
            
            // Start volume monitoring
            this.startVolumeMonitoring(stream);
            
        } catch (error) {
            console.error('Recording error:', error);
            this.showStatus('recordStatus', 'خطا در دسترسی به میکروفون: ' + error.message, 'error');
        }
    }
    
    stopRecording() {
        if (this.mediaRecorder && this.isRecording) {
            this.mediaRecorder.stop();
            this.showStatus('recordStatus', 'در حال پردازش...', 'info');
        }
    }
    
    startVolumeMonitoring(stream) {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const analyser = audioContext.createAnalyser();
        const microphone = audioContext.createMediaStreamSource(stream);
        const dataArray = new Uint8Array(analyser.frequencyBinCount);
        
        microphone.connect(analyser);
        analyser.fftSize = 256;
        
        const updateVolume = () => {
            if (!this.isRecording) return;
            
            analyser.getByteFrequencyData(dataArray);
            const average = dataArray.reduce((a, b) => a + b) / dataArray.length;
            const volume = (average / 255) * 100;
            
            document.getElementById('volumeLevel').style.width = volume + '%';
            
            requestAnimationFrame(updateVolume);
        };
        
        updateVolume();
    }
    
    updateRecordingProgress() {
        if (!this.recordingStartTime) return;
        
        const elapsed = Date.now() - this.recordingStartTime;
        const duration = parseInt(document.getElementById('duration').value) * 60 * 1000;
        const progress = Math.min((elapsed / duration) * 100, 100);
        
        document.getElementById('recordProgress').style.width = progress + '%';
    }
    
    async uploadRecording(audioBlob, profileName, language) {
        const formData = new FormData();
        formData.append('audio', audioBlob, 'recording.webm');
        formData.append('profileName', profileName);
        formData.append('language', language);
        
        try {
            const response = await fetch(`${this.apiBase}/profiles/upload`, {
                method: 'POST',
                body: formData
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showStatus('recordStatus', 'پروفایل صوتی با موفقیت ایجاد شد!', 'success');
                await this.loadProfiles();
                document.getElementById('recordProgress').style.width = '100%';
            } else {
                this.showStatus('recordStatus', 'خطا در ایجاد پروفایل: ' + result.error, 'error');
            }
        } catch (error) {
            console.error('Upload error:', error);
            this.showStatus('recordStatus', 'خطا در آپلود: ' + error.message, 'error');
        }
    }
    
    async uploadAudioFile() {
        const fileInput = document.getElementById('audioFile');
        const profileName = document.getElementById('profileName').value.trim();
        const language = document.getElementById('recordLanguage').value;
        
        if (!fileInput.files[0]) {
            this.showStatus('recordStatus', 'لطفاً فایل صوتی را انتخاب کنید', 'error');
            return;
        }
        
        if (!profileName) {
            this.showStatus('recordStatus', 'لطفاً نام پروفایل را وارد کنید', 'error');
            return;
        }
        
        const formData = new FormData();
        formData.append('audio', fileInput.files[0]);
        formData.append('profileName', profileName);
        formData.append('language', language);
        
        try {
            this.showStatus('recordStatus', 'در حال آپلود...', 'info');
            
            const response = await fetch(`${this.apiBase}/profiles/upload`, {
                method: 'POST',
                body: formData
            });
            
            const result = await response.json();
            
            if (result.success) {
                this.showStatus('recordStatus', 'فایل با موفقیت آپلود شد!', 'success');
                await this.loadProfiles();
                fileInput.value = '';
            } else {
                this.showStatus('recordStatus', 'خطا در آپلود: ' + result.error, 'error');
            }
        } catch (error) {
            console.error('Upload error:', error);
            this.showStatus('recordStatus', 'خطا در آپلود: ' + error.message, 'error');
        }
    }
    
    // Text-to-Speech Functions
    updateCharacterCount() {
        const inputText = document.getElementById('inputText');
        const charCount = document.getElementById('charCount');
        
        if (!inputText || !charCount) return;
        
        const count = inputText.value.length;
        charCount.textContent = `${count.toLocaleString('fa')} / 50,000 کاراکتر`;
        
        // Update styling based on count
        charCount.className = 'character-count';
        if (count > 45000) {
            charCount.classList.add('danger');
        } else if (count > 40000) {
            charCount.classList.add('warning');
        }
    }
    
    async detectLanguage() {
        const text = document.getElementById('inputText').value.trim();
        
        if (!text) {
            this.showStatus('ttsStatus', 'لطفاً متن را وارد کنید', 'error');
            return;
        }
        
        try {
            const response = await fetch(`${this.apiBase}/detect-language`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ text })
            });
            
            const result = await response.json();
            
            if (result.success) {
                const languageSelect = document.getElementById('textLanguage');
                const detectedLang = result.data.language;
                const langInfo = result.data.languageInfo;
                
                // Set detected language
                languageSelect.value = detectedLang;
                
                this.showStatus('ttsStatus', 
                    `زبان تشخیص داده شده: ${langInfo ? langInfo.name : detectedLang}`, 
                    'success'
                );
            } else {
                this.showStatus('ttsStatus', 'خطا در تشخیص زبان: ' + result.error, 'error');
            }
        } catch (error) {
            console.error('Language detection error:', error);
            this.showStatus('ttsStatus', 'خطا در تشخیص زبان: ' + error.message, 'error');
        }
    }
    
    async generateSpeech() {
        const text = document.getElementById('inputText').value.trim();
        const profileId = document.getElementById('voiceProfile').value;
        const language = document.getElementById('textLanguage').value;
        const speed = parseFloat(document.getElementById('speed').value);
        const pitch = parseFloat(document.getElementById('pitch').value);
        const emotion = document.getElementById('emotion').value;
        
        // Validation
        if (!text) {
            this.showStatus('ttsStatus', 'لطفاً متن را وارد کنید', 'error');
            return;
        }
        
        if (!profileId) {
            this.showStatus('ttsStatus', 'لطفاً پروفایل صوتی را انتخاب کنید', 'error');
            return;
        }
        
        if (text.length > 50000) {
            this.showStatus('ttsStatus', 'متن نباید بیش از 50,000 کاراکتر باشد', 'error');
            return;
        }
        
        // UI updates
        const generateBtn = document.getElementById('generateBtn');
        const loading = document.getElementById('ttsLoading');
        const audioPlayer = document.getElementById('audioPlayer');
        
        generateBtn.disabled = true;
        loading.classList.add('show');
        audioPlayer.classList.add('hidden');
        
        try {
            const response = await fetch(`${this.apiBase}/synthesize`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    text,
                    profileId,
                    language,
                    speed,
                    pitch,
                    emotion
                })
            });
            
            const result = await response.json();
            
            if (result.success) {
                const audioUrl = result.data.audioUrl;
                
                // Set up audio player
                audioPlayer.src = audioUrl;
                audioPlayer.classList.remove('hidden');
                
                this.showStatus('ttsStatus', 
                    `گفتار با موفقیت تولید شد! (${result.data.characterCount} کاراکتر)`, 
                    'success'
                );
                
                // Auto-play if supported
                try {
                    await audioPlayer.play();
                } catch (playError) {
                    console.log('Auto-play prevented by browser');
                }
                
            } else {
                this.showStatus('ttsStatus', 'خطا در تولید گفتار: ' + result.error, 'error');
            }
            
        } catch (error) {
            console.error('Speech synthesis error:', error);
            this.showStatus('ttsStatus', 'خطا در تولید گفتار: ' + error.message, 'error');
        } finally {
            generateBtn.disabled = false;
            loading.classList.remove('show');
        }
    }
    
    // Profile Management
    async loadProfiles() {
        const profilesLoading = document.getElementById('profilesLoading');
        const profilesList = document.getElementById('profilesList');
        const voiceProfileSelect = document.getElementById('voiceProfile');
        
        if (profilesLoading) profilesLoading.classList.add('show');
        
        try {
            const response = await fetch(`${this.apiBase}/profiles`);
            const result = await response.json();
            
            if (result.success) {
                const profiles = result.data;
                
                // Update profiles list
                if (profilesList) {
                    this.renderProfilesList(profiles);
                }
                
                // Update voice profile select
                if (voiceProfileSelect) {
                    voiceProfileSelect.innerHTML = '<option value="">لطفاً پروفایل را انتخاب کنید</option>';
                    profiles.forEach(profile => {
                        const option = document.createElement('option');
                        option.value = profile.id;
                        option.textContent = `${profile.name} (${profile.language})`;
                        voiceProfileSelect.appendChild(option);
                    });
                }
            } else {
                console.error('Failed to load profiles:', result.error);
            }
        } catch (error) {
            console.error('Profile loading error:', error);
        } finally {
            if (profilesLoading) profilesLoading.classList.remove('show');
        }
    }
    
    renderProfilesList(profiles) {
        const profilesList = document.getElementById('profilesList');
        
        if (profiles.length === 0) {
            profilesList.innerHTML = '<p style="text-align: center; color: #6c757d;">هیچ پروفایل صوتی یافت نشد</p>';
            return;
        }
        
        profilesList.innerHTML = profiles.map(profile => `
            <div class="profile-item">
                <div class="profile-info">
                    <h4>${profile.name}</h4>
                    <small>
                        زبان: ${profile.language} | 
                        مدت: ${profile.duration} دقیقه | 
                        تاریخ: ${new Date(profile.createdAt).toLocaleDateString('fa-IR')}
                    </small>
                </div>
                <div class="profile-actions">
                    <button class="btn btn-secondary" onclick="app.exportProfile('${profile.id}')">
                        <i class="fas fa-download"></i> صادرات
                    </button>
                    <button class="btn btn-danger" onclick="app.deleteProfile('${profile.id}')">
                        <i class="fas fa-trash"></i> حذف
                    </button>
                </div>
            </div>
        `).join('');
    }
    
    async deleteProfile(profileId) {
        if (!confirm('آیا مطمئن هستید که می‌خواهید این پروفایل را حذف کنید؟')) {
            return;
        }
        
        try {
            const response = await fetch(`${this.apiBase}/profiles/${profileId}`, {
                method: 'DELETE'
            });
            
            const result = await response.json();
            
            if (result.success) {
                await this.loadProfiles();
                this.showNotification('پروفایل با موفقیت حذف شد', 'success');
            } else {
                this.showNotification('خطا در حذف پروفایل: ' + result.error, 'error');
            }
        } catch (error) {
            console.error('Delete profile error:', error);
            this.showNotification('خطا در حذف پروفایل: ' + error.message, 'error');
        }
    }
    
    async exportProfile(profileId) {
        try {
            const response = await fetch(`${this.apiBase}/profiles/${profileId}/export`);
            
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.style.display = 'none';
                a.href = url;
                a.download = `voice_profile_${profileId}.zip`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
                
                this.showNotification('پروفایل با موفقیت صادر شد', 'success');
            } else {
                this.showNotification('خطا در صادرات پروفایل', 'error');
            }
        } catch (error) {
            console.error('Export profile error:', error);
            this.showNotification('خطا در صادرات پروفایل: ' + error.message, 'error');
        }
    }
    
    // Settings and Updates
    async checkForUpdates() {
        try {
            this.showStatus('settingsStatus', 'در حال بررسی بروزرسانی...', 'info');
            
            // This would connect to your update server
            // For now, we'll simulate the check
            setTimeout(() => {
                this.showStatus('settingsStatus', 'برنامه به‌روز است', 'success');
            }, 2000);
            
        } catch (error) {
            console.error('Update check error:', error);
            this.showStatus('settingsStatus', 'خطا در بررسی بروزرسانی: ' + error.message, 'error');
        }
    }
    
    saveSettings() {
        const autoUpdate = document.getElementById('autoUpdate').checked;
        const interfaceLanguage = document.getElementById('interfaceLanguage').value;
        const audioQuality = document.getElementById('audioQuality').value;
        
        // Save to localStorage
        localStorage.setItem('voiceCloneSettings', JSON.stringify({
            autoUpdate,
            interfaceLanguage,
            audioQuality
        }));
        
        this.showStatus('settingsStatus', 'تنظیمات ذخیره شد', 'success');
    }
    
    loadSettings() {
        const settings = localStorage.getItem('voiceCloneSettings');
        if (settings) {
            const parsed = JSON.parse(settings);
            
            const autoUpdate = document.getElementById('autoUpdate');
            const interfaceLanguage = document.getElementById('interfaceLanguage');
            const audioQuality = document.getElementById('audioQuality');
            
            if (autoUpdate) autoUpdate.checked = parsed.autoUpdate || false;
            if (interfaceLanguage) interfaceLanguage.value = parsed.interfaceLanguage || 'fa';
            if (audioQuality) audioQuality.value = parsed.audioQuality || 'high';
        }
    }
    
    changeLanguage() {
        const language = document.getElementById('interfaceLanguage').value;
        
        // This would implement actual language switching
        // For now, just save the preference
        this.saveSettings();
        this.showStatus('settingsStatus', 'زبان رابط کاربری تغییر یافت', 'info');
    }
    
    // Language Support
    async loadSupportedLanguages() {
        try {
            const response = await fetch(`${this.apiBase}/info`);
            const result = await response.json();
            
            if (result.success && result.data.supportedLanguages) {
                const textLanguageSelect = document.getElementById('textLanguage');
                
                if (textLanguageSelect) {
                    // Keep auto-detect option
                    const autoOption = textLanguageSelect.querySelector('option[value="auto"]');
                    textLanguageSelect.innerHTML = '';
                    if (autoOption) {
                        textLanguageSelect.appendChild(autoOption);
                    }
                    
                    // Add supported languages
                    result.data.supportedLanguages.forEach(lang => {
                        const option = document.createElement('option');
                        option.value = lang;
                        option.textContent = this.getLanguageName(lang);
                        textLanguageSelect.appendChild(option);
                    });
                }
            }
        } catch (error) {
            console.error('Failed to load supported languages:', error);
        }
    }
    
    getLanguageName(code) {
        const languages = {
            'fa': 'فارسی (Persian)',
            'en': 'انگلیسی (English)',
            'ar': 'عربی (Arabic)',
            'zh': 'چینی (Chinese)',
            'hi': 'هندی (Hindi)',
            'es': 'اسپانیایی (Spanish)',
            'fr': 'فرانسوی (French)',
            'de': 'آلمانی (German)',
            'ja': 'ژاپنی (Japanese)',
            'ko': 'کره‌ای (Korean)',
            'ru': 'روسی (Russian)',
            'pt': 'پرتغالی (Portuguese)',
            'it': 'ایتالیایی (Italian)',
            'tr': 'ترکی (Turkish)'
        };
        
        return languages[code] || code;
    }
    
    // Utility Functions
    showStatus(elementId, message, type = 'info') {
        const element = document.getElementById(elementId);
        if (!element) return;
        
        element.textContent = message;
        element.className = `status status-${type}`;
        
        // Auto-hide success/error messages after 5 seconds
        if (type === 'success' || type === 'error') {
            setTimeout(() => {
                element.textContent = '';
                element.className = 'status';
            }, 5000);
        }
    }
    
    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <span>${message}</span>
            <button onclick="this.parentElement.remove()">×</button>
        `;
        
        // Add styles
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${type === 'success' ? '#d4edda' : type === 'error' ? '#f8d7da' : '#d1ecf1'};
            color: ${type === 'success' ? '#155724' : type === 'error' ? '#721c24' : '#0c5460'};
            padding: 15px 20px;
            border-radius: 8px;
            border: 1px solid ${type === 'success' ? '#c3e6cb' : type === 'error' ? '#f5c6cb' : '#bee5eb'};
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            z-index: 1000;
            max-width: 300px;
            display: flex;
            justify-content: space-between;
            align-items: center;
        `;
        
        notification.querySelector('button').style.cssText = `
            background: none;
            border: none;
            font-size: 18px;
            cursor: pointer;
            margin-left: 10px;
        `;
        
        document.body.appendChild(notification);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentElement) {
                notification.remove();
            }
        }, 5000);
    }
}

// Global functions for HTML onclick handlers
function showTab(tabName) {
    app.showTab(tabName);
}

function startRecording() {
    app.startRecording();
}

function stopRecording() {
    app.stopRecording();
}

function uploadAudioFile() {
    app.uploadAudioFile();
}

function updateCharacterCount() {
    app.updateCharacterCount();
}

function detectLanguage() {
    app.detectLanguage();
}

function generateSpeech() {
    app.generateSpeech();
}

function loadProfiles() {
    app.loadProfiles();
}

function checkForUpdates() {
    app.checkForUpdates();
}

// Initialize app when DOM is loaded
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new VoiceCloneApp();
});

// Handle page visibility changes to pause/resume recording
document.addEventListener('visibilitychange', () => {
    if (document.hidden && app && app.isRecording) {
        console.log('Page hidden during recording - consider pausing');
    }
});

// Handle beforeunload to warn about ongoing recording
window.addEventListener('beforeunload', (event) => {
    if (app && app.isRecording) {
        event.preventDefault();
        event.returnValue = 'ضبط صدا در حال انجام است. آیا مطمئن هستید که می‌خواهید صفحه را ترک کنید؟';
    }
});