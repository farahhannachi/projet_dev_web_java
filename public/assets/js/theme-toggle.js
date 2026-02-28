document.addEventListener('DOMContentLoaded', () => {
    const themeToggle = document.getElementById('theme-toggle');
    const sunIcon = document.querySelector('.theme-icon--sun');
    const moonIcon = document.querySelector('.theme-icon--moon');
    
    // Function to update icons based on theme
    function updateThemeIcons(isDark) {
        if (sunIcon && moonIcon) {
            sunIcon.style.display = isDark ? 'none' : 'inline-block';
            moonIcon.style.display = isDark ? 'inline-block' : 'none';
        }
    }
    
    // Initialize icons based on current theme
    const currentTheme = localStorage.getItem('theme') || 
        (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    updateThemeIcons(currentTheme === 'dark');
    
    if (themeToggle) {
        // Set initial state
        themeToggle.checked = currentTheme === 'dark';
        
        themeToggle.addEventListener('change', function () {
            if (this.checked) {
                // Switch to dark mode
                document.documentElement.classList.remove('light-mode');
                document.documentElement.classList.add('dark-mode');
                document.body.classList.remove('light-mode');
                document.body.classList.add('dark-mode');
                localStorage.setItem('theme', 'dark');
                updateThemeIcons(true);
            } else {
                // Switch to light mode
                document.documentElement.classList.remove('dark-mode');
                document.documentElement.classList.add('light-mode');
                document.body.classList.remove('dark-mode');
                document.body.classList.add('light-mode');
                localStorage.setItem('theme', 'light');
                updateThemeIcons(false);
            }
        });
    }
});
