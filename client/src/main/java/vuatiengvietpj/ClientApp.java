package vuatiengvietpj;

import vuatiengvietpj.controller.UserController;
import java.io.IOException;

// ...existing code...
public class ClientApp {

    public static void main(String[] args) throws IOException {
        String stt = "5";
        // SIGNUP
        UserController uc = new UserController("localhost", 2208);
        uc.signup("testNEW_" + stt + "@gmail.com", "PHAN VAN HOAN", "hoan");
        uc.disconnect();

        // // LOGIN
        // uc = new UserController("localhost", 2208);
        // uc.login("testNEW_" + stt + "@gmail.com", "hoan");
        // uc.disconnect();

        // // CHANGE PASSWORD
        // uc = new UserController("localhost", 2208);
        // uc.changePassword("testNEW_" + stt + "@gmail.com", "hoan", "da doi mk1");
        // uc.disconnect();

        uc = new UserController("localhost", 2208);
        System.out.println(uc.login("testNEW_" + stt + "@gmail.com", "hoan"));
        uc.disconnect();

        uc = new UserController("localhost", 2208);
        System.out.println(uc.login("testNEW_" + stt + "@gmail.com", "hoan"));
        uc.disconnect();

        uc = new UserController("localhost", 2208);
        System.out.println(uc.alive("13", "hoan@gmail.com"));
        uc.disconnect();
    }
}
// ...existing code...