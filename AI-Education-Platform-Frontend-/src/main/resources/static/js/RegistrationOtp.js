
(function() {
            emailjs.init("i64qWT-q9GKKNdepE");  // Replace with your EmailJS public key
        })();

        // Function to validate email format
        function isValidEmail(email) {
            var emailPattern = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
            return emailPattern.test(email);
        }

        // Function to generate random 4-digit OTP
        const generateOTP = () => {
            return Math.floor(1000 + Math.random() * 9000).toString();
        };

        // Event listener for sending OTP
        $('#sendOtp').click(function() {
            var email = $('#email').val();
			$(".otpsec").addClass("active").fadeIn();
            if (!isValidEmail(email)) {
                alert("Please enter a valid email address.");
                return;
            }

            // Check if the email already exists in the backend
            $.post("/registration/check-email", { email: email }, function(response) {
                if (response === true) {
                    alert("This email is	 already registered. Please use a different email.");
                } else {
                    // Generate OTP
                    var generatedOTP = generateOTP();

					var templateParams = {
					    reply_to: email,  // Use the email entered by the user here
					    from_name: "nexlearn",
					    message: `Please use this OTP: ${generatedOTP} to complete the verification of your email. Please do not share.`
					};

                    // Send the OTP via EmailJS
                    emailjs.send('service_04xs8uk', 'template_q9xwjb8', templateParams)
                        .then(function(response) {
                            console.log('EmailJS SUCCESS!', response.status, response.text);
                            // Store OTP in backend
                            $.post("/registration/generate-otp", { email: email, otp: generatedOTP }, function(response) {
                                if (response === "OTP stored successfully") {
                                    alert("OTP sent successfully to " + email);
                                } else {
                                    alert("Failed to send OTP. Please try again.");
                                }
                            }).fail(function(error) {
                                console.error("Failed to store OTP in backend:", error);
                                alert("Failed to send OTP. Please try again.");
                            });
                        }, function(error) {
                            console.error('EmailJS FAILED...', error);
                            alert("Failed to send OTP. Error: " + JSON.stringify(error));
                        });
                }
            }).fail(function(error) {
                console.error("Error checking email:", error);
                alert("Error checking email. Please try again.");
            });
			
        });

        // Event listener for verifying OTP
        $('#verifyOtp').click(function() {
            var email = $('#email').val();
            var enteredOtp = $('#otp').val();

            if (!isValidEmail(email)) {
                alert("Please enter a valid email address.");
                return;
            }

            // Send verification request to backend
            $.post("/registration/verify-otp", { email: email, otp: enteredOtp }, function(response) {
                if (response === true) {
                    $('#registerButton').prop('disabled', false); // Enable the register button
                    alert("OTP verified successfully! You can now register.");
                } else {
                    alert("Invalid OTP. Please try again.");
                }
            }).fail(function(error) {
                console.error("Verification request failed:", error);
                alert("Failed to verify OTP. Please try again.");
            });
        });