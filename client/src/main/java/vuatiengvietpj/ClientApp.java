package vuatiengvietpj;

import vuatiengvietpj.controller.UserController;
import vuatiengvietpj.controller.RoomController;
import vuatiengvietpj.model.Response;
import java.io.IOException;
import java.util.List;

public class ClientApp {

    public static void main(String[] args) throws IOException {
        // Uncomment the function you want to test
        // testUserController();
        testRoomController();
    }

    public static void testUserController() throws IOException {
        String stt = "5";
        UserController uc = new UserController("localhost", 2208);
        uc.signup("testNEW_" + stt + "@gmail.com", "PHAN VAN HOAN", "hoan");
        uc.disconnect();

        uc = new UserController("localhost", 2208);
        System.out.println(uc.login("testNEW_" + stt + "@gmail.com", "hoan"));
        uc.disconnect();

        uc = new UserController("localhost", 2208);
        System.out.println(uc.alive("13", "hoan@gmail.com"));
        uc.disconnect();
    }

    public static void testRoomController() throws IOException {
        RoomController rc = new RoomController("localhost", 2208);

        // Test createRoom
        Response createResponse = rc.createRoom(1L);
        System.out.println("Create Room Response: " + createResponse);

        // Test joinRoom
        Response joinResponse = rc.joinRoom(1L, 2L);
        System.out.println("Join Room Response: " + joinResponse);

        // Test editRoom
        Response editResponse = rc.editRoom(1L, 6);
        System.out.println("Edit Room Response: " + editResponse);

        // Test outRoom
        Response outResponse = rc.outRoom(1L, 2L);
        System.out.println("Out Room Response: " + outResponse);

        // Test getAllRooms
        Response getAllResponse = rc.getAllRooms();
        System.out.println("Get All Rooms Response: " + getAllResponse);

        // Test refreshRoom
        Response refreshResponse = rc.refreshRoom(1L, true);
        System.out.println("Refresh Room Response: " + refreshResponse);

        // Test kickPlayer
        Response kickResponse = rc.kickPlayer(1L, 1L, 2L);
        System.out.println("Kick Player Response: " + kickResponse);

        // Test checkAlive
        rc.checkAlive(1L, List.of(2L, 3L));
    }
}