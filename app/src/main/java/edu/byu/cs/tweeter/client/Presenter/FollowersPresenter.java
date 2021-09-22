package edu.byu.cs.tweeter.client.Presenter;

import android.util.Log;

import java.util.List;

import edu.byu.cs.tweeter.client.model.service.FollowService;
import edu.byu.cs.tweeter.client.model.service.UserService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;

public class FollowersPresenter implements FollowService.GetFollowersObserver{
    private static final String LOG_TAG = "FollowersFragment";
    private static final int PAGE_SIZE = 10;

    private final User user;
    private final View view;
    private final AuthToken authToken;

    private User lastFollower;
    private boolean hasMorePages = true;
    private boolean isLoading = false;

    public interface View{
        void setLoading(boolean value);
        void addItems(List<User> newUsers);
        void displayErrorMessage(String message);
        void displayInfoMessage(String message);
        void navigateToUser(User user);

    }

    public FollowersPresenter(View view, User user, AuthToken authToken)
    {
        this.user = user;
        this.view = view;
        this.authToken = authToken;
    }

    public User getLastFollower() {
        return lastFollower;
    }

    private void setLastFollower(User lastFollower)
    {
        this.lastFollower = lastFollower;
    }

    public boolean isHasMorePages() {return hasMorePages;}

    private void setHasMorePages(boolean hasMorePages) {this.hasMorePages = hasMorePages;}

    public boolean isLoading() {
        return isLoading;
    }

    private void setIsLoading(boolean isLoading) {this.isLoading = isLoading;}

    /**
     * Causes the Adapter to display a loading footer and make a request to get more following
     * data.
     */
    public void loadMoreFollowers() {
        if (!isLoading && hasMorePages) {   // This guard is important for avoiding a race condition in the scrolling code.
            isLoading = true;
            setIsLoading(true);
            view.setLoading(true);
            getFollowers(authToken, user, PAGE_SIZE, lastFollower);
        }
    }

    public void getFollowers(AuthToken authToken, User user, int limit, User lastFollower)
    {
        new FollowService().getFollowers(authToken, user, limit, lastFollower, this);
    }


    public void gotoUser(String alias)
    {
        view.displayInfoMessage("Getting user's profile...");
        new UserService().getUser(authToken, alias, new UserService.GetUserObserver()
        {
            @Override
            public void getUserHandleSuccess(User user, AuthToken authToken)
            {
                String message = "Getting user's profile...";
                view.displayInfoMessage(message);
                view.navigateToUser(user);
            }
            @Override
            public void getUserHandleFailure(String message)
            {
                String errorMessage = "Failed to load user: " + message;
                Log.e(LOG_TAG, errorMessage);
                view.displayErrorMessage(errorMessage);
            }
            @Override
            public void getUserHandleException(Exception ex)
            {
                String errorMessage = "Failed to retrieve user because of exception: " + ex.getMessage();
                Log.e(LOG_TAG, errorMessage, ex);

                view.displayErrorMessage(errorMessage);
            }

        });
    }


    @Override
    public void getFollowersSuccess(List<User> folowees, boolean hasMorePages) {
        setLastFollower((folowees.size() > 0 ) ? folowees.get(folowees.size() - 1) : null);
        setHasMorePages(hasMorePages);
        view.setLoading(false);
        view.addItems(folowees);
        setIsLoading(false);
    }

    @Override
    public void getFollowersFailure(String message) {
        String errorMessage = "Failed to retrieve followers: " + message;
        Log.e(LOG_TAG, errorMessage);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setIsLoading(false);
    }

    @Override
    public void getFollowersException(Exception exception) {
        String errorMessage = "Failed to retrieve followers because of exception: " + exception.getMessage();
        Log.e(LOG_TAG, errorMessage, exception);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setIsLoading(false);
    }
}
