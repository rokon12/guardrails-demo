class ChatApp {
    constructor() {
        this.messageCount = 0;
        this.guardrailCount = 0;
        this.init();
    }

    init() {
        this.bindEvents();
        this.updateStats();
    }

    bindEvents() {
        const sendBtn = document.getElementById('sendBtn');
        const analyzeBtn = document.getElementById('analyzeBtn');
        const messageInput = document.getElementById('messageInput');

        sendBtn.addEventListener('click', () => this.sendMessage());
        analyzeBtn.addEventListener('click', () => this.analyzeQuery());
        
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        // Bind example buttons
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('example-btn')) {
                const example = e.target.getAttribute('data-example');
                if (example !== null) {
                    messageInput.value = example;
                    messageInput.focus();
                    
                    // Add visual feedback
                    e.target.style.transform = 'scale(0.95)';
                    setTimeout(() => {
                        e.target.style.transform = '';
                    }, 150);
                    
                    // Show hint about what will happen
                    if (e.target.classList.contains('danger')) {
                        this.showTemporaryHint('This example will trigger a guardrail block', 'warning');
                    } else {
                        this.showTemporaryHint('This example should pass all guardrails', 'success');
                    }
                }
            }
        });

        messageInput.addEventListener('input', () => {
            const remaining = 1000 - messageInput.value.length;
            const charCount = document.querySelector('.text-muted');
            if (remaining < 100) {
                charCount.style.color = remaining < 0 ? '#dc2626' : '#f59e0b';
                charCount.textContent = `${remaining} characters remaining`;
            } else {
                charCount.style.color = '#6b7280';
                charCount.textContent = 'Max 1000 characters';
            }
        });
    }

    async sendMessage() {
        const messageInput = document.getElementById('messageInput');
        const message = messageInput.value.trim();

        if (!message) {
            this.showError('Please enter a message');
            return;
        }

        if (message.length > 1000) {
            this.showError('Message is too long. Please keep it under 1000 characters.');
            return;
        }

        this.clearError();
        this.addUserMessage(message);
        messageInput.value = '';
        this.showTyping(true);
        this.messageCount++;
        this.guardrailCount++;
        this.updateStats();

        try {
            const response = await fetch('/api/support/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ message: message })
            });

            const data = await response.json();
            this.showTyping(false);

            if (data.success) {
                this.addBotMessage(data.response);
            } else {
                this.addBotMessage(data.error || 'Sorry, I encountered an error processing your request.', true);
                this.showError(data.error);
            }
        } catch (error) {
            this.showTyping(false);
            this.addBotMessage('Sorry, I\'m having trouble connecting right now. Please try again later.', true);
            this.showError('Connection error. Please check your network and try again.');
        }
    }

    async analyzeQuery() {
        const messageInput = document.getElementById('messageInput');
        const message = messageInput.value.trim();

        if (!message) {
            this.showError('Please enter a message to analyze');
            return;
        }

        this.clearError();
        this.guardrailCount++;
        this.updateStats();

        try {
            const response = await fetch('/api/support/analyze', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ message: message })
            });

            if (response.ok) {
                const data = await response.json();
                this.showAnalysis(data);
            } else {
                this.showError('Failed to analyze query. Please try again.');
            }
        } catch (error) {
            this.showError('Connection error during analysis. Please try again.');
        }
    }

    addUserMessage(message) {
        const chatContainer = document.getElementById('chatContainer');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message user';
        messageDiv.innerHTML = `
            <div class="message-content">
                ${this.escapeHtml(message)}
            </div>
            <div class="message-avatar">
                <i class="fas fa-user"></i>
            </div>
        `;
        chatContainer.appendChild(messageDiv);
        this.scrollToBottom();
    }

    addBotMessage(message, isError = false) {
        const chatContainer = document.getElementById('chatContainer');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot';
        
        const messageContent = isError ? 
            `<i class="fas fa-exclamation-triangle"></i> ${this.escapeHtml(message)}` :
            this.escapeHtml(message);

        messageDiv.innerHTML = `
            <div class="message-avatar">
                <i class="fas fa-robot"></i>
            </div>
            <div class="message-content ${isError ? 'error-message' : ''}">
                ${messageContent}
            </div>
        `;
        chatContainer.appendChild(messageDiv);
        this.scrollToBottom();
    }

    showTyping(show) {
        const typingIndicator = document.getElementById('typingIndicator');
        typingIndicator.style.display = show ? 'flex' : 'none';
        if (show) {
            this.scrollToBottom();
        }
    }

    showAnalysis(data) {
        const analysisPanel = document.getElementById('analysisPanel');
        const analysisContent = document.getElementById('analysisContent');
        
        let html = '<div class="mt-3">';
        
        // Always show the main analysis fields
        if (data.answer) {
            html += `<div class="mb-2"><strong>Summary:</strong> ${this.escapeHtml(data.answer)}</div>`;
        }
        
        if (data.category) {
            const categoryColor = this.getCategoryColor(data.category);
            html += `<div class="mb-2"><strong>Category:</strong> <span class="analysis-badge ${categoryColor}">${data.category}</span></div>`;
        }
        
        if (data.confidence !== undefined) {
            const confidencePercent = Math.round(data.confidence * 100);
            const confidenceClass = data.confidence >= 0.8 ? 'badge-low' : data.confidence >= 0.5 ? 'badge-medium' : 'badge-high';
            html += `<div class="mb-2"><strong>Confidence:</strong> <span class="analysis-badge ${confidenceClass}">${confidencePercent}%</span></div>`;
        }
        
        // Optional fields
        if (data.intent) {
            html += `<div class="mb-2"><strong>Intent:</strong> ${this.escapeHtml(data.intent)}</div>`;
        }
        
        if (data.priority) {
            const priorityClass = data.priority.toLowerCase() === 'high' ? 'badge-high' : 
                                data.priority.toLowerCase() === 'medium' ? 'badge-medium' : 'badge-low';
            html += `<div class="mb-2"><strong>Priority:</strong> <span class="analysis-badge ${priorityClass}">${data.priority}</span></div>`;
        }
        
        if (data.sentiment) {
            const sentimentClass = data.sentiment.toLowerCase() === 'negative' ? 'badge-high' : 
                                  data.sentiment.toLowerCase() === 'neutral' ? 'badge-medium' : 'badge-low';
            html += `<div class="mb-2"><strong>Sentiment:</strong> <span class="analysis-badge ${sentimentClass}">${data.sentiment}</span></div>`;
        }
        
        if (data.suggestedResponse) {
            html += `<div class="mt-3"><strong>Suggested Response:</strong><br><small class="text-muted">${this.escapeHtml(data.suggestedResponse)}</small></div>`;
        }
        
        html += '</div>';
        
        analysisContent.innerHTML = html;
        analysisPanel.style.display = 'block';
    }

    getCategoryColor(category) {
        switch(category.toUpperCase()) {
            case 'ACCOUNT': return 'badge-medium';
            case 'BILLING': return 'badge-high';
            case 'TECHNICAL': return 'badge-high';
            case 'PRODUCT': return 'badge-low';
            case 'GENERAL': return 'badge-low';
            default: return 'badge-medium';
        }
    }

    showError(message) {
        const errorContainer = document.getElementById('errorContainer');
        errorContainer.innerHTML = `
            <div class="error-message mt-2">
                <i class="fas fa-exclamation-circle"></i> ${this.escapeHtml(message)}
            </div>
        `;
        setTimeout(() => this.clearError(), 5000);
    }

    clearError() {
        const errorContainer = document.getElementById('errorContainer');
        errorContainer.innerHTML = '';
    }

    updateStats() {
        document.getElementById('messageCount').textContent = this.messageCount;
        document.getElementById('guardrailCount').textContent = this.guardrailCount;
    }

    scrollToBottom() {
        const chatContainer = document.getElementById('chatContainer');
        setTimeout(() => {
            chatContainer.scrollTop = chatContainer.scrollHeight;
        }, 100);
    }

    showTemporaryHint(message, type = 'info') {
        const hintDiv = document.createElement('div');
        hintDiv.className = `alert alert-${type === 'warning' ? 'warning' : type === 'success' ? 'success' : 'info'} mt-2`;
        hintDiv.style.cssText = 'animation: fadeInUp 0.3s ease-out; font-size: 0.9rem;';
        hintDiv.innerHTML = `<i class="fas fa-${type === 'warning' ? 'exclamation-triangle' : type === 'success' ? 'check-circle' : 'info-circle'}"></i> ${message}`;
        
        const inputSection = document.querySelector('.input-section');
        inputSection.appendChild(hintDiv);
        
        setTimeout(() => {
            hintDiv.style.animation = 'fadeOutDown 0.3s ease-out';
            setTimeout(() => {
                if (hintDiv.parentNode) {
                    hintDiv.parentNode.removeChild(hintDiv);
                }
            }, 300);
        }, 3000);
    }

    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }
}

