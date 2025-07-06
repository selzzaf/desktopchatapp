package com.chatrealtime.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String name;
    private String email;
    private String password;
    private String status;
    private String token;
    
    public boolean isOnline() {
        return "online".equals(status);
    }
} 