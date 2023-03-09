package ch.uzh.ifi.hase.soprafs23.controller;

import ch.uzh.ifi.hase.soprafs23.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs23.entity.User;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs23.rest.dto.UserTokenDTO;
import ch.uzh.ifi.hase.soprafs23.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.text.SimpleDateFormat;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    Date date = new Date();
    // given
    User user = new User();
    user.setId(1L);
    user.setCreation_date(date);
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

    // have to reformat user creation_date because Date.toString() doesn't convert the object
    // to string in the same manner as the DTOMapperImplementation
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'", Locale.getDefault());
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String formattedDate = sdf.format(user.getCreation_date());
    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(user.getId().intValue())))
        .andExpect(jsonPath("$[0].creation_date", is(formattedDate)))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

  @Test
  public void createUser_validInput_userCreated() throws Exception {
    // init new User
    User user = new User();
    user.setId(1L);
    user.setPassword("testPassword");
    user.setUsername("testUsername");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    // init corresponding UserPostDTO
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setPassword("testPassword");
    userPostDTO.setUsername("testUsername");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then -> UserTokenDTO is returned
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.token", is(user.getToken())));
  }

  // new
  @Test
  public void User_GetUser_ReturnJsonObject() throws Exception {

      String id = "1";
      Date cd = new Date();
      User user = new User();
      user.setId(Long.parseLong(id));
      user.setPassword("testPassword");
      user.setUsername("testUsername");
      user.setToken("1");
      user.setStatus(UserStatus.ONLINE);
      user.setCreation_date(cd);

      given(userService.getUserById(id)).willReturn(user);

      MockHttpServletRequestBuilder getRequest = get(String.format("/users/%s", id))
              .contentType(MediaType.APPLICATION_JSON);

      // have to reformat user creation_date because Date.toString() doesn't convert the object
      // to string in the same manner as the DTOMapperImplementation
      // JSON return
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'", Locale.getDefault());
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      String formattedDate = sdf.format(user.getCreation_date());

      mockMvc.perform(getRequest)
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.id", is(user.getId().intValue())))
              .andExpect(jsonPath("$.username", is(user.getUsername())))
              .andExpect(jsonPath("$.status", is(user.getStatus().toString())))
              .andExpect(jsonPath("$.creation_date", is(formattedDate)))
              .andExpect(jsonPath("$.birthday", is(user.getBirthday())));
  }

  // new
  @Test
  public void updateUser_validInput_userUpdated() throws Exception {

      String id = "1";

      UserPutDTO userPutDTO = new UserPutDTO();
      userPutDTO.setUsername("updatedUsername");
      userPutDTO.setId(Long.parseLong(id));
      userPutDTO.setStatus(UserStatus.ONLINE);
      Date bd = new Date();
      userPutDTO.setBirthday(bd);

      MockHttpServletRequestBuilder putRequest = put(String.format("/users/%s", id))
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(userPutDTO));

      mockMvc.perform(putRequest)
              .andExpect(status().isNoContent());
  }

  // new
  @Test
  public void matchToken_validInput_statusOk() throws Exception {
      String id = "1";

      UserTokenDTO userTokenDTO = new UserTokenDTO();
      userTokenDTO.setToken("testToken");

      MockHttpServletRequestBuilder postRequest = post(("/users/%s/edit"), id)
              .contentType(MediaType.APPLICATION_JSON)
              .content(asJsonString(userTokenDTO));

      mockMvc.perform(postRequest)
              .andExpect(status().isOk());
  }
  /**
   * Helper Method to convert userPostDTO (should work for any DTO?) into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }
}