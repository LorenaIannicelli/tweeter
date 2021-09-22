package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

import edu.byu.cs.client.R;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.BackgroundTaskUtils;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.FollowTask;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowersCountTask;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowingCountTask;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.IsFollowerTask;
import edu.byu.cs.tweeter.client.view.main.MainActivity;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;
import edu.byu.cs.tweeter.util.Pair;

public class FollowService {

    public interface GetFollowersObserver
    {
        void getFollowersSuccess(List<User> followees, boolean hasMorePages);
        void getFollowersFailure(String message);
        void getFollowersException(Exception exception);
    }

    public interface GetFollowingObserver
    {
        void getFollowingSuccess(List<User> followees, boolean hasMorePages);
        void getFollowingFailure(String message);
        void getFollowingException(Exception exception);
    }

    public interface GetFollowersCountObserver
    {
        void getFollowersCountSuccess(int count);
        void getFollowersCountFailure(String message);
        void getFollowersCountException(Exception ex);
    }

    public interface GetFollowingCountObserver {
        void getFollowingCountSuccess(int count);
        void getFollowingCountFailure(String message);
        void getFollowingCountException(Exception exception);
    }

    public interface FollowObserver {
        void followSuccess();
        void followFailure(String message);
        void followException(Exception exception);
        void followUpdateAll();
    }

    public interface UnfollowObserver {
        void unfollowSuccess();
        void unfollowFailure(String message);
        void unfollowException(Exception exception);
    }

    public interface IsFollowerObserver {
        void isFollowerSuccess(boolean isFollower);
        void isFollowerFailure(String message);
        void isFollowerException(Exception exception);
    }

    //create an instance
    public FollowService()
    { }

    //get the followees from the following task
    public void getFollowing(AuthToken authToken, User targetUser, int limit, User lastFollowee, GetFollowingObserver observer)
    {
        GetFollowingTask followingTask = getGetFollowingTask(authToken, targetUser, limit, lastFollowee, observer);
        BackgroundTaskUtils.runTask(followingTask);
    }

    //return a new instance of getfollowing task so we can run it in background task
    public GetFollowingTask getGetFollowingTask(AuthToken authToken, User targetUser, int limit, User lastFollowee, GetFollowingObserver observer)
    {
        return new GetFollowingTask(authToken, targetUser, limit, lastFollowee, new FollowingMessageHandler(Looper.getMainLooper(), observer));
    }

    //get the followers from the followers task
    public void getFollowers(AuthToken authToken, User targetUser, int limit, User lastFollower, GetFollowersObserver observer)
    {
        GetFollowersTask followersTask = getGetFollowersTask(authToken, targetUser, limit, lastFollower, observer);
        BackgroundTaskUtils.runTask(followersTask);
    }

    //return a new instance of getfollowers task so we can run it in the background task
    public GetFollowersTask getGetFollowersTask(AuthToken authToken, User targetUser, int limit, User lastFollower, GetFollowersObserver observer)
    {
        return new GetFollowersTask(authToken, targetUser, limit, lastFollower, new FollowersMessageHandler(Looper.getMainLooper(), observer));
    }

    public void getFollowersCount(AuthToken authToken, User user, GetFollowersCountObserver observer)
    {
        GetFollowersCountTask getFollowersCountTask = getGetFollowersCountTask(authToken, user, observer);
        BackgroundTaskUtils.runTask(getFollowersCountTask);
    }

    public GetFollowersCountTask getGetFollowersCountTask(AuthToken authToken, User user, GetFollowersCountObserver observer)
    {
        return new GetFollowersCountTask(authToken, user, new GetFollowersCountHandler(Looper.getMainLooper(), observer));
    }

    public void getFollowingCount(AuthToken authToken, User user, GetFollowingCountObserver observer)
    {
        GetFollowingCountTask getFollowingCountTask = getGetFollowingCountTask(authToken, user, observer);
        BackgroundTaskUtils.runTask(getFollowingCountTask);
    }

    public GetFollowingCountTask getGetFollowingCountTask(AuthToken authToken, User user, GetFollowingCountObserver observer)
    {
        return new GetFollowingCountTask(authToken, user, new GetFollowingCountHandler(Looper.getMainLooper(), observer));
    }

