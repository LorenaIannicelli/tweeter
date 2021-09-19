package edu.byu.cs.tweeter.client.Presenter;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.FeedService;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFeedTask;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.Status;
import edu.byu.cs.tweeter.model.domain.User;

public class FeedPresenter implements FeedService.FeedObserver{
    private static final int PAGE_SIZE = 10;

    private final View view;

    private User user;
    private Status lastStatus;
    private boolean isLoading = false;
    private AuthToken authToken;

    private boolean hasMorePages = true;

    public interface View {
        void setLoading(boolean value);
        void displayErrorMessage(String message);
        void displayInfoMessage(String message);
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
        if (!isLoading) {   // This guard is important for avoiding a race condition in the scrolling code.
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


    @Override
    public void handleSuccess(boolean hasMorePages, List<Status> statuses, Status lastStatus) {
        setLastStatus(lastStatus);
        setHasMorePages(hasMorePages);

        view.setLoading(false);
        //add view method for adding those methods
        //feedRecyclerViewAdapter.addItems(statuses);
        setIsLoading(false);



    }

    @Override
    public void handleFailure(String message) {

    }

    @Override
    public void handleException(Exception exception) {

    }
}
