// ===========================
// AIR MOUSE - Full Browser Navigation
// ===========================

class AirMouse {
    constructor(isModal = false) {
        // State
        this.isActive = false;
        this.handDetected = false;
        this.lastClickTime = 0;
        this.clickDebounce = 400; // ms - prevent double clicks
        this.fps = 0;
        this.lastFrameTime = Date.now();
        this.frameCount = 0;
        this.isModal = isModal;

        // Pinch state tracking
        this.isPinching = false;
        this.pinchStartTime = 0;
        this.pinchThreshold = 0.06; // distance threshold for pinch
        this.pinchReleaseThreshold = 0.08; // hysteresis to prevent flicker

        // Scroll gesture state
        this.isScrollMode = false;
        this.scrollBaseY = null;
        this.scrollStartPageY = null;
        
        // Smooth scroll - TikTok/Reels style
        this.scrollVelocity = 0;
        this.scrollTargetY = null; // Target position for smooth scroll
        this.scrollAnimationId = null;
        this.lastScrollUpdate = 0;
        this.scrollSmoothFactor = 0.1; // Lower = smoother, higher = more responsive
        this.scrollDeadzone = 0.05; // Larger deadzone to prevent jitter

        // Hover tracking
        this.lastHoveredElement = null;

        // MediaPipe
        this.hands = null;
        this.camera = null;
        this.canvasCtx = null;

        // DOM Elements - Support both Modal and Global suffixes
        // isModal=true can use either videoInputModal or videoInputGlobal
        const suffix = isModal ? 'Modal' : '';
        const globalSuffix = isModal ? 'Global' : '';
        this.videoInput = document.getElementById(`videoInput${suffix}`) || document.getElementById(`videoInput${globalSuffix}`);
        this.canvasOutput = document.getElementById(`canvasOutput${suffix}`) || document.getElementById(`canvasOutput${globalSuffix}`);
        this.toggleButton = document.getElementById(`toggleButton${suffix}`) || document.getElementById(`toggleButton${globalSuffix}`);
        this.switchLabel = document.getElementById(`switchLabel${suffix}`) || document.getElementById(`switchLabel${globalSuffix}`);
        
        // Check if required elements exist - if not, don't initialize yet
        this.elementsReady = !!(this.videoInput && this.canvasOutput);
        
        // Create or get cursor - check for existing cursor first
        if (isModal) {
            // Check if cursor already exists in the DOM
            this.cursor = document.querySelector('.air-mouse-cursor-modal') || document.querySelector('.air-mouse-cursor');
            if (!this.cursor) {
                this.cursor = document.createElement('div');
                this.cursor.className = 'air-mouse-cursor-modal';
                this.cursor.innerHTML = '<div class="cursor-ring"><div class="cursor-dot"></div></div>';
                this.cursor.style.cssText = 'position:fixed;width:40px;height:40px;pointer-events:none;z-index:100000;display:none;';
                document.body.appendChild(this.cursor);
            }
        } else {
            this.cursor = document.querySelector('.air-mouse-cursor');
        }

        // Stats elements (full page mode)
        const statsSuffix = isModal ? 'Modal' : '';
        const statsGlobalSuffix = isModal ? 'Global' : '';
        this.statsElements = {
            status: document.getElementById(isModal ? 'statusValueModal' : 'statusValue') || document.getElementById('statusValueGlobal'),
            fps: document.getElementById(isModal ? 'fpsModal' : 'fps') || document.getElementById('fpsGlobal'),
            handDetected: document.getElementById(isModal ? 'handDetectedModal' : 'handDetected') || document.getElementById('handDetectedGlobal'),
            posX: document.getElementById('posX'),
            posY: document.getElementById('posY'),
            distance: document.getElementById('distance'),
        };

        // Cursor position (start at center of viewport)
        this.cursorX = window.innerWidth / 2;
        this.cursorY = window.innerHeight / 2;
        this.targetX = this.cursorX;
        this.targetY = this.cursorY;

        // Smoothing factor (lower = smoother but more lag)
        this.smoothingFactor = 0.25;

        // Track if we've initialized MediaPipe
        this.mediaPipeInitialized = false;

        // Setup event listeners (done immediately for toggle button)
        this.setupEventListeners();
    }

