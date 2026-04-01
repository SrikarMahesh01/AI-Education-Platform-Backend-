// Sidebar toggle functionality
document.getElementById('sidebar-toggle').addEventListener('click', function() {
    toggleSidebar();
});

// Add click handler for sidebar content
document.getElementById('sidebarContent').addEventListener('click', function(e) {
    // Check if click was on empty space (the sidebar-content itself)
    if (e.target === this) {
        toggleSidebar();
    }
});

// Function to toggle sidebar
function toggleSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const body = document.body;
    
    sidebar.classList.toggle('active');
    body.classList.toggle('sidebar-closed');
    
    // Store sidebar state in localStorage
    localStorage.setItem('sidebarOpen', sidebar.classList.contains('active'));
}

// Check localStorage on page load to restore sidebar state
window.addEventListener('load', function() {
    const sidebarOpen = localStorage.getItem('sidebarOpen') === 'true';
    if (sidebarOpen) {
        document.querySelector('.sidebar').classList.add('active');
    } else {
        document.body.classList.add('sidebar-closed');
    }
});

document.addEventListener('DOMContentLoaded', function() {
    const dashboardGrid = document.querySelector('.dashboard-grid');
    const profileSection = document.getElementById('profileSection');
    const menuItems = document.querySelectorAll('.sidebar-menu li');
    
    menuItems.forEach(item => {
        item.addEventListener('click', function(e) {
            const link = this.querySelector('a');
            if (link.innerHTML.includes('Profile')) {
                e.preventDefault();
                dashboardGrid.style.display = 'none';
                profileSection.style.display = 'block';
                menuItems.forEach(i => i.classList.remove('active'));
                this.classList.add('active');
            } else if (link.innerHTML.includes('Dashboard')) {
                e.preventDefault();
                dashboardGrid.style.display = 'grid';
                profileSection.style.display = 'none';
                menuItems.forEach(i => i.classList.remove('active'));
                this.classList.add('active');
            }
        });
    });

    // Profile dropdown functionality
    const profileDropdownItems = document.querySelectorAll('.dropdown-item');
    profileDropdownItems.forEach(item => {
        item.addEventListener('click', function(e) {
            const linkText = this.textContent.trim();
            const dropdownMenu = document.getElementById('profileDropdownMenu');
            
            // Close dropdown
            dropdownMenu.classList.remove('active');
            
            // Profile button action (same as sidebar profile menu)
            if (linkText === 'Profile') {
                e.preventDefault();
                dashboardGrid.style.display = 'none';
                profileSection.style.display = 'block';
                
                menuItems.forEach(i => i.classList.remove('active'));
                // Find and activate the profile menu item
                const profileMenuItem = Array.from(menuItems).find(item => 
                    item.querySelector('a').innerHTML.includes('Profile')
                );
                if (profileMenuItem) {
                    profileMenuItem.classList.add('active');
                }
            }
        });
    });
});

// AI Chat functionality
const aiChatButton = document.getElementById('aiChatButton');
const chatContent = document.querySelector('.chat-content');
const closeChat = document.getElementById('closeChat');
const sendMessage = document.getElementById('sendMessage');
const chatInput = document.getElementById('chatInput');

aiChatButton.addEventListener('click', () => {
    chatContent.style.display = chatContent.style.display === 'none' ? 'flex' : 'none';
});

closeChat.addEventListener('click', () => {
    chatContent.style.display = 'none';
});

sendMessage.addEventListener('click', () => {
    const message = chatInput.value.trim();
    if (message) {
        // Here you'll add the logic to send the message to your AI service
        console.log('Message to send:', message);
        chatInput.value = '';
    }
});

chatInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        sendMessage.click();
    }
});

const editProfileBtn = document.getElementById('editProfileBtn');
const completeProfileForm = document.getElementById('completeProfileForm');
const cancelProfileEdit = document.getElementById('cancelProfileEdit');

document.addEventListener('DOMContentLoaded', function() {
    const editProfileBtn = document.getElementById('editProfileBtn');
    if (editProfileBtn) {
        editProfileBtn.addEventListener('click', function() {
            // Do nothing or add future edit profile functionality
            console.log('Edit Profile clicked');
        });
    }

    const profileLinks = document.querySelectorAll('a[onclick="toggleProfileSection()"]');
    profileLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            toggleProfileSection();
        });
    });
});

