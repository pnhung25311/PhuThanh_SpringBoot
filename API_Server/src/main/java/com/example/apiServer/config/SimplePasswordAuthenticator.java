package com.example.apiServer.config;


import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

public class SimplePasswordAuthenticator implements PasswordAuthenticator {

    private final String username;
    private final String password;

    public SimplePasswordAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        return this.username.equals(username) && this.password.equals(password);
    }
}