    public void isFollower(AuthToken authToken, User follower, User followee, IsFollowerObserver observer)
    {
        IsFollowerTask isFollowerTask = getIsFollowerTask(authToken, follower, followee, observer);
        BackgroundTaskUtils.runTask(isFollowerTask);
    }

    public IsFollowerTask getIsFollowerTask(AuthToken authToken, User follower, User followee, IsFollowerObserver observer){
        return new IsFollowerTask(authToken, follower, followee, new IsFollowerHandler(Looper.getMainLooper(), observer));
    }

    public void follow(AuthToken authToken, User followee, FollowObserver observer) {
        FollowTask followTask = getFollowTask(authToken, followee, observer);
        BackgroundTaskUtils.runTask(followTask);
    }

    public FollowTask getFollowTask(AuthToken authToken, User followee, FollowObserver observer)
    {
        return new FollowTask(authToken, followee, new FollowHandler(Looper.getMainLooper(), observer));
    }



    public static class FollowingMessageHandler extends Handler {

        private final GetFollowingObserver observer;

        public FollowingMessageHandler(Looper looper, GetFollowingObserver observer) {
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
                observer.getFollowingSuccess(followees, hasMorePages);
            } else if (bundle.containsKey(GetFollowingTask.MESSAGE_KEY)) {
                String errorMessage = bundle.getString(GetFollowingTask.MESSAGE_KEY);
                observer.getFollowingFailure(errorMessage);
            } else if (bundle.containsKey(GetFollowingTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(GetFollowingTask.EXCEPTION_KEY);
                observer.getFollowingException(ex);
            }
        }
    }

    public static class FollowersMessageHandler extends Handler {

        private final GetFollowersObserver observer;

        public FollowersMessageHandler(Looper looper, GetFollowersObserver observer) {
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
                observer.getFollowersSuccess(followers, hasMorePages);
            } else if (bundle.containsKey(GetFollowingTask.MESSAGE_KEY)) {
                String errorMessage = bundle.getString(GetFollowingTask.MESSAGE_KEY);
                observer.getFollowersFailure(errorMessage);
            } else if (bundle.containsKey(GetFollowingTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(GetFollowingTask.EXCEPTION_KEY);
                observer.getFollowersException(ex);
            }
        }
    }

    private class GetFollowersCountHandler extends Handler {
        private final GetFollowersCountObserver observer;

        public GetFollowersCountHandler(Looper looper, GetFollowersCountObserver observer)
        {
            super(looper);
            this.observer = observer;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            boolean success = msg.getData().getBoolean(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowersCountTask.SUCCESS_KEY);
            if (success) {
                int count = msg.getData().getInt(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowersCountTask.COUNT_KEY);
                observer.getFollowersCountSuccess(count);
            } else if (msg.getData().containsKey(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowersCountTask.MESSAGE_KEY)) {
                String message = msg.getData().getString(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowersCountTask.MESSAGE_KEY);
                observer.getFollowersCountFailure(message);
            } else if (msg.getData().containsKey(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowersCountTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) msg.getData().getSerializable(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowersCountTask.EXCEPTION_KEY);
                observer.getFollowersCountException(ex);
            }
        }
    }

    private class GetFollowingCountHandler extends Handler {
        private final GetFollowingCountObserver observer;

        public GetFollowingCountHandler(Looper looper, GetFollowingCountObserver observer)
        {
            super(looper);
            this.observer = observer;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            boolean success = msg.getData().getBoolean(GetFollowingCountTask.SUCCESS_KEY);
            if (success) {
                int count = msg.getData().getInt(GetFollowingCountTask.COUNT_KEY);
                observer.getFollowingCountSuccess(count);
            } else if (msg.getData().containsKey(GetFollowingCountTask.MESSAGE_KEY)) {
                String message = msg.getData().getString(GetFollowingCountTask.MESSAGE_KEY);
                observer.getFollowingCountFailure(message);
            } else if (msg.getData().containsKey(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowingCountTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) msg.getData().getSerializable(edu.byu.cs.tweeter.client.model.service.backgroundTask.GetFollowingCountTask.EXCEPTION_KEY);
                observer.getFollowingCountException(ex);
            }
        }
    }

