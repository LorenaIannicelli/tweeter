package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import edu.byu.cs.tweeter.client.model.service.backgroundTask.BackgroundTaskUtils;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;
import edu.byu.cs.tweeter.util.Pair;

public class FollowService {

    private final Observer observer;

    public interface Observer
    {
        void handleSuccess(List<User> followees, boolean hasMorePages);
        void handleFailure(String message);
        void handleException(Exception exception);
    }

    //create an instance
    public FollowService(Observer observer)
    {
        if(observer == null)
            throw new NullPointerException();
        this.observer = observer;
    }

    //get the followees from the following task
    public void getFollowees (AuthToken authToken, User targetUser, int limit, User lastFollowee)
    {
        GetFollowingTask followingTask = getGetFollowingTask(authToken, targetUser, limit, lastFollowee);
        BackgroundTaskUtils.runTask(followingTask);
    }

    //return a new instance of getfollowing task so we can run it in background task
    public GetFollowingTask getGetFollowingTask(AuthToken authToken, User targetUser, int limit, User lastFollowee)
    {
        return new GetFollowingTask(authToken, targetUser, limit, lastFollowee, new FollowingMessageHandler(Looper.getMainLooper(), observer));
    }

    //get the followers from the followers task
    public void getFollowers(AuthToken authToken, User targetUser, int limit, User lastFollower)
    {
        GetFollowersTask followersTask = getGetFollowersTask(authToken, targetUser, limit, lastFollower);
        BackgroundTaskUtils.runTask(followersTask);
    }

    //return a new instance of getfollowers task so we can run it in the background task
    public GetFollowersTask getGetFollowersTask(AuthToken authToken, User targetUser, int limit, User lastFollower)
    {
        return new GetFollowersTask(authToken, targetUser, limit, lastFollower, new FollowersMessageHandler(Looper.getMainLooper(), observer));
    }


    public static class FollowingMessageHandler extends Handler {

        private final Observer observer;

        public FollowingMessageHandler(Looper looper, Observer observer) {
            super(looper);
            this.observer = observer;
        }

        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            boolean success = bundle.getBoolean(GetFollowingTask.SUCCESS_KEY);
            if (success) {
                List<User> followees = (List<User>) bundle.getSerializable(GetFollowingTask.FOLLOWEES_KEY);
                boolean hasMorePages = bundle.getBoolean(GetFollowingTask.MORE_PAGES_KEY);
                observer.handleSuccess(followees, hasMorePages);
            } else if (bundle.containsKey(GetFollowingTask.MESSAGE_KEY)) {
                String errorMessage = bundle.getString(GetFollowingTask.MESSAGE_KEY);
                observer.handleFailure(errorMessage);
            } else if (bundle.containsKey(GetFollowingTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(GetFollowingTask.EXCEPTION_KEY);
                observer.handleException(ex);
            }
        }
    }

    public static class FollowersMessageHandler extends Handler {

        private final Observer observer;

        public FollowersMessageHandler(Looper looper, Observer observer) {
            super(looper);
            this.observer = observer;
        }

        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            boolean success = bundle.getBoolean(GetFollowersTask.SUCCESS_KEY);
            if (success) {
                List<User> followers = (List<User>) bundle.getSerializable(GetFollowersTask.FOLLOWERS_KEY);
                boolean hasMorePages = bundle.getBoolean(GetFollowingTask.MORE_PAGES_KEY);
                observer.handleSuccess(followers, hasMorePages);
            } else if (bundle.containsKey(GetFollowingTask.MESSAGE_KEY)) {
                String errorMessage = bundle.getString(GetFollowingTask.MESSAGE_KEY);
                observer.handleFailure(errorMessage);
            } else if (bundle.containsKey(GetFollowingTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(GetFollowingTask.EXCEPTION_KEY);
                observer.handleException(ex);
            }
        }
    }

