package com.festapp.dashboar.security;

import com.festapp.dashboar.entity.User;
import com.festapp.dashboar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return CustomUserDetails.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .role(user.getRole().toString())
                .isActive(user.getIsActive())
                .build();
    }
}

