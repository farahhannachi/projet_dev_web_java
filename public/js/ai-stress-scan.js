/**
 * AI Stress Detection - Curavita
 * Experimental Beta Feature
 * 
 * Features:
 * - Real-time facial landmark detection using MediaPipe FaceMesh
 * - Stress analysis based on facial indicators
 * - Animated results display
 * - Product recommendations
 */

class AIStressScan {
    constructor() {
        this.video = null;
        this.canvas = null;
        this.ctx = null;
        this.stream = null;
        this.isScanning = false;
        this.faceMesh = null;
        this.camera = null;
        this.landmarks = null;
        this.analysisInterval = null;
        this.stressData = [];
        
        // DOM elements
        this.modal = null;
        this.videoElement = null;
        this.canvasElement = null;
        this.scanOverlay = null;
        this.statusText = null;
        this.analyzeBtn = null;
        this.closeBtn = null;
        this.startBtn = null;
        
        this.init();
    }
    
    init() {
        // Initialize button
        this.startBtn = document.getElementById('startStressScan');
        if (this.startBtn) {
            this.startBtn.addEventListener('click', () => this.openModal());
        }
        
        // Initialize modal elements
        this.modal = document.getElementById('stressScanModal');
        if (!this.modal) return;
        
        this.videoElement = document.getElementById('stressScanVideo');
        this.canvasElement = document.getElementById('stressScanCanvas');
        this.scanOverlay = document.getElementById('stressScanOverlay');
        this.statusText = document.getElementById('stressScanStatus');
        this.analyzeBtn = document.getElementById('analyzeStressBtn');
        this.closeBtn = document.getElementById('closeStressModal');
        
        // Bind events
        if (this.analyzeBtn) {
            this.analyzeBtn.addEventListener('click', () => this.startAnalysis());
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
        
        // Load MediaPipe
        this.loadMediaPipe();
    }
    
    /**
     * Load MediaPipe FaceMesh from CDN
     */
    loadMediaPipe() {
        if (typeof window.FaceMesh === 'undefined') {
            console.log('Loading MediaPipe FaceMesh...');
            
            // Load scripts dynamically
            const scripts = [
                'https://cdn.jsdelivr.net/npm/@mediapipe/camera_utils/camera_utils.js',
                'https://cdn.jsdelivr.net/npm/@mediapipe/control_utils/control_utils.js',
                'https://cdn.jsdelivr.net/npm/@mediapipe/drawing_utils/drawing_utils.js',
                'https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh/face_mesh.js'
            ];
            
            let loadedCount = 0;
            scripts.forEach(src => {
                const script = document.createElement('script');
                script.src = src;
                script.crossOrigin = 'anonymous';
                script.onload = () => {
                    loadedCount++;
                    if (loadedCount === scripts.length) {
                        this.initializeFaceMesh();
                    }
                };
                script.onerror = () => {
                    console.error('Failed to load MediaPipe script:', src);
                    this.updateStatus('Failed to load AI components', 'error');
                };
                document.head.appendChild(script);
            });
        } else {
            this.initializeFaceMesh();
        }
    }
    
    /**
     * Initialize MediaPipe FaceMesh
     */
    initializeFaceMesh() {
        try {
            this.faceMesh = new window.FaceMesh({
                locateFile: (file) => {
                    return `https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh/${file}`;
                }
            });
            
            this.faceMesh.setOptions({
                maxNumFaces: 1,
                refineLandmarks: true,
                minDetectionConfidence: 0.5,
                minTrackingConfidence: 0.5
            });
            
            this.faceMesh.onResults((results) => this.onFaceMeshResults(results));
            
            console.log('✅ MediaPipe FaceMesh initialized');
        } catch (error) {
            console.error('Failed to initialize FaceMesh:', error);
        }
    }
    
    /**
     * Handle FaceMesh detection results
     */
    onFaceMeshResults(results) {
        if (!this.canvasElement) return;
        
        const ctx = this.canvasElement.getContext('2d');
        ctx.save();
        ctx.clearRect(0, 0, this.canvasElement.width, this.canvasElement.height);
        ctx.drawImage(results.image, 0, 0, this.canvasElement.width, this.canvasElement.height);
        
        if (results.multiFaceLandmarks && results.multiFaceLandmarks.length > 0) {
            this.landmarks = results.multiFaceLandmarks[0];
            
            // Draw facial landmarks
            window.drawConnectors(ctx, this.landmarks, window.FACEMESH_TESSELATION, {
                color: '#00FF7F',
                lineWidth: 1
            });
            window.drawConnectors(ctx, this.landmarks, window.FACEMESH_CONTOURS, {
                color: '#00BFFF',
                lineWidth: 1
            });
            window.drawConnectors(ctx, this.landmarks, window.FACEMESH_FACE_OVAL, {
                color: '#00FF7F',
                lineWidth: 2
            });
            window.drawConnectors(ctx, this.landmarks, window.FACEMESH_LEFT_EYE, {
                color: '#FFD700',
                lineWidth: 2
            });
            window.drawConnectors(ctx, this.landmarks, window.FACEMESH_RIGHT_EYE, {
                color: '#FFD700',
                lineWidth: 2
            });
            window.drawConnectors(ctx, this.landmarks, window.FACEMESH_LIPS, {
                color: '#FF69B4',
                lineWidth: 2
            });
            
            // Enable analyze button when face is detected
            if (this.analyzeBtn) {
                this.analyzeBtn.disabled = false;
            }
        } else {
            this.landmarks = null;
            if (this.analyzeBtn) {
                this.analyzeBtn.disabled = true;
            }
        }
        
        ctx.restore();
    }
    
    /**
     * Open modal and start camera
     */
    async openModal() {
        if (!this.modal) {
            console.error('Stress Scan Modal not found');
            return;
        }
        
        this.modal.classList.add('active');
        document.body.style.overflow = 'hidden';
        
        this.updateStatus('Initializing camera...', 'loading');
        
        try {
            await this.startCamera();
            this.updateStatus('Position your face in the frame', 'ready');
        } catch (error) {
            console.error('Camera error:', error);
            this.updateStatus('Camera access denied. Please allow camera access.', 'error');
        }
    }
    
    /**
     * Start camera stream
     */
    async startCamera() {
        if (!this.videoElement) return;
        
        this.stream = await navigator.mediaDevices.getUserMedia({
            video: {
                facingMode: 'user',
                width: { ideal: 640 },
                height: { ideal: 480 }
            }
        });
        
        this.videoElement.srcObject = this.stream;
        
        return new Promise((resolve, reject) => {
            this.videoElement.onloadedmetadata = () => {
                this.videoElement.play();
                
                // Set canvas size
                if (this.canvasElement) {
                    this.canvasElement.width = this.videoElement.videoWidth;
                    this.canvasElement.height = this.videoElement.videoHeight;
                }
                
                // Start MediaPipe camera
                if (this.faceMesh) {
                    this.camera = new window.Camera(this.videoElement, {
                        onFrame: async () => {
                            await this.faceMesh.send({ image: this.videoElement });
                        },
                        width: 640,
                        height: 480
                    });
                    this.camera.start();
                }
                
                resolve();
            };
            
            this.videoElement.onerror = reject;
        });
    }
    
    /**
     * Start stress analysis
     */
    startAnalysis() {
        if (!this.landmarks) {
            this.updateStatus('No face detected. Please position your face in the frame.', 'error');
            return;
        }
        
        this.isScanning = true;
        this.stressData = [];
        this.analyzeBtn.disabled = true;
        this.analyzeBtn.textContent = 'Analyzing...';
        
        this.updateStatus('Analyzing facial patterns...', 'analyzing');
        
        // Add scanning animation class
        if (this.scanOverlay) {
            this.scanOverlay.classList.add('scanning');
        }
        
        // Collect data for 3 seconds
        let samples = 0;
        const maxSamples = 30; // ~3 seconds at 10 samples per second
        
        this.analysisInterval = setInterval(() => {
            if (this.landmarks && samples < maxSamples) {
                const stressMetrics = this.calculateStressMetrics(this.landmarks);
                this.stressData.push(stressMetrics);
                samples++;
            }
            
            if (samples >= maxSamples) {
                clearInterval(this.analysisInterval);
                this.completeAnalysis();
            }
        }, 100);
    }
    
    /**
     * Calculate stress metrics from facial landmarks
     */
    calculateStressMetrics(landmarks) {
        // Key landmark indices for FaceMesh
        const LEFT_EYE_TOP = 386;
        const LEFT_EYE_BOTTOM = 374;
        const RIGHT_EYE_TOP = 159;
        const RIGHT_EYE_BOTTOM = 145;
        const LEFT_BROW = 105;
        const RIGHT_BROW = 334;
        const UPPER_LIP = 0;
        const LOWER_LIP = 17;
        const LEFT_MOUTH = 61;
        const RIGHT_MOUTH = 291;
        
        // Calculate eye openness (average of both eyes)
        const leftEyeOpenness = this.calculateDistance(landmarks[LEFT_EYE_TOP], landmarks[LEFT_EYE_BOTTOM]);
        const rightEyeOpenness = this.calculateDistance(landmarks[RIGHT_EYE_TOP], landmarks[RIGHT_EYE_BOTTOM]);
        const avgEyeOpenness = (leftEyeOpenness + rightEyeOpenness) / 2;
        const eyeFatigue = Math.max(0, Math.min(1, 1 - (avgEyeOpenness * 5))); // Normalize
        
        // Calculate brow tension (distance from brow to eye center)
        const leftEyeCenter = this.getCenter(landmarks[LEFT_EYE_TOP], landmarks[LEFT_EYE_BOTTOM]);
        const rightEyeCenter = this.getCenter(landmarks[RIGHT_EYE_TOP], landmarks[RIGHT_EYE_BOTTOM]);
        const leftBrowTension = this.calculateDistance(landmarks[LEFT_BROW], leftEyeCenter);
        const rightBrowTension = this.calculateDistance(landmarks[RIGHT_BROW], rightEyeCenter);
        const avgBrowTension = (leftBrowTension + rightBrowTension) / 2;
        const browTension = Math.max(0, Math.min(1, avgBrowTension * 3)); // Normalize
        
        // Calculate mouth compression
        const mouthHeight = this.calculateDistance(landmarks[UPPER_LIP], landmarks[LOWER_LIP]);
        const mouthWidth = this.calculateDistance(landmarks[LEFT_MOUTH], landmarks[RIGHT_MOUTH]);
        const mouthCompression = Math.max(0, Math.min(1, 1 - (mouthHeight / (mouthWidth * 0.3 + 0.001))));
        
        // Calculate overall stress score
        const stressScore = (browTension * 0.4) + (mouthCompression * 0.3) + (eyeFatigue * 0.3);
        
        return {
            stressScore,
            browTension,
            mouthCompression,
            eyeFatigue
        };
    }
    
    /**
     * Calculate distance between two landmarks
     */
    calculateDistance(point1, point2) {
        return Math.sqrt(
            Math.pow(point1.x - point2.x, 2) + 
            Math.pow(point1.y - point2.y, 2)
        );
    }
    
    /**
     * Get center point between two landmarks
     */
    getCenter(point1, point2) {
        return {
            x: (point1.x + point2.x) / 2,
            y: (point1.y + point2.y) / 2,
            z: (point1.z + point2.z) / 2
        };
    }
    
    /**
     * Complete analysis and show results
     */
    completeAnalysis() {
        this.isScanning = false;
        
        // Remove scanning animation
        if (this.scanOverlay) {
            this.scanOverlay.classList.remove('scanning');
        }
        
        // Calculate average stress score
        const avgStressScore = this.stressData.reduce((sum, data) => sum + data.stressScore, 0) / this.stressData.length;
        
        // Determine stress level
        let result;
        if (avgStressScore > 0.6) {
            result = {
                level: 'High',
                pattern: 'High Stress Indicators',
                score: Math.round(avgStressScore * 100),
                color: '#ef4444',
                product: {
                    name: 'Magnésium + Passiflore',
                    description: 'Complement alimentaire pour la relaxation et la reduction du stress',
                    url: '/products'
                },
                generalAdvice: 'Pratiquez la respiration profonde, le yoga ou la meditation - disponibles partout gratuitement'
            };
        } else if (avgStressScore >= 0.3) {
            result = {
                level: 'Moderate',
                pattern: 'Fatigue Detecte',
                score: Math.round(avgStressScore * 100),
                color: '#f97316',
                product: {
                    name: 'Vitamine B12 + Ginseng',
                    description: 'Complement energisant pour combattre la fatigue',
                    url: '/products'
                },
                generalAdvice: 'Hydratation reguliere et sommeil de qualite - essentiels pour retrouver votre energie'
            };
        } else {
            result = {
                level: 'Good',
                pattern: 'Aucun Stress Visible',
                score: Math.round(avgStressScore * 100),
                color: '#22c55e',
                product: null,
                generalAdvice: 'Continuez vos bonnes habitudes : activite physique et alimentation equilibree'
            };
        }
        
        this.showResult(result);
        
        // Reset button
        this.analyzeBtn.textContent = 'Analyze Again';
        this.analyzeBtn.disabled = false;
    }
    
    /**
     * Show result popup
     */
    showResult(result) {
        const resultModal = document.getElementById('stressResultModal');
        if (!resultModal) return;
        
        // Update result content
        const scoreElement = resultModal.querySelector('.stress-score-value');
        const patternElement = resultModal.querySelector('.stress-pattern');
        const levelElement = resultModal.querySelector('.stress-level');
        const productSection = resultModal.querySelector('.product-recommendation');
        
        // Animate score counter
        if (scoreElement) {
            this.animateCounter(scoreElement, 0, result.score, 1000);
        }
        
        if (patternElement) {
            patternElement.textContent = result.pattern;
        }
        
        if (levelElement) {
            levelElement.textContent = result.level;
            levelElement.style.color = result.color;
            levelElement.className = `stress-level stress-level-${result.level.toLowerCase()}`;
        }
        
        // Handle product recommendation
        if (productSection) {
            if (result.product) {
                productSection.innerHTML = `
                    <div class="recommended-product">
                        <h4>Produit Recommande</h4>
                        <div class="product-card">
                            <span class="product-name">${result.product.name}</span>
                            <span class="product-description">${result.product.description}</span>
                            <a href="${result.product.url}" class="btn btn-primary btn-sm">
                                Voir les Produits
                            </a>
                        </div>
                        <div class="general-advice">
                            <h5>💡 Conseil Bien-etre</h5>
                            <p>${result.generalAdvice}</p>
                        </div>
                        <div class="contact-admin-section">
                            <button type="button" class="btn-contact-admin" onclick="showContactAdminForm('${result.product.name}', '${result.level}')">
                                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                    <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
                                </svg>
                                Contacter l'Admin
                            </button>
                        </div>
                    </div>
                `;
                productSection.style.display = 'block';
            } else {
                productSection.innerHTML = `
                    <div class="wellness-message">
                        <div class="wellness-icon">✨</div>
                        <p>Vous etes en excellente forme !</p>
                        <p>Aucun produit bien-etre necessaire.</p>
                    </div>
                    <div class="general-advice">
                        <h5>💡 Conseil Bien-etre</h5>
                        <p>${result.generalAdvice}</p>
                    </div>
                    <div class="contact-admin-section">
                        <button type="button" class="btn-contact-admin" onclick="showContactAdminForm('Bien-etre General', '${result.level}')">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
                            </svg>
                            Contacter l'Admin
                        </button>
                    </div>
                `;
                productSection.style.display = 'block';
            }
        }
        
        // Show result modal with animation
        resultModal.classList.add('active');
        
        // Close camera modal
        if (this.modal) {
            this.modal.classList.remove('active');
        }
    }
    
    /**
     * Animate counter from start to end
     */
    animateCounter(element, start, end, duration) {
        const startTime = performance.now();
        
        const update = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            // Easing function
            const easeOut = 1 - Math.pow(1 - progress, 3);
            const current = Math.round(start + (end - start) * easeOut);
            
            element.textContent = `${current}%`;
            
            if (progress < 1) {
                requestAnimationFrame(update);
            }
        };
        
        requestAnimationFrame(update);
    }
    
