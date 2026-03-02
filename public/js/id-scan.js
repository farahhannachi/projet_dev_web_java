/**
 * ID Card Scan Login - Curavita
 * Experimental Beta Feature
 * 
 * Features:
 * - Camera-based ID card scanning
 * - Tesseract.js OCR integration
 * - Animated scanning UI
 * - Secure login via extracted student ID
 */

class IdScanLogin {
    constructor() {
        this.video = null;
        this.canvas = null;
        this.ctx = null;
        this.stream = null;
        this.isScanning = false;
        this.scanAnimationId = null;
        
        // DOM elements
        this.modal = null;
        this.videoElement = null;
        this.canvasElement = null;
        this.scanOverlay = null;
        this.statusText = null;
        this.scanBtn = null;
        this.closeBtn = null;
        
        // Tesseract worker
        this.tesseractWorker = null;
        this.isTesseractReady = false;
        
        this.init();
    }
    
    init() {
        // Initialize modal elements
        this.modal = document.getElementById('idScanModal');
        if (!this.modal) return;
        
        this.videoElement = document.getElementById('idScanVideo');
        this.canvasElement = document.getElementById('idScanCanvas');
        this.scanOverlay = document.getElementById('scanOverlay');
        this.statusText = document.getElementById('scanStatus');
        this.scanBtn = document.getElementById('scanNowBtn');
        this.closeBtn = document.getElementById('closeScanModal');
        
        // Bind events
        if (this.scanBtn) {
            this.scanBtn.addEventListener('click', () => this.performScan());
        }
        
        if (this.closeBtn) {
            this.closeBtn.addEventListener('click', () => this.closeModal());
        }
        
        // Close on backdrop click
        this.modal.addEventListener('click', (e) => {
            if (e.target === this.modal) {
                this.closeModal();
            }
        });
        
        // Initialize Tesseract
        this.initTesseract();
    }
    
    /**
     * Initialize Tesseract.js OCR
     */
    async initTesseract() {
        if (typeof Tesseract === 'undefined') {
            console.error('Tesseract.js not loaded');
            return;
        }
        
        try {
            this.updateStatus('Initializing OCR...', 'loading');
            
            this.tesseractWorker = await Tesseract.createWorker('eng');
            this.isTesseractReady = true;
            
            this.updateStatus('Ready to scan', 'ready');
            console.log('✅ Tesseract.js initialized');
        } catch (error) {
            console.error('Failed to initialize Tesseract:', error);
            this.updateStatus('OCR initialization failed', 'error');
        }
    }
    
    /**
     * Open modal and start camera
     */
    async openModal() {
        if (!this.modal) {
            console.error('ID Scan Modal not found');
            return;
        }
        
        this.modal.classList.add('active');
        document.body.style.overflow = 'hidden';
        
        await this.startCamera();
    }
    
    /**
     * Close modal and stop camera
     */
    closeModal() {
        if (!this.modal) return;
        
        this.modal.classList.remove('active');
        document.body.style.overflow = '';
        
        this.stopCamera();
        this.stopScanAnimation();
    }
    
    /**
     * Start camera stream
     */
    async startCamera() {
        if (!this.videoElement) return;
        
        try {
            this.updateStatus('Accessing camera...', 'loading');
            
            this.stream = await navigator.mediaDevices.getUserMedia({
                video: {
                    facingMode: 'environment',
                    width: { ideal: 1920 },
                    height: { ideal: 1080 }
                },
                audio: false
            });
            
            this.videoElement.srcObject = this.stream;
            
            await new Promise((resolve) => {
                this.videoElement.onloadedmetadata = () => {
                    resolve();
                };
            });
            
            await this.videoElement.play();
            
            // Setup canvas
            this.canvasElement.width = this.videoElement.videoWidth;
            this.canvasElement.height = this.videoElement.videoHeight;
            this.ctx = this.canvasElement.getContext('2d');
            
            this.updateStatus('Position your ID card in the frame', 'ready');
            
        } catch (error) {
            console.error('Camera access error:', error);
            this.updateStatus('Camera access denied. Please allow camera permissions.', 'error');
        }
    }
    
    /**
     * Stop camera stream
     */
    stopCamera() {
        if (this.stream) {
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }
        
        if (this.videoElement) {
            this.videoElement.srcObject = null;
        }
    }
    