    // ===========================
    // INITIALIZATION
    // ===========================
    async init() {
        // Check if elements are ready - try both Modal and Global suffixes
        const suffix = this.isModal ? 'Modal' : '';
        const globalSuffix = this.isModal ? 'Global' : '';
        
        if (!this.videoInput) {
            this.videoInput = document.getElementById(`videoInput${suffix}`) || document.getElementById(`videoInput${globalSuffix}`);
        }
        if (!this.canvasOutput) {
            this.canvasOutput = document.getElementById(`canvasOutput${suffix}`) || document.getElementById(`canvasOutput${globalSuffix}`);
        }
        
        if (!this.videoInput || !this.canvasOutput) {
            console.warn('⚠️ Air Mouse elements not found, retrying...');
            // Retry after short delay for DOM to be ready
            await new Promise(resolve => setTimeout(resolve, 500));
            this.videoInput = document.getElementById(`videoInput${suffix}`) || document.getElementById(`videoInput${globalSuffix}`);
            this.canvasOutput = document.getElementById(`canvasOutput${suffix}`) || document.getElementById(`canvasOutput${globalSuffix}`);
            
            if (!this.videoInput || !this.canvasOutput) {
                console.error('❌ Air Mouse elements still not found!');
                return;
            }
        }
        
        try {
            if (typeof Hands === 'undefined') {
                console.error('❌ Hands library not loaded!');
                return;
            }

            // Initialize MediaPipe Hands
            this.hands = new Hands({
                locateFile: (file) => {
                    return `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`;
                }
            });

            this.hands.setOptions({
                maxNumHands: 1,
                modelComplexity: 1,
                minDetectionConfidence: 0.6,
                minTrackingConfidence: 0.6
            });

            this.hands.onResults(this.onHandResults.bind(this));

            // Setup canvas context
            this.canvasCtx = this.canvasOutput.getContext('2d');

            this.mediaPipeInitialized = true;
            console.log('✅ Air Mouse initialized successfully');
        } catch (error) {
            console.error('❌ Error initializing Air Mouse:', error);
        }
    }

    setupEventListeners() {
        if (this.toggleButton) {
            this.toggleButton.addEventListener('click', () => this.toggle());
        }

        // Handle visibility change
        if (!this.isModal) {
            document.addEventListener('visibilitychange', () => {
                if (document.hidden && this.isActive) {
                    this.stop(true);
                }
            });

            window.addEventListener('beforeunload', () => {
                this.stop(true);
            });
        }
    }

    // ===========================
    // TOGGLE ON/OFF
    // ===========================
    async toggle() {
        if (this.isActive) {
            this.stop();
        } else {
            await this.start();
        }
    }

    async start() {
        // Initialize MediaPipe if not already done
        if (!this.mediaPipeInitialized) {
            await this.init();
            if (!this.hands) {
                console.error('❌ Failed to initialize MediaPipe Hands');
                this.isActive = false;
                this.updateUI();
                return;
            }
            this.mediaPipeInitialized = true;
        }
        
        // Re-fetch elements in case they changed after page navigation
        const suffix = this.isModal ? 'Modal' : '';
        const globalSuffix = this.isModal ? 'Global' : '';
        if (!this.videoInput) {
            this.videoInput = document.getElementById(`videoInput${suffix}`) || document.getElementById(`videoInput${globalSuffix}`);
        }
        if (!this.canvasOutput) {
            this.canvasOutput = document.getElementById(`canvasOutput${suffix}`) || document.getElementById(`canvasOutput${globalSuffix}`);
        }
        
        if (!this.videoInput || !this.canvasOutput) {
            console.error('❌ Video/Canvas elements not found!');
            this.isActive = false;
            this.updateUI();
            return;
        }
        
        try {
            this.isActive = true;
            this.updateUI();
            
            // Reset scroll state when starting
            this.isScrollMode = false;
            this.scrollBaseY = null;
            this.scrollStartPageY = null;
            this.scrollVelocity = 0;

            console.log('📹 Requesting camera access...');
            const stream = await navigator.mediaDevices.getUserMedia({
                video: {
                    width: { ideal: 640 },
                    height: { ideal: 480 },
                    facingMode: 'user'
                },
                audio: false
            });

            this.videoInput.srcObject = stream;

            await new Promise(resolve => {
                this.videoInput.onloadedmetadata = () => {
                    this.canvasOutput.width = this.videoInput.videoWidth;
                    this.canvasOutput.height = this.videoInput.videoHeight;
                    resolve();
                };
                setTimeout(resolve, 2000);
            });

            if (typeof Camera === 'undefined') {
                console.error('❌ Camera class not loaded!');
                this.stop();
                return;
            }

            this.camera = new Camera(this.videoInput, {
                onFrame: async () => {
                    if (this.isActive && this.hands) {
                        try {
                            await this.hands.send({ image: this.videoInput });
                        } catch (error) {
                            // Silently handle frame errors
                        }
                    }
                }
            });

            this.camera.start();

            console.log('✅ Air Mouse active - move your hand to navigate!');
            if (this.statsElements?.status) {
                this.statsElements.status.textContent = 'Actif ✅';
                this.statsElements.status.style.color = '#22c55e';
            }
        } catch (error) {
            console.error('❌ Error starting camera:', error);
            if (this.statsElements?.status) {
                this.statsElements.status.textContent = 'Erreur: ' + error.message;
                this.statsElements.status.style.color = '#ff4444';
            }
            alert('Erreur caméra: ' + error.message);
            this.isActive = false;
            this.updateUI();
        }
    }

