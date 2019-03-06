package rocks.inspectit.oce.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class DummyClassLoader extends ClassLoader {

    public DummyClassLoader(ClassLoader parent, Class<?>... classesToCopy) {
        super(parent);
        loadCopiesOfClasses(classesToCopy);
    }

    public DummyClassLoader(Class<?>... classesToCopy) {
        this(null, classesToCopy);
    }

    public void loadCopiesOfClasses(Class<?>... classesToCopy) {
        for (Class<?> clazz : classesToCopy) {
            byte[] code = readByteCode(clazz);
            defineClass(clazz.getName(), code, 0, code.length);
        }
    }

    public void defineNewClass(String name, byte[] code) {
        defineClass(name, code, 0, code.length);
    }

    public static byte[] readByteCode(Class<?> clazz) {
        try {
            String file = "/" + clazz.getName().replace('.', '/') + ".class";
            InputStream in = clazz.getResourceAsStream(file);
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            while (in.available() > 0) {
                byte[] buffer = new byte[1024];
                int read = in.read(buffer);
                data.write(buffer, 0, read);
            }
            return data.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
