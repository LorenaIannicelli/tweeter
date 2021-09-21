package edu.byu.cs.tweeter.client.model.service;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.Serializable;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.backgroundTask.BackgroundTaskUtils;
import edu.byu.cs.tweeter.client.view.main.MainActivity;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;
import edu.byu.cs.tweeter.util.FakeData;
import edu.byu.cs.tweeter.util.Pair;

public class UserService {

    public interface LoginObserver {
        void loginSucceeded(User user, AuthToken authToken);
        void loginFailed(String message);
        void loginThrewException(Exception ex);
    }

    public interface GetUserObserver {
        void getUserHandleSuccess(User user, AuthToken authToken);
        void getUserHandleFailure(String message);
        void getUserHandleException(Exception exception);
    }

    public interface RegisterObserver {
        void registerSucceeded(User user, AuthToken authToken);
        void registerFailed(String message);
        void registerThrewException(Exception ex);
    }

    //create an instance
    public UserService()
    { }


    /*Create login request */
    public void login(String alias, String password, LoginObserver observer)
    {
        UserService.LoginTask loginTask = getLoginTask(alias, password, observer);
        BackgroundTaskUtils.runTask(loginTask);
    }

    public UserService.LoginTask getLoginTask(String alias, String password, LoginObserver observer)
    {
        return new UserService.LoginTask(alias, password, new LoginHandler(Looper.getMainLooper(), observer));
    }

    /*Create login request */
    public void register(String firstName, String lastName, String alias, String password, String image, RegisterObserver observer)
    {
        UserService.RegisterTask registerTask = getRegisterTask(firstName, lastName, alias, password, image, observer);
        BackgroundTaskUtils.runTask(registerTask);
    }

    public UserService.RegisterTask getRegisterTask(String firstName, String lastName, String alias, String password, String image, RegisterObserver observer)
    {
        return new UserService.RegisterTask(firstName, lastName, alias, password, image, new RegisterHandler(Looper.getMainLooper(), observer));
    }



    public void getUser (AuthToken authToken, String alias, GetUserObserver observer)
    {
        UserService.GetUserTask userTask = getGetUserTask(authToken, alias, observer);
        BackgroundTaskUtils.runTask(userTask);
    }