    stop(silent = false) {
        this.isActive = false;
        this.handDetected = false;

        // Stop camera
        if (this.camera) {
            this.camera.stop();
            this.camera = null;
        }

        // Stop video stream
        if (this.videoInput && this.videoInput.srcObject) {
            const tracks = this.videoInput.srcObject.getTracks();
            tracks.forEach(track => track.stop());
            this.videoInput.srcObject = null;
        }

        // Clear canvas
        if (this.canvasCtx) {
            this.canvasCtx.clearRect(0, 0, this.canvasOutput.width, this.canvasOutput.height);
        }

        // Hide cursor
        if (this.cursor) {
            this.cursor.style.display = 'none';
            if (this.isModal && this.cursor.parentNode) {
                this.cursor.parentNode.removeChild(this.cursor);
            }
        }

        // Remove hover highlight
        this.clearHoverHighlight();
        
        // Reset scroll state
        this.scrollVelocity = 0;
        this.isScrollMode = false;
        this.scrollBaseY = null;
        this.scrollStartPageY = null;

        this.updateUI();
        console.log('✅ Air Mouse stopped');
    }

    // ===========================
    // HAND DETECTION CALLBACK
    // ===========================
    onHandResults(results) {
        if (!this.isActive) return;

        this.updateFPS();

        // Clear canvas and draw mirrored video
        this.canvasCtx.save();
        this.canvasCtx.clearRect(0, 0, this.canvasOutput.width, this.canvasOutput.height);
        
        // Mirror the canvas for natural movement
        this.canvasCtx.translate(this.canvasOutput.width, 0);
        this.canvasCtx.scale(-1, 1);
        this.canvasCtx.drawImage(this.videoInput, 0, 0, this.canvasOutput.width, this.canvasOutput.height);
        this.canvasCtx.restore();

        if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
            const landmarks = results.multiHandLandmarks[0];
            this.handDetected = true;

            // Process hand for cursor movement + gestures
            this.processHandLandmarks(landmarks);

            // Draw hand skeleton (mirrored)
            this.drawHandSkeleton(landmarks);
            
            // Show cursor
            if (this.cursor) {
                this.cursor.style.display = 'block';
            }
        } else {
            this.handDetected = false;
            this.isPinching = false;
            this.isScrollMode = false;
            this.scrollBaseY = null;
            this.scrollStartPageY = null;
            
            // Hide cursor
            if (this.cursor) {
                this.cursor.style.display = 'none';
            }
            this.clearHoverHighlight();
        }

