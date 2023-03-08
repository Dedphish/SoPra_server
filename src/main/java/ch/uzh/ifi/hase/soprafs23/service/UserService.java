package ch.uzh.ifi.hase.soprafs23.service;

import ch.uzh.ifi.hase.soprafs23.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserTokenDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User createUser(User newUser) {
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);

    newUser.setCreation_date(new Date());
    /* try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date dateWithoutTime = sdf.parse(sdf.format(new Date()));
        newUser.setCreation_date(dateWithoutTime);
    }
    catch (ParseException p) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "ParseException when trying to initialize creation date");
    }*/


    checkIfUsernameTaken(newUser);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  /**
   * converts given String id to a Long and returns the corresponding User entity from the database
   * @throws org.springframework.web.server.ResponseStatusException (Http-Status 404; not found)
   * */
  public User getUserById(String userid) {

        Long id;
        Optional<User> user;
        try{
            id = Long.parseLong(userid);
            user= this.userRepository.findById(id);
        }
        catch(NumberFormatException e)
        {
            // if the provided id isn't a long it cannot exist
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: The requested id does not exist");
        }
        if(user.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Error: The requested user does not exist");
        }
        return user.get();
    }

  public void updateUser(User updatedUser, String id) {
      User origUser = getUserById(id);

      if (origUser == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: The provided user-id is invalid");
      }

      // if there is a new username
      if(!updatedUser.getUsername().equals(origUser.getUsername())) {
          checkIfUsernameTaken(updatedUser);
          origUser.setUsername(updatedUser.getUsername());
      }

      if(updatedUser.getBirthday() != null) {
          origUser.setBirthday(updatedUser.getBirthday());

          /*
          try {
              SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
              Date dateWithoutTime = sdf.parse(sdf.format(updatedUser.getBirthday()));
              origUser.setBirthday(dateWithoutTime);
          }
          catch (ParseException p) {
              throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The provided date could not be parsed");
          }*/
      }

      userRepository.save(origUser);
      userRepository.flush();
  }

  /**
   * Changes a users status from offline to online or vice versa
   * userId is converted to Long inside getUserByID()
   * @throws ResponseStatusException if the userId does not correspond to a user
   * */
  public void changeStatus(String userId) {
      User user = getUserById(userId);

      if(user == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Error: The requested user was not found");
      }

      UserStatus status = user.getStatus();
      // returns the opposite status; (ordinal + 1) % 2
      user.setStatus(UserStatus.values()[(status.ordinal() + 1) % 2]);

      userRepository.save(user);
      userRepository.flush();
  }

  public void logout(String token) {
      User user = userRepository.findByToken(token);

      if(user == null) {
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The user you tried to log out was not found");
      }

      user.setStatus(UserStatus.OFFLINE);
      userRepository.save(user);
      userRepository.flush();


  }

  // TODO try implementing authorization with http headers

  public User validateUser(User userToBeValidated){
      String givenUsername = userToBeValidated.getUsername();
      String givenPassword = userToBeValidated.getPassword();

      // get user and check if username is valid
      User user = this.userRepository.findByUsername(givenUsername);

      // if user doesn't exist
      if (user == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                  "Error: Password or username incorrect");
      }

      // retrieve password
      String actualPassword = user.getPassword();

      // if passwords don't match
      if(!(actualPassword.equals(givenPassword))) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                  "Error: Password or username incorrect");
      }

      user.setStatus(UserStatus.ONLINE);

      return user;
  }

  /**
   * Matches provided token with token of user with given ID
   * @throws ResponseStatusException HttpStatus.UNAUTHORIZED if the tokens don't match*/
  public void matchToken(String userToken, String userToMatchId) {

      User requestedUser = getUserById(userToMatchId);

      if(!requestedUser.getToken().equals(userToken)) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Error: Token mismatch");
      }
  }

  private void checkIfUsernameExists(String username) {
      User userByUsername = userRepository.findByUsername(username);
      if (userByUsername == null)
      {
          // throws 404 (conflict) http status if no user with give username is found
          throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                  "The given username does not exist");
      }
  }

  /**
   * checks if a username is taken, throws an error if it is, does nothing if not
   * @throws org.springframework.web.server.ResponseStatusException (Http-Status 409; Conflict)
   */
  private void checkIfUsernameTaken(User userToBeCreated) {
      User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

      if (userByUsername != null) {
          throw new ResponseStatusException(HttpStatus.CONFLICT,
                  "Error: The username you entered is already taken.");
      }
  }

    /**
     * This is a helper method that will check the uniqueness criteria of the
     * username and the name
     * defined in the User entity. The method will do nothing if the input is unique
     * and throw an error otherwise.
     *
     * @param userToBeCreated
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */

  /*
  private void checkIfUserExists(User userToBeCreated) {
    User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());
    User userByName = userRepository.findByName(userToBeCreated.getName());
    // TODO change thrown http-status to CONFLICT (409) --> in accordance to task-sheet
    String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
    if (userByUsername != null && userByName != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format(baseErrorMessage, "username and the name", "are"));
    } else if (userByUsername != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "username", "is"));
    } else if (userByName != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(baseErrorMessage, "name", "is"));
    }
  }*/

}