    /**
     * Close modal and cleanup
     */
    closeModal() {
        if (this.modal) {
            this.modal.classList.remove('active');
        }
        
        document.body.style.overflow = '';
        
        // Stop analysis if running
        if (this.analysisInterval) {
            clearInterval(this.analysisInterval);
            this.analysisInterval = null;
        }
        
        // Stop camera
        if (this.camera) {
            this.camera.stop();
            this.camera = null;
        }
        
        if (this.stream) {
            this.stream.getTracks().forEach(track => track.stop());
            this.stream = null;
        }
        
        if (this.videoElement) {
            this.videoElement.srcObject = null;
        }
        
        // Reset state
        this.isScanning = false;
        this.landmarks = null;
        this.stressData = [];
        
        if (this.analyzeBtn) {
            this.analyzeBtn.textContent = 'Analyze';
            this.analyzeBtn.disabled = true;
        }
        
        if (this.scanOverlay) {
            this.scanOverlay.classList.remove('scanning');
        }
    }
    
    /**
     * Update status text
     */
    updateStatus(message, type) {
        if (this.statusText) {
            this.statusText.textContent = message;
            this.statusText.className = 'scan-status status-' + type;
        }
    }
}

// Close result modal function
function closeStressResult() {
    const resultModal = document.getElementById('stressResultModal');
    if (resultModal) {
        resultModal.classList.remove('active');
    }
    
    // Also close the scan modal and stop camera
    const scanModal = document.getElementById('stressScanModal');
    if (scanModal) {
        // Stop camera stream if running
        const video = document.getElementById('stressScanVideo');
        if (video && video.srcObject) {
            video.srcObject.getTracks().forEach(track => track.stop());
            video.srcObject = null;
        }
        
        // Hide modal
        scanModal.classList.remove('active');
        document.body.style.overflow = '';
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new AIStressScan();
});

