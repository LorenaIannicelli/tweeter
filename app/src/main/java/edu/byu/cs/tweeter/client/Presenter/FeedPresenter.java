package edu.byu.cs.tweeter.client.Presenter;

import android.util.Log;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.FeedService;
import edu.byu.cs.tweeter.client.model.service.UserService;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFeedTask;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.Status;
import edu.byu.cs.tweeter.model.domain.User;

public class FeedPresenter implements FeedService.FeedObserver{
    private static final int PAGE_SIZE = 10;
    private static final String LOG_TAG = "FeedFragment";

    private final View view;
    private User user;
    private AuthToken authToken;

    private Status lastStatus;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

    public interface View {
        void setLoading(boolean value);
        void addItems(List<Status> newStatuses);
        void displayErrorMessage(String message);
        void displayInfoMessage(String message);
        void navigateToUser(User user);
    }

    public FeedPresenter(View view, User user, AuthToken authToken) {
        this.view = view;
        this.user = user;
        this.authToken = authToken;
    }

    public boolean isHasMorePages() { return hasMorePages;}

    private void setHasMorePages(boolean hasMorePages) {this.hasMorePages = hasMorePages;}

    public boolean isLoading() { return isLoading;}

    private void setIsLoading(boolean isLoading) { this.isLoading = isLoading;}

    public Status getLastStatus() { return lastStatus;}

    private void setLastStatus(Status lastStatus) { this.lastStatus = lastStatus;}

    /**
     * Causes the Adapter to display a loading footer and make a request to get more feed
     * data.
     */
    public void loadMoreItems() throws MalformedURLException {
        if (!isLoading && hasMorePages) {   // This guard is important for avoiding a race condition in the scrolling code.
            isLoading = true;
            setIsLoading(true);
            view.setLoading(true);
            getFeed(authToken, user, PAGE_SIZE, lastStatus);

        }
    }

    public void getFeed(AuthToken authToken, User user, int limit, Status lastStatus)
    {
        getFeedService(this).getFeed(authToken, user, limit, lastStatus);
    }

    public FeedService getFeedService(FeedService.FeedObserver observer) {
        return new FeedService(observer);
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
    public void handleSuccess(boolean hasMorePages, List<Status> statuses, Status lastStatus) {
        setLastStatus(lastStatus);
        setHasMorePages(hasMorePages);

        view.setLoading(false);
        view.addItems(statuses);
        setIsLoading(false);
    }

    @Override
    public void handleFailure(String message) {
        String errorMessage = "Failed to retrieve statuses " + message;
        Log.e(LOG_TAG, errorMessage);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setIsLoading(false);

    }

    @Override
    public void handleException(Exception exception) {
        String errorMessage = "Failed to retrieve statuses because of exception" + exception;
        Log.e(LOG_TAG, errorMessage, exception);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setIsLoading(false);
    }
}
