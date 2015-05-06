package ch.zhaw.ficore.p2abc.services;

import javax.ws.rs.core.Response;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;


public class ExceptionDumper {

    private static String exceptionAsString(Throwable t) {
        StringBuffer estring = new StringBuffer();
        estring.append("[").append(t.getClass().getName()).append("]");
        String msg = t.getMessage();
        if (msg == null)
            msg = "n/a";
        estring.append("(").append(msg).append(")");
        StackTraceElement[] trace = t.getStackTrace();
        for (StackTraceElement ste : trace) {
            String meth = ste.getMethodName();
            if (meth == null)
                meth = "n/a";
            String file = ste.getFileName();
            if (file == null)
                file = "n/a";
            int lno = ste.getLineNumber();
            estring.append(meth).append(":").append(file).append(":")
                    .append(lno).append(";");
        }
        if (t.getCause() != null)
            estring.append(exceptionAsString(t.getCause()));
        return estring.toString();
    }
    
    private static String htmlEntities(String in) {
    	if(in == null)
    		return "";
    	return in.replace("&","&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static Response dumpException(Throwable t, Logger l) {
        String asString = exceptionAsString(t);
        String hash = DigestUtils.sha1Hex(asString);
        l.warn("!!EXCEPTION!! " + hash + " " + asString);
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Your request could not be completed. This is most likely due to you providing malformed "
                        + "or otherwise invalid input. If you think this is a bug in the server please contact your system administrator, issuer and/or verifier "
                        + "and include the following text: " + hash +". Details: " + htmlEntities(t.getMessage())).build();
    }

    public static String dumpExceptionStr(Throwable t, Logger l) {
        String asString = exceptionAsString(t);
        String hash = DigestUtils.sha1Hex(asString);
        l.warn("!!EXCEPTION!! " + hash + " " + asString);
        return "Your request could not be completed. This is most likely due to you providing malformed "
                + "or otherwise invalid input. If you think this is a bug please contact your system administrator, issuer and/or verifier "
                + "and include the following text: " + hash + ". Details: " + htmlEntities(t.getMessage());
    }

}