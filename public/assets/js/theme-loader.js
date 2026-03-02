(function() {
    const savedTheme = localStorage.getItem('theme');
    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    // Choose theme: default to LIGHT if nothing saved, otherwise use saved preference
    // Only use system preference if no saved theme
    let theme = 'light'; // Default to light mode
    if (savedTheme) {
        theme = savedTheme;
    } else if (systemPrefersDark) {
        theme = 'dark';
    }
    
    // Apply class immediately to html and body
    document.documentElement.classList.add(theme + '-mode');
    
    // Save preference if first visit (now defaults to light)
    if (!savedTheme) {
        localStorage.setItem('theme', theme);
    }
    
    // Re-apply to body once it's available
    document.addEventListener('DOMContentLoaded', () => {
        // Add the theme class to body
        document.body.classList.add(theme + '-mode');
        
        // Sync the checkbox if we are on the profile page
        const themeToggle = document.getElementById('theme-toggle');
        if (themeToggle) {
            themeToggle.checked = (theme === 'dark');
        }
        
        // Update icons based on theme
        const sunIcon = document.querySelector('.theme-icon--sun');
        const moonIcon = document.querySelector('.theme-icon--moon');
        if (sunIcon && moonIcon) {
            sunIcon.style.display = theme === 'dark' ? 'none' : 'inline-block';
            moonIcon.style.display = theme === 'dark' ? 'inline-block' : 'none';
        }
    });
})();