    /**
     * Perform the scan
     */
    async performScan() {
        if (this.isScanning || !this.isTesseractReady) return;
        
        this.isScanning = true;
        this.startScanAnimation();
        this.updateStatus('Scanning...', 'scanning');
        
        try {
            // Capture frame
            this.ctx.drawImage(this.videoElement, 0, 0, this.canvasElement.width, this.canvasElement.height);
            
            // Convert to image data
            const imageData = this.canvasElement.toDataURL('image/jpeg', 0.9);
            
            // Run OCR
            const result = await this.tesseractWorker.recognize(imageData);
            const text = result.data.text;
            
            console.log('OCR Result:', text);
            
            // Extract student ID
            const studentId = this.extractStudentId(text);
            
            if (studentId) {
                this.updateStatus('ID found! Validating...', 'success');
                await this.sendToServer(studentId);
            } else {
                this.updateStatus('No ID detected. Please try again.', 'error');
                setTimeout(() => {
                    this.updateStatus('Position your ID card in the frame', 'ready');
                }, 2000);
            }
            
        } catch (error) {
            console.error('Scan error:', error);
            this.updateStatus('Scan failed. Please try again.', 'error');
        } finally {
            this.isScanning = false;
            this.stopScanAnimation();
        }
    }
    
    /**
     * Extract student ID from OCR text
     * Format: 3 digits + 3 uppercase letters + 4 digits (e.g., 231JMT0405)
     */
    extractStudentId(text) {
        // Common patterns for student IDs
        const patterns = [
            // Specific format: 3 digits + 3 uppercase letters + 4 digits (e.g., 231JMT0405)
            /(?:^|\s)(\d{3}[A-Z]{3}\d{4})(?:\s|$)/,
            
            // Identifiant pattern (French student cards)
            /identifiant\s*:?\s*([a-zA-Z0-9]+)/i,
            /identifiant\s+([a-zA-Z0-9]+)/i,
            
            // Generic student ID patterns
            /student\s*id\s*:?\s*([a-zA-Z0-9_-]+)/i,
            /id\s*number\s*:?\s*([a-zA-Z0-9_-]+)/i,
            /matricule\s*:?\s*([a-zA-Z0-9_-]+)/i,
            /student\s*number\s*:?\s*([a-zA-Z0-9_-]+)/i,
            
            // Other common formats
            /(?:^|\s)([A-Z]{2,4}\d{6,10})(?:\s|$)/,  // e.g., ABC123456
            /(?:^|\s)(\d{8,12})(?:\s|$)/,  // 8-12 digit numbers
            /(?:^|\s)(\d{4}[A-Z]{2,3}\d{4})(?:\s|$)/,  // e.g., 2313MT0405 (OCR might read 3 as MT)
        ];
        
        for (const pattern of patterns) {
            const match = text.match(pattern);
            if (match && match[1]) {
                return match[1].trim().toUpperCase();
            }
        }
        
        // Fallback: look for pattern matching 3 digits + 3 letters + 4 digits
        const fallbackMatch = text.match(/\d{3}[A-Z]{3}\d{4}/);
        if (fallbackMatch) {
            return fallbackMatch[0].toUpperCase();
        }
        
        // Last resort: any alphanumeric sequence of 10+ characters that looks like an ID
        const lastResort = text.match(/[A-Z0-9]{10,11}/);
        if (lastResort) {
            return lastResort[0].toUpperCase();
        }
        
        return null;
    }
    
