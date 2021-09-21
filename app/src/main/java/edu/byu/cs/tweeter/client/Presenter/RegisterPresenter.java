package edu.byu.cs.tweeter.client.Presenter;

import android.widget.Toast;

import edu.byu.cs.tweeter.client.cache.Cache;
import edu.byu.cs.tweeter.client.model.service.UserService;
import edu.byu.cs.tweeter.model.domain.AuthToken;
import edu.byu.cs.tweeter.model.domain.User;

public class RegisterPresenter implements UserService.RegisterObserver{

    public interface View {
        void navigateToRegisteredUser(User user);

        void displayErrorMessage(String message);
        void clearErrorMessage();
        void displayInfoMessage(String message);
        void clearInfoMessage();
    }

    private final View view;

    public RegisterPresenter(View view) {
        this.view = view;
    }

    public void register(String firstName, String lastName, String alias, String password, String image) {
        view.clearErrorMessage();
        view.clearInfoMessage();
        String message = validateRegistration(firstName, lastName, alias, password);
        //register succeeded
        if(message == null) {
            view.displayInfoMessage("Registering ...");
            new UserService().register(firstName, lastName, alias, password, image,this);
        }
        else {
            view.displayErrorMessage("Register failed: " + message);
        }
    }

    private String validateRegistration(String firstName, String lastName, String alias, String password) {
        if (firstName.length() == 0) {
            return "First Name cannot be empty.";
        }
        if (lastName.length() == 0) {
            return "Last Name cannot be empty.";
        }
        if (alias.length() == 0) {
            return "Alias cannot be empty.";
        }
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
    public void registerSucceeded(User user, AuthToken authToken) {
        view.navigateToRegisteredUser(user);
        view.clearInfoMessage();
        view.clearErrorMessage();
        view.displayInfoMessage("Hello " + user.getName());
    }

    @Override
    public void registerFailed(String message) {
        view.displayErrorMessage("Register failed " + message);
    }

    @Override
    public void registerThrewException(Exception ex) {
        view.displayErrorMessage("Register failed due to exception: " + ex.getMessage());
    }

}
