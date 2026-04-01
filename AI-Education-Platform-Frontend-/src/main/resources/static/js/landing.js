// Smooth scrolling for navigation links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();

        document.querySelector(this.getAttribute('href')).scrollIntoView({
            behavior: 'smooth'
        });
    });
});

// Navbar scroll effect
window.addEventListener('scroll', function() {
    const navbar = document.querySelector('.navbar');
    if (window.scrollY > 50) {
        navbar.classList.add('scrolled');
    } else {
        navbar.classList.remove('scrolled');
    }
});

// Video background handling
document.addEventListener('DOMContentLoaded', function() {
    const heroVideo = document.getElementById('heroVideo');
    const heroVideoFallback = document.getElementById('heroVideoFallback');

    if (heroVideo) {
        heroVideo.addEventListener('ended', function() {
            // Hide video and show fallback image when video ends
            heroVideo.style.display = 'none';
            heroVideoFallback.style.display = 'block';
        });
    }
});

// Navbar color change on scroll
document.addEventListener('DOMContentLoaded', function() {
    const navbar = document.querySelector('.navbar');
    const heroSection = document.querySelector('.hero-section');
    const featuresSection = document.querySelector('.features-section');

    // Initial state - transparent
    navbar.classList.add('transparent');

    function updateNavbarColor() {
        if (!heroSection || !featuresSection) return;

        const heroSectionBottom = heroSection.getBoundingClientRect().bottom;
        const featuresSectionTop = featuresSection.getBoundingClientRect().top;

        if (heroSectionBottom <= 0) {
            // We've scrolled past the hero section
            navbar.classList.remove('transparent');
            navbar.classList.add('white');
        } else {
            // We're still in or above the hero section
            navbar.classList.remove('white');
            navbar.classList.add('transparent');
        }
    }

    // Add scroll event listener
    window.addEventListener('scroll', updateNavbarColor);
    
    // Initial check in case page loads with scroll
    updateNavbarColor();
});
