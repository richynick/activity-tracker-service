package com.richard.activitytracker.mapper;

import com.richard.activitytracker.dto.AuthRequest;
import com.richard.activitytracker.dto.AuthResponse;
import com.richard.activitytracker.dto.RegisterRequest;
import com.richard.activitytracker.model.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public User toUser(RegisterRequest request) {

        if (request == null) {
            return null;
        }
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();
    }

//    public AuthResponse fromUser(User user){
//        if(user == null){
//            return null;
//        }
//        return new AuthResponse(
//                u
//        )
//    }
}
