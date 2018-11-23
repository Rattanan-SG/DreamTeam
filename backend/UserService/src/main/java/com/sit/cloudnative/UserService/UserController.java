package com.sit.cloudnative.UserService;

import com.sit.cloudnative.UserService.exception.BadRequestException;
import com.sit.cloudnative.UserService.exception.NotFoundException;
import com.sit.cloudnative.UserService.exception.UnauthorizedException;
import java.util.HashMap;
import java.util.List;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    Logger logger = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/login")
    public ResponseEntity<User> authenticate(@RequestBody HashMap<String, String> inputUser) {
        if (!inputUser.containsKey("username") || !inputUser.containsKey("password")) {
            logger.warn("Request Body doesn't have username, password");
            throw new BadRequestException("RequestBody not have username or password.");
        }
        User user = userService.findByUsernameAndPassword(inputUser.get("username"), inputUser.get("password"));
        if (user == null) {
            logger.warn("user " + inputUser.get("username") + "not found or wrong password");
            throw new NotFoundException("Not Found user. incorrect username or password.");
        }
        String token = tokenService.createToken(user);
        logger.info("User " + inputUser.get("username") + " has login");
        user.setToken(token);
        return new ResponseEntity<User>(user, HttpStatus.OK);
    }

    @PostMapping("/user")
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        if (user == null) {
            logger.warn("Invalid create user");
            throw new BadRequestException("RequestBody not have user");
        }
        logger.info("User " + user.getUsername() + " was create");
        return new ResponseEntity<User>(userService.createUser(user), HttpStatus.OK);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUserList(@RequestHeader("Authorization") String auth) {
        if(auth.isEmpty()){
            logger.warn("Someone has attempt to access without Auuthorization header");
            throw new BadRequestException("Not have value in Authorization");
        }
        try {
            tokenService.checkToken(auth);
        } catch (Exception e) {
            logger.warn("Invalid token has try access /users");
            throw new UnauthorizedException(e.getMessage());
        }
        return new ResponseEntity<List<User>>(userService.findAll(), HttpStatus.OK);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUser(@PathVariable long id, @RequestHeader("Authorization") String auth ) {
        return new ResponseEntity<User>(userService.findById(id), HttpStatus.OK);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Long> deleteUser(@PathVariable long id) {
        return new ResponseEntity<Long>(userService.deleteById(id), HttpStatus.OK);
    }
}
