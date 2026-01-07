package com.nxq.perform.stackoverflow.service;

import com.nxq.perform.stackoverflow.entity.sql.User;
import com.nxq.perform.stackoverflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Integer id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(Integer id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            user.setDisplayName(userDetails.getDisplayName());
            user.setAboutMe(userDetails.getAboutMe());
            user.setAge(userDetails.getAge());
            user.setCreationDate(userDetails.getCreationDate());
            user.setDownVotes(userDetails.getDownVotes());
            return userRepository.save(user);
        }).orElse(null);
    }

    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }
}