// Export for global access
window.AIStressScan = AIStressScan;
window.closeStressResult = closeStressResult;

/**
 * Show contact admin form
 */
function showContactAdminForm(recommendedProduct, stressLevel) {
    const resultModal = document.getElementById('stressResultModal');
    if (!resultModal) return;
    
    const productSection = resultModal.querySelector('.product-recommendation');
    if (!productSection) return;
    
    // Replace content with contact form
    productSection.innerHTML = `
        <div class="contact-admin-form">
            <h4>Contact Admin</h4>
            <p class="contact-subtitle">Send a message about your wellness needs</p>
            <div class="form-group">
                <textarea id="adminMessageText" class="contact-textarea" placeholder="Type your message here... For example: I'm looking for a specific product for stress relief that was recommended by the AI scan."></textarea>
            </div>
            <div class="contact-actions">
                <button type="button" class="btn btn-primary btn-sm" onclick="sendAdminMessage('${recommendedProduct}', '${stressLevel}')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="22" y1="2" x2="11" y2="13"></line>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                    </svg>
                    Send Message
                </button>
                <button type="button" class="btn btn-secondary btn-sm" onclick="hideContactAdminForm('${recommendedProduct}', '${stressLevel}')">
                    Cancel
                </button>
            </div>
            <div id="contactFormStatus" class="form-status"></div>
        </div>
    `;
}