    public class GetFollowingTask extends BackgroundTask {
        private static final String LOG_TAG = "GetFollowingTask";

        public static final String SUCCESS_KEY = "success";
        public static final String FOLLOWEES_KEY = "followees";
        public static final String MORE_PAGES_KEY = "more-pages";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * The user whose following is being retrieved.
         * (This can be any user, not just the currently logged-in user.)
         */
        private User targetUser;
        /**
         * Maximum number of followed users to return (i.e., page size).
         */
        private int limit;
        /**
         * The last person being followed returned in the previous page of results (can be null).
         * This allows the new page to begin where the previous page ended.
         */
        private User lastFollowee;

        public GetFollowingTask(AuthToken authToken, User targetUser, int limit, User lastFollowee,
                                Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.targetUser = targetUser;
            this.limit = limit;
            this.lastFollowee = lastFollowee;
        }

        protected void sendSuccessMessage(List<User> followees, boolean hasMorePages)
        {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putSerializable(FOLLOWEES_KEY, (Serializable) followees);
                    msgBundle.putBoolean(MORE_PAGES_KEY, hasMorePages);
                }
            });
        }

        @Override
        protected void runTask() {
            try {
                Pair<List<User>, Boolean> pageOfUsers = getFollowees();

                List<User> followees = pageOfUsers.getFirst();
                boolean hasMorePages = pageOfUsers.getSecond();

                loadImages(followees);

                sendSuccessMessage(followees, hasMorePages);

            } catch (Exception ex) {
                Log.e(LOG_TAG, "Failed to get followees", ex);
                sendExceptionMessage(ex);
            }
        }

        private FakeData getFakeData() {
            return new FakeData();
        }

        private Pair<List<User>, Boolean> getFollowees() {
            return getFakeData().getPageOfUsers((User) lastFollowee, limit, targetUser);
        }

        private void loadImages(List<User> followees) throws IOException {
            for (User u : followees) {
                BackgroundTaskUtils.loadImage(u);
            }
        }


    }

    public class GetFollowersTask extends BackgroundTask {
        private static final String LOG_TAG = "GetFollowersTask";

        public static final String SUCCESS_KEY = "success";
        public static final String FOLLOWERS_KEY = "followers";
        public static final String MORE_PAGES_KEY = "more-pages";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * The user whose followers are being retrieved.
         * (This can be any user, not just the currently logged-in user.)
         */
        private User targetUser;
        /**
         * Maximum number of followers to return (i.e., page size).
         */
        private int limit;
        /**
         * The last follower returned in the previous page of results (can be null).
         * This allows the new page to begin where the previous page ended.
         */
        private User lastFollower;


        public GetFollowersTask(AuthToken authToken, User targetUser, int limit, User lastFollower,
                                Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.targetUser = targetUser;
            this.limit = limit;
            this.lastFollower = lastFollower;
        }

        @Override
        public void runTask() {
            try {
                Pair<List<User>, Boolean> pageOfUsers = getFollowers();

                List<User> followers = pageOfUsers.getFirst();
                boolean hasMorePages = pageOfUsers.getSecond();

                for (User u : followers) {
                    BackgroundTaskUtils.loadImage(u);
                }

                sendSuccessMessage(followers, hasMorePages);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private FakeData getFakeData() {
            return new FakeData();
        }

        private Pair<List<User>, Boolean> getFollowers() {
            Pair<List<User>, Boolean> pageOfUsers = getFakeData().getPageOfUsers(lastFollower, limit, targetUser);
            return pageOfUsers;
        }

        protected void sendSuccessMessage(List<User> followers, boolean hasMorePages) {
            sendSuccessMessage(new BundleLoader()
            {
                @Override
                public void load(Bundle msgBundle)
                {
                    msgBundle.putSerializable(FOLLOWERS_KEY, (Serializable) followers);
                    msgBundle.putBoolean(MORE_PAGES_KEY, hasMorePages);
                }
            });
        }


    }



    }
