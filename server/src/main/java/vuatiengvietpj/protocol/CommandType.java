package vuatiengvietpj.protocol;

public enum CommandType {
    LOGIN(false, "LOGIN"),
    LOGOUT(true, "LOGOUT"),
    SIGNUP(false, "SIGNUP"),
    FGPASS(false, "FGPASS");

    private final boolean requiresAuth;
    private final String token;

    // check lệnh này cần xác thực session hay là ko
    private final boolean requireAuth() {
        return requiresAuth;

    }

    CommandType(boolean requireAuth, String token) {
        this.requiresAuth = requireAuth;
        this.token = token;
    }

    public boolean isRequiresAuth() {
        return requiresAuth;
    }

    public String getToken() {
        return token;
    }

    // hàm chuyển đổi chuỗi string thành chuỗi lệnh
    public static CommandType toCommandType(String chuoiLenh) {
        if (chuoiLenh == null) {
            return null;
        }
        String s = chuoiLenh.trim().toUpperCase();
        switch (s) {
            case "LOGIN":
                return LOGIN;

            case "LOGOUT":
                return LOGOUT;
            case "SIGNUP":
                return SIGNUP;
            case "FGPASS":
                return FGPASS;
            default:
                return null;
        }
    }

    // chuyển cmt thành string
    public static String toString(CommandType cmt) {
        if (cmt == null)
            return null;
        switch (cmt) {
            case LOGIN:
                return "LOGIN";

            case LOGOUT:
                return "LOGOUT";
            case SIGNUP:
                return "SIGNUP";
            case FGPASS:
                return "FGPASS";
            default:
                return null;
        }
    }
}