    /**
     * Send student ID to server for login
     */
    async sendToServer(studentId) {
        try {
            const response = await fetch('/scan-id-login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({ student_id: studentId })
            });
            
            const data = await response.json();
            
            if (data.status === 'success') {
                this.updateStatus('Login successful! Redirecting...', 'success');
                this.showSuccessAnimation();
                
                setTimeout(() => {
                    window.location.href = data.redirect;
                }, 1500);
            } else {
                this.updateStatus(data.message || 'ID not recognized', 'error');
                setTimeout(() => {
                    this.updateStatus('Position your ID card in the frame', 'ready');
                }, 3000);
            }
            
        } catch (error) {
            console.error('Server error:', error);
            this.updateStatus('Connection error. Please try again.', 'error');
        }
    }
    
    /**
     * Start scan animation
     */
    startScanAnimation() {
        if (!this.scanOverlay) return;
        
        this.scanOverlay.classList.add('scanning');
        
        // Animate scanning line
        let position = 0;
        let direction = 1;
        
        const animate = () => {
            if (!this.isScanning) return;
            
            position += direction * 2;
            if (position >= 100) direction = -1;
            if (position <= 0) direction = 1;
            
            this.scanOverlay.style.setProperty('--scan-position', `${position}%`);
            this.scanAnimationId = requestAnimationFrame(animate);
        };
        
        animate();
    }
    
    /**
     * Stop scan animation
     */
    stopScanAnimation() {
        if (this.scanAnimationId) {
            cancelAnimationFrame(this.scanAnimationId);
            this.scanAnimationId = null;
        }
        
        if (this.scanOverlay) {
            this.scanOverlay.classList.remove('scanning');
        }
    }
    
    /**
     * Show success animation
     */
    showSuccessAnimation() {
        if (!this.scanOverlay) return;
        
        this.scanOverlay.classList.add('success');
    }
    
    /**
     * Update status text
     */
    updateStatus(message, type) {
        if (!this.statusText) return;
        
        this.statusText.textContent = message;
        this.statusText.className = 'scan-status ' + type;
    }
}

/**
 * ID Card Upload for Profile Page
 */
class IdCardUpload {
    constructor() {
        this.fileInput = document.getElementById('idCardInput');
        this.uploadBtn = document.getElementById('uploadIdCardBtn');
        this.previewContainer = document.getElementById('idCardPreview');
        this.ocrResult = document.getElementById('ocrResult');
        
        this.tesseractWorker = null;
        this.isTesseractReady = false;
        
        this.init();
    }
    
    init() {
        if (!this.uploadBtn) return;
        
        this.uploadBtn.addEventListener('click', () => this.fileInput?.click());
        
        if (this.fileInput) {
            this.fileInput.addEventListener('change', (e) => this.handleFileSelect(e));
        }
        
        // Initialize Tesseract
        this.initTesseract();
    }
    
    async initTesseract() {
        if (typeof Tesseract === 'undefined') return;
        
        try {
            this.tesseractWorker = await Tesseract.createWorker('eng');
            this.isTesseractReady = true;
        } catch (error) {
            console.error('Tesseract initialization failed:', error);
        }
    }
    
    async handleFileSelect(event) {
        const file = event.target.files[0];
        if (!file) return;
        
        // Validate file
        const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            this.showMessage('Please select an image file (JPEG, PNG, or WebP).', 'error');
            return;
        }
        
        if (file.size > 5 * 1024 * 1024) {
            this.showMessage('File size must be less than 5MB.', 'error');
            return;
        }
        
        // Show preview
        await this.showPreview(file);
        
        // Run OCR
        await this.processOCR(file);
        
