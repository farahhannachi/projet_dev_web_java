document.addEventListener('DOMContentLoaded', () => {
    // Mobile Menu Toggle
    const burger = document.querySelector('.burger-menu');
    const navMenu = document.querySelector('.nav-menu');

    if (burger) {
        burger.addEventListener('click', () => {
            navMenu.classList.toggle('active');
        });
    }

    // Dropdown for Mobile (Tap to open)
    const productDropdown = document.querySelector('.dropdown');
    if (productDropdown) {
        productDropdown.addEventListener('click', (e) => {
            if (window.innerWidth <= 768) {
                // e.preventDefault();
                productDropdown.classList.toggle('active');
            }
        });
    }

    // Scroll to "Ordonnance" manually if needed (browsers handle ID hash, but just in case)
    const ordonnanceLink = document.querySelector('a[href="#ordonnance"]');
    if (ordonnanceLink) {
        ordonnanceLink.addEventListener('click', (e) => {
            if (window.location.pathname.endsWith('index.html') || window.location.pathname === '/') {
                // e.preventDefault();
                // document.getElementById('ordonnance').scrollIntoView({ behavior: 'smooth' });
            }
        });
    }

    // Accordion Logic
    const accordions = document.querySelectorAll('.accordion-header');
    accordions.forEach(acc => {
        acc.addEventListener('click', () => {
            const content = acc.nextElementSibling;
            content.classList.toggle('active');
        });
    });

    // ...existing code...
});