function toggleProfileSection() {
    const profileSection = document.getElementById('profileSection');
    const dashboardGrid = document.querySelector('.dashboard-grid');
    const menuItems = document.querySelectorAll('.sidebar-menu li');
    const dropdownMenu = document.getElementById('profileDropdownMenu');
    
    // Toggle profile section visibility
    dashboardGrid.style.display = 'none';
    profileSection.style.display = 'block';
    
    // Remove active class from all menu items
    menuItems.forEach(i => i.classList.remove('active'));
    
    // Find and activate the profile menu item
    const profileMenuItem = Array.from(menuItems).find(item => 
        item.querySelector('a').innerHTML.includes('Profile')
    );
    if (profileMenuItem) {
        profileMenuItem.classList.add('active');
    }
    
    // Close dropdown menu
    if (dropdownMenu) {
        dropdownMenu.classList.remove('active');
    }
    
    // Always hide the complete profile form
    const completeProfileForm = document.getElementById('completeProfileForm');
    if (completeProfileForm) {
        completeProfileForm.style.display = 'none';
    }
}

// Ensure toggleProfileSection is globally available
window.toggleProfileSection = toggleProfileSection;

editProfileBtn.addEventListener('click', function() {
    const completeProfileForm = document.getElementById('completeProfileForm');
    completeProfileForm.style.display = 'none';
});

// Safely handle edit profile button
const editProfileBtn = document.getElementById('editProfileBtn');
if (editProfileBtn) {
    editProfileBtn.addEventListener('click', function() {
        const completeProfileForm = document.getElementById('completeProfileForm');
        if (completeProfileForm) {
            completeProfileForm.style.display = 'none';
        }
    });
})

// Profile and Edit Profile dropdown actions
const profileDropdownItems = document.querySelectorAll('.dropdown-item');
profileDropdownItems.forEach(item => {
    item.addEventListener('click', function(e) {
        const linkText = this.textContent.trim();
        const dropdownMenu = document.getElementById('profileDropdownMenu');
        
        // Close dropdown
        dropdownMenu.classList.remove('active');
        
        // Profile button action (same as sidebar profile menu)
        if (linkText === 'Profile') {
            e.preventDefault();
            const dashboardGrid = document.querySelector('.dashboard-grid');
            const profileSection = document.getElementById('profileSection');
            const menuItems = document.querySelectorAll('.sidebar-menu li');
            
            dashboardGrid.style.display = 'none';
            profileSection.style.display = 'block';
            
            menuItems.forEach(i => i.classList.remove('active'));
            // Find and activate the profile menu item
            const profileMenuItem = Array.from(menuItems).find(item => 
                item.querySelector('a').innerHTML.includes('Profile')
            );
            if (profileMenuItem) {
                profileMenuItem.classList.add('active');
            }
        }
        
        // Edit Profile button action (redirect to edit profile page)
        if (linkText === 'Edit Profile') {
            window.location.href = this.getAttribute('href');
        }
    });
});

// Profile Dropdown functionality
document.addEventListener('DOMContentLoaded', function() {
    const profileTrigger = document.getElementById('profileDropdownTrigger');
    const dropdownMenu = document.getElementById('profileDropdownMenu');
    const dashboardGrid = document.querySelector('.dashboard-grid');
    const profileSection = document.getElementById('profileSection');
    const menuItems = document.querySelectorAll('.sidebar-menu li');

    // Toggle dropdown on profile picture click
    if (profileTrigger) {
        profileTrigger.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdownMenu.classList.toggle('active');
        });
    }

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
        if (dropdownMenu && !profileTrigger.contains(e.target)) {
            dropdownMenu.classList.remove('active');
        }
    });

    // Handle dropdown menu item clicks
    const dropdownItems = document.querySelectorAll('.dropdown-menu .dropdown-item');
    dropdownItems.forEach(item => {
        item.addEventListener('click', function(e) {
            const action = this.getAttribute('onclick');
            if (action && action.includes('toggleProfileSection')) {
                e.preventDefault();
                // Hide dashboard grid
                if (dashboardGrid) dashboardGrid.style.display = 'none';
                // Show profile section
                if (profileSection) profileSection.style.display = 'block';
                // Update sidebar menu active state
                menuItems.forEach(i => i.classList.remove('active'));
                const profileMenuItem = Array.from(menuItems).find(item => 
                    item.querySelector('a').innerHTML.includes('Profile')
                );
                if (profileMenuItem) {
                    profileMenuItem.classList.add('active');
                }
                // Close dropdown
                dropdownMenu.classList.remove('active');
            }
        });
    });
});

// Make toggleProfileSection function available globally
window.toggleProfileSection = function() {
    const dashboardGrid = document.querySelector('.dashboard-grid');
    const profileSection = document.getElementById('profileSection');
    const menuItems = document.querySelectorAll('.sidebar-menu li');

    if (dashboardGrid) dashboardGrid.style.display = 'none';
    if (profileSection) profileSection.style.display = 'block';

    menuItems.forEach(i => i.classList.remove('active'));
    const profileMenuItem = Array.from(menuItems).find(item => 
        item.querySelector('a').innerHTML.includes('Profile')
    );
    if (profileMenuItem) {
        profileMenuItem.classList.add('active');
    }
};
