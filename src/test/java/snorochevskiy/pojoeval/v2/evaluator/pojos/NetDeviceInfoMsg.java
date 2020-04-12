package snorochevskiy.pojoeval.v2.evaluator.pojos;

public class NetDeviceInfoMsg {
    private String fqdn;
    private String interfaceName;
    private String message;
    private int level;

    public NetDeviceInfoMsg(String fqdn, String interfaceName, String message, int level) {
        this.fqdn = fqdn;
        this.interfaceName = interfaceName;
        this.message = message;
        this.level = level;
    }

    public String getFqdn() {
        return fqdn;
    }
    public String getInterfaceName() {
        return interfaceName;
    }
    public String getMessage() {
        return message;
    }
    public int getLevel() {
        return level;
    }
}
