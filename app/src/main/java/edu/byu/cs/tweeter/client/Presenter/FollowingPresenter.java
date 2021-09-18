package edu.byu.cs.tweeter.client.Presenter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.FollowService;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowingTask;
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
    private boolean hasMorePages;
    private boolean isLoading = false;

    public interface View {
        void setLoading(boolean value);
        void addItems(List<User> newUsers);
        void displayErrorMessage(String message);
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
    public  void loadMoreItems() {
        if (!isLoading & hasMorePages) {   // This guard is important for avoiding a race condition in the scrolling code.
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
    {}

    @Override
    public void handleException(Exception exception)
    {

    }


}