/**
 * Hide contact admin form and restore simple view
 */
function hideContactAdminForm(recommendedProduct, stressLevel) {
    const resultModal = document.getElementById('stressResultModal');
    if (!resultModal) return;
    
    const productSection = resultModal.querySelector('.product-recommendation');
    if (!productSection) return;
    
    // Simple restoration - just show the contact button again
    const isGoodLevel = stressLevel === 'Good';
    
    if (isGoodLevel) {
        productSection.innerHTML = `
            <div class="wellness-message">
                <div class="wellness-icon">✨</div>
                <p>Vous etes en excellente forme !</p>
                <p>Aucun produit bien-etre necessaire.</p>
            </div>
            <div class="contact-admin-section">
                <button type="button" class="btn-contact-admin" onclick="showContactAdminForm('Bien-etre General', 'Good')">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
                    </svg>
                    Contacter l'Admin
                </button>
            </div>
        `;
    } else {
        productSection.innerHTML = `
            <div class="recommended-product">
                <h4>Produit Recommande</h4>
                <div class="product-card">
                    <span class="product-name">${recommendedProduct}</span>
                    <a href="/products" class="btn btn-primary btn-sm">
                        Voir les Produits
                    </a>
                </div>
                <div class="contact-admin-section">
                    <button type="button" class="btn-contact-admin" onclick="showContactAdminForm('${recommendedProduct}', '${stressLevel}')">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
                        </svg>
                        Contacter l'Admin
                    </button>
                </div>
            </div>
        `;
    }
}

