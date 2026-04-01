$(document).ready(function() {
    // Toggle content overview panel
    $('#toggleOverview').click(function() {
        $('.content-overview').toggleClass('collapsed');
        $(this).find('i').toggleClass('fa-chevron-left fa-chevron-right');
    });

    // Handle module header click
    $('.module-header').click(function() {
        const moduleItem = $(this).parent('.module-item');
        const subModules = moduleItem.find('.sub-modules').first();
        
        $(this).toggleClass('expanded');
        $(this).find('i').toggleClass('fa-chevron-right fa-chevron-down');
        subModules.slideToggle();
    });

    // Handle sub-module header click
    $('.sub-module-header').click(function() {
        const subModuleItem = $(this).parent('.sub-module-item');
        const activities = subModuleItem.find('.activities').first();
        
        $(this).toggleClass('expanded');
        $(this).find('i').toggleClass('fa-chevron-right fa-chevron-down');
        activities.slideToggle();
    });

    // Handle activity header click
    $('.activity-header').click(function(e) {
        e.stopPropagation(); // Prevent triggering parent clicks
        const activityItem = $(this).parent('.activity-item');
        const contentItems = activityItem.find('.content-items');
        const activityId = activityItem.data('activity-id');
        const courseId = activityItem.data('course-id');
        
        console.log('Clicked activity:', activityId);
        
        $(this).toggleClass('expanded');
        $(this).find('i').toggleClass('fa-chevron-right fa-chevron-down');
        
        // Only load content items if they haven't been loaded yet
        if (contentItems.is(':empty')) {
            // Show loading indicator
            contentItems.html('<div class="loading">Loading content items...</div>');
            contentItems.show();

            // Fetch content from the JSON file
            fetch('/data/data/all_user_learning_paths.json')
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to load content');
                    }
                    return response.json();
                })
                .then(data => {
                    let contentItemsHtml = '';
                    
                    // Find the specific course and activity
                    const courseData = data.users['1'].courses[courseId].learning_path.learning_path;
                    const modules = courseData.modules;
                    
                    // Find the activity in the modules
                    let activityFound = false;
                    modules.forEach(module => {
                        module.sub_modules.forEach(subModule => {
                            const activity = subModule.activities.find(a => a.id === activityId);
                            if (activity) {
                                activityFound = true;
                                
                                // Generate content items for the activity
                                contentItemsHtml = `
                                    <div class="content-item" data-content-id="${activity.id}">
                                        <i class="fas fa-${getIconForActivityType(activity.type)}"></i>
                                        <span>${activity.title}</span>
                                        ${activity.duration ? `<span class="duration">${activity.duration} mins</span>` : ''}
                                    </div>
                                `;

                                // Add learning objectives if available
                                if (activity.learning_objectives && activity.learning_objectives.length > 0) {
                                    contentItemsHtml += `
                                        <div class="learning-objectives">
                                            <h4>Learning Objectives:</h4>
                                            <ul>
                                                ${activity.learning_objectives.map(obj => `
                                                    <li>${obj.text}</li>
                                                `).join('')}
                                            </ul>
                                        </div>
                                    `;
                                }
                            }
                        });
                    });
                    
                    if (activityFound) {
                        contentItems.html(contentItemsHtml);
                    } else {
                        contentItems.html('<div class="no-content">No matching content available for this activity</div>');
                    }
                    
                    contentItems.show();

                    // Make content items clickable if they exist
                    if (contentItemsHtml.includes('content-item')) {
                        contentItems.find('.content-item').click(function(e) {
                            e.stopPropagation();
                            const contentId = $(this).data('content-id');
                            const contentTitle = $(this).find('span').first().text();
                            
                            // Update active state
                            $('.content-item').removeClass('active');
                            $(this).addClass('active');
                            
                            // Load content in the main area
                            loadContent(contentId);
                        });
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    contentItems.html(`
                        <div class="error-message">
                            <p>Failed to load content items. Please try again.</p>
                        </div>
                    `);
                });
        } else {
            contentItems.slideToggle();
        }
    });

    // Helper function to get icon based on activity type
    function getIconForActivityType(type) {
        switch(type) {
            case 'quiz': return 'clipboard-list';
            case 'video': return 'video';
            case 'exercise': return 'tasks';
            default: return 'file-alt';
        }
    }

    // Function to load content into the main area
    function loadContent(contentId) {
        const contentContainer = $('#content-container');
        contentContainer.html('<div class="loading">Loading content...</div>');

        fetch('/data/data/all_user_learning_paths.json')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to load content');
                }
                return response.json();
            })
            .then(data => {
                // Find the specific course and activity
                const courseId = $('.activity-item.active').data('course-id');
                const courseData = data.users['1'].courses[courseId].learning_path.learning_path;
                const modules = courseData.modules;
                
                // Find the activity in the modules
                let activityFound = false;
                let activity;
                modules.forEach(module => {
                    module.sub_modules.forEach(subModule => {
                        activity = subModule.activities.find(a => a.id === contentId);
                        if (activity) {
                            activityFound = true;
                        }
                    });
                });
                
                if (activityFound) {
                    let contentHtml = `
                        <div class="content-section">
                            <h1>${activity.title}</h1>
                    `;

                    if (activity.duration) {
                        contentHtml += `
                            <div class="content-duration">
                                <i class="far fa-clock"></i> ${activity.duration} mins
                            </div>
                        `;
                    }

                    if (activity.content) {
                        contentHtml += `
                            <div class="content-text">
                                ${activity.content.split('\n').map(p => `<p>${p}</p>`).join('')}
                            </div>
                        `;
                    }

                    contentHtml += '</div>';
                    contentContainer.html(contentHtml);
                } else {
                    contentContainer.html('<div class="error-message">Content not found</div>');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                contentContainer.html(`
                    <div class="error-message">
                        <p>Failed to load content. Please try again.</p>
                    </div>
                `);
            });
    }
});