        this.updateStats();
    }

    // ===========================
    // HAND PROCESSING - FULL VIEWPORT MAPPING
    // ===========================
    processHandLandmarks(landmarks) {
        // Key landmarks
        const indexTip = landmarks[8];   // Index finger tip
        const thumbTip = landmarks[4];   // Thumb tip
        const middleTip = landmarks[12]; // Middle finger tip
        const ringTip = landmarks[16];   // Ring finger tip
        const pinkyTip = landmarks[20];  // Pinky tip
        const wrist = landmarks[0];      // Wrist

        // === MAP HAND POSITION TO FULL VIEWPORT ===
        // Mirror X axis (webcam is mirrored) and map to full screen
        // Use a "virtual zone" in the camera view (center 70%) to map to full viewport
        // This gives more comfortable range of motion
        
        const margin = 0.15; // 15% margin on each side
        const rangeX = 1.0 - (margin * 2);
        const rangeY = 1.0 - (margin * 2);
        
        // Normalize to 0-1 within the active zone, then clamp
        let normalizedX = (indexTip.x - margin) / rangeX;
        let normalizedY = (indexTip.y - margin) / rangeY;
        
        // Clamp to 0-1
        normalizedX = Math.max(0, Math.min(1, normalizedX));
        normalizedY = Math.max(0, Math.min(1, normalizedY));
        
        // Mirror X (moving hand right = cursor moves right on screen)
        normalizedX = 1.0 - normalizedX;
        
        // Map to full viewport
        this.targetX = normalizedX * window.innerWidth;
        this.targetY = normalizedY * window.innerHeight;

        // Smooth cursor movement (exponential smoothing)
        this.cursorX += (this.targetX - this.cursorX) * this.smoothingFactor;
        this.cursorY += (this.targetY - this.cursorY) * this.smoothingFactor;

        // Update cursor position on screen
        this.updateCursorPosition(this.cursorX, this.cursorY);

        // === GESTURE DETECTION ===
        
        // Calculate pinch distance (thumb to index)
        const pinchDistance = this.calculateDistance(thumbTip, indexTip);
        this.currentDistance = pinchDistance;

        // Detect how many fingers are extended
        const fingersUp = this.countFingersUp(landmarks);

        // === SCROLL GESTURE: Open palm (3+ fingers up) moving up/down ===
        // TikTok/Reels style: smooth position-based scrolling
        if (fingersUp >= 3) {
            if (!this.isScrollMode) {
                this.isScrollMode = true;
                this.scrollBaseY = indexTip.y;
                this.scrollStartPageY = window.scrollY;
                this.scrollVelocity = 0;
                console.log('📜 Scroll mode activated -', fingersUp, 'fingers up');
            } else {
                // Calculate how far hand moved from starting position
                const handDeltaY = indexTip.y - this.scrollBaseY;
                
                // Large deadzone (0.08) to prevent jitter - hand must move significantly
                if (Math.abs(handDeltaY) > this.scrollDeadzone) {
                    // Map hand movement to scroll position (inverted: hand down = scroll down)
                    // Multiplier: hand moving 50% of frame height scrolls ~1.5 screens
                    const scrollMultiplier = window.innerHeight * 3;
                    const targetScrollY = this.scrollStartPageY - (handDeltaY * scrollMultiplier);
                    
                    // Smoothly interpolate to target (TikTok style smoothness)
                    const currentY = window.scrollY;
                    const newY = currentY + (targetScrollY - currentY) * this.scrollSmoothFactor;
                    
                    window.scrollTo(0, newY);
                }
            }
            // Update cursor to show scroll mode
            if (this.cursor) {
                this.cursor.classList.add('scroll-mode');
            }
            return; // Don't process clicks during scroll
        } else {
            this.isScrollMode = false;
            this.scrollBaseY = null;
            if (this.cursor) {
                this.cursor.classList.remove('scroll-mode');
            }
        }

        // === CLICK GESTURE: Pinch (thumb + index close together) ===
        if (pinchDistance < this.pinchThreshold && !this.isPinching) {
            this.isPinching = true;
            this.pinchStartTime = Date.now();
            this.performClick();
        } else if (pinchDistance > this.pinchReleaseThreshold) {
            this.isPinching = false;
        }

        // === HOVER HIGHLIGHT ===
        this.updateHoverHighlight();
    }

    /**
     * Count how many fingers are extended (up)
     */
    countFingersUp(landmarks) {
        let count = 0;
        
        // Thumb: compare tip (4) x with IP joint (3) x
        // For right hand, thumb is up if tip.x < ip.x (mirrored)
        const thumbUp = landmarks[4].y < landmarks[3].y;
        if (thumbUp) count++;
        
        // Index: tip (8) above PIP (6)
        if (landmarks[8].y < landmarks[6].y) count++;
        
        // Middle: tip (12) above PIP (10)
        if (landmarks[12].y < landmarks[10].y) count++;
        
        // Ring: tip (16) above PIP (14)
        if (landmarks[16].y < landmarks[14].y) count++;
        
        // Pinky: tip (20) above PIP (18)
        if (landmarks[20].y < landmarks[18].y) count++;
        
        return count;
    }

    calculateDistance(point1, point2) {
        const dx = point1.x - point2.x;
        const dy = point1.y - point2.y;
        const dz = (point1.z || 0) - (point2.z || 0);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // ===========================
    // CURSOR CONTROL
    // ===========================
    updateCursorPosition(x, y) {
        if (!this.cursor) {
            // Create cursor if it doesn't exist
            this.cursor = document.createElement('div');
            this.cursor.className = this.isModal ? 'air-mouse-cursor-modal' : 'air-mouse-cursor';
            this.cursor.innerHTML = '<div class="cursor-ring"><div class="cursor-dot"></div></div>';
            document.body.appendChild(this.cursor);
        }
        
        // Position cursor centered on the point
        this.cursor.style.left = (x - 20) + 'px';
        this.cursor.style.top = (y - 20) + 'px';
        this.cursor.style.display = 'block';
    }

    // ===========================
    // HOVER HIGHLIGHT
    // ===========================
    updateHoverHighlight() {
        const element = document.elementFromPoint(this.cursorX, this.cursorY);
        
        if (element && element !== this.lastHoveredElement) {
            // Remove old highlight
            this.clearHoverHighlight();
            
            // Check if element is interactive
            const isInteractive = this.isInteractiveElement(element);
            
            if (isInteractive) {
                element.classList.add('air-mouse-hover');
                this.lastHoveredElement = element;
                
                // Change cursor to indicate clickable
                if (this.cursor) {
                    this.cursor.classList.add('clickable');
                }
            } else {
                if (this.cursor) {
                    this.cursor.classList.remove('clickable');
                }
            }
        } else if (!element) {
            this.clearHoverHighlight();
        }
    }

    clearHoverHighlight() {
        if (this.lastHoveredElement) {
            this.lastHoveredElement.classList.remove('air-mouse-hover');
            this.lastHoveredElement = null;
        }
        if (this.cursor) {
            this.cursor.classList.remove('clickable');
        }
    }

    isInteractiveElement(element) {
        if (!element) return false;
        
        const tag = element.tagName.toUpperCase();
        
        // Direct interactive elements
        if (['A', 'BUTTON', 'INPUT', 'TEXTAREA', 'SELECT', 'LABEL'].includes(tag)) {
            return true;
        }
        
        // Elements with click handlers or roles
        if (element.onclick || element.getAttribute('role') === 'button' || 
            element.getAttribute('tabindex') !== null ||
            element.classList.contains('btn') ||
            element.classList.contains('nav-link') ||
            element.classList.contains('dropdown-item') ||
            element.classList.contains('card') ||
            element.dataset.toggle ||
            element.dataset.bsToggle) {
            return true;
        }
        
        // Check parent (for spans inside buttons, etc.)
        const parent = element.closest('a, button, [onclick], [role="button"], .btn, .nav-link, .card');
        if (parent) return true;
        
        return false;
    }

    // ===========================
    // CLICK
    // ===========================
    performClick() {
        const now = Date.now();

        // Debounce
        if (now - this.lastClickTime < this.clickDebounce) {
            return;
        }
        this.lastClickTime = now;

        // Visual feedback - ripple
        this.createRipple(this.cursorX, this.cursorY);

        // Pulse cursor
        if (this.cursor) {
            this.cursor.classList.add('clicking');
            setTimeout(() => this.cursor.classList.remove('clicking'), 300);
        }

        // Find element at cursor position
        // Temporarily hide cursor so elementFromPoint doesn't hit it
        if (this.cursor) this.cursor.style.pointerEvents = 'none';
        const element = document.elementFromPoint(this.cursorX, this.cursorY);
        if (this.cursor) this.cursor.style.pointerEvents = '';

        if (!element) return;

        console.log('🖱️ Click at', Math.round(this.cursorX), Math.round(this.cursorY), '→', element.tagName, element.className);

        // Find the best clickable target (walk up the DOM)
        const clickTarget = element.closest('a, button, [onclick], [role="button"], .btn, input[type="submit"], input[type="checkbox"], input[type="radio"], label, .nav-link, .dropdown-item') || element;

        // Dispatch proper mouse events for maximum compatibility
        const eventOptions = {
            bubbles: true,
            cancelable: true,
            clientX: this.cursorX,
            clientY: this.cursorY,
            view: window
        };

        clickTarget.dispatchEvent(new MouseEvent('mouseenter', eventOptions));
        clickTarget.dispatchEvent(new MouseEvent('mouseover', eventOptions));
        clickTarget.dispatchEvent(new MouseEvent('mousedown', eventOptions));
        clickTarget.dispatchEvent(new MouseEvent('mouseup', eventOptions));
        clickTarget.dispatchEvent(new MouseEvent('click', eventOptions));

        // Special handling for form elements
        const tag = clickTarget.tagName.toUpperCase();
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') {
            clickTarget.focus();
        }

        // Handle links explicitly
        if (tag === 'A' && clickTarget.href) {
            // The click event should handle navigation, but as fallback:
            console.log('🔗 Link clicked:', clickTarget.href);
        }
    }

    // ===========================
    // VISUAL EFFECTS
    // ===========================
    createRipple(x, y) {
        const ripple = document.createElement('div');
        ripple.className = 'click-ripple';
        ripple.style.left = (x - 25) + 'px';
        ripple.style.top = (y - 25) + 'px';
        document.body.appendChild(ripple);
        setTimeout(() => ripple.remove(), 600);
    }

    drawHandSkeleton(landmarks) {
        const connections = [
            [0, 1], [1, 2], [2, 3], [3, 4],
            [5, 6], [6, 7], [7, 8],
            [9, 10], [10, 11], [11, 12],
            [13, 14], [14, 15], [15, 16],
            [17, 18], [18, 19], [19, 20],
            [0, 5], [5, 9], [9, 13], [13, 17], [17, 0]
        ];

        const w = this.canvasOutput.width;
        const h = this.canvasOutput.height;

        // Draw connections (mirrored)
        this.canvasCtx.strokeStyle = 'rgba(34, 197, 94, 0.8)';
        this.canvasCtx.lineWidth = 3;
        this.canvasCtx.lineCap = 'round';
        this.canvasCtx.lineJoin = 'round';

        connections.forEach(([start, end]) => {
            const p1 = landmarks[start];
            const p2 = landmarks[end];

            this.canvasCtx.beginPath();
            // Mirror X coordinates for display
            this.canvasCtx.moveTo((1 - p1.x) * w, p1.y * h);
            this.canvasCtx.lineTo((1 - p2.x) * w, p2.y * h);
            this.canvasCtx.stroke();
        });

        // Draw landmarks
        landmarks.forEach((landmark, index) => {
            const x = (1 - landmark.x) * w; // Mirror X
            const y = landmark.y * h;

            if (index === 4 || index === 8) {
                // Thumb and index tips - larger, red
                this.canvasCtx.fillStyle = '#ff6b6b';
                this.canvasCtx.beginPath();
                this.canvasCtx.arc(x, y, 8, 0, 2 * Math.PI);
                this.canvasCtx.fill();
                
                // Glow effect
                this.canvasCtx.strokeStyle = 'rgba(255, 107, 107, 0.5)';
                this.canvasCtx.lineWidth = 2;
                this.canvasCtx.beginPath();
                this.canvasCtx.arc(x, y, 12, 0, 2 * Math.PI);
                this.canvasCtx.stroke();
            } else {
                this.canvasCtx.fillStyle = 'rgba(34, 197, 94, 0.9)';
                this.canvasCtx.beginPath();
                this.canvasCtx.arc(x, y, 4, 0, 2 * Math.PI);
                this.canvasCtx.fill();
            }
        });

        // Draw pinch distance indicator
        const thumbX = (1 - landmarks[4].x) * w;
        const thumbY = landmarks[4].y * h;
        const indexX = (1 - landmarks[8].x) * w;
        const indexY = landmarks[8].y * h;
        
        const dist = this.currentDistance || 0;
        const lineColor = dist < this.pinchThreshold ? 'rgba(255, 50, 50, 0.9)' : 'rgba(255, 255, 255, 0.4)';
        
        this.canvasCtx.strokeStyle = lineColor;
        this.canvasCtx.lineWidth = 2;
        this.canvasCtx.setLineDash([5, 5]);
        this.canvasCtx.beginPath();
        this.canvasCtx.moveTo(thumbX, thumbY);
        this.canvasCtx.lineTo(indexX, indexY);
        this.canvasCtx.stroke();
        this.canvasCtx.setLineDash([]);
    }

    // ===========================
    // UI UPDATES
    // ===========================
    updateUI() {
        if (this.toggleButton) {
            this.toggleButton.classList.toggle('active', this.isActive);
        }
        if (this.switchLabel) {
            this.switchLabel.textContent = this.isActive ? 'ON' : 'OFF';
        }

        // Update status indicator
        const indicator = document.querySelector('.status-indicator');
        if (indicator) {
            indicator.classList.toggle('active', this.isActive);
        }

        if (!this.isActive && this.statsElements) {
            if (this.statsElements.status) {
                this.statsElements.status.textContent = 'Inactif';
                this.statsElements.status.style.color = '#999';
            }
            if (this.statsElements.handDetected) {
                this.statsElements.handDetected.textContent = 'Non';
            }
            if (this.statsElements.posX) this.statsElements.posX.textContent = '--';
            if (this.statsElements.posY) this.statsElements.posY.textContent = '--';
            if (this.statsElements.distance) this.statsElements.distance.textContent = '--';
        }
    }

    updateStats() {
        if (!this.statsElements) return;

        if (this.statsElements.handDetected) {
            this.statsElements.handDetected.textContent = this.handDetected ? 'Oui ✋' : 'Non';
            this.statsElements.handDetected.style.color = this.handDetected ? '#22c55e' : '#ff4444';
        }
        if (this.statsElements.fps) {
            this.statsElements.fps.textContent = this.fps.toFixed(0);
        }
        if (this.statsElements.posX) {
            this.statsElements.posX.textContent = Math.round(this.cursorX) + 'px';
        }
        if (this.statsElements.posY) {
            this.statsElements.posY.textContent = Math.round(this.cursorY) + 'px';
        }
        if (this.statsElements.distance) {
            const dist = this.currentDistance || 0;
            const pct = (dist * 100).toFixed(1);
            this.statsElements.distance.textContent = pct + '%';
            this.statsElements.distance.style.color = dist < this.pinchThreshold ? '#ff4444' : '#22c55e';
        }
        if (this.statsElements.status && this.isActive) {
            if (this.isScrollMode) {
                this.statsElements.status.textContent = 'Scroll 📜';
                this.statsElements.status.style.color = '#fbbf24';
            } else if (this.handDetected) {
                this.statsElements.status.textContent = 'Actif ✅';
                this.statsElements.status.style.color = '#22c55e';
            } else {
                this.statsElements.status.textContent = 'En attente... 👋';
                this.statsElements.status.style.color = '#fbbf24';
            }
        }
    }

    updateFPS() {
        const now = Date.now();
        this.frameCount++;

        if (now - this.lastFrameTime >= 1000) {
            this.fps = this.frameCount;
            this.frameCount = 0;
            this.lastFrameTime = now;
        }
    }
}

