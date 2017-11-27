package se.simonsoft.publish.worker.startup;

public class Environment {
    public String getVariable(String key) {
        return System.getenv(key);
    }
}
