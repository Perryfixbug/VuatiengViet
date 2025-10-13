package vuatiengvietpj.model;

import java.io.Serializable;

public class Response implements Serializable {
    private String modunle;
    private String maLenh;
    private String data;
    private boolean success;
    private static final long serialVersionUID = 1L;

    public Response(String modul, String maLenh, String data, boolean success) {
        this.modunle = modul;
        this.maLenh = maLenh;
        this.data = data;
        this.success = success;
    }

    public String getModunle() {
        return modunle;
    }

    public void setModunle(String modunl) {
        this.modunle = modunl;
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
        return "Response [modunle=" + modunle + ", maLenh=" + maLenh + ", data=" + data + ", success=" + success + "]";
    }

}
