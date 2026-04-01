$(document).ready(function() {
    const quizData = {
        questions: [
            {
                question: "If you fold this unfolded cube, which configuration will it create?",
                questionImage: "/images/questionimage.jpg",
                answers: [
                    { image: "/images/answer1.jpg", correct: true },
                    { image: "/images/answer2.jpg", correct: false },
                    { image: "/images/answer3.jpg", correct: false },
                    { image: "/images/answer4.jpg", correct: false }
                ]
            },
            {
                question: "Problem-Solving Approach: When faced with a complex problem, I most prefer to:",
                answers: [
                    { text: "Break it down into smaller, manageable steps", correct: true },
                    { text: "Find a similar solution I've used before", correct: false },
                    { text: "Seek advice from experts or colleagues", correct: false },
                    { text: "Solve it intuitively in one attempt", correct: false }
                ]
            },
            {
                question: "Learning Preference: I learn best through:",
                answers: [
                    { text: "Reading detailed text and instructions", correct: false },
                    { text: "Watching video demonstrations", correct: false },
                    { text: "Listening to explanations", correct: false },
                    { text: "Hands-on practical experience", correct: true }
                ]
            },
            {
                question: "Abstract Reasoning: If all Blargs are Zorks, and some Zorks are Morks, then:",
                answers: [
                    { text: "All Blargs are Morks", correct: false },
                    { text: "Some Blargs are Morks", correct: true },
                    { text: "No Blargs are Morks", correct: false },
                    { text: "Cannot be determined", correct: false }
                ]
            },
            {
                question: "Analytical Thinking: A machine produces 12 items in 3 hours. How many items will it produce in 7.5 hours?",
                answers: [
                    { text: "28", correct: false },
                    { text: "30", correct: true },
                    { text: "24", correct: false },
                    { text: "36", correct: false }
                ]
            },
            {
                question: "Pattern Recognition: Which number should replace the question mark? 1, 4, 9, 16, 25, ?",
                answers: [
                    { text: "30", correct: false },
                    { text: "36", correct: true },
                    { text: "42", correct: false },
                    { text: "49", correct: false }
                ]
            },
            {
                question: "Motivation in Learning: What most motivates you to learn something new?",
                answers: [
                    { text: "Career advancement opportunities", correct: false },
                    { text: "Personal interest and curiosity", correct: true },
                    { text: "Solving a specific practical problem", correct: false },
                    { text: "Personal challenge and growth", correct: false }
                ]
            },
            {
                question: "Time Management: How do you approach a large learning project?",
                answers: [
                    { text: "Create a detailed, step-by-step plan", correct: true },
                    { text: "Start immediately and adapt as I go", correct: false },
                    { text: "Break it into smaller, manageable tasks", correct: false },
                    { text: "Wait for inspiration or motivation", correct: false }
                ]
            },
            {
                question: "Information Processing: When learning something new, I prefer:",
                answers: [
                    { text: "Comprehensive, detailed information", correct: false },
                    { text: "Practical, immediately applicable knowledge", correct: true },
                    { text: "Theoretical background and underlying principles", correct: false },
                    { text: "Visual and interactive content", correct: false }
                ]
            },
            {
                question: "Critical Thinking: If all painters are artists, and some artists are musicians, then:",
                answers: [
                    { text: "All painters are musicians", correct: false },
                    { text: "Some painters might be musicians", correct: true },
                    { text: "No painters are musicians", correct: false },
                    { text: "Cannot be conclusively determined", correct: false }
                ]
            }
        ]
    };

    let currentQuestionIndex = 0;
    let score = 0;
    let timerInterval;
    let timeLeft = 15;
    let userAnswers = [];

    function selectAnswer(answer) {
        clearInterval(timerInterval);
        const buttons = document.getElementsByClassName('answer-btn');
        Array.from(buttons).forEach(button => {
            button.disabled = true;
        });

        const currentQuestion = quizData.questions[currentQuestionIndex];
        let selectedAnswer = answer.image || answer.text;

        userAnswers.push({
            question: currentQuestion.question,
            selectedAnswer: selectedAnswer,
            isCorrect: answer.correct,
            questionNumber: currentQuestionIndex + 1
        });

        if (answer.correct) {
            score++;
        }
        
        setTimeout(() => {
            nextQuestion();
        }, 1000);
    }

    function endQuiz() {
        $('#quiz-screen').hide();
        $('#results-screen').show();
        
        const totalQuestions = quizData.questions.length;
        const percentage = Math.round((score / totalQuestions) * 100);
        
        $('#score-display').html(`<p>Assessment completed successfully!</p>`);

        // Submit results to server
        fetch('/quiz/submit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                score: score,
                totalQuestions: totalQuestions,
                answers: userAnswers
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log('Success:', data);
            if (data.redirectUrl) {
                window.location.href = data.redirectUrl;
            } else {
                window.location.href = '/';
            }
        })
        .catch((error) => {
            console.error('Error:', error);
            alert('There was an error saving your results. Please try again.');
        });
    }

    $('#start-quiz').click(function() {
        $('#name-screen').hide();
        $('#quiz-screen').show();
        startQuiz();
    });

    function startQuiz() {
        currentQuestionIndex = 0;
        score = 0;
        showQuestion();
    }

    function showQuestion() {
        resetState();
        if (currentQuestionIndex < quizData.questions.length) {
            let currentQuestion = quizData.questions[currentQuestionIndex];
            let questionText = document.getElementById('question-text');
            questionText.innerHTML = currentQuestion.question;
            
            // Handle question image
            const questionImage = document.getElementById('question-image');
            if (currentQuestion.questionImage) {
                questionImage.src = currentQuestion.questionImage;
                questionImage.style.display = 'block';
            } else {
                questionImage.style.display = 'none';
            }

            // Create answer buttons
            currentQuestion.answers.forEach((answer, index) => {
                const button = document.createElement('button');
                button.classList.add('answer-btn');
                
                if (answer.image) {
                    const img = document.createElement('img');
                    img.src = answer.image;
                    img.alt = `Option ${index + 1}`;
                    button.appendChild(img);
                } else {
                    button.textContent = answer.text;
                }
                
                button.addEventListener('click', () => selectAnswer(answer));
                document.getElementById('answer-buttons').appendChild(button);
            });
            startTimer();
        } else {
            endQuiz();
        }
    }

    function startTimer() {
        timeLeft = 15;
        $('#timer-bar').css('width', '100%');
        
        timerInterval = setInterval(() => {
            timeLeft--;
            let percentage = (timeLeft / 15) * 100;
            $('#timer-bar').css('width', `${percentage}%`);

            if (timeLeft <= 0) {
                clearInterval(timerInterval);
                nextQuestion();
            }
        }, 1000);
    }

    function nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex < quizData.questions.length) {
            showQuestion();
        } else {
            endQuiz();
        }
    }

    function resetState() {
        clearInterval(timerInterval);
        document.getElementById('answer-buttons').innerHTML = '';
    }
}); 