(function() {
            emailjs.init("i64qWT-q9GKKNdepE");  // Replace with your actual EmailJS public key
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

            if (!isValidEmail(email)) {
                alert("Please enter a valid email address.");
                return;
            }

            // Generate OTP
            var generatedOTP = generateOTP();

            var templateParams = {
                reply_to: email,
                from_name: "nexlearn",
                message: `Please use this OTP: ${generatedOTP} to complete the verification of your email.`
            };

            // Send the OTP via EmailJS
            emailjs.send('service_04xs8uk', 'template_gklzjyh', templateParams)
                .then(function(response) {
                    console.log('EmailJS SUCCESS!', response.status, response.text);
                    $.post("/forgot-password/generate-otp", { email: email, otp: generatedOTP }, function(response) {
                        if (response === "OTP stored successfully") {
                            alert("OTP sent successfully to " + email);
                        } else if (response === "Email not found") {
                            alert("Email not found in our records.");
                        } else {
                            alert("Failed to send OTP. Please try again.");
                        }
                    }).fail(function(error) {
                        console.error("Failed to store OTP:", error);
                        alert("Failed to send OTP. Please try again.");
                    });
                }, function(error) {
                    console.error('EmailJS FAILED...', error);
                    alert("Failed to send OTP. Error: " + JSON.stringify(error));
                });
        });

        // Event listener for verifying OTP
        $('#verifyOtp').click(function() {
            var email = $('#email').val();
            var enteredOtp = $('#otp').val();

            $.post("/forgot-password/verify-otp", { email: email, otp: enteredOtp }, function(response) {
                if (response === true) {
                    $('.verification-section').hide();
                    $('.password-section').show();
                    alert("OTP verified successfully! You can now reset your password.");
                } else {
                    alert("Invalid OTP. Please try again.");
                }
            }).fail(function(error) {
                console.error("Verification failed:", error);
                alert("Failed to verify OTP. Please try again.");
            });
        });

        // Event listener for updating password
        $('#updatePassword').click(function() {
            var email = $('#email').val();
            var newPassword = $('#newPassword').val();
            var confirmPassword = $('#confirmPassword').val();

            if (newPassword !== confirmPassword) {
                alert("Passwords do not match!");
                return;
            }

            if (newPassword.length < 6) {
                alert("Password must be at least 6 characters long!");
                return;
            }

            $.post("/forgot-password/update-password", {
                email: email,
                password: newPassword
            }, function(response) {
                if (response === "Password updated successfully") {
                    alert("Password updated successfully! You can now login with your new password.");
                    window.location.href = "/login";
                } else {
                    alert("Failed to update password. Please try again.");
                }
            }).fail(function(error) {
                console.error("Update failed:", error);
                alert("Failed to update password. Please try again.");
            });
        });