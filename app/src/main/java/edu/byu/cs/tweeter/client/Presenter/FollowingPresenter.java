package edu.byu.cs.tweeter.client.Presenter;

import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.FollowService;
import edu.byu.cs.tweeter.client.model.service.UserService;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowingTask;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetUserTask;
import edu.byu.cs.tweeter.client.view.main.following.FollowingFragment;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;

public class FollowingPresenter implements FollowService.Observer{

    private static final String LOG_TAG = "FollowingPresenter";
    private static final int PAGE_SIZE = 10;

    private final User user;
    private final View view;
    private final AuthToken authToken;

    private User lastFollowee;
    private boolean hasMorePages = true;
    private boolean isLoading = false;


    public interface View {
        void setLoading(boolean value);
        void addItems(List<User> newUsers);
        void displayErrorMessage(String message);
        void displayInfoMessage(String message);
        void navigateToUser(User user);
    }

    //constructor creates an instance
    public FollowingPresenter(View view, User user, AuthToken authToken)
    {
        this.view = view;
        this.user = user;
        this.authToken = authToken;
    }

    /*return the lastFollowee so the fragment can have access to it*/
    public User getLastFollowee()
    {
        return lastFollowee;
    }

    private void setLastFollowee(User lastFollowee)
    {
        this.lastFollowee = lastFollowee;
    }

    public boolean isHasMorePages()
    {
        return hasMorePages;
    }

    private void setHasMorePages(boolean hasMorePages)
    {
        this.hasMorePages = hasMorePages;
    }

    public boolean isLoading()
    {
        return isLoading;
    }

    private void setLoading(boolean loading)
    {
        this.isLoading = loading;
    }

    /**
     * Causes the Adapter to display a loading footer and make a request to get more following
     * data.
     */
    public void loadMoreFollowees() {
        if (!isLoading & hasMorePages) {   // This guard is important for avoiding a race condition in the scrolling code.
            isLoading = true;
            setLoading(true);
            view.setLoading(true);
            getFollowing(authToken, user, PAGE_SIZE, lastFollowee);
        }
    }

    public void getFollowing(AuthToken authToken, User targetUser, int limit, User lastFollowee)
    {
        getFollowingService(this).getFollowees(authToken, targetUser, limit, lastFollowee);
    }

    public FollowService getFollowingService(FollowService.Observer observer)
    {
        return new FollowService(observer);
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
    public void handleSuccess(List<User> followees, boolean hasMorePages)
    {
        setLastFollowee((followees.size() > 0 ) ? followees.get(followees.size() - 1) : null);
        setHasMorePages(hasMorePages);

        view.setLoading(false);
        view.addItems(followees);
        setLoading(false);
    }

    @Override
    public void handleFailure(String message)
    {
        String errorMessage = "Failed to retrieve followees: " + message;
        Log.e(LOG_TAG, errorMessage);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setLoading(false);
    }

    @Override
    public void handleException(Exception exception)
    {
        String errorMessage = "Failed to retrieve followees because of exception: " + exception.getMessage();
        Log.e(LOG_TAG, errorMessage, exception);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setLoading(false);
    }


}
