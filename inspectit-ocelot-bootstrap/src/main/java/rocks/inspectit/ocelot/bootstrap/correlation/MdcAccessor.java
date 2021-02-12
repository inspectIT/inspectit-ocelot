package rocks.inspectit.ocelot.bootstrap.correlation;

public interface MdcAccessor {

    String get(String key);

    void put(String key, String value);

    void remove(String key);

}
