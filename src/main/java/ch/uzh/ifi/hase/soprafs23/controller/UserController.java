package ch.uzh.ifi.hase.soprafs23.controller;

import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserTokenDTO;
import ch.uzh.ifi.hase.soprafs23.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs23.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<UserGetDTO> getAllUsers() {
        // fetch all users in the internal representation
        List<User> users = userService.getUsers();
        List<UserGetDTO> userGetDTOs = new ArrayList<>();

        // convert each user to the API representation
        for (User user : users) {
            userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
        }
        return userGetDTOs;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public UserTokenDTO createUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        // create user
        User createdUser = userService.createUser(userInput);
        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertUserToUserTokenDTO(createdUser);
    }

    /* ORIGINAL
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
        // convert API user to internal representation
        User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // create user
        User createdUser = userService.createUser(userInput);
        // convert internal representation of user back to API
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
    }
    */

    @GetMapping("/users/{userid}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO getUser(@PathVariable String userid) {
        User user = userService.getUserById(userid);
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
    }

    @PutMapping("/users/{userid}/edit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUser(@RequestBody UserPutDTO userPutDTO, @PathVariable String userid)
    {
        User user = DTOMapper.INSTANCE.convertUserPutDTOtoUser(userPutDTO);
        userService.updateUser(user, userid);
    }

    @PostMapping("/users/{userid}/edit")
    @ResponseStatus(HttpStatus.OK)
    public void matchToken(@RequestBody UserTokenDTO userTokenDTO, @PathVariable String userid) {
        String token = userTokenDTO.getToken();
        userService.matchToken(token, userid);
    }

    /**
     * Validation Request - takes a UserPostDTO as input from client as it has
     * password & username attributes; even-though a PostDTO in a GET request is a bit strange
     * @param userPostDTO
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserTokenDTO validateLogin(@RequestBody UserPostDTO userPostDTO){
        User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);
        User validatedUser = userService.validateUser(user);
        return DTOMapper.INSTANCE.convertUserToUserTokenDTO(validatedUser);
    }

    @PutMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public void logout(@RequestBody UserTokenDTO userTokenDTO)
    {
        userService.logout(userTokenDTO.getToken());
    }

    @PutMapping("/users/{userid}")
    @ResponseStatus(HttpStatus.OK)
    public void changeStatus(@PathVariable String userid) {
        userService.changeStatus(userid);
    }

}
