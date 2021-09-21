package edu.byu.cs.tweeter.client.Presenter;

import edu.byu.cs.tweeter.client.model.service.UserService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;

public class LoginPresenter implements UserService.LoginObserver{

    private final View view;

    public interface View {
        void navigateToLoggedInUser(User user);

        void displayErrorMessage(String message);
        void clearErrorMessage();
        void displayInfoMessage(String message);
        void clearInfoMessage();
    }

    public LoginPresenter(View view) {
        this.view = view;
    }


    public void login(String alias, String password)
    {
        //clear out old messages
        view.clearErrorMessage();
        view.clearInfoMessage();
        String message = validateLogin(alias, password);
        //login succeeded
        if(message == null) {
            view.displayInfoMessage("Logging in...");
            new UserService().login(alias, password, this);
        }
        else {
            view.displayErrorMessage("Login failed: " + message);
        }

    }

    private String validateLogin(String alias, String password) {
        if (alias.charAt(0) != '@') {
            return "Alias must begin with @.";
        }
        if (alias.length() < 2) {
            return "Alias must contain 1 or more characters after the @.";
        }
        if (password.length() == 0) {
           return "Password cannot be empty.";
        }
        return null;
    }

    @Override
    public void loginSucceeded(User user, AuthToken authToken) {
        view.navigateToLoggedInUser(user);
        view.clearErrorMessage();
        view.displayInfoMessage("Hello " + user.getName());
    }

    @Override
    public void loginFailed(String message) {
        view.displayErrorMessage("Login failed " + message);
    }

    @Override
    public void loginThrewException(Exception ex) {
        view.displayErrorMessage("Login failed: " + ex.getMessage());
    }

}
