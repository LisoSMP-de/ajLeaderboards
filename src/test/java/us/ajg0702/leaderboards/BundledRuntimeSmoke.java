package us.ajg0702.leaderboards;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

/** Checks the shipped artifact, with no dependency cache, agent or server classpath. */
public final class BundledRuntimeSmoke {
    public static void main(String[] args) throws Exception {
        File jar = new File(args[0]);
        try (JarFile archive = new JarFile(jar)) {
            if (archive.stream().anyMatch(e -> e.getName().contains("slimjar/"))) {
                throw new AssertionError("Runtime injector still bundled");
            }
        }
        // SLF4J is supplied by the server, all other tested dependencies must come from our JAR.
        URL loggingApi = org.slf4j.Logger.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL(), loggingApi},
                ClassLoader.getSystemClassLoader().getParent())) {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                Driver driver = (Driver) loader.loadClass("us.ajg0702.leaderboards.libs.h2.Driver")
                        .getDeclaredConstructor().newInstance();
                try (Connection connection = driver.connect("jdbc:h2:mem:bundle_smoke", new Properties());
                     Statement statement = connection.createStatement()) {
                    statement.execute("CREATE TABLE scores (score INT)");
                    statement.execute("INSERT INTO scores VALUES (42)");
                    try (ResultSet result = statement.executeQuery("SELECT score FROM scores")) {
                        if (!result.next() || result.getInt(1) != 42) throw new AssertionError("H2 query failed");
                    }
                }
                boolean registered = false;
                for (Driver service : ServiceLoader.load(Driver.class, loader)) {
                    if (service.getClass().equals(driver.getClass())) registered = true;
                }
                if (!registered) throw new AssertionError("Relocated JDBC service descriptor missing");

                Class<?> configType = loader.loadClass("us.ajg0702.leaderboards.libs.hikari.HikariConfig");
                Object config = configType.getDeclaredConstructor().newInstance();
                configType.getMethod("setDriverClassName", String.class).invoke(config, driver.getClass().getName());
                configType.getMethod("setJdbcUrl", String.class).invoke(config, "jdbc:h2:mem:pool_smoke");
                configType.getMethod("setMaximumPoolSize", int.class).invoke(config, 1);
                Class<?> poolType = loader.loadClass("us.ajg0702.leaderboards.libs.hikari.HikariDataSource");
                Object pool = poolType.getConstructor(configType).newInstance(config);
                try (Connection connection = (Connection) poolType.getMethod("getConnection").invoke(pool)) {
                    if (!connection.isValid(2)) throw new AssertionError("Hikari connection invalid");
                } finally { poolType.getMethod("close").invoke(pool); }

                Class<?> client = loader.loadClass("us.ajg0702.leaderboards.libs.okhttp3.OkHttpClient$Builder");
                client.getMethod("build").invoke(client.getDeclaredConstructor().newInstance());
                Class<?> httpUrl = loader.loadClass("us.ajg0702.leaderboards.libs.okhttp3.HttpUrl");
                Object url = httpUrl.getMethod("get", String.class).invoke(null, "https://example.co.uk/path");
                if (!"example.co.uk".equals(httpUrl.getMethod("topPrivateDomain").invoke(url))) {
                    throw new AssertionError("OkHttp public suffix resource missing");
                }
                System.out.println("BUNDLED_RUNTIME_OK: H2 SQL, JDBC services, Hikari pool, OkHttp/Kotlin/Okio and public suffix resource");
            } finally { thread.setContextClassLoader(previous); }
        }
    }
}
