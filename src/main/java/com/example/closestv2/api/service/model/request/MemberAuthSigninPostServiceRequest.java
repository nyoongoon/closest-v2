package com.example.closestv2.api.service.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberAuthSigninPostServiceRequest {
    private String Email;
    private String password;
}
