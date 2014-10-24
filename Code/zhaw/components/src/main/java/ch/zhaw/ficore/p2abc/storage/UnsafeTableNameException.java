package ch.zhaw.ficore.p2abc.storage;

public class UnsafeTableNameException extends Exception {
  private static final long serialVersionUID = 3201829865528305290L;

  public UnsafeTableNameException(String tableName) {
    super("Unsafe table name: \"" + tableName
        + "\" (contains non-ASCII characters and/or non-letters)");
  }
}
