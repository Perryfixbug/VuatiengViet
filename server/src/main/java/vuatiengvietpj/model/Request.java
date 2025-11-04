package vuatiengvietpj.model;

import java.io.Serializable;

public class Request implements Serializable {
    private String module;
    private String maLenh;
    private String data;
    private String ip;
    private static final long serialVersionUID = 1L;

    public Request(String module, String maLenh, String data) {
        this.module = module;
        this.maLenh = maLenh;
        this.data = data;

    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getModule() {
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

    @Override
    public String toString() {
        return "Request [module=" + module + ", maLenh=" + maLenh + ", data=" + data + ", ip=" + ip + "]";
    }

}