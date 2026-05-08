document.addEventListener('DOMContentLoaded', () => {
    // Mobile Menu Toggle
    const burger = document.querySelector('.burger-menu');
    const navMenu = document.querySelector('.nav-menu');
    const navLinks = document.querySelectorAll('.nav-menu .nav-link');
    const dropdownLinks = document.querySelectorAll('.nav-menu .dropdown > .nav-link');

    const closeMobileMenu = () => {
        if (!burger || !navMenu) {
            return;
        }

        navMenu.classList.remove('active');
        burger.classList.remove('active');
        burger.setAttribute('aria-expanded', 'false');
        document.body.classList.remove('mobile-nav-open');
    };

    if (burger && navMenu) {
        burger.addEventListener('click', () => {
            navMenu.classList.toggle('active');
            burger.classList.toggle('active');
            const isOpen = navMenu.classList.contains('active');
            burger.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
            document.body.classList.toggle('mobile-nav-open', isOpen);
        });
    }

    dropdownLinks.forEach((link) => {
        link.addEventListener('click', (e) => {
            if (window.innerWidth > 900) {
                return;
            }

            const dropdown = link.closest('.dropdown');
            if (!dropdown) {
                return;
            }

            e.preventDefault();
            dropdown.classList.toggle('active');
        });
    });

    navLinks.forEach((link) => {
        link.addEventListener('click', () => {
            if (window.innerWidth <= 900 && !link.closest('.dropdown')) {
                closeMobileMenu();
            }
        });
    });

    document.querySelectorAll('.nav-menu .dropdown-item').forEach((item) => {
        item.addEventListener('click', () => {
            if (window.innerWidth <= 900) {
                closeMobileMenu();
            }
        });
    });

    document.addEventListener('click', (e) => {
        if (!burger || !navMenu || window.innerWidth > 900) {
            return;
        }

        if (!navMenu.contains(e.target) && !burger.contains(e.target)) {
            closeMobileMenu();
        }
    });

    window.addEventListener('resize', () => {
        if (window.innerWidth > 900) {
            closeMobileMenu();
            document.querySelectorAll('.nav-menu .dropdown.active').forEach((dropdown) => {
                dropdown.classList.remove('active');
            });
        }
    });

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
