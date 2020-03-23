package app.entities;

public class Client {
    public static final int NO_REFERRER_VALUE = -1;
    private int id;
    private String clientName;
    private String email;
    private Server server;
    private String login;
    private String conn;
    private long regTime;
    private long subscrTo;
    private int swapServerAttempt;
    private long noswapTo;
    private long telNum;
    private String countryFrom;
    private int refDays;
    private int referredFrom;
    private String lang;

    public Client(int id, String clientName, String email, long tel_num, Server server, String conn, String login, long register_date, long subscr_to, int swap_server_attempt, long noswap_to, String countryFrom, int refDays, int referredFrom, String lang) {

        this.id = id;
        this.clientName = clientName;
        this.email = email;
        this.server = server;
        this.login = login;
        this.conn = conn;
        this.regTime = register_date;
        this.subscrTo = subscr_to;
        this.swapServerAttempt = swap_server_attempt;
        this.noswapTo = noswap_to;
        this.telNum = tel_num;
        this.countryFrom = countryFrom;
        this.refDays = refDays;
        this.referredFrom = referredFrom;
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public int getRefDays() {
        return refDays;
    }

    public String getCountryFrom() {
        return countryFrom;
    }

    public int getReferredFrom() {
        return referredFrom;
    }

    public String getClientName() {
        return clientName;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public long getTelNum() {
        return telNum;
    }

    public Server getServer() {
        return server;
    }

    public String getLogin() {
        return login;
    }

    public String getConn() {
        return conn;
    }

    public long getRegTime() {
        return regTime;
    }

    public long getSubscrTo() {
        return subscrTo;
    }

    public int getSwapServerAttempt() {
        return swapServerAttempt;
    }

    public long getNoswapTo() {
        return noswapTo;
    }
}
