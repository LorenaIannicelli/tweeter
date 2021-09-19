package edu.byu.cs.tweeter.client.model.service;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.byu.cs.tweeter.client.model.service.backgroundTask.BackgroundTaskUtils;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.GetUserTask;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.LoginTask;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;

public class UserService {


    public interface GetUserObserver {
        void getUserHandleSuccess(User user, AuthToken authToken);
        void getUserHandleFailure(String message);
        void getUserHandleException(Exception exception);
    }

    //create an instance
    public UserService()
    {

    }


    public void getUser (AuthToken authToken, String alias, GetUserObserver observer)
    {
        UserService.GetUserTask userTask = getGetUserTask(authToken, alias, observer);
        BackgroundTaskUtils.runTask(userTask);
    }



    public UserService.GetUserTask getGetUserTask(AuthToken authToken, String alias, GetUserObserver observer)
    {
        return new UserService.GetUserTask(authToken, alias, new UserService.MessageHandler(Looper.getMainLooper(), observer));
    }



    public class GetUserTask extends BackgroundTask {
        private static final String LOG_TAG = "GetUserTask";

        public static final String SUCCESS_KEY = "success";
        public static final String USER_KEY = "user";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;
        /**
         * Alias (or handle) for user whose profile is being retrieved.
         */
        private String alias;

        public GetUserTask(AuthToken authToken, String alias, Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
            this.alias = alias;
        }

        @Override
        public void runTask() {
            try {
                User user = getUser();

                sendSuccessMessage(user);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private FakeData getFakeData() {
            return new FakeData();
        }

        private User getUser() {
            User user = getFakeData().findUserByAlias(alias);
            return user;
        }

        private void sendSuccessMessage(User user) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putSerializable(USER_KEY, (Serializable) user);

                }
            });
        }


    }


    private static class MessageHandler extends Handler {

        private final GetUserObserver observer;

        MessageHandler(Looper looper, GetUserObserver observer)
        {
            super(looper);
            this.observer = observer;
        }

        @Override
        public void handleMessage(Message message) {
            Bundle bundle = message.getData();
            boolean success = bundle.getBoolean(LoginTask.SUCCESS_KEY);
            if (success) {
                User user = (User) bundle.getSerializable(LoginTask.USER_KEY);
                AuthToken authToken = (AuthToken) bundle.getSerializable(LoginTask.AUTH_TOKEN_KEY);
                observer.getUserHandleSuccess(user, authToken);
            } else if (bundle.containsKey(LoginTask.MESSAGE_KEY)) {
                String errorMessage = bundle.getString(LoginTask.MESSAGE_KEY);
                observer.getUserHandleFailure(errorMessage);
            } else if (bundle.containsKey(LoginTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) bundle.getSerializable(LoginTask.EXCEPTION_KEY);
                observer.getUserHandleException(ex);
            }
        }
    }




}
