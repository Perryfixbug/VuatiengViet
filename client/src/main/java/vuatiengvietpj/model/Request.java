package vuatiengvietpj.model;

import java.io.Serializable;

public class Request implements Serializable {
    private String modunle;
    private String maLenh;
    private String data;
    private String ip;
    private static final long serialVersionUID = 1L;

    public Request(String module, String maLenh, String data) {
        this.modunle = module;
        this.maLenh = maLenh;
        this.data = data;

    }

    public String getModunle() {
        return modunle;
    }

    public void setModunl(String modunl) {
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

    @Override
    public String toString() {
        return "Request [modunle=" + modunle + ", maLenh=" + maLenh + ", data=" + data + "]";
    }

}