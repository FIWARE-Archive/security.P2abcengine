package ch.zhaw.ficore.p2abc.services;

import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Logger;

public class ExceptionDumper {
    
    private static String exceptionAsString(Throwable t) {
        String estring = "";
        estring += "[" + t.getClass().getName() + "]";
        String msg = t.getMessage();
        if(msg == null)
            msg = "n/a";
        estring += "(" + msg + ")";
        StackTraceElement[] trace = t.getStackTrace();
        for(StackTraceElement ste : trace) {
            String meth = ste.getMethodName();
            if(meth == null)
                meth = "n/a";
            String file = ste.getFileName();
            if(file == null)
                file = "n/a";
            int lno = ste.getLineNumber();
            estring += (meth+":"+file+":"+lno+";");
        }
        if(t.getCause() != null)
            estring += exceptionAsString(t.getCause());
        return estring;
    }
    
    public static Response dumpException(Throwable t, Logger l) {
        String asString = exceptionAsString(t);
        String hash = DigestUtils.sha1Hex(asString);
        l.warn("!!EXCEPTION!! " + hash + " " + asString);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Your request could not be completed. This is most likely due to you providing malformed " +
                		"or otherwise invalid input. If you think this is a bug in the server please contact <email> " +
                		"and include the following text: " + hash)
                .build();
    }
    
}