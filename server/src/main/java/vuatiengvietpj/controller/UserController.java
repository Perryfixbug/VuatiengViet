package vuatiengvietpj.controller;

import java.util.List;

import vuatiengvietpj.DAO.UserDAO;
import vuatiengvietpj.model.User;
import org.mindrot.jbcrypt.BCrypt;

public class UserController {
    private static final UserDAO userDAO = new UserDAO();
    // public boolean login(User user) {

    // }

    // public void logout() {

    // }

    // public boolean forgetPassword(String email) {

    // }

    // // public User viewInfor() {

    // // }
    public String sigup(User user) {
        int code = userDAO.checkUser(user);
        if (code == 0) {
            userDAO.create(user);
            return "Đăng ký tài khoản thành công, hãy đăng nhập để chơi gem";

        } else {
            return "Email đã có người sử dụng";
        }
    }

    public String changePassword(User user) {
        userDAO.save(user);
        return "Thay đổi mật khẩu thành công";
    }

    public List<User> getListUser() {
        return userDAO.getListUser();
    }

    public String login(User user) {
        User userCheck = userDAO.getUserbyEmail(user.getEmail());
        if (userCheck == null || user.getPassword().equals(userCheck.getPassword())) {
            return "Tài khoản hoặc mật khẩu không đúng";
        }
        userCheck.setPassword(null);
        return "Đăng nhập thành công vào tài khoản " + userCheck.toString();
    }

    public static void main(String[] args) {

        UserController userController = new UserController();
        User user = new User(0, "", "TMH@gmail.com", BCrypt.hashpw("hieu123", BCrypt.gensalt(12)));
        System.out.println(userController.login(user));
    }
}