/**
 * Send message to admin
 */
async function sendAdminMessage(recommendedProduct, stressLevel) {
    const messageTextarea = document.getElementById('adminMessageText');
    const statusDiv = document.getElementById('contactFormStatus');
    
    if (!messageTextarea || !statusDiv) return;
    
    const message = messageTextarea.value.trim();
    
    if (!message) {
        statusDiv.innerHTML = '<span class="status-error">Please type a message before sending.</span>';
        return;
    }
    
    statusDiv.innerHTML = '<span class="status-loading">Sending message...</span>';
    
    try {
        const response = await fetch('/api/stress-scan/contact-admin', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify({
                message: message,
                recommendedProduct: recommendedProduct,
                stressLevel: stressLevel
            })
        });
        
        const data = await response.json();
        
        if (response.ok && data.success) {
            statusDiv.innerHTML = '<span class="status-success">✅ Message sent successfully! Admin will contact you soon.</span>';
            messageTextarea.value = '';
            
            // Close the form after 2 seconds
            setTimeout(() => {
                hideContactAdminForm(recommendedProduct, stressLevel);
            }, 2000);
        } else {
            statusDiv.innerHTML = `<span class="status-error">❌ Failed to send: ${data.error || 'Unknown error'}</span>`;
        }
    } catch (error) {
        console.error('Error sending message:', error);
        statusDiv.innerHTML = '<span class="status-error">❌ Network error. Please try again.</span>';
    }
}

// Export for global access
window.showContactAdminForm = showContactAdminForm;
window.hideContactAdminForm = hideContactAdminForm;
window.sendAdminMessage = sendAdminMessage;