package ch.uzh.ifi.hase.soprafs23.rest.dto;

import ch.uzh.ifi.hase.soprafs23.constant.UserStatus;

public class UserGetDTO {

  private Long id;
  private String username;
  private UserStatus status;
  private String creation_date;
  private String birthday;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getCreation_date(){
      return creation_date;
  }

  public void setCreation_date(String creation_date){
      this.creation_date = creation_date;
  }

  public String getBirthday() {
      return birthday;
  }

  public void setBirthday(String birthday) {
      this.birthday = birthday;
  }
}
