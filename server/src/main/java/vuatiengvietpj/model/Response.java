package vuatiengvietpj.model;

import java.io.Serializable;

public class Response implements Serializable {
    private String module;
    private String maLenh;
    private String data;
    private boolean success;
    private static final long serialVersionUID = 1L;

    public Response(String module, String maLenh, String data, boolean success) {
        this.module = module;
        this.maLenh = maLenh;
        this.data = data;
        this.success = success;
    }

    public String getmodule() {
        return module;
    }

    public void setmodule(String module) {
        this.module = module;
    }

    public String getMaLenh() {
        return maLenh;
    }

    public void setMaLenh(String maLenh) {
        this.maLenh = maLenh;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String toString() {
        return "Response [module=" + module + ", maLenh=" + maLenh + ", data=" + data + ", success=" + success + "]";
    }

}