// ===========================
// GLOBAL STYLES FOR AIR MOUSE
// ===========================
(function injectAirMouseStyles() {
    const style = document.createElement('style');
    style.textContent = `
        /* Hover highlight for interactive elements */
        .air-mouse-hover {
            outline: 3px solid rgba(34, 197, 94, 0.7) !important;
            outline-offset: 3px !important;
            box-shadow: 0 0 15px rgba(34, 197, 94, 0.3) !important;
            transition: outline 0.15s ease, box-shadow 0.15s ease !important;
        }

        /* Base cursor styles */
        .air-mouse-cursor,
        .air-mouse-cursor-modal {
            position: fixed;
            width: 40px;
            height: 40px;
            pointer-events: none;
            z-index: 100000;
            display: none;
            transition: transform 0.1s ease;
        }

        .cursor-ring {
            width: 100%;
            height: 100%;
            border: 3px solid #22c55e;
            border-radius: 50%;
            box-shadow: 0 0 20px rgba(34, 197, 94, 0.8),
                        inset 0 0 10px rgba(34, 197, 94, 0.4);
            animation: cursorGlow 1.5s infinite;
            position: relative;
        }

        .cursor-dot {
            position: absolute;
            width: 10px;
            height: 10px;
            background: #22c55e;
            border-radius: 50%;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            box-shadow: 0 0 10px rgba(34, 197, 94, 0.9);
        }

        @keyframes cursorGlow {
            0%, 100% {
                box-shadow: 0 0 20px rgba(34, 197, 94, 0.8),
                            inset 0 0 10px rgba(34, 197, 94, 0.4);
            }
            50% {
                box-shadow: 0 0 30px rgba(34, 197, 94, 1),
                            inset 0 0 15px rgba(34, 197, 94, 0.6);
            }
        }

        /* Cursor states */
        .air-mouse-cursor.clickable .cursor-ring,
        .air-mouse-cursor-modal.clickable .cursor-ring {
            border-color: #fbbf24 !important;
            box-shadow: 0 0 20px rgba(251, 191, 36, 0.8),
                        inset 0 0 10px rgba(251, 191, 36, 0.4) !important;
        }

        .air-mouse-cursor.clickable .cursor-dot,
        .air-mouse-cursor-modal.clickable .cursor-dot {
            background: #fbbf24 !important;
        }

        .air-mouse-cursor.clicking .cursor-ring,
        .air-mouse-cursor-modal.clicking .cursor-ring {
            transform: scale(1.5);
            border-color: #ff6b6b !important;
            box-shadow: 0 0 30px rgba(255, 107, 107, 1) !important;
            transition: transform 0.15s ease, border-color 0.1s ease;
        }

        .air-mouse-cursor.scroll-mode .cursor-ring,
        .air-mouse-cursor-modal.scroll-mode .cursor-ring {
            border-color: #60a5fa !important;
            box-shadow: 0 0 20px rgba(96, 165, 250, 0.8) !important;
        }

        .air-mouse-cursor.scroll-mode .cursor-dot,
        .air-mouse-cursor-modal.scroll-mode .cursor-dot {
            background: #60a5fa !important;
        }

        /* Click ripple */
        .click-ripple {
            position: fixed;
            width: 50px;
            height: 50px;
            border-radius: 50%;
            background: radial-gradient(circle, rgba(34, 197, 94, 0.6) 0%, rgba(34, 197, 94, 0) 70%);
            pointer-events: none;
            z-index: 99999;
            animation: airMouseRipple 0.6s ease-out forwards;
        }

        @keyframes airMouseRipple {
            0% {
                transform: scale(0.5);
                opacity: 1;
            }
            100% {
                transform: scale(3);
                opacity: 0;
            }
        }
    `;
    document.head.appendChild(style);
})();