// Initialize the chat app when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
    
    // Initialize Bootstrap tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});

// Add some visual feedback for button clicks
document.addEventListener('DOMContentLoaded', () => {
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(button => {
        button.addEventListener('click', function() {
            this.style.transform = 'scale(0.95)';
            setTimeout(() => {
                this.style.transform = '';
            }, 150);
        });
    });
});

// Add smooth scrolling animation
document.addEventListener('DOMContentLoaded', () => {
    const chatContainer = document.getElementById('chatContainer');
    chatContainer.style.scrollBehavior = 'smooth';
});

// Add particle effect to header (optional enhancement)
document.addEventListener('DOMContentLoaded', () => {
    const header = document.querySelector('.header');
    
    // Create floating particles
    for (let i = 0; i < 20; i++) {
        const particle = document.createElement('div');
        particle.style.cssText = `
            position: absolute;
            width: 4px;
            height: 4px;
            background: rgba(255, 255, 255, 0.3);
            border-radius: 50%;
            pointer-events: none;
            animation: float 6s ease-in-out infinite;
            animation-delay: ${Math.random() * 6}s;
            left: ${Math.random() * 100}%;
            top: ${Math.random() * 100}%;
        `;
        header.style.position = 'relative';
        header.appendChild(particle);
    }
    
    // Add CSS animation for particles
    const style = document.createElement('style');
    style.textContent = `
        @keyframes float {
            0%, 100% { transform: translateY(0px) rotate(0deg); opacity: 0.3; }
            50% { transform: translateY(-20px) rotate(180deg); opacity: 1; }
        }
    `;
    document.head.appendChild(style);
});