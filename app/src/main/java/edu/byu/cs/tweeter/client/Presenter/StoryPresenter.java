package edu.byu.cs.tweeter.client.Presenter;

import android.util.Log;

import java.net.MalformedURLException;
import java.util.List;

import edu.byu.cs.tweeter.client.model.service.StoryService;
import edu.byu.cs.tweeter.client.model.service.UserService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.Status;
import edu.byu.cs.tweeter.model.domain.User;

public class StoryPresenter implements StoryService.StoryObserver {
    private static final String LOG_TAG = "StoryFragment";
    private static final int PAGE_SIZE = 10;

    private User user;
    private final View view;
    private final AuthToken authToken;

    private Status lastStatus;
    private boolean hasMorePages = true;
    private boolean isLoading = false;

    public interface View {
        void setLoading(boolean value);
        void displayErrorMessage(String message);
        void displayInfoMessage(String message);
        void addItems(List<Status> statuses);
        void navigateToUser(User user);
    }

    public StoryPresenter(View view, User user, AuthToken authToken)
    {
        this.view = view;
        this.user = user;
        this.authToken = authToken;
    }

    public Status getLastStatus() {return lastStatus;}

    private void setLastStatus(Status lastStatus) {this.lastStatus = lastStatus;}

    public boolean isHasMorePages() {return hasMorePages;}

    private void setHasMorePages(boolean hasMorePages) {this.hasMorePages = hasMorePages;}

    public boolean isLoading() { return isLoading;}

    private void setIsLoading(boolean isLoading) {this.isLoading = isLoading; }


    /**
     * Causes the Adapter to display a loading footer and make a request to get more story
     * data.
     */
    public void loadMoreItems() throws MalformedURLException {
        if (!isLoading && hasMorePages) {   // This guard is important for avoiding a race condition in the scrolling code.
            isLoading = true;
            setIsLoading(true);
            view.setLoading(true);
            //kick off task
            getStory(authToken, user, PAGE_SIZE, lastStatus);
        }
    }

    public void getStory(AuthToken authToken, User user, int limit, Status lastStatus){
        getStoryService(this).getStory(authToken, user, limit, lastStatus);
    }

    public StoryService getStoryService(StoryService.StoryObserver observer){
        return new StoryService(observer);
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
    public void handleSuccess(List<Status> statuses, boolean hasMorePages, Status lastStatus) {
        setLastStatus(lastStatus);
        setHasMorePages(hasMorePages);

        view.setLoading(false);
        view.addItems(statuses);
        setIsLoading(false);
    }

    @Override
    public void handleFailure(String message) {
        String errorMessage = "Failed to retrieve followees: " + message;
        Log.e(LOG_TAG, errorMessage);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setIsLoading(false);
    }

    @Override
    public void handleException(Exception ex) {
        String errorMessage = "Failed to retrieve followees because of exception: " + ex.getMessage();
        Log.e(LOG_TAG, errorMessage, ex);

        view.setLoading(false);
        view.displayErrorMessage(errorMessage);
        setIsLoading(false);
    }
}
