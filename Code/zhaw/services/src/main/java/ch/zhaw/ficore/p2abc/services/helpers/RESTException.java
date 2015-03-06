package ch.zhaw.ficore.p2abc.services.helpers;

public class RESTException extends RuntimeException {
    private int statusCode = 0;
    
    public RESTException(String msg, int statusCode) {
        super(msg);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