    public UserService.GetUserTask getGetUserTask(AuthToken authToken, String alias, GetUserObserver observer)
    {
        return new UserService.GetUserTask(authToken, alias, new GetUserHandler(Looper.getMainLooper(), observer));
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

    public class LoginTask extends BackgroundTask {

        private static final String LOG_TAG = "LoginTask";

        public static final String SUCCESS_KEY = "success";
        public static final String USER_KEY = "user";
        public static final String AUTH_TOKEN_KEY = "auth-token";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * The user's username (or "alias" or "handle"). E.g., "@susan".
         */
        private String username;
        /**
         * The user's password.
         */
        private String password;


        public LoginTask(String username, String password, Handler messageHandler) {
            super(messageHandler);
            this.username = username;
            this.password = password;
        }

        @Override
        public void runTask() {
            try {
                Pair<User, AuthToken> loginResult = doLogin();

                User loggedInUser = loginResult.getFirst();
                AuthToken authToken = loginResult.getSecond();

                BackgroundTaskUtils.loadImage(loggedInUser);

                sendSuccessMessage(loggedInUser, authToken);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private FakeData getFakeData() {
            return new FakeData();
        }

        private Pair<User, AuthToken> doLogin() {
            User loggedInUser = getFakeData().getFirstUser();
            AuthToken authToken = getFakeData().getAuthToken();
            return new Pair<>(loggedInUser, authToken);
        }

        private void sendSuccessMessage(User loggedInUser, AuthToken authToken) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putSerializable(USER_KEY, loggedInUser);
                    msgBundle.putSerializable(AUTH_TOKEN_KEY, authToken);
                }
            });
        }
    }

    public class LogoutTask extends BackgroundTask {
        private static final String LOG_TAG = "LogoutTask";

        public static final String SUCCESS_KEY = "success";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * Auth token for logged-in user.
         */
        private AuthToken authToken;


        public LogoutTask(AuthToken authToken, Handler messageHandler) {
            super(messageHandler);
            this.authToken = authToken;
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

//        //do we even need this?
//        protected void sendSuccessMessage() {
//            sendSuccessMessage(new BundleLoader() {
//                @Override
//                public void load(Bundle msgBundle) {
//                    msgBundle.putBoolean(SUCCESS_KEY, true);
//                }
//            });
//        }
    }

    /**
     * Background task that creates a new user account and logs in the new user (i.e., starts a session).
     */
    public class RegisterTask extends BackgroundTask {
        private static final String LOG_TAG = "RegisterTask";

        public static final String SUCCESS_KEY = "success";
        public static final String USER_KEY = "user";
        public static final String AUTH_TOKEN_KEY = "auth-token";
        public static final String MESSAGE_KEY = "message";
        public static final String EXCEPTION_KEY = "exception";

        /**
         * The user's first name.
         */
        private String firstName;
        /**
         * The user's last name.
         */
        private String lastName;
        /**
         * The user's username (or "alias" or "handle"). E.g., "@susan".
         */
        private String username;
        /**
         * The user's password.
         */
        private String password;
        /**
         * The base-64 encoded bytes of the user's profile image.
         */
        private String image;


        public RegisterTask(String firstName, String lastName, String username, String password,
                            String image, Handler messageHandler) {
            super(messageHandler);
            this.firstName = firstName;
            this.lastName = lastName;
            this.username = username;
            this.password = password;
            this.image = image;
        }

        @Override
        public void runTask() {
            try {
                Pair<User, AuthToken> registerResult = doRegister();

                User registeredUser = registerResult.getFirst();
                AuthToken authToken = registerResult.getSecond();

                BackgroundTaskUtils.loadImage(registeredUser);

                sendSuccessMessage(registeredUser, authToken);

            } catch (Exception ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
                sendExceptionMessage(ex);
            }
        }

        private FakeData getFakeData() {
            return new FakeData();
        }

        private Pair<User, AuthToken> doRegister() {
            User registeredUser = getFakeData().getFirstUser();
            AuthToken authToken = getFakeData().getAuthToken();
            return new Pair<>(registeredUser, authToken);
        }

        private void sendSuccessMessage(User registeredUser, AuthToken authToken) {
            sendSuccessMessage(new BundleLoader() {
                @Override
                public void load(Bundle msgBundle) {
                    msgBundle.putSerializable(USER_KEY, registeredUser);
                    msgBundle.putSerializable(AUTH_TOKEN_KEY, authToken);
                }
            });
        }
    }

    private static class GetUserHandler extends Handler {

        private final GetUserObserver observer;

        GetUserHandler(Looper looper, GetUserObserver observer)
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

    /**
     * Message handler (i.e., observer) for LoginTask
     */
    private class LoginHandler extends Handler {
        private final LoginObserver observer;

        public LoginHandler(Looper looper, LoginObserver observer){
            super(looper);
            this.observer = observer;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle bundle = msg.getData();
            boolean success = bundle.getBoolean(LoginTask.SUCCESS_KEY);
            if (success) {
                User loggedInUser = (User) bundle.getSerializable(LoginTask.USER_KEY);
                AuthToken authToken = (AuthToken) bundle.getSerializable(LoginTask.AUTH_TOKEN_KEY);
                //cache the information
                Cache.getInstance().setCurrUser(loggedInUser);
                Cache.getInstance().setCurrUserAuthToken(authToken);
                observer.loginSucceeded(loggedInUser, authToken);
            } else if (bundle.containsKey(LoginTask.MESSAGE_KEY)) {
                String message = msg.getData().getString(LoginTask.MESSAGE_KEY);
                observer.loginFailed(message);
            } else if (bundle.containsKey(LoginTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) msg.getData().getSerializable(LoginTask.EXCEPTION_KEY);
                observer.loginThrewException(ex);
            }
        }
    }

    private class RegisterHandler extends Handler {
        private final RegisterObserver observer;

        public RegisterHandler(Looper looper, RegisterObserver observer) {
            super(looper);
            this.observer = observer;
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            Bundle bundle = msg.getData();
            boolean success = bundle.getBoolean(RegisterTask.SUCCESS_KEY);
            if (success) {
                User registeredUser = (User) bundle.getSerializable(RegisterTask.USER_KEY);
                AuthToken authToken = (AuthToken) bundle.getSerializable(RegisterTask.AUTH_TOKEN_KEY);
                Cache.getInstance().setCurrUser(registeredUser);
                Cache.getInstance().setCurrUserAuthToken(authToken);
                observer.registerSucceeded(registeredUser, authToken);
            } else if (msg.getData().containsKey(edu.byu.cs.tweeter.client.model.service.backgroundTask.RegisterTask.MESSAGE_KEY)) {
                String message = msg.getData().getString(edu.byu.cs.tweeter.client.model.service.backgroundTask.RegisterTask.MESSAGE_KEY);
                observer.registerFailed(message);
            } else if (msg.getData().containsKey(edu.byu.cs.tweeter.client.model.service.backgroundTask.RegisterTask.EXCEPTION_KEY)) {
                Exception ex = (Exception) msg.getData().getSerializable(edu.byu.cs.tweeter.client.model.service.backgroundTask.RegisterTask.EXCEPTION_KEY);
                observer.registerThrewException(ex);
            }
        }
    }




}