// ===========================
// INITIALIZATION
// ===========================
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Initializing Air Mouse...');

    const checkLibraries = setInterval(() => {
        if (typeof Hands !== 'undefined' && typeof Camera !== 'undefined') {
            clearInterval(checkLibraries);
            
            const isModalMode = !!document.getElementById('toggleButtonModal');

            if (isModalMode) {
                const modal = document.getElementById('airMouseModal');
                const openBtn = document.getElementById('airMouseBtn');
                const closeBtn = document.getElementById('airMouseClose');

                window.airMouse = new AirMouse(true);

                if (openBtn) {
                    openBtn.addEventListener('click', () => {
                        modal.classList.add('active');
                    });
                }

                if (closeBtn) {
                    closeBtn.addEventListener('click', () => {
                        window.airMouse.stop(true);
                        modal.classList.remove('active');
                    });
                }

                modal.addEventListener('click', (e) => {
                    if (e.target === modal) {
                        window.airMouse.stop(true);
                        modal.classList.remove('active');
                    }
                });
            } else {
                window.airMouse = new AirMouse(false);
            }
            
            console.log('✅ Air Mouse ready - Gestures:');
            console.log('   👆 Point with index = move cursor');
            console.log('   🤏 Pinch thumb+index = click');
            console.log('   🖐️ Open palm + move up/down = scroll');
        }
    }, 100);

    setTimeout(() => {
        if (typeof window.airMouse === 'undefined') {
            console.error('❌ MediaPipe libraries failed to load after 10 seconds');
        }
    }, 10000);
});

window.addEventListener('beforeunload', () => {
    if (window.airMouse) {
        window.airMouse.stop(true);
    }
});
