package ch.heigvd.bdr.controllers;

import java.io.IOException;
import java.sql.SQLException;

import io.javalin.http.Context;

public interface ResourceControllerInterface {
    void all(Context ctx) throws ClassNotFoundException, IOException, SQLException;

    void show(Context ctx) throws ClassNotFoundException, IOException, SQLException;

    void create(Context ctx) throws ClassNotFoundException, IOException, SQLException;

    void update(Context ctx) throws ClassNotFoundException, IOException, SQLException;

    void delete(Context ctx) throws ClassNotFoundException, IOException, SQLException;
}
