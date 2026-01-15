package com.example.closestv2.api.service.model.request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_VALID_CONFIRM_PASSWORD_FORM;
import static com.example.closestv2.api.exception.ExceptionMessageConstants.NOT_VALID_PASSWORD_FORM;
import static com.example.closestv2.util.validation.Pattern.PASSWORD;

@Getter
@AllArgsConstructor
public class MemberAuthSignupPostServiceRequest {
    private String Email;
    @Pattern(regexp = PASSWORD, message = NOT_VALID_PASSWORD_FORM)
    private String password;
    @Pattern(regexp = PASSWORD, message = NOT_VALID_CONFIRM_PASSWORD_FORM)
    private String confirmPassword;
}
