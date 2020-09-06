package app.khatrisoftwares.chatapp.listeners;


import app.khatrisoftwares.chatapp.models.User;

public interface UsersListener {

    void initiateVideoMeeting(User user);

    void initiateAudioMeeting(User user);

    void onMultipleUserAction(boolean isMultipleUsersSelected);
}
