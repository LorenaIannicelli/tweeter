package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

import edu.byu.cs.tweeter.client.model.service.backgroundTask.BackgroundTaskUtils;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.Status;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;
import edu.byu.cs.tweeter.util.Pair;

public class StoryService {

    private final StoryObserver observer;

    public interface StoryObserver
    {
        void handleSuccess(List<Status> statuses, boolean hasMorePages, Status lastStatus);
        void handleFailure(String message);
        void handleException(Exception ex);
    }

    public StoryService(StoryObserver observer)
    {
        if(observer == null)
            throw new NullPointerException();
        this.observer = observer;
    }

    public void getStory(AuthToken authToken, User user, int limit, Status lastStatus) {
        GetStoryTask getStoryTask = getGetStoryTask(authToken, user, limit, lastStatus);
        BackgroundTaskUtils.runTask(getStoryTask);
    }

    public GetStoryTask getGetStoryTask(AuthToken authToken, User user, int limit, Status lastStatus)
    {
        return new GetStoryTask(authToken, user, limit, lastStatus, new GetStoryHandler(this, Looper.getMainLooper(), observer));
    }



    /**
     * Message handler (i.e., observer) for GetStoryTask.
     */
    private class GetStoryHandler extends Handler {
        private final StoryObserver observer;

        public GetStoryHandler(StoryService storyService, Looper looper, StoryObserver observer)
        {
            super(looper);
            this.observer = observer;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle bundle = msg.getData();
            boolean success = msg.getData().getBoolean(GetStoryTask.SUCCESS_KEY);
            if (success) {
                List<Status> statuses = (List<Status>) msg.getData().getSerializable(GetStoryTask.STATUSES_KEY);
                boolean hasMorePages = msg.getData().getBoolean(GetStoryTask.MORE_PAGES_KEY);
                Status lastStatus = (statuses.size() > 0) ? statuses.get(statuses.size() - 1) : null;
                observer.handleSuccess(statuses, hasMorePages, lastStatus);
            } else if (msg.getData().containsKey(GetStoryTask.MESSAGE_KEY)) {
                String message = "Failed to get story: " + msg.getData().getString(GetStoryTask.MESSAGE_KEY);
                observer.handleFailure(message);
            } else if (msg.getData().containsKey(GetStoryTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) msg.getData().getSerializable(GetStoryTask.EXCEPTION_KEY);
                observer.handleException(ex);
            }
        }
    }


    public class GetStoryTask extends BackgroundTask {
        private static final String LOG_TAG = "GetStoryTask";

        public static final String SUCCESS_KEY = "success";
        public static final String STATUSES_KEY = "statuses";
        public static final String MORE_PAGES_KEY = "more-pages";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * The user whose story is being retrieved.
         * (This can be any user, not just the currently logged-in user.)
         */
        private User targetUser;
        /**
         * Maximum number of statuses to return (i.e., page size).
         */
        private int limit;
        /**
         * The last status returned in the previous page of results (can be null).
         * This allows the new page to begin where the previous page ended.
         */
        private Status lastStatus;


        public GetStoryTask(AuthToken authToken, User targetUser, int limit, Status lastStatus,
                            Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.targetUser = targetUser;
            this.limit = limit;
            this.lastStatus = lastStatus;
        }

        @Override
        public void runTask() {
            try {
                Pair<List<Status>, Boolean> pageOfStatus = getStory();

                List<Status> statuses = pageOfStatus.getFirst();
                boolean hasMorePages = pageOfStatus.getSecond();

                for (Status s : statuses) {
                    BackgroundTaskUtils.loadImage(s.getUser());
                }

                sendSuccessMessage(statuses, hasMorePages);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private FakeData getFakeData() {
            return new FakeData();
        }

        private Pair<List<Status>, Boolean> getStory() {
            Pair<List<Status>, Boolean> pageOfStatus = getFakeData().getPageOfStatus(lastStatus, limit);
            return pageOfStatus;
        }

        private void sendSuccessMessage(List<Status> statuses, boolean hasMorePages) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putSerializable(STATUSES_KEY, (Serializable) statuses);
                    msgBundle.putBoolean(MORE_PAGES_KEY, hasMorePages);
                }
            });
        }

    }

}
