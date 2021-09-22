package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import edu.byu.cs.tweeter.client.model.service.backgroundTask.BackgroundTaskUtils;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.Status;

public class StatusService {

    public interface StatusObserver
    {
        void handleSuccess();
        void handleFailure(String message);
        void handleException(Exception ex);
    }

    public StatusService()
    {

    }

    public void postStatus(AuthToken authToken, Status status, StatusObserver observer)
    {
        PostStatusTask postStatusTask = getPostStatusTask(authToken, status, observer);
        BackgroundTaskUtils.runTask(postStatusTask);
    }

    public PostStatusTask getPostStatusTask(AuthToken authToken, Status status, StatusObserver observer )
    {
        return new PostStatusTask(authToken, status, new PostStatusHandler(this, Looper.getMainLooper(), observer));
    }

    private class PostStatusHandler extends Handler {
        private final StatusObserver observer;

        public PostStatusHandler(StatusService statusService, Looper looper, StatusObserver observer)
        {
            super(looper);
            this.observer = observer;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle bundle = msg.getData();
            boolean success = bundle.getBoolean(edu.byu.cs.tweeter.client.model.service.backgroundTask.PostStatusTask.SUCCESS_KEY);
            if (success) {
                observer.handleSuccess();
            } else if (msg.getData().containsKey(PostStatusTask.MESSAGE_KEY)) {
                String message = bundle.getString(PostStatusTask.MESSAGE_KEY);
                observer.handleFailure(message);
            } else if (msg.getData().containsKey(PostStatusTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(PostStatusTask.EXCEPTION_KEY);
                observer.handleException(ex);
            }
        }
    }

    public class PostStatusTask extends BackgroundTask {
        private static final String LOG_TAG = "PostStatusTask";

        public static final String SUCCESS_KEY = "success";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * The new status being sent. Contains all properties of the status,
         * including the identity of the user sending the status.
         */
        private Status status;


        public PostStatusTask(AuthToken authToken, Status status, Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.status = status;
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
        //don't need to implement the successful message because it is default implementation
    }
}
