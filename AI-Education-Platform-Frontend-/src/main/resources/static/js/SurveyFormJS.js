document.addEventListener('DOMContentLoaded', function() {
    let currentStep = 0;
    const surveySteps = document.querySelectorAll('.survey-step');
    const progressSteps = document.querySelectorAll('.progress-step');
    const prevButton = document.querySelector('.prev-button');
    const nextButton = document.querySelector('.next-button');
    const submitButton = document.querySelector('.submit-button');
    const surveyForm = document.querySelector('.survey-form');

    function updateProgressIndicator() {
        progressSteps.forEach((step, index) => {
            step.classList.toggle('active', index <= currentStep);
        });
    }

    function showCurrentStep() {
        surveySteps.forEach((step, index) => {
            step.classList.toggle('active', index === currentStep);
        });

        // Update navigation buttons
        if (currentStep === 0) {
            prevButton.style.display = 'none';
        } else {
            prevButton.style.display = 'inline-block';
        }

        // Show/hide submit button on last step
        if (currentStep === surveySteps.length - 1) {
            nextButton.style.display = 'none';
            submitButton.style.display = 'inline-block';
        } else {
            nextButton.style.display = 'inline-block';
            submitButton.style.display = 'none';
        }
    }

    function validateCurrentStep() {
        const currentStepElement = surveySteps[currentStep];
        const radioButtons = currentStepElement.querySelectorAll('input[type="radio"]');
        const checked = Array.from(radioButtons).some(radio => radio.checked);
        return checked;
    }

    function goToNextStep() {
        if (!validateCurrentStep()) {
            alert('Please select an option before proceeding.');
            return;
        }
        
        if (currentStep < surveySteps.length - 1) {
            currentStep++;
            updateProgressIndicator();
            showCurrentStep();
        }
    }

    function goToPreviousStep() {
        if (currentStep > 0) {
            currentStep--;
            updateProgressIndicator();
            showCurrentStep();
        }
    }

    // Add event listeners to radio buttons for automatic next step
    surveySteps.forEach(step => {
        const radioButtons = step.querySelectorAll('input[type="radio"]');
        radioButtons.forEach(radio => {
            radio.addEventListener('change', function() {
                if (this.checked) {
                    // Wait a brief moment before moving to next step
                    setTimeout(goToNextStep, 300);
                }
            });
        });
    });

    // Navigation button event listeners
    prevButton.addEventListener('click', goToPreviousStep);
    nextButton.addEventListener('click', goToNextStep);

    // Form submission
    surveyForm.addEventListener('submit', function(e) {
        if (!validateCurrentStep()) {
            e.preventDefault();
            alert('Please complete all questions before submitting.');
        }
    });

    // Initial setup
    updateProgressIndicator();
    showCurrentStep();
});