    private class IsFollowerHandler extends Handler {
        private final IsFollowerObserver observer;

        public IsFollowerHandler(Looper looper, IsFollowerObserver observer)
        {
            super(looper);
            this.observer = observer;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            boolean success = msg.getData().getBoolean(IsFollowerTask.SUCCESS_KEY);
            if (success) {
                boolean isFollower = msg.getData().getBoolean(IsFollowerTask.IS_FOLLOWER_KEY);
                // If logged in user if a follower of the selected user, display the follow button as "following"
                observer.isFollowerSuccess(isFollower);
            } else if (msg.getData().containsKey(IsFollowerTask.MESSAGE_KEY)) {
                String message = msg.getData().getString(IsFollowerTask.MESSAGE_KEY);
                observer.isFollowerFailure(message);
            } else if (msg.getData().containsKey(IsFollowerTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) msg.getData().getSerializable(IsFollowerTask.EXCEPTION_KEY);
            }
        }
    }

    private class FollowHandler extends Handler {
        private final FollowObserver observer;

        public FollowHandler(Looper looper, FollowObserver observer)
        {
            super(looper);
            this.observer = observer;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            boolean success = msg.getData().getBoolean(FollowTask.SUCCESS_KEY);
            if (success) {
                observer.followSuccess();
            } else if (msg.getData().containsKey(FollowTask.MESSAGE_KEY)) {
                String message = msg.getData().getString(FollowTask.MESSAGE_KEY);
                observer.followFailure(message);
            } else if (msg.getData().containsKey(FollowTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) msg.getData().getSerializable(FollowTask.EXCEPTION_KEY);
                observer.followException(ex);
            }
            observer.followUpdateAll();


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

    public class GetFollowersCountTask extends BackgroundTask {
        private static final String LOG_TAG = "LogoutTask";

        public static final String SUCCESS_KEY = "success";
        public static final String COUNT_KEY = "count";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * The user whose follower count is being retrieved.
         * (This can be any user, not just the currently logged-in user.)
         */
        private User targetUser;


        public GetFollowersCountTask(AuthToken authToken, User targetUser, Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.targetUser = targetUser;
        }

        @Override
        public void runTask() {
            try {

                sendSuccessMessage(20);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private void sendSuccessMessage(int count) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putInt(COUNT_KEY, count);
                }
            });
        }

    }

    public class GetFollowingCountTask extends BackgroundTask {
        private static final String LOG_TAG = "LogoutTask";

        public static final String SUCCESS_KEY = "success";
        public static final String COUNT_KEY = "count";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * The user whose following count is being retrieved.
         * (This can be any user, not just the currently logged-in user.)
         */
        private User targetUser;

        public GetFollowingCountTask(AuthToken authToken, User targetUser, Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.targetUser = targetUser;
        }

        @Override
        public void runTask() {
            try {

                sendSuccessMessage(20);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private void sendSuccessMessage(int count) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putInt(COUNT_KEY, count);
                }
            });
        }
    }

    public class IsFollowerTask extends BackgroundTask {
        private static final String LOG_TAG = "IsFollowerTask";

        public static final String SUCCESS_KEY = "success";
        public static final String IS_FOLLOWER_KEY = "is-follower";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * The alleged follower.
         */
        private User follower;
        /**
         * The alleged followee.
         */
        private User followee;


        public IsFollowerTask(AuthToken authToken, User follower, User followee, Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.follower = follower;
            this.followee = followee;
        }

        @Override
        public void runTask() {
            try {

                sendSuccessMessage(new Random().nextInt() > 0);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private void sendSuccessMessage(boolean isFollower) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putBoolean(IS_FOLLOWER_KEY, isFollower);
                }
            });
        }
    }

    public class FollowTask extends BackgroundTask {
        private static final String LOG_TAG = "FollowTask";

        public static final String SUCCESS_KEY = "success";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         * This user is the "follower" in the relationship.
         */
        private AuthToken authToken;
        /**
         * The user that is being followed.
         */
        private User followee;


        public FollowTask(AuthToken authToken, User followee, Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.followee = followee;
        }

        @Override
        public void runTask() {
            try {
                sendSuccessMessage();
            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }
    }



}
