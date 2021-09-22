package edu.byu.cs.tweeter.client.Presenter;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.FollowService;
import edu.byu.cs.tweeter.client.model.service.StatusService;
import edu.byu.cs.tweeter.client.model.service.UserService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.Status;
import edu.byu.cs.tweeter.model.domain.User;

public class MainPresenter {

    public interface View {
        void displayPostMessage();
        void clearPostMessage();
        void displayLogoutMessage();
        void clearLogoutMessage();

        void displayInfoMessage(String message);
        void displayErrorMessage(String message);

        void updateFollowerCount(int count);
        void updateFollowingCount(int count);

        void renderFollowButton();
        void renderFollowingButton();

        void enableFollowButton(boolean enabled);
        void updateFollowersAndFollowing();
    }

    private final View view;
    private AuthToken authToken;        //NEED TO IMPLEMENT
    private User user;                  //NEED TO IMPLEMENT

    public MainPresenter(View view, User user)
    {
        this.view = view;
        this.user = user;
    }

    public void postStatus(String post)
    {
        try {
            Status newStatus = new Status(post, Cache.getInstance().getCurrUser(), getFormattedDateTime(), parseURLs(post), parseMentions(post));
            new StatusService().postStatus(authToken, newStatus, new StatusService.StatusObserver()
            {
                @Override
                public void handleSuccess() {
                    view.clearPostMessage();
                    view.displayInfoMessage("Successfully Posted!");
                }

                @Override
                public void handleFailure(String message) {
                    view.displayErrorMessage("Failed to post status: " + message);
                }

                @Override
                public void handleException(Exception ex) {
                    view.displayErrorMessage("Failed to post status: " + ex.getMessage());
                }
            });

        } catch(ParseException exception) {

        }catch(MalformedURLException exception) {

        }
    }

    public void logout() {
        view.displayLogoutMessage();
        new UserService().logout(authToken, new UserService.LogoutObserver()
        {
            @Override
            public void logoutSucceeded() {
                view.clearLogoutMessage();
            }

            @Override
            public void logoutFailed(String message) {
                view.displayErrorMessage("Logout failed: " + message);
            }

            @Override
            public void logoutThrewException(Exception ex) {
                view.displayErrorMessage("Logout failed due to exception: " + ex.getMessage());
            }
        });
    }

    public void getFollowersCount()
    {
        new FollowService().getFollowersCount(authToken, user, new FollowService.GetFollowersCountObserver()
        {
            @Override
            public void getFollowersCountSuccess(int count) {
                view.updateFollowerCount(count);
            }

            @Override
            public void getFollowersCountFailure(String message) {
                view.displayErrorMessage("Failed to get followers count " + message);
            }

            @Override
            public void getFollowersCountException(Exception ex) {
                view.displayErrorMessage("Failed to get followers count because of exception: " + ex.getMessage());
            }
        });
    }

    public void getFollowingCount() {
        new FollowService().getFollowingCount(authToken, user, new FollowService.GetFollowingCountObserver()
        {
            @Override
            public void getFollowingCountSuccess(int count) {
                view.updateFollowingCount(count);
            }

            @Override
            public void getFollowingCountFailure(String message) {
                view.displayErrorMessage("Failed to get following count " + message);
            }

            @Override
            public void getFollowingCountException(Exception exception) {
                view.displayErrorMessage("Failed to get following count because of exception: " + exception.getMessage());
            }
        });
    }

    public void isFollower(User followee){
        new FollowService().isFollower(authToken, user, followee, new FollowService.IsFollowerObserver()
        {
            @Override
            public void isFollowerSuccess(boolean isFollower) {
                if (isFollower) {
                    view.renderFollowingButton();
                } else {
                    view.renderFollowButton();
                }
            }

            @Override
            public void isFollowerFailure(String message) {
                view.displayErrorMessage("Failed to determine following relationship: " + message);
            }

            @Override
            public void isFollowerException(Exception exception) {
                view.displayErrorMessage("Failed to determine following relationship because of exception: " + exception.getMessage());
            }
        });
    }

    public void follow(User followee)
    {
        view.displayInfoMessage("Adding " + followee.getName());
        new FollowService().follow(authToken, followee, new FollowService.FollowObserver()
        {
            @Override
            public void followSuccess() {
                view.updateFollowersAndFollowing();

                view.renderFollowingButton();
            }

            @Override
            public void followFailure(String message) {
                view.displayErrorMessage("Failed to follow: " + message);
            }

            @Override
            public void followException(Exception exception) {
                view.displayErrorMessage("Failed to follow because of exception: " + exception.getMessage());
            }

            @Override
            public void followUpdateAll() {
                view.enableFollowButton(true);
            }
        });
    }


    private String getFormattedDateTime() throws ParseException {
        SimpleDateFormat userFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat statusFormat = new SimpleDateFormat("MMM d yyyy h:mm aaa");

        return statusFormat.format(userFormat.parse(LocalDate.now().toString() + " " + LocalTime.now().toString().substring(0, 8)));
    }

    private List<String> parseURLs(String post) throws MalformedURLException {
        List<String> containedUrls = new ArrayList<>();
        for (String word : post.split("\\s")) {
            if (word.startsWith("http://") || word.startsWith("https://")) {

                int index = findUrlEndIndex(word);

                word = word.substring(0, index);

                containedUrls.add(word);
            }
        }

        return containedUrls;
    }

    private int findUrlEndIndex(String word) {
        if (word.contains(".com")) {
            int index = word.indexOf(".com");
            index += 4;
            return index;
        } else if (word.contains(".org")) {
            int index = word.indexOf(".org");
            index += 4;
            return index;
        } else if (word.contains(".edu")) {
            int index = word.indexOf(".edu");
            index += 4;
            return index;
        } else if (word.contains(".net")) {
            int index = word.indexOf(".net");
            index += 4;
            return index;
        } else if (word.contains(".mil")) {
            int index = word.indexOf(".mil");
            index += 4;
            return index;
        } else {
            return word.length();
        }
    }

    private List<String> parseMentions(String post) {
        List<String> containedMentions = new ArrayList<>();

        for (String word : post.split("\\s")) {
            if (word.startsWith("@")) {
                word = word.replaceAll("[^a-zA-Z0-9]", "");
                word = "@".concat(word);

                containedMentions.add(word);
            }
        }

        return containedMentions;
    }
}