        // Upload file
        await this.uploadFile(file);
    }
    
    async showPreview(file) {
        if (!this.previewContainer) return;
        
        const reader = new FileReader();
        reader.onload = (e) => {
            this.previewContainer.innerHTML = `
                <img src="${e.target.result}" alt="ID Card Preview" class="id-card-preview-img">
            `;
        };
        reader.readAsDataURL(file);
    }
    
    async processOCR(file) {
        if (!this.isTesseractReady) {
            this.showOcrResult('OCR not ready. Please try again.', 'error');
            return;
        }
        
        this.showOcrResult('Extracting text from ID card...', 'loading');
        
        try {
            const imageUrl = URL.createObjectURL(file);
            const result = await this.tesseractWorker.recognize(imageUrl);
            const text = result.data.text;
            
            URL.revokeObjectURL(imageUrl);
            
            console.log('OCR Result:', text);
            
            // Extract student ID
            const studentId = this.extractStudentId(text);
            
            if (studentId) {
                this.showOcrResult(`Student ID detected: <strong>${studentId}</strong>`, 'success');
                await this.saveStudentId(studentId);
            } else {
                this.showOcrResult('Could not detect student ID. Please enter it manually.', 'warning');
                this.showManualInput();
            }
            
        } catch (error) {
            console.error('OCR error:', error);
            this.showOcrResult('Failed to extract text. Please enter ID manually.', 'error');
            this.showManualInput();
        }
    }
    
    extractStudentId(text) {
        // Format: 3 digits + 3 uppercase letters + 4 digits (e.g., 231JMT0405)
        const patterns = [
            // Specific format: 3 digits + 3 uppercase letters + 4 digits
            /(?:^|\s)(\d{3}[A-Z]{3}\d{4})(?:\s|$)/,
            
            // Identifiant pattern (French student cards)
            /identifiant\s*:?\s*([a-zA-Z0-9]+)/i,
            /identifiant\s+([a-zA-Z0-9]+)/i,
            
            // Generic patterns
            /student\s*id\s*:?\s*([a-zA-Z0-9_-]+)/i,
            /id\s*number\s*:?\s*([a-zA-Z0-9_-]+)/i,
            /matricule\s*:?\s*([a-zA-Z0-9_-]+)/i,
            /(?:^|\s)([A-Z]{2,4}\d{6,10})(?:\s|$)/,
            /(?:^|\s)(\d{8,12})(?:\s|$)/,
            /(?:^|\s)(\d{4}[A-Z]{2,3}\d{4})(?:\s|$)/,
        ];
        
        for (const pattern of patterns) {
            const match = text.match(pattern);
            if (match && match[1]) {
                return match[1].trim().toUpperCase();
            }
        }
        
        // Fallback: look for pattern matching 3 digits + 3 letters + 4 digits
        const fallbackMatch = text.match(/\d{3}[A-Z]{3}\d{4}/);
        if (fallbackMatch) {
            return fallbackMatch[0].toUpperCase();
        }
        
        // Last resort: any alphanumeric sequence of 10+ characters
        const lastResort = text.match(/[A-Z0-9]{10,11}/);
        if (lastResort) {
            return lastResort[0].toUpperCase();
        }
        
        return null;
    }
    
    async uploadFile(file) {
        const formData = new FormData();
        formData.append('id_card', file);
        
        try {
            const response = await fetch('/profile/id-card/upload', {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            
            const data = await response.json();
            
            if (data.status === 'success') {
                this.showMessage('ID card uploaded successfully!', 'success');
            } else {
                this.showMessage(data.message, 'error');
            }
            
        } catch (error) {
            console.error('Upload error:', error);
            this.showMessage('Failed to upload ID card.', 'error');
        }
    }
    
    async saveStudentId(studentId) {
        try {
            const response = await fetch('/profile/student-id/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({ student_id: studentId })
            });
            
            const data = await response.json();
            
            if (data.status === 'success') {
                this.showMessage(data.message, 'success');
                // Reload page to show updated status
                setTimeout(() => location.reload(), 1500);
            } else {
                this.showMessage(data.message, 'error');
            }
            
        } catch (error) {
            console.error('Save error:', error);
            this.showMessage('Failed to save student ID.', 'error');
        }
    }
    
    showManualInput() {
        if (!this.ocrResult) return;
        
        const manualDiv = document.createElement('div');
        manualDiv.className = 'manual-id-input';
        manualDiv.innerHTML = `
            <input type="text" id="manualStudentId" placeholder="Enter your Student ID" class="form-control">
            <button id="saveManualId" class="btn btn-primary mt-2">Save ID</button>
        `;
        
        this.ocrResult.appendChild(manualDiv);
        
        document.getElementById('saveManualId')?.addEventListener('click', () => {
            const input = document.getElementById('manualStudentId');
            if (input && input.value.trim()) {
                this.saveStudentId(input.value.trim());
            }
        });
    }
    
    showOcrResult(html, type) {
        if (!this.ocrResult) return;
        this.ocrResult.innerHTML = `<div class="ocr-result ${type}">${html}</div>`;
    }
    
    showMessage(message, type) {
        // Use a toast or notification system if available
        // Fallback to alert for now
        if (type === 'error') {
            console.error(message);
        } else {
            console.log(message);
        }
        
        // Show in ocrResult if available
        if (this.ocrResult && type !== 'loading') {
            this.ocrResult.innerHTML = `<div class="ocr-result ${type}">${message}</div>`;
        }
    }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
    // Initialize ID scan login (for login page)
    window.idScanLogin = new IdScanLogin();
    
    // Initialize ID card upload (for profile page)
    window.idCardUpload = new IdCardUpload();
    
    console.log('🔐 ID Scan Login initialized');
});

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { IdScanLogin, IdCardUpload };
}
