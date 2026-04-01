package com.example.demo.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get error status
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            // Only log non-404 errors
            if (statusCode != 404) {
                logger.error("Error {}: {}", statusCode, message);
            }
            
            model.addAttribute("errorCode", statusCode);
            model.addAttribute("errorTitle", getErrorTitle(statusCode));
            model.addAttribute("errorMessage", getErrorMessage(statusCode, message));
            
            // Add technical details for non-production environments
            if (!isProdEnvironment()) {
                model.addAttribute("errorDetails", exception != null ? 
                    exception.toString() : "No additional details available");
            }
        } else {
            model.addAttribute("errorCode", 500);
            model.addAttribute("errorTitle", "Unexpected Error");
            model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        }

        return "error";
    }

    private String getErrorTitle(int statusCode) {
        switch (statusCode) {
            case 404:
                return "Page Not Found";
            case 403:
                return "Access Denied";
            case 500:
                return "Server Error";
            default:
                return "Oops! Something Went Wrong";
        }
    }

    private String getErrorMessage(int statusCode, Object message) {
        switch (statusCode) {
            case 404:
                return "The page you're looking for doesn't exist.";
            case 403:
                return "You don't have permission to access this resource.";
            case 500:
                return "An internal server error occurred. Please try again later.";
            default:
                return message != null ? message.toString() : 
                    "An unexpected error occurred. Please try again.";
        }
    }

    private boolean isProdEnvironment() {
        // You can implement your own logic to determine if this is a production environment
        return false;
    }
}
