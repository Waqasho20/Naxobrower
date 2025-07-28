package com.naxobrowser;

public class Cookie {
    public String name;
    public String value;
    public String domain;
    public String path;
    public long expires;
    public boolean secure;
    public boolean httpOnly;
    public boolean selected; // Added for UI selection

    public Cookie(String name, String value, String domain, String path, long expires, boolean secure, boolean httpOnly) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.expires = expires;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.selected = false; // Default to not selected
    }

    @Override
    public String toString() {
        return "Name: " + name + "\nValue: " + value + "\nDomain: " + domain + "\nPath: " + path + "\nExpires: " + expires + "\nSecure: " + secure + "\nHttpOnly: " + httpOnly;
    }
